package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 库存流水（不可删改）。quantity 为实物库存的有符号变化：
 * IN +n / OUT -n / REFUND_IN +n / ADJUST ±diff；RESERVE/RELEASE/FREEZE/UNFREEZE 实物不变记 0。
 * balanceAfter 为变动后的实物库存，日终核对：实物 = Σ流水净和。
 */
@Data
@Entity
@Table(name = "stock_flow")
public class StockFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skuId;

    /** IN / OUT / RESERVE / RELEASE / REFUND_IN / ADJUST / FREEZE / UNFREEZE */
    private String type;

    /** 实物库存变化量（有符号） */
    private Integer quantity;

    /** 变动后实物库存 */
    private Integer balanceAfter;

    /** 关联单号（订单号 / 入库单号 / 盘点单号 / 售后单号） */
    private String relatedNo;

    private String remark;

    private LocalDateTime createdAt;
}
