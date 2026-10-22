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
 * 订单。状态机：WAIT_PAY→PAID→SHIPPED→COMPLETED；PAID→PICKUP_WAIT→COMPLETED（自提）；
 * WAIT_PAY→CLOSED；COMPLETED↔AFTER_SALE。非法流转抛 ServiceException。
 */
@Data
@Entity
@Table(name = "mall_order")
public class MallOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNo;

    private Long memberId;

    /** EXPRESS / PICKUP */
    private String fulfillmentType;

    /** 自提门店 ID（PICKUP 必填） */
    private Long storeId;

    /** 收货地址 ID（EXPRESS 快照来源） */
    private Long addressId;

    /** 收货信息快照 */
    private String receiver;

    private String receiverPhone;

    private String address;

    /** 商品合计（分） */
    private Integer goodsAmount;

    /** 运费（分） */
    private Integer freight;

    /** 积分抵扣金额（分） */
    private Integer pointsDeduction;

    /** 使用的积分个数 */
    private Integer pointsUsed;

    /** 应付（分） */
    private Integer payAmount;

    /** WAIT_PAY / PAID / SHIPPED / PICKUP_WAIT / COMPLETED / CLOSED / AFTER_SALE */
    private String status;

    /** 支付方式：SIM 模拟通道 */
    private String payMethod;

    private String logisticsCompany;

    private String logisticsNo;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime completeTime;

    private LocalDateTime closeTime;

    private LocalDateTime payDeadline;

    private LocalDateTime createdAt;
}
