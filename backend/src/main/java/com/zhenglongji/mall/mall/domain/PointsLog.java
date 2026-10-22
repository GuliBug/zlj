package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 积分流水（全留痕）
 */
@Data
@Entity
@Table(name = "points_log")
public class PointsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    /** 有符号变化量 */
    private Integer change;

    /** 变动后余额 */
    private Integer balanceAfter;

    private String reason;

    private String relatedNo;

    private LocalDateTime createdAt;
}
