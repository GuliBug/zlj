package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.AfterSaleBody;
import com.zhenglongji.mall.mall.domain.dto.ReturnLogisticsBody;
import com.zhenglongji.mall.mall.service.AfterSaleService;
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
 * 售后接口（AC-08）
 */
@RestController
@RequestMapping("/api/aftersales")
@RequiredArgsConstructor
@RequireLogin(LoginType.MEMBER)
public class AfterSaleController extends BaseController {

    private final AfterSaleService afterSaleService;

    @GetMapping("/reasons")
    public AjaxResult<List<String>> reasons() {
        return success(AfterSaleService.REASONS);
    }

    @PostMapping
    public AjaxResult<Map<String, Object>> apply(@RequestBody AfterSaleBody body) {
        return success(afterSaleService.apply(getMemberId(), body));
    }

    @GetMapping
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return success(afterSaleService.memberPage(getMemberId(), page, size));
    }

    @GetMapping("/{no}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable String no) {
        return success(afterSaleService.memberDetail(getMemberId(), no));
    }

    @PostMapping("/{no}/cancel")
    public AjaxResult<Void> cancel(@PathVariable String no) {
        afterSaleService.cancel(getMemberId(), no);
        return success();
    }

    @PostMapping("/{no}/return-logistics")
    public AjaxResult<Void> returnLogistics(@PathVariable String no, @RequestBody ReturnLogisticsBody body) {
        afterSaleService.returnLogistics(getMemberId(), no, body.company(), body.no());
        return success();
    }
}
