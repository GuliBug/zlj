package com.zhenglongji.mall.mall.domain.dto;

import java.util.List;

public record ProductBody(
        String name,
        Long categoryId,
        String brand,
        String origin,
        String description,
        String status,
        List<String> images,
        List<String> detailImages,
        List<ProductSkuBody> skus) {}
