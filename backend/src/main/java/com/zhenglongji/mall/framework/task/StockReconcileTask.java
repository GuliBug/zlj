package com.zhenglongji.mall.framework.task;

import com.zhenglongji.mall.mall.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日终库存核对（演示：启动后每 5 分钟执行一次）：实物 ≠ Σ流水净和 → RECON_DIFF 告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReconcileTask {

    private final StockService stockService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        int diff = stockService.reconcile();
        if (diff > 0) {
            log.warn("[库存核对] 发现 {} 个 SKU 账实不符，已登记 RECON_DIFF 告警", diff);
        } else {
            log.info("[库存核对] 全部 SKU 账实相符");
        }
    }
}
