package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.mall.domain.Inbound;
import com.zhenglongji.mall.mall.domain.Product;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.domain.StockFlow;
import com.zhenglongji.mall.mall.domain.Stocktake;
import com.zhenglongji.mall.mall.domain.StocktakeItem;
import com.zhenglongji.mall.mall.repository.InboundRepository;
import com.zhenglongji.mall.mall.repository.ProductRepository;
import com.zhenglongji.mall.mall.repository.ProductSkuRepository;
import com.zhenglongji.mall.mall.repository.StockFlowRepository;
import com.zhenglongji.mall.mall.repository.StocktakeItemRepository;
import com.zhenglongji.mall.mall.repository.StocktakeRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务：三维口径（实物=可用+预占+冻结）、原子条件更新、流水追溯、盘点二级确认、日终核对
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductSkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final StockFlowRepository flowRepository;
    private final InboundRepository inboundRepository;
    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository stocktakeItemRepository;
    private final AuditService auditService;
    private final AlertService alertService;

    // ---------- 基础原子操作（订单链路） ----------

    /** 下单预占：available-=n reserved+=n，不足返回 false */
    @Transactional
    public boolean reserve(Long skuId, int qty, String orderNo) {
        int rows = skuRepository.reserve(skuId, qty);
        if (rows > 0) {
            writeFlow(skuId, "RESERVE", 0, orderNo, "下单预占 " + qty + " 件");
        }
        return rows > 0;
    }

    /** 支付实扣 / 核销实扣：reserved-=n（实物减少） */
    @Transactional
    public void deduct(Long skuId, int qty, String relatedNo, String remark) {
        int rows = skuRepository.deductReserved(skuId, qty);
        if (rows == 0) {
            throw new ServiceException("库存预占异常，SKU " + skuId);
        }
        writeFlow(skuId, "OUT", -qty, relatedNo, remark);
    }

    /** 取消 / 超时释放：reserved-=n available+=n */
    @Transactional
    public void release(Long skuId, int qty, String orderNo) {
        skuRepository.release(skuId, qty);
        writeFlow(skuId, "RELEASE", 0, orderNo, "释放预占 " + qty + " 件");
    }

    /** 退款回补：available+=n */
    @Transactional
    public void refundIn(Long skuId, int qty, String relatedNo) {
        skuRepository.refundIn(skuId, qty);
        writeFlow(skuId, "REFUND_IN", qty, relatedNo, "退款回补 " + qty + " 件");
        checkAlert(skuId, null);
    }

    /** 变动后检查库存预警 */
    @Transactional
    public void checkAlert(Long skuId, String orderNo) {
        ProductSku sku = skuRepository.findById(skuId).orElse(null);
        if (sku == null) {
            return;
        }
        Product product = productRepository.findById(sku.getProductId()).orElse(null);
        alertService.raiseStockAlertIfLow(sku, product == null ? "未知商品" : product.getName());
    }

    private void writeFlow(Long skuId, String type, int quantity, String relatedNo, String remark) {
        ProductSku sku = skuRepository.findById(skuId).orElseThrow(() -> new ServiceException("SKU 不存在"));
        StockFlow flow = new StockFlow();
        flow.setSkuId(skuId);
        flow.setType(type);
        flow.setQuantity(quantity);
        flow.setBalanceAfter(sku.physical());
        flow.setRelatedNo(relatedNo);
        flow.setRemark(remark);
        flow.setCreatedAt(LocalDateTime.now());
        flowRepository.save(flow);
    }

    // ---------- 管理端 ----------

    public List<Map<String, Object>> adminSkuList(String keyword) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductSku sku : skuRepository.findAll()) {
            Product product = productRepository.findById(sku.getProductId()).orElse(null);
            String productName = product == null ? "未知商品" : product.getName();
            if (keyword != null
                    && !keyword.isBlank()
                    && !productName.contains(keyword.trim())
                    && (sku.getBarcode() == null || !sku.getBarcode().contains(keyword.trim()))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", sku.getId());
            row.put("productName", productName);
            row.put("flavor", sku.getFlavor());
            row.put("spec", sku.getSpec());
            row.put("barcode", sku.getBarcode());
            row.put("price", sku.getPrice());
            row.put("physical", sku.physical());
            row.put("available", ProductSku.n(sku.getAvailable()));
            row.put("reserved", ProductSku.n(sku.getReserved()));
            row.put("frozen", ProductSku.n(sku.getFrozen()));
            row.put("threshold", sku.getThreshold());
            row.put("status", sku.getStatus());
            rows.add(row);
        }
        rows.sort((a, b) -> Long.compare((long) a.get("id"), (long) b.get("id")));
        return rows;
    }

    /** 入库：数量>0、批次必填，生成 IN 流水 */
    @Transactional
    public Inbound inbound(Long skuId, Integer quantity, String batchNo, String supplier, String remark, String operator) {
        if (quantity == null || quantity <= 0) {
            throw new ServiceException("入库数量必须大于 0");
        }
        if (batchNo == null || batchNo.isBlank()) {
            throw new ServiceException("批次号必填");
        }
        ProductSku sku = getSku(skuId);
        if ("FROZEN".equals(sku.getStatus())) {
            throw new ServiceException("SKU 已冻结，不能入库");
        }
        skuRepository.inbound(skuId, quantity);
        String inboundNo = "IN" + System.currentTimeMillis();
        writeFlow(skuId, "IN", quantity, inboundNo, "入库 批次 " + batchNo);
        checkAlert(skuId, null);

        Inbound inbound = new Inbound();
        inbound.setSkuId(skuId);
        inbound.setQuantity(quantity);
        inbound.setBatchNo(batchNo);
        inbound.setSupplier(supplier);
        inbound.setRemark(remark);
        inbound.setCreatedBy(operator);
        inbound.setCreatedAt(LocalDateTime.now());
        inboundRepository.save(inbound);
        return inbound;
    }

    public List<Inbound> inboundList() {
        return inboundRepository.findAllByOrderByIdDesc();
    }

    /** 冻结 / 释放：冻结后可用清零（预占不受影响），均生成流水 */
    @Transactional
    public void freeze(Long skuId, String action, String reason) {
        ProductSku sku = getSku(skuId);
        int rows;
        if ("FREEZE".equals(action)) {
            if (ProductSku.n(sku.getAvailable()) == 0) {
                throw new ServiceException("可用库存为 0，无需冻结");
            }
            rows = skuRepository.freeze(skuId);
            if (rows > 0) {
                writeFlow(skuId, "FREEZE", 0, null, "冻结库存，原因: " + reason);
            }
        } else if ("RELEASE".equals(action)) {
            if (ProductSku.n(sku.getFrozen()) == 0) {
                throw new ServiceException("无冻结库存，无需释放");
            }
            rows = skuRepository.unfreeze(skuId);
            if (rows > 0) {
                writeFlow(skuId, "UNFREEZE", 0, null, "释放冻结，原因: " + reason);
            }
        } else {
            throw new ServiceException("action 非法，须为 FREEZE 或 RELEASE");
        }
        if (rows == 0) {
            throw new ServiceException("SKU 状态不允许该操作");
        }
        auditService.log("stock", "FREEZE".equals(action) ? "冻结库存" : "释放冻结", "SKU[" + skuId + "] 原因: " + reason);
    }

    /** 阈值修改（留审计） */
    @Transactional
    public void threshold(Long skuId, Integer threshold) {
        if (threshold == null || threshold < 0) {
            throw new ServiceException("阈值非法");
        }
        ProductSku sku = getSku(skuId);
        Integer old = sku.getThreshold();
        sku.setThreshold(threshold);
        skuRepository.save(sku);
        auditService.log("stock", "修改预警阈值", "SKU[" + skuId + "] " + old + "->" + threshold);
        checkAlert(skuId, null);
    }

    /** 库存流水查询 */
    public List<Map<String, Object>> flows(Long skuId, String type) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<StockFlow> all = flowRepository.findAll();
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        for (StockFlow flow : all) {
            if (skuId != null && !skuId.equals(flow.getSkuId())) {
                continue;
            }
            if (type != null && !type.isBlank() && !type.equals(flow.getType())) {
                continue;
            }
            ProductSku sku = skuRepository.findById(flow.getSkuId()).orElse(null);
            Product product = sku == null ? null : productRepository.findById(sku.getProductId()).orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", flow.getId());
            row.put("skuId", flow.getSkuId());
            row.put("productName", product == null ? "未知商品" : product.getName());
            row.put("type", flow.getType());
            row.put("quantity", flow.getQuantity());
            row.put("balanceAfter", flow.getBalanceAfter());
            row.put("relatedNo", flow.getRelatedNo());
            row.put("remark", flow.getRemark());
            row.put("createdAt", flow.getCreatedAt());
            rows.add(row);
        }
        return rows;
    }

    /** 创建盘点单（差异自动计算，PENDING_CONFIRM） */
    @Transactional
    public Stocktake createStocktake(List<Map<String, Object>> items, String remark, String operator) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("盘点明细不能为空");
        }
        Stocktake stocktake = new Stocktake();
        stocktake.setStatus("PENDING_CONFIRM");
        stocktake.setRemark(remark);
        stocktake.setCreatedBy(operator);
        stocktake.setCreatedAt(LocalDateTime.now());
        stocktake = stocktakeRepository.save(stocktake);
        for (Map<String, Object> item : items) {
            Long skuId = ((Number) item.get("skuId")).longValue();
            Integer actual = (Integer) item.get("actual");
            if (actual == null || actual < 0) {
                throw new ServiceException("实盘数非法");
            }
            ProductSku sku = getSku(skuId);
            StocktakeItem stocktakeItem = new StocktakeItem();
            stocktakeItem.setStocktakeId(stocktake.getId());
            stocktakeItem.setSkuId(skuId);
            stocktakeItem.setSystemCount(sku.physical());
            stocktakeItem.setActual(actual);
            stocktakeItem.setDiff(actual - sku.physical());
            stocktakeItemRepository.save(stocktakeItem);
        }
        return stocktake;
    }

    public List<Map<String, Object>> stocktakeList() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Stocktake st : stocktakeRepository.findAllByOrderByIdDesc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", st.getId());
            row.put("status", st.getStatus());
            row.put("remark", st.getRemark());
            row.put("createdBy", st.getCreatedBy());
            row.put("createdAt", st.getCreatedAt());
            row.put("confirmedBy", st.getConfirmedBy());
            List<Map<String, Object>> items = new ArrayList<>();
            for (StocktakeItem item : stocktakeItemRepository.findByStocktakeId(st.getId())) {
                ProductSku sku = skuRepository.findById(item.getSkuId()).orElse(null);
                Product product = sku == null ? null : productRepository.findById(sku.getProductId()).orElse(null);
                Map<String, Object> itemRow = new LinkedHashMap<>();
                itemRow.put("skuId", item.getSkuId());
                itemRow.put("productName", product == null ? "未知商品" : product.getName());
                itemRow.put("systemCount", item.getSystemCount());
                itemRow.put("actual", item.getActual());
                itemRow.put("diff", item.getDiff());
                items.add(itemRow);
            }
            row.put("items", items);
            rows.add(row);
        }
        return rows;
    }

    /** 盘点二级确认：提交人≠确认人；确认后按差异修正库存并生成 ADJUST 流水 */
    @Transactional
    public void confirmStocktake(Long id, String operator) {
        Stocktake stocktake = stocktakeRepository
                .findById(id)
                .orElseThrow(() -> new ServiceException("盘点单不存在"));
        if (!"PENDING_CONFIRM".equals(stocktake.getStatus())) {
            throw new ServiceException("盘点单不是待确认状态");
        }
        if (operator.equals(stocktake.getCreatedBy())) {
            throw new ServiceException("提交人不得同时担任确认人");
        }
        for (StocktakeItem item : stocktakeItemRepository.findByStocktakeId(id)) {
            if (item.getDiff() != null && item.getDiff() != 0) {
                int rows = skuRepository.adjust(item.getSkuId(), item.getDiff());
                if (rows == 0) {
                    throw new ServiceException("SKU " + item.getSkuId() + " 库存修正失败（可用不足扣减）");
                }
                writeFlow(item.getSkuId(), "ADJUST", item.getDiff(), "ST" + id, "盘点单 " + id + " 修正");
                checkAlert(item.getSkuId(), null);
            }
        }
        stocktake.setStatus("CONFIRMED");
        stocktake.setConfirmedBy(operator);
        stocktake.setConfirmedAt(LocalDateTime.now());
        stocktakeRepository.save(stocktake);
        auditService.log("stock", "盘点确认", "盘点单[" + id + "] 确认人:" + operator);
    }

    /** 日终核对：实物 ≠ Σ流水净和 → RECON_DIFF 告警 */
    @Transactional
    public int reconcile() {
        int diffCount = 0;
        for (ProductSku sku : skuRepository.findAll()) {
            int physical = sku.physical();
            int flowSum = flowRepository.sumQuantityBySkuId(sku.getId());
            if (physical != flowSum) {
                diffCount++;
                var openAlert =
                        alertService.list().stream()
                                .filter(a -> "RECON_DIFF".equals(a.getType())
                                        && "OPEN".equals(a.getStatus())
                                        && sku.getId().equals(a.getSkuId()))
                                .findFirst();
                if (openAlert.isEmpty()) {
                    alertService.raise(
                            "RECON_DIFF",
                            "SKU[" + sku.getId() + "] 库存核对差异：实物 " + physical + " ≠ 流水净和 " + flowSum,
                            sku.getId());
                    log.warn("[库存核对] SKU {} 实物 {} ≠ 流水净和 {}", sku.getId(), physical, flowSum);
                }
            }
        }
        return diffCount;
    }

    private ProductSku getSku(Long skuId) {
        return skuRepository.findById(skuId).orElseThrow(() -> new ServiceException("SKU 不存在"));
    }
}
