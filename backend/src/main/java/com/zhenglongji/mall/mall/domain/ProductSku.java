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
 * SKU（口味 × 规格），库存三维口径：实物 = 可用 + 预占 + 冻结
 */
@Data
@Entity
@Table(name = "product_sku")
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    /** 口味：原味 / 枸杞 / 薄荷 等 */
    private String flavor;

    /** 规格：单包 / 整条5包 等 */
    private String spec;

    @Column(unique = true)
    private String barcode;

    /** 售价（分） */
    private Integer price;

    /** 成本价（分） */
    private Integer costPrice;

    /** 可用库存 */
    private Integer available;

    /** 预占库存（下单占用） */
    private Integer reserved;

    /** 冻结库存 */
    private Integer frozen;

    /** 库存预警阈值 */
    private Integer threshold;

    private Integer sales;

    /** NORMAL / FROZEN */
    private String status;

    private LocalDateTime createdAt;

    public int physical() {
        return n(available) + n(reserved) + n(frozen);
    }

    public static int n(Integer v) {
        return v == null ? 0 : v;
    }
}
