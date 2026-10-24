package com.zhenglongji.mall.mall.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品（SPU）
 */
@Data
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long categoryId;

    private String brand;

    /** 产地 */
    private String origin;

    private String description;

    /** 主图，逗号分隔（URL 或 dataURL，base64 不含逗号）；大文本存储 */
    @Lob
    private String images;

    /** 详情图，逗号分隔（URL 或 dataURL）；大文本存储 */
    @Lob
    private String detailImages;

    private Integer sales;

    /** ON 上架 / OFF 下架 */
    private String status;

    private LocalDateTime createdAt;
}
