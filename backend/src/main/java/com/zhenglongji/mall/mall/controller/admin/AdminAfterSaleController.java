package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.AuditBody;
import com.zhenglongji.mall.mall.service.AfterSaleService;
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
 * 管理端售后（AC-14）
 */
@RestController
@RequestMapping("/admin-api/aftersales")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminAfterSaleController extends BaseController {

    private final AfterSaleService afterSaleService;

    @GetMapping
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(afterSaleService.adminPage(status, overdue, page, size));
    }

    @GetMapping("/{no}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable String no) {
        return success(afterSaleService.adminDetail(no));
    }

    @PostMapping("/{no}/audit")
    public AjaxResult<Void> audit(@PathVariable String no, @RequestBody AuditBody body) {
        afterSaleService.audit(getAdminName(), no, body.approve(), body.opinion());
        return success();
    }

    @PostMapping("/{no}/confirm-return")
    public AjaxResult<Void> confirmReturn(@PathVariable String no) {
        afterSaleService.confirmReturn(getAdminName(), no);
        return success();
    }

    @PostMapping("/{no}/refund")
    public AjaxResult<Void> refund(@PathVariable String no) {
        afterSaleService.refund(getAdminName(), no);
        return success();
    }
}
