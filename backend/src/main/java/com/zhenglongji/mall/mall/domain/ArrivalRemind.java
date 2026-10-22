package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 到货提醒登记
 */
@Data
@Entity
@Table(name = "arrival_remind")
public class ArrivalRemind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Long productId;

    private LocalDateTime createdAt;
}
