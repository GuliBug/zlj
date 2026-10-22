package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 资损 / 库存告警
 */
@Data
@Entity
@Table(name = "alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DUPLICATE_PAY / ABNORMAL_AMOUNT / STATUS_ROLLBACK / RECON_DIFF / STOCK_ALERT */
    private String type;

    private String content;

    /** 关联 SKU（库存类告警去重用），可空 */
    private Long skuId;

    /** OPEN / RESOLVED */
    private String status;

    private LocalDateTime createdAt;
}
