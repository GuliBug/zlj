package com.zhenglongji.mall.mall.domain.dto;

import java.util.List;

public record AfterSaleBody(
        String orderNo, Long skuId, String type, String reason, String description, List<String> images) {}
