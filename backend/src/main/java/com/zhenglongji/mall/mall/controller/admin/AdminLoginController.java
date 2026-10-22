package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.mall.domain.dto.AdminLoginBody;
import com.zhenglongji.mall.mall.service.AdminService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端登录与看板（AC-09）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
public class AdminLoginController extends BaseController {

    private final AdminService adminService;

    @PostMapping("/login")
    public AjaxResult<Map<String, Object>> login(@RequestBody AdminLoginBody body) {
        return success(adminService.login(body.username(), body.password()));
    }

    @GetMapping("/dashboard")
    @com.zhenglongji.mall.framework.security.annotation.RequireLogin(
            com.zhenglongji.mall.framework.security.annotation.LoginType.ADMIN)
    public AjaxResult<Map<String, Object>> dashboard() {
        return success(adminService.dashboard());
    }
}
