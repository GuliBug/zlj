package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 售后单。状态机：APPLYING→(审核通过)REFUNDING[仅退款]|WAIT_RETURN[退货退款]→REFUNDED；
 * RETURN_REFUND 买家寄回后 REFUNDING→(商家确认收货+退款)REFUNDED；拒绝→REJECTED；撤销→CLOSED。
 */
@Data
@Entity
@Table(name = "after_sale")
public class AfterSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String afterSaleNo;

    private Long orderId;

    private String orderNo;

    private Long memberId;

    /** 可空：整单售后时为 null */
    private Long skuId;

    /** REFUND_ONLY / RETURN_REFUND */
    private String type;

    /** 质量问题 / 少件漏发 / 发错货 / 不想要了 / 其他 */
    private String reason;

    private String description;

    /** 凭证图，逗号分隔 */
    private String images;

    /** 退款金额（分），服务端计算 */
    private Integer refundAmount;

    /** APPLYING / REFUNDING / WAIT_RETURN / REFUNDED / REJECTED / CLOSED */
    private String status;

    /** 售后前订单状态（COMPLETED/SHIPPED），售后终止后恢复 */
    private String orderPrevStatus;

    /** 审核意见 */
    private String opinion;

    /** 买家寄回物流 */
    private String returnCompany;

    private String returnNo;

    /** 退款序号：售后单号-1 */
    private String refundNo;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;
}
