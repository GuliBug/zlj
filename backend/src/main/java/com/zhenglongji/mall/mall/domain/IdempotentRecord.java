package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 下单幂等键（5 分钟窗口内同键返回原订单）
 */
@Data
@Entity
@Table(name = "idempotent_record")
public class IdempotentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(unique = true)
    private String idemKey;

    private String orderNo;

    private LocalDateTime createdAt;
}
