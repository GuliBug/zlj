package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 盘点单（二级确认：提交人≠确认人）
 */
@Data
@Entity
@Table(name = "stocktake")
public class Stocktake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PENDING_CONFIRM / CONFIRMED */
    private String status;

    private String remark;

    private String createdBy;

    private LocalDateTime createdAt;

    private String confirmedBy;

    private LocalDateTime confirmedAt;
}
