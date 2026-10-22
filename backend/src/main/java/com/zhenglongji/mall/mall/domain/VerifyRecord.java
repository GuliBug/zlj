package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自提核销记录
 */
@Data
@Entity
@Table(name = "verify_record")
public class VerifyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private Long storeId;

    private String storeName;

    /** 核销店员（管理员账号） */
    private String verifier;

    private LocalDateTime verifyTime;

    private Integer payAmount;
}
