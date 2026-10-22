package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 订单明细（商品快照）
 */
@Data
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long skuId;

    private Long productId;

    private String name;

    private String image;

    private String flavor;

    private String spec;

    /** 成交单价（分） */
    private Integer price;

    private Integer quantity;

    /** 小计（分） */
    private Integer subtotal;
}
