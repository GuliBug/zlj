package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.mall.domain.Alert;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.repository.AlertRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 告警服务：资损四类 + 库存预警（同 SKU 24h 去重）
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    /** 告警必须在独立事务中落库：业务事务（如重复支付被拒）回滚时告警仍要保留 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void raise(String type, String content, Long skuId) {
        Alert alert = new Alert();
        alert.setType(type);
        alert.setContent(content);
        alert.setSkuId(skuId);
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }

    /** 可用库存低于阈值时登记 STOCK_ALERT，同 SKU 24 小时内不重复推送 */
    public void raiseStockAlertIfLow(ProductSku sku, String productName) {
        if (ProductSku.n(sku.getAvailable()) >= ProductSku.n(sku.getThreshold())) {
            return;
        }
        var last = alertRepository.findTopBySkuIdAndType(sku.getId(), "STOCK_ALERT");
        if (last.isPresent()
                && last.get().getCreatedAt() != null
                && last.get().getCreatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            return;
        }
        raise(
                "STOCK_ALERT",
                "SKU「" + productName + " " + sku.getFlavor() + " " + sku.getSpec() + "」可用库存 "
                        + ProductSku.n(sku.getAvailable()) + " 低于预警阈值 " + ProductSku.n(sku.getThreshold()),
                sku.getId());
    }

    public List<Alert> list() {
        return alertRepository.findAllByOrderByIdDesc();
    }

    public long stockAlertCount() {
        return list().stream().filter(a -> "STOCK_ALERT".equals(a.getType()) && "OPEN".equals(a.getStatus())).count();
    }
}
