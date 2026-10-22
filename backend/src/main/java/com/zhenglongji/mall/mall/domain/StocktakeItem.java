package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 盘点明细
 */
@Data
@Entity
@Table(name = "stocktake_item")
public class StocktakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long stocktakeId;

    private Long skuId;

    /** 系统账面数（实物口径） */
    private Integer systemCount;

    /** 实盘数 */
    private Integer actual;

    /** 差异 = 实盘 - 账面 */
    private Integer diff;
}
