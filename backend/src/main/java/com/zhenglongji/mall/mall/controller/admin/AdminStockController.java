package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.Inbound;
import com.zhenglongji.mall.mall.domain.dto.FreezeBody;
import com.zhenglongji.mall.mall.domain.dto.InboundBody;
import com.zhenglongji.mall.mall.domain.dto.StocktakeBody;
import com.zhenglongji.mall.mall.domain.dto.ThresholdBody;
import com.zhenglongji.mall.mall.service.StockService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端库存（AC-11）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminStockController extends BaseController {

    private final StockService stockService;

    @GetMapping("/skus")
    public AjaxResult<TableDataInfo<Map<String, Object>>> skus(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(PageUtils.page(stockService.adminSkuList(keyword), page, size));
    }

    @PostMapping("/stock/inbounds")
    public AjaxResult<Inbound> inbound(@RequestBody InboundBody body) {
        return success(stockService.inbound(
                body.skuId(), body.quantity(), body.batchNo(), body.supplier(), body.remark(), getAdminName()));
    }

    @GetMapping("/stock/inbounds")
    public AjaxResult<TableDataInfo<Inbound>> inbounds(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return success(PageUtils.page(stockService.inboundList(), page, size));
    }

    @PostMapping("/stock/freeze")
    public AjaxResult<Void> freeze(@RequestBody FreezeBody body) {
        stockService.freeze(body.skuId(), body.action(), body.reason());
        return success();
    }

    @PostMapping("/stock/threshold")
    public AjaxResult<Void> threshold(@RequestBody ThresholdBody body) {
        stockService.threshold(body.skuId(), body.threshold());
        return success();
    }

    @GetMapping("/stock/flows")
    public AjaxResult<TableDataInfo<Map<String, Object>>> flows(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(PageUtils.page(stockService.flows(skuId, type), page, size));
    }

    @PostMapping("/stock/stocktakes")
    public AjaxResult<java.util.Map<String, Object>> createStocktake(@RequestBody StocktakeBody body) {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        if (body.items() != null) {
            for (var item : body.items()) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("skuId", item.skuId());
                row.put("actual", item.actual());
                items.add(row);
            }
        }
        Long id = stockService.createStocktake(items, body.remark(), getAdminName()).getId();
        return success(java.util.Map.of("id", id));
    }

    @GetMapping("/stock/stocktakes")
    public AjaxResult<TableDataInfo<Map<String, Object>>> stocktakes(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return success(PageUtils.page(stockService.stocktakeList(), page, size));
    }

    @PostMapping("/stock/stocktakes/{id}/confirm")
    public AjaxResult<Void> confirmStocktake(@PathVariable Long id) {
        stockService.confirmStocktake(id, getAdminName());
        return success();
    }
}
