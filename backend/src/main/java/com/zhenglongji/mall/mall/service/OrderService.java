package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.constant.Constants;
import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.common.utils.MallUtils;
import com.zhenglongji.mall.mall.domain.Address;
import com.zhenglongji.mall.mall.domain.AfterSale;
import com.zhenglongji.mall.mall.domain.AfterSaleFlow;
import com.zhenglongji.mall.mall.domain.IdempotentRecord;
import com.zhenglongji.mall.mall.domain.MallOrder;
import com.zhenglongji.mall.mall.domain.Member;
import com.zhenglongji.mall.mall.domain.OrderItem;
import com.zhenglongji.mall.mall.domain.OrderTimeline;
import com.zhenglongji.mall.mall.domain.Product;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.domain.Store;
import com.zhenglongji.mall.mall.domain.VerifyRecord;
import com.zhenglongji.mall.mall.domain.dto.OrderBody;
import com.zhenglongji.mall.mall.domain.dto.VerifyBody;
import com.zhenglongji.mall.mall.repository.AddressRepository;
import com.zhenglongji.mall.mall.repository.AfterSaleFlowRepository;
import com.zhenglongji.mall.mall.repository.AfterSaleRepository;
import com.zhenglongji.mall.mall.repository.IdempotentRecordRepository;
import com.zhenglongji.mall.mall.repository.OrderItemRepository;
import com.zhenglongji.mall.mall.repository.OrderRepository;
import com.zhenglongji.mall.mall.repository.OrderTimelineRepository;
import com.zhenglongji.mall.mall.repository.ProductRepository;
import com.zhenglongji.mall.mall.repository.ProductSkuRepository;
import com.zhenglongji.mall.mall.repository.VerifyRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单服务：下单（幂等+预占+积分抵现+运费）、支付、取消、确认收货、超时关单、自提核销、发货、导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<String, String> STATUS_TEXT = Map.of(
            "WAIT_PAY", "待付款",
            "PAID", "待发货",
            "SHIPPED", "待收货",
            "PICKUP_WAIT", "待自提",
            "COMPLETED", "已完成",
            "CLOSED", "已关闭",
            "AFTER_SALE", "售后中");

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineRepository timelineRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final AfterSaleFlowRepository afterSaleFlowRepository;
    private final IdempotentRecordRepository idempotentRecordRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final AddressRepository addressRepository;
    private final MemberService memberService;
    private final StockService stockService;
    private final AlertService alertService;
    private final StoreService storeService;
    private final AuditService auditService;
    private final VerifyRecordRepository verifyRecordRepository;

    public static String statusText(String status) {
        return STATUS_TEXT.getOrDefault(status, status);
    }

    // ==================== 买家端 ====================

    /** 下单：幂等键 5 分钟去重；订单创建与库存预占同事务 */
    @Transactional
    public Map<String, Object> create(Long memberId, OrderBody body) {
        if (body == null || body.idempotentKey() == null || body.idempotentKey().isBlank()) {
            throw new ServiceException("幂等键不能为空");
        }
        String idemKey = body.idempotentKey().trim();
        var existing = idempotentRecordRepository.findByIdemKey(idemKey);
        if (existing.isPresent()
                && existing.get().getCreatedAt() != null
                && existing.get().getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return createdVo(existing.get().getOrderNo());
        }

        if (body.items() == null || body.items().isEmpty()) {
            throw new ServiceException("订单明细不能为空");
        }
        String fulfillmentType = body.fulfillmentType();
        if (!"EXPRESS".equals(fulfillmentType) && !"PICKUP".equals(fulfillmentType)) {
            throw new ServiceException("履约方式非法");
        }
        String receiver = null;
        String receiverPhone = null;
        String addressText = null;
        if ("PICKUP".equals(fulfillmentType)) {
            if (body.storeId() == null) {
                throw new ServiceException("自提订单必须选择门店");
            }
            Store store = storeService.getById(body.storeId());
            if (!"OPEN".equals(store.getStatus())) {
                throw new ServiceException("门店已关闭，无法自提");
            }
        } else {
            if (body.addressId() == null) {
                throw new ServiceException("快递订单必须选择收货地址");
            }
            Address address = addressRepository
                    .findById(body.addressId())
                    .orElseThrow(() -> new ServiceException("收货地址不存在"));
            if (!address.getMemberId().equals(memberId)) {
                throw new ServiceException("无权使用该地址");
            }
            receiver = address.getReceiver();
            receiverPhone = address.getPhone();
            addressText = address.getRegion() + " " + address.getDetail();
        }

        Member member = memberService.getById(memberId);
        String orderNo = MallUtils.genOrderNo();
        int goodsAmount = 0;
        List<OrderItem> items = new ArrayList<>();
        for (var line : body.items()) {
            if (line == null || line.skuId() == null || line.quantity() == null || line.quantity() <= 0) {
                throw new ServiceException("订单明细参数错误");
            }
            ProductSku sku = skuRepository
                    .findById(line.skuId())
                    .orElseThrow(() -> new ServiceException("SKU 不存在"));
            Product product =
                    productRepository.findById(sku.getProductId()).orElseThrow(() -> new ServiceException("商品不存在"));
            if (!"ON".equals(product.getStatus())) {
                throw new ServiceException("商品「" + product.getName() + "」已下架，禁止下单");
            }
            if (!stockService.reserve(sku.getId(), line.quantity(), orderNo)) {
                ProductSku latest = skuRepository.findById(sku.getId()).orElse(sku);
                throw new ServiceException(
                        "商品「" + product.getName() + " " + sku.getFlavor() + " " + sku.getSpec() + "」库存不足，剩余可购 "
                                + ProductSku.n(latest.getAvailable()));
            }
            int subtotal = sku.getPrice() * line.quantity();
            goodsAmount += subtotal;

            OrderItem item = new OrderItem();
            item.setSkuId(sku.getId());
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setImage(firstImage(product.getImages()));
            item.setFlavor(sku.getFlavor());
            item.setSpec(sku.getSpec());
            item.setPrice(sku.getPrice());
            item.setQuantity(line.quantity());
            item.setSubtotal(subtotal);
            items.add(item);
        }

        int freight = "PICKUP".equals(fulfillmentType) ? 0 : (goodsAmount >= Constants.FREE_FREIGHT_THRESHOLD ? 0 : Constants.EXPRESS_FREIGHT);

        int pointsUsed = 0;
        if (Boolean.TRUE.equals(body.usePoints()) && member.getPoints() != null && member.getPoints() > 0) {
            int preDeduction = goodsAmount + freight;
            pointsUsed = Math.min(member.getPoints(), preDeduction * 5 / 100);
        }
        int pointsDeduction = pointsUsed;
        int payAmount = goodsAmount + freight - pointsDeduction;
        if (payAmount > Constants.MAX_PAY_AMOUNT) {
            alertService.raise(
                    "ABNORMAL_AMOUNT",
                    "订单金额异常：应付 " + payAmount + " 分超过单笔上限 " + Constants.MAX_PAY_AMOUNT + " 分，已拦截",
                    null);
            throw new ServiceException("单笔订单应付不能超过 " + Constants.MAX_PAY_AMOUNT / 100 + " 元");
        }

        MallOrder order = new MallOrder();
        order.setOrderNo(orderNo);
        order.setMemberId(memberId);
        order.setFulfillmentType(fulfillmentType);
        order.setStoreId(body.storeId());
        order.setAddressId(body.addressId());
        order.setReceiver(receiver);
        order.setReceiverPhone(receiverPhone);
        order.setAddress(addressText);
        order.setGoodsAmount(goodsAmount);
        order.setFreight(freight);
        order.setPointsDeduction(pointsDeduction);
        order.setPointsUsed(pointsUsed);
        order.setPayAmount(payAmount);
        order.setStatus("WAIT_PAY");
        order.setPayDeadline(LocalDateTime.now().plusMinutes(Constants.PAY_TIMEOUT_MINUTES));
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
            stockService.checkAlert(item.getSkuId(), null);
        }
        if (pointsUsed > 0) {
            memberService.changePoints(member, -pointsUsed, "下单积分抵扣", order.getOrderNo());
        }
        timeline(order.getId(), "创建订单", "订单创建，应付 " + payAmount + " 分");

        IdempotentRecord record =
                existing.orElseGet(() -> {
                    IdempotentRecord r = new IdempotentRecord();
                    r.setIdemKey(idemKey);
                    return r;
                });
        record.setOrderNo(order.getOrderNo());
        record.setCreatedAt(LocalDateTime.now());
        idempotentRecordRepository.save(record);

        return createdVo(order.getOrderNo());
    }

    /** 模拟支付：即成功；非 WAIT_PAY 拒绝并登记 DUPLICATE_PAY 告警 */
    @Transactional
    public Map<String, Object> pay(Long memberId, String orderNo) {
        MallOrder order = getOwnedOrder(memberId, orderNo);
        if (!"WAIT_PAY".equals(order.getStatus())) {
            alertService.raise(
                    "DUPLICATE_PAY",
                    "订单 " + orderNo + " 状态为 " + order.getStatus() + "，收到重复支付请求，已拒绝",
                    null);
            throw new ServiceException("订单状态不允许支付");
        }
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod("SIM");
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if ("PICKUP".equals(order.getFulfillmentType())) {
            // 自提单：支付后进入待自提，核销时实扣库存
            transition(order, "PICKUP_WAIT");
            order.setStatus("PICKUP_WAIT");
        } else {
            transition(order, "PAID");
            order.setStatus("PAID");
            for (OrderItem item : items) {
                stockService.deduct(item.getSkuId(), item.getQuantity(), orderNo, "支付实扣");
                stockService.checkAlert(item.getSkuId(), null);
            }
        }
        orderRepository.save(order);
        memberService.onPaid(memberService.getById(order.getMemberId()), order.getPayAmount(), orderNo);
        timeline(order.getId(), "支付成功", "模拟支付通道，实付 " + order.getPayAmount() + " 分");
        return orderVo(order);
    }

    /** 取消：仅 WAIT_PAY 可取消，释放预占并返还积分 */
    @Transactional
    public void cancel(Long memberId, String orderNo) {
        MallOrder order = getOwnedOrder(memberId, orderNo);
        transition(order, "CLOSED");
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            stockService.release(item.getSkuId(), item.getQuantity(), orderNo);
            stockService.checkAlert(item.getSkuId(), null);
        }
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            memberService.changePoints(memberService.getById(memberId), order.getPointsUsed(), "订单取消返还积分", orderNo);
        }
        order.setStatus("CLOSED");
        order.setCloseTime(LocalDateTime.now());
        orderRepository.save(order);
        timeline(order.getId(), "取消订单", "买家取消，预占库存已释放");
    }

    /** 确认收货：仅 SHIPPED 可确认 */
    @Transactional
    public void confirm(Long memberId, String orderNo) {
        MallOrder order = getOwnedOrder(memberId, orderNo);
        transition(order, "COMPLETED");
        order.setStatus("COMPLETED");
        order.setCompleteTime(LocalDateTime.now());
        orderRepository.save(order);
        timeline(order.getId(), "确认收货", "买家确认收货，订单完成");
    }

    /** 买家订单列表 */
    public TableDataInfo<Map<String, Object>> memberPage(Long memberId, String status, int page, int size) {
        List<MallOrder> orders = (status == null || status.isBlank())
                ? orderRepository.findByMemberIdOrderByIdDesc(memberId)
                : orderRepository.findByMemberIdAndStatusOrderByIdDesc(memberId, status);
        List<Map<String, Object>> rows = orders.stream().map(o -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", o.getOrderNo());
            row.put("status", o.getStatus());
            row.put("statusText", statusText(o.getStatus()));
            row.put("payAmount", o.getPayAmount());
            row.put("createdAt", o.getCreatedAt());
            row.put("fulfillmentType", o.getFulfillmentType());
            row.put("items", itemVos(orderItemRepository.findByOrderId(o.getId())));
            return row;
        }).toList();
        return PageUtils.page(rows, page, size);
    }

    /** 订单详情 */
    public Map<String, Object> detail(Long memberId, String orderNo) {
        MallOrder order = memberId == null ? getOrder(orderNo) : getOwnedOrder(memberId, orderNo);
        return orderDetailVo(order);
    }

    /** 自提核销码（分钟窗口刷新） */
    public Map<String, Object> verifyCode(String orderNo) {
        MallOrder order = getOrder(orderNo);
        if (!"PICKUP_WAIT".equals(order.getStatus())) {
            throw new ServiceException("订单不是待自提状态");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", MallUtils.currentVerifyCode(orderNo));
        data.put("expiresInSec", MallUtils.verifyCodeExpiresIn());
        return data;
    }

    // ==================== 管理端 ====================

    public TableDataInfo<Map<String, Object>> adminPage(String status, String keyword, Boolean overdueShip, int page, int size) {
        List<MallOrder> orders = orderRepository.findAll();
        orders.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MallOrder o : orders) {
            if (status != null && !status.isBlank() && !status.equals(o.getStatus())) {
                continue;
            }
            boolean overdue = "PAID".equals(o.getStatus())
                    && o.getPayTime() != null
                    && o.getPayTime().isBefore(LocalDateTime.now().minusHours(24));
            if (Boolean.TRUE.equals(overdueShip) && !overdue) {
                continue;
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim();
                boolean matchNo = o.getOrderNo() != null && o.getOrderNo().contains(kw);
                boolean matchPhone = o.getReceiverPhone() != null && o.getReceiverPhone().contains(kw);
                if (!matchNo && !matchPhone) {
                    continue;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", o.getOrderNo());
            row.put("status", o.getStatus());
            row.put("statusText", statusText(o.getStatus()));
            row.put("payAmount", o.getPayAmount());
            row.put("receiver", o.getReceiver());
            row.put("receiverPhone", o.getReceiverPhone() == null ? null : MallUtils.maskPhone(o.getReceiverPhone()));
            row.put("fulfillmentType", o.getFulfillmentType());
            row.put("payTime", o.getPayTime());
            row.put("createdAt", o.getCreatedAt());
            row.put("shipOverdue", overdue);
            row.put("items", itemVos(orderItemRepository.findByOrderId(o.getId())));
            rows.add(row);
        }
        return PageUtils.page(rows, page, size);
    }

    /** 发货：仅 PAID 可发货 */
    @Transactional
    public void ship(String orderNo, String company, String no) {
        if (company == null || company.isBlank() || no == null || no.isBlank()) {
            throw new ServiceException("物流公司与单号必填");
        }
        MallOrder order = getOrder(orderNo);
        transition(order, "SHIPPED");
        order.setStatus("SHIPPED");
        order.setLogisticsCompany(company);
        order.setLogisticsNo(no);
        order.setShipTime(LocalDateTime.now());
        orderRepository.save(order);
        timeline(order.getId(), "商家发货", company + " " + no);
        auditService.log("order", "发货", "订单 " + orderNo + " -> SHIPPED, " + company + " " + no);
    }

    /** 批量发货 */
    @Transactional
    public Map<String, Object> batchShip(List<com.zhenglongji.mall.mall.domain.dto.BatchShipRow> rows) {
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        for (var row : rows) {
            try {
                ship(row.orderNo(), row.company(), row.no());
                success++;
            } catch (com.zhenglongji.mall.common.exception.ServiceException e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("orderNo", row.orderNo());
                fail.put("reason", e.getMessage());
                failures.add(fail);
            } catch (Exception e) {
                log.error("批量发货系统异常 orderNo={}", row.orderNo(), e);
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("orderNo", row.orderNo());
                fail.put("reason", "系统繁忙，请稍后重试");
                failures.add(fail);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", success);
        data.put("failures", failures);
        return data;
    }

    /** 订单导出 CSV（text/csv） */
    public String exportCsv(LocalDate from, LocalDate to) {
        LocalDateTime fromTime = from == null ? LocalDateTime.now().minusYears(1) : from.atStartOfDay();
        LocalDateTime toTime = to == null ? LocalDateTime.now() : to.plusDays(1).atStartOfDay();
        StringBuilder csv = new StringBuilder("订单号,状态,商品,金额(分),支付方式,履约方式,下单时间\n");
        List<MallOrder> orders = orderRepository.findAll();
        orders.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        for (MallOrder o : orders) {
            if (o.getCreatedAt() == null || o.getCreatedAt().isBefore(fromTime) || !o.getCreatedAt().isBefore(toTime)) {
                continue;
            }
            StringBuilder goods = new StringBuilder();
            for (OrderItem item : orderItemRepository.findByOrderId(o.getId())) {
                if (goods.length() > 0) {
                    goods.append(" / ");
                }
                goods.append(item.getName())
                        .append("(")
                        .append(item.getFlavor() == null ? "" : item.getFlavor())
                        .append(item.getSpec() == null ? "" : item.getSpec())
                        .append("x")
                        .append(item.getQuantity())
                        .append(")");
            }
            csv.append(csvCell(o.getOrderNo())).append(',')
                    .append(csvCell(statusText(o.getStatus()))).append(',')
                    .append(csvCell(goods.toString())).append(',')
                    .append(o.getPayAmount()).append(',')
                    .append(csvCell(o.getPayMethod() == null ? "未支付" : "模拟支付")).append(',')
                    .append(csvCell("PICKUP".equals(o.getFulfillmentType()) ? "到店自提" : "快递配送")).append(',')
                    .append(o.getCreatedAt() == null ? "" : o.getCreatedAt().format(CSV_TIME))
                    .append('\n');
        }
        return csv.toString();
    }

    /** 超时关单（定时任务）：关单前二次校验状态 */
    @Transactional
    public int closeExpiredOrders() {
        List<MallOrder> expired =
                orderRepository.findByStatusAndPayDeadlineBefore("WAIT_PAY", LocalDateTime.now());
        int count = 0;
        for (MallOrder order : expired) {
            MallOrder latest = getOrder(order.getOrderNo());
            if (!"WAIT_PAY".equals(latest.getStatus())) {
                continue;
            }
            for (OrderItem item : orderItemRepository.findByOrderId(latest.getId())) {
                stockService.release(item.getSkuId(), item.getQuantity(), latest.getOrderNo());
                stockService.checkAlert(item.getSkuId(), null);
            }
            if (latest.getPointsUsed() != null && latest.getPointsUsed() > 0) {
                memberService.changePoints(
                        memberService.getById(latest.getMemberId()), latest.getPointsUsed(), "订单超时关闭返还积分", latest.getOrderNo());
            }
            latest.setStatus("CLOSED");
            latest.setCloseTime(LocalDateTime.now());
            orderRepository.save(latest);
            timeline(latest.getId(), "超时关闭", "超 15 分钟未支付自动关闭，预占库存已释放");
            count++;
        }
        return count;
    }

    /** 到店核销：状态=PICKUP_WAIT、门店匹配、码为当前或上一窗口 */
    @Transactional
    public Map<String, Object> verify(String operator, VerifyBody body) {
        if (body == null || body.orderNo() == null || body.code() == null || body.storeId() == null) {
            throw new ServiceException("核销参数不完整");
        }
        MallOrder order = getOrder(body.orderNo());
        if (!"PICKUP_WAIT".equals(order.getStatus())) {
            throw new ServiceException("订单不是待自提状态，无法核销");
        }
        if (!body.storeId().equals(order.getStoreId())) {
            throw new ServiceException("门店不匹配，无法核销");
        }
        long window = System.currentTimeMillis() / 60000;
        boolean codeValid = body.code().equals(MallUtils.verifyCode(order.getOrderNo(), window))
                || body.code().equals(MallUtils.verifyCode(order.getOrderNo(), window - 1));
        if (!codeValid) {
            throw new ServiceException("核销码无效或已过期");
        }
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            stockService.deduct(item.getSkuId(), item.getQuantity(), order.getOrderNo(), "到店自提核销实扣");
            stockService.checkAlert(item.getSkuId(), null);
        }
        order.setStatus("COMPLETED");
        order.setCompleteTime(LocalDateTime.now());
        orderRepository.save(order);

        Store store = storeService.getById(order.getStoreId());
        VerifyRecord record = new VerifyRecord();
        record.setOrderNo(order.getOrderNo());
        record.setStoreId(store.getId());
        record.setStoreName(store.getName());
        record.setVerifier(operator);
        record.setVerifyTime(LocalDateTime.now());
        record.setPayAmount(order.getPayAmount());
        verifyRecordRepository.save(record);

        timeline(order.getId(), "到店自提核销", "门店「" + store.getName() + "」核销完成");
        auditService.log("order", "自提核销", "订单 " + order.getOrderNo() + " 于门店[" + store.getName() + "]核销");
        return Map.of("orderNo", order.getOrderNo(), "status", "COMPLETED");
    }

    // ==================== 售后联动 ====================

    /** 订单进入售后中 */
    @Transactional
    public void markAftersale(String orderNo) {
        MallOrder order = getOrder(orderNo);
        if ("COMPLETED".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus())) {
            transition(order, "AFTER_SALE");
            order.setStatus("AFTER_SALE");
            orderRepository.save(order);
            timeline(order.getId(), "发起售后", "订单进入售后流程");
        }
    }

    /** 售后结束回到完成态 */
    @Transactional
    public void completeAftersale(String orderNo) {
        MallOrder order = getOrder(orderNo);
        if ("AFTER_SALE".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            if (order.getCompleteTime() == null) {
                order.setCompleteTime(LocalDateTime.now());
            }
            orderRepository.save(order);
            timeline(order.getId(), "售后完成", "售后处理完毕，订单回到已完成");
        }
    }

    // ==================== helpers ====================

    /** 状态机校验：非法流转抛异常（禁止回跳） */
    private void transition(MallOrder order, String target) {
        String from = order.getStatus();
        boolean ok = switch (from + "->" + target) {
            case "WAIT_PAY->PAID", "WAIT_PAY->PICKUP_WAIT", "PAID->SHIPPED",
                    "SHIPPED->COMPLETED", "PICKUP_WAIT->COMPLETED", "WAIT_PAY->CLOSED",
                    "COMPLETED->AFTER_SALE", "SHIPPED->AFTER_SALE", "AFTER_SALE->COMPLETED",
                    "AFTER_SALE->SHIPPED" -> true;
            default -> false;
        };
        if (!ok) {
            throw new ServiceException("订单状态不允许该操作（" + from + " -> " + target + "）");
        }
    }

    private void timeline(Long orderId, String action, String remark) {
        OrderTimeline node = new OrderTimeline();
        node.setOrderId(orderId);
        node.setAction(action);
        node.setRemark(remark);
        node.setCreatedAt(LocalDateTime.now());
        timelineRepository.save(node);
    }

    public MallOrder getOrder(String orderNo) {
        return orderRepository
                .findByOrderNo(orderNo)
                .orElseThrow(() -> new ServiceException("订单不存在"));
    }

    /** 供售后等模块直接保存订单状态 */
    @Transactional
    public void saveOrder(MallOrder order) {
        orderRepository.save(order);
    }

    private MallOrder getOwnedOrder(Long memberId, String orderNo) {
        MallOrder order = getOrder(orderNo);
        if (!order.getMemberId().equals(memberId)) {
            throw new ServiceException("无权操作该订单");
        }
        return order;
    }

    private Map<String, Object> createdVo(String orderNo) {
        MallOrder order = getOrder(orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("goodsAmount", order.getGoodsAmount());
        data.put("freight", order.getFreight());
        data.put("pointsDeduction", order.getPointsDeduction());
        data.put("payAmount", order.getPayAmount());
        data.put("pointsUsed", order.getPointsUsed());
        data.put("status", order.getStatus());
        return data;
    }

    private Map<String, Object> orderVo(MallOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("status", order.getStatus());
        data.put("statusText", statusText(order.getStatus()));
        data.put("payAmount", order.getPayAmount());
        return data;
    }

    private List<Map<String, Object>> itemVos(List<OrderItem> items) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("skuId", item.getSkuId());
            row.put("name", item.getName());
            row.put("image", item.getImage());
            row.put("flavor", item.getFlavor());
            row.put("spec", item.getSpec());
            row.put("price", item.getPrice());
            row.put("quantity", item.getQuantity());
            row.put("subtotal", item.getSubtotal());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> orderDetailVo(MallOrder order) {
        Map<String, Object> data = orderVo(order);
        data.put("items", itemVos(orderItemRepository.findByOrderId(order.getId())));
        data.put("fulfillmentType", order.getFulfillmentType());
        data.put("goodsAmount", order.getGoodsAmount());
        data.put("freight", order.getFreight());
        data.put("pointsDeduction", order.getPointsDeduction());
        data.put("pointsUsed", order.getPointsUsed());
        data.put("receiver", order.getReceiver());
        data.put("receiverPhone", order.getReceiverPhone());
        data.put("address", order.getAddress());
        if ("PICKUP".equals(order.getFulfillmentType()) && order.getStoreId() != null) {
            try {
                Store store = storeService.getById(order.getStoreId());
                Map<String, Object> storeVo = new LinkedHashMap<>();
                storeVo.put("id", store.getId());
                storeVo.put("name", store.getName());
                storeVo.put("address", store.getAddress());
                storeVo.put("phone", store.getPhone());
                data.put("store", storeVo);
            } catch (Exception ignore) {
                // 门店可能被删除
            }
        }
        Map<String, Object> logistics = new LinkedHashMap<>();
        logistics.put("logisticsCompany", order.getLogisticsCompany());
        logistics.put("logisticsNo", order.getLogisticsNo());
        data.put("logistics", logistics);
        data.put("payMethod", order.getPayMethod());
        data.put("payTime", order.getPayTime());
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (OrderTimeline node :
                timelineRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId())) {
            Map<String, Object> nodeVo = new LinkedHashMap<>();
            nodeVo.put("action", node.getAction());
            nodeVo.put("time", node.getCreatedAt());
            nodeVo.put("remark", node.getRemark());
            timeline.add(nodeVo);
        }

        // 售后联动：合并最新售后单的流转记录到订单时间线，并附带售后单摘要（H5 订单详情展示售后进度）
        AfterSale latest = afterSaleRepository.findByOrderNo(order.getOrderNo()).stream()
                .max(java.util.Comparator.comparingLong(AfterSale::getId)).orElse(null);
        if (latest != null) {
            for (AfterSaleFlow f : afterSaleFlowRepository.findByAfterSaleIdOrderByCreatedAtAscIdAsc(latest.getId())) {
                Map<String, Object> nodeVo = new LinkedHashMap<>();
                nodeVo.put("action", "售后 · " + f.getAction());
                nodeVo.put("time", f.getCreatedAt());
                nodeVo.put("remark", f.getRemark() != null ? f.getRemark() : f.getOperator());
                timeline.add(nodeVo);
            }
            Map<String, Object> asVo = new LinkedHashMap<>();
            asVo.put("afterSaleNo", latest.getAfterSaleNo());
            asVo.put("status", latest.getStatus());
            asVo.put("statusText", AfterSaleService.statusText(latest.getStatus()));
            asVo.put("typeText", AfterSaleService.typeText(latest.getType()));
            asVo.put("refundAmount", latest.getRefundAmount());
            data.put("aftersale", asVo);
        }
        data.put("timeline", timeline);
        if ("PICKUP_WAIT".equals(order.getStatus())) {
            data.put("pickupCode", MallUtils.currentVerifyCode(order.getOrderNo()));
        }
        return data;
    }

    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        return images.split("\\|", -1)[0];
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
