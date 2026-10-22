package com.zhenglongji.mall.mall.domain.dto;

public record InboundBody(Long skuId, Integer quantity, String batchNo, String supplier, String remark) {}
