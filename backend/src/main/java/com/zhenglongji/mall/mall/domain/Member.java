package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 会员：积分（1 积分=1 分）、累计实付、等级（V0~V3 只升不降）
 */
@Data
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String phone;

    /** 可用积分 */
    private Integer points;

    /** 累计实付（分） */
    private Long totalPaid;

    /** 当前等级：V0 / V1 / V2 / V3 */
    private String levelCode;

    private LocalDateTime createdAt;
}
