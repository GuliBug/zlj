package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.common.utils.MallUtils;
import com.zhenglongji.mall.mall.domain.AfterSale;
import com.zhenglongji.mall.mall.domain.AfterSaleFlow;
import com.zhenglongji.mall.mall.domain.MallOrder;
import com.zhenglongji.mall.mall.domain.Member;
import com.zhenglongji.mall.mall.domain.OrderItem;
import com.zhenglongji.mall.mall.domain.dto.AfterSaleBody;
import com.zhenglongji.mall.mall.repository.AfterSaleFlowRepository;
import com.zhenglongji.mall.mall.repository.AfterSaleRepository;
import com.zhenglongji.mall.mall.repository.OrderItemRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 售后服务：申请 / 审核 / 退货 / 幂等退款 / 流转记录（只插不改）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    public static final List<String> REASONS = List.of("质量问题", "少件漏发", "发错货", "不想要了", "其他");

    private static final Map<String, String> TYPE_TEXT = Map.of("REFUND_ONLY", "仅退款", "RETURN_REFUND", "退货退款");

    private static final Map<String, String> STATUS_TEXT = Map.of(
            "APPLYING", "待审核",
            "REFUNDING", "退款中",
            "WAIT_RETURN", "待买家寄回",
            "REFUNDED", "已退款",
            "REJECTED", "已拒绝",
            "CLOSED", "已关闭");

    private final AfterSaleRepository afterSaleRepository;
    private final AfterSaleFlowRepository flowRepository;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final MemberService memberService;
    private final StockService stockService;
    private final AuditService auditService;

    public static String statusText(String status) {
        return STATUS_TEXT.getOrDefault(status, status);
    }

    public static String typeText(String type) {
        return TYPE_TEXT.getOrDefault(type, type);
    }

    // ==================== 买家端 ====================

    @Transactional
    public Map<String, Object> apply(Long memberId, AfterSaleBody body) {
        if (body == null || body.orderNo() == null || body.type() == null || body.reason() == null) {
            throw new ServiceException("申请参数不完整");
        }
        if (!REASONS.contains(body.reason())) {
            throw new ServiceException("售后原因非法");
        }
        if (!"REFUND_ONLY".equals(body.type()) && !"RETURN_REFUND".equals(body.type())) {
            throw new ServiceException("售后类型非法");
        }
        MallOrder order = orderService.getOrder(body.orderNo());
        if (!order.getMemberId().equals(memberId)) {
            throw new ServiceException("无权操作该订单");
        }
        if (!"COMPLETED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) {
            throw new ServiceException("仅已完成/已发货订单可申请售后");
        }
        LocalDateTime base = order.getCompleteTime() != null ? order.getCompleteTime() : order.getShipTime();
        if (base != null && ChronoUnit.DAYS.between(base, LocalDateTime.now()) > 7) {
            throw new ServiceException("已超过签收后 7 天，无法申请售后");
        }
        long applied = afterSaleRepository.countByOrderNoAndSkuId(body.orderNo(), body.skuId());
        if (applied >= 3) {
            throw new ServiceException("同一订单同一 SKU 最多申请 3 次售后");
        }

        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleNo(MallUtils.genAfterSaleNo());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setMemberId(memberId);
        afterSale.setSkuId(body.skuId());
        afterSale.setType(body.type());
        afterSale.setReason(body.reason());
        afterSale.setDescription(body.description());
        afterSale.setImages(body.images() == null ? null : String.join("|", body.images()));
        afterSale.setRefundAmount(computeRefundAmount(order, body.skuId()));
        afterSale.setStatus("APPLYING");
        afterSale.setOrderPrevStatus(order.getStatus());
        afterSale.setCreatedAt(LocalDateTime.now());
        afterSaleRepository.save(afterSale);

        orderService.markAftersale(order.getOrderNo());
        flow(afterSale.getId(), "买家提交售后申请", "买家", body.reason());
        flow(afterSale.getId(), "系统受理", "系统", "工单已受理，等待商家审核");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("afterSaleNo", afterSale.getAfterSaleNo());
        return data;
    }

    /** 撤销：仅商家审核前可撤销 */
    @Transactional
    public void cancel(Long memberId, String afterSaleNo) {
        AfterSale afterSale = getOwned(memberId, afterSaleNo);
        if (!"APPLYING".equals(afterSale.getStatus())) {
            throw new ServiceException("仅商家审核前可撤销");
        }
        afterSale.setStatus("CLOSED");
        afterSaleRepository.save(afterSale);
        restoreOrder(afterSale);
        flow(afterSale.getId(), "买家撤销申请", "买家", null);
    }

    /** 退货退款：买家填写寄回物流 */
    @Transactional
    public void returnLogistics(Long memberId, String afterSaleNo, String company, String no) {
        if (company == null || company.isBlank() || no == null || no.isBlank()) {
            throw new ServiceException("物流公司与单号必填");
        }
        AfterSale afterSale = getOwned(memberId, afterSaleNo);
        if (!"RETURN_REFUND".equals(afterSale.getType()) || !"WAIT_RETURN".equals(afterSale.getStatus())) {
            throw new ServiceException("当前状态不允许填写退货物流");
        }
        afterSale.setReturnCompany(company);
        afterSale.setReturnNo(no);
        afterSale.setStatus("REFUNDING");
        afterSaleRepository.save(afterSale);
        flow(afterSale.getId(), "买家已寄回商品", "买家", company + " " + no);
    }

    public TableDataInfo<Map<String, Object>> memberPage(Long memberId, int page, int size) {
        List<AfterSale> all = afterSaleRepository.findAll();
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        List<Map<String, Object>> rows = all.stream()
                .filter(a -> a.getMemberId().equals(memberId))
                .map(this::rowVo)
                .toList();
        return PageUtils.page(rows, page, size);
    }

    public Map<String, Object> memberDetail(Long memberId, String afterSaleNo) {
        AfterSale afterSale = getOwned(memberId, afterSaleNo);
        return detailVo(afterSale);
    }

    // ==================== 管理端 ====================

    public TableDataInfo<Map<String, Object>> adminPage(String status, Boolean overdue, int page, int size) {
        List<AfterSale> all = afterSaleRepository.findAll();
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AfterSale a : all) {
            boolean auditOverdue = "APPLYING".equals(a.getStatus())
                    && a.getCreatedAt() != null
                    && a.getCreatedAt().isBefore(LocalDateTime.now().minusHours(48));
            if (status != null && !status.isBlank() && !status.equals(a.getStatus())) {
                continue;
            }
            if (Boolean.TRUE.equals(overdue) && !auditOverdue) {
                continue;
            }
            Map<String, Object> row = rowVo(a);
            row.put("auditOverdue", auditOverdue);
            rows.add(row);
        }
        return PageUtils.page(rows, page, size);
    }

    public Map<String, Object> adminDetail(String afterSaleNo) {
        AfterSale afterSale =
                afterSaleRepository.findByAfterSaleNo(afterSaleNo).orElseThrow(() -> new ServiceException("售后单不存在"));
        return detailVo(afterSale);
    }

    /** 审核：意见必填；仅退款通过直接触发退款，退货退款进入待寄回 */
    @Transactional
    public void audit(String operator, String afterSaleNo, Boolean approve, String opinion) {
        if (opinion == null || opinion.isBlank()) {
            throw new ServiceException("审核意见必填");
        }
        AfterSale afterSale = get(afterSaleNo);
        if (!"APPLYING".equals(afterSale.getStatus())) {
            throw new ServiceException("售后单不是待审核状态");
        }
        afterSale.setOpinion(opinion);
        if (Boolean.TRUE.equals(approve)) {
            if ("REFUND_ONLY".equals(afterSale.getType())) {
                afterSale.setStatus("REFUNDING");
                afterSaleRepository.save(afterSale);
                flow(afterSale.getId(), "商家审核通过", operator, opinion);
                doRefund(afterSale, operator);
            } else {
                afterSale.setStatus("WAIT_RETURN");
                afterSaleRepository.save(afterSale);
                flow(afterSale.getId(), "商家审核通过，等待买家寄回", operator, opinion);
            }
        } else {
            afterSale.setStatus("REJECTED");
            afterSaleRepository.save(afterSale);
            restoreOrder(afterSale);
            flow(afterSale.getId(), "商家审核拒绝", operator, opinion);
        }
        auditService.log("aftersale", "售后审核", "售后单 " + afterSaleNo + " 审核" + (Boolean.TRUE.equals(approve) ? "通过" : "拒绝")
                + "，意见: " + opinion);
    }

    /** 退货退款确认收货 → 触发退款 + 库存回补 */
    @Transactional
    public void confirmReturn(String operator, String afterSaleNo) {
        AfterSale afterSale = get(afterSaleNo);
        if (!"RETURN_REFUND".equals(afterSale.getType()) || !"REFUNDING".equals(afterSale.getStatus())) {
            throw new ServiceException("当前状态不允许确认收货");
        }
        flow(afterSale.getId(), "商家确认收货", operator, null);
        doRefund(afterSale, operator);
    }

    /** 幂等退款：重复调用返回成功但不重复退款 */
    @Transactional
    public void refund(String operator, String afterSaleNo) {
        AfterSale afterSale = get(afterSaleNo);
        if ("REFUNDED".equals(afterSale.getStatus())) {
            log.info("售后单 {} 已退款，幂等返回", afterSaleNo);
            return;
        }
        if (!"REFUNDING".equals(afterSale.getStatus())) {
            throw new ServiceException("售后单不是退款中状态");
        }
        doRefund(afterSale, operator);
    }

    // ==================== 内部 ====================

    /** 执行退款：售后单→REFUNDED、库存回补（退货退款）、积分按比例扣减、订单回已完成 */
    private void doRefund(AfterSale afterSale, String operator) {
        if ("REFUNDED".equals(afterSale.getStatus()) && afterSale.getRefundNo() != null) {
            return;
        }
        afterSale.setRefundNo(afterSale.getAfterSaleNo() + "-1");
        afterSale.setRefundedAt(LocalDateTime.now());
        afterSale.setStatus("REFUNDED");
        afterSaleRepository.save(afterSale);

        // 退货退款回补库存
        if ("RETURN_REFUND".equals(afterSale.getType()) && afterSale.getSkuId() != null) {
            OrderItem item = findItem(afterSale.getOrderId(), afterSale.getSkuId());
            if (item != null) {
                stockService.refundIn(item.getSkuId(), item.getQuantity(), afterSale.getAfterSaleNo());
            }
        }
        // 积分按比例扣减（余额不足扣至 0）
        MallOrder order = orderService.getOrder(afterSale.getOrderNo());
        Member member = memberService.getById(afterSale.getMemberId());
        int earned = (int) Math.round(order.getPayAmount() / 100.0);
        int deduct = order.getPayAmount() == 0
                ? 0
                : (int) Math.round(earned * (afterSale.getRefundAmount() * 1.0 / order.getPayAmount()));
        if (deduct > 0) {
            memberService.changePoints(member, -deduct, "退款扣减积分", afterSale.getAfterSaleNo());
        }
        restoreOrder(afterSale);
        flow(afterSale.getId(), "退款成功", operator, "退款 " + afterSale.getRefundAmount() + " 分，退款单 " + afterSale.getRefundNo());
        auditService.log("aftersale", "退款", "售后单 " + afterSale.getAfterSaleNo() + " 退款 "
                + afterSale.getRefundAmount() + " 分，退款单 " + afterSale.getRefundNo());
    }

    private void restoreOrder(AfterSale afterSale) {
        if (afterSale.getOrderId() == null) {
            return;
        }
        // 仅当该订单没有其他进行中的售后单时才恢复
        boolean hasActive = afterSaleRepository.findByOrderNo(afterSale.getOrderNo()).stream()
                .anyMatch(a -> !a.getId().equals(afterSale.getId())
                        && ("APPLYING".equals(a.getStatus()) || "REFUNDING".equals(a.getStatus())
                                || "WAIT_RETURN".equals(a.getStatus())));
        if (hasActive) {
            return;
        }
        if ("REJECTED".equals(afterSale.getStatus()) || "CLOSED".equals(afterSale.getStatus())) {
            // 拒绝/撤销回到售后前状态
            if ("AFTER_SALE".equals(orderService.getOrder(afterSale.getOrderNo()).getStatus())) {
                String prev = afterSale.getOrderPrevStatus() == null ? "COMPLETED" : afterSale.getOrderPrevStatus();
                MallOrder order = orderService.getOrder(afterSale.getOrderNo());
                order.setStatus(prev);
                // 通过 orderService 保存
                orderService.saveOrder(order);
            }
        } else {
            orderService.completeAftersale(afterSale.getOrderNo());
        }
    }

    private Integer computeRefundAmount(MallOrder order, Long skuId) {
        if (skuId == null) {
            return order.getPayAmount();
        }
        OrderItem item = findItem(order.getId(), skuId);
        if (item == null) {
            throw new ServiceException("订单中不存在该 SKU");
        }
        return item.getSubtotal();
    }

    private OrderItem findItem(Long orderId, Long skuId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElse(null);
    }

    private void flow(Long afterSaleId, String action, String operator, String remark) {
        AfterSaleFlow node = new AfterSaleFlow();
        node.setAfterSaleId(afterSaleId);
        node.setAction(action);
        node.setOperator(operator);
        node.setRemark(remark);
        node.setCreatedAt(LocalDateTime.now());
        flowRepository.save(node);
    }

    private AfterSale get(String afterSaleNo) {
        return afterSaleRepository
                .findByAfterSaleNo(afterSaleNo)
                .orElseThrow(() -> new ServiceException("售后单不存在"));
    }

    private AfterSale getOwned(Long memberId, String afterSaleNo) {
        AfterSale afterSale = get(afterSaleNo);
        if (!afterSale.getMemberId().equals(memberId)) {
            throw new ServiceException("无权操作该售后单");
        }
        return afterSale;
    }

    private Map<String, Object> rowVo(AfterSale a) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("afterSaleNo", a.getAfterSaleNo());
        row.put("orderNo", a.getOrderNo());
        row.put("type", a.getType());
        row.put("typeText", typeText(a.getType()));
        row.put("status", a.getStatus());
        row.put("statusText", statusText(a.getStatus()));
        row.put("refundAmount", a.getRefundAmount());
        row.put("createdAt", a.getCreatedAt());
        return row;
    }

    private Map<String, Object> detailVo(AfterSale a) {
        Map<String, Object> data = rowVo(a);
        data.put("skuId", a.getSkuId());
        data.put("reason", a.getReason());
        data.put("description", a.getDescription());
        data.put("images", a.getImages() == null ? List.of() : List.of(a.getImages().split("\\|", -1)));
        data.put("opinion", a.getOpinion());
        data.put("returnCompany", a.getReturnCompany());
        data.put("returnNo", a.getReturnNo());
        data.put("refundNo", a.getRefundNo());
        data.put("refundedAt", a.getRefundedAt());
        List<Map<String, Object>> flow = new ArrayList<>();
        for (AfterSaleFlow node : flowRepository.findByAfterSaleIdOrderByCreatedAtAscIdAsc(a.getId())) {
            Map<String, Object> nodeVo = new LinkedHashMap<>();
            nodeVo.put("action", node.getAction());
            nodeVo.put("operator", node.getOperator());
            nodeVo.put("remark", node.getRemark());
            nodeVo.put("time", node.getCreatedAt());
            flow.add(nodeVo);
        }
        data.put("flow", flow);
        return data;
    }
}
