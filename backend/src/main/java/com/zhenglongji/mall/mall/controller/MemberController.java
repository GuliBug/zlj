package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.MemberLoginBody;
import com.zhenglongji.mall.mall.service.MemberService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员接口（AC-01）
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController extends BaseController {

    private final MemberService memberService;

    @PostMapping("/login")
    public AjaxResult<Map<String, Object>> login(@RequestBody MemberLoginBody body) {
        return success(memberService.login(body.phone(), body.smsCode()));
    }

    @GetMapping("/me")
    @RequireLogin(LoginType.MEMBER)
    public AjaxResult<Map<String, Object>> me() {
        return success(memberService.me(getMemberId()));
    }

    @GetMapping("/points/logs")
    @RequireLogin(LoginType.MEMBER)
    public AjaxResult<com.zhenglongji.mall.common.core.page.TableDataInfo<Map<String, Object>>> pointsLogs(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return success(memberService.pointsLogs(getMemberId(), page, size));
    }
}
