package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * 购物车（同 SKU 合并累加）
 */
@Data
@Entity
@Table(name = "cart_item", uniqueConstraints = @UniqueConstraint(columnNames = {"memberId", "skuId"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long skuId;

    private Integer quantity;

    private Boolean checked;
}
