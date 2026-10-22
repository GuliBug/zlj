package com.zhenglongji.mall.framework.task;

import com.zhenglongji.mall.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时关单：每 30 秒扫描超过 15 分钟未支付订单 → CLOSED + 释放预占
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void cancelExpiredOrders() {
        int count = orderService.closeExpiredOrders();
        if (count > 0) {
            log.info("[超时关单] 自动关闭超时未支付订单 {} 笔，预占库存已释放", count);
        }
    }
}
