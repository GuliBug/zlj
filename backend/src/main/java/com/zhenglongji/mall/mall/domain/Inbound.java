package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 入库单
 */
@Data
@Entity
@Table(name = "inbound")
public class Inbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long skuId;

    private Integer quantity;

    /** 批次号（必填） */
    private String batchNo;

    private String supplier;

    private String remark;

    private String createdBy;

    private LocalDateTime createdAt;
}
