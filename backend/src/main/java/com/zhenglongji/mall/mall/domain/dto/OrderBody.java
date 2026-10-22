package com.zhenglongji.mall.mall.domain.dto;

import java.util.List;

/**
 * 下单请求
 */
public record OrderBody(
        String idempotentKey,
        String fulfillmentType,
        Long storeId,
        Long addressId,
        Boolean usePoints,
        List<OrderItemBody> items) {}
