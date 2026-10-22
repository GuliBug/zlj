package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 售后流转记录（只插不改，双端同源时间线）
 */
@Data
@Entity
@Table(name = "after_sale_flow")
public class AfterSaleFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long afterSaleId;

    private String action;

    private String operator;

    private String remark;

    private LocalDateTime createdAt;
}
