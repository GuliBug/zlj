package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 会员等级规则：按累计实付（分）自动升级
 */
@Data
@Entity
@Table(name = "level_rule")
public class LevelRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String levelCode;

    private String levelName;

    /** 累计实付阈值（分） */
    private Long threshold;
}
