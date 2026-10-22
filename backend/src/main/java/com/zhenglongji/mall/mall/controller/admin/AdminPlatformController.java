package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.service.AdminService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端审计与监控（AC-16）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminPlatformController extends BaseController {

    private final AdminService adminService;

    @GetMapping("/audit-logs")
    public AjaxResult<TableDataInfo<Map<String, Object>>> auditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(adminService.auditLogs(module, page, size));
    }

    @GetMapping("/alerts")
    public AjaxResult<TableDataInfo<Map<String, Object>>> alerts(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return success(adminService.alerts(page, size));
    }
}
