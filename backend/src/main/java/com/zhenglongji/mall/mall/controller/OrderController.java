package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.OrderBody;
import com.zhenglongji.mall.mall.service.OrderService;
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
 * 订单接口（AC-06 / AC-07）
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@RequireLogin(LoginType.MEMBER)
public class OrderController extends BaseController {

    private final OrderService orderService;

    @PostMapping
    public AjaxResult<Map<String, Object>> create(@RequestBody OrderBody body) {
        return success(orderService.create(getMemberId(), body));
    }

    @GetMapping
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(orderService.memberPage(getMemberId(), status, page, size));
    }

    @GetMapping("/{no}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable String no) {
        return success(orderService.detail(getMemberId(), no));
    }

    @PostMapping("/{no}/pay")
    public AjaxResult<Map<String, Object>> pay(@PathVariable String no) {
        return success(orderService.pay(getMemberId(), no));
    }

    @PostMapping("/{no}/cancel")
    public AjaxResult<Void> cancel(@PathVariable String no) {
        orderService.cancel(getMemberId(), no);
        return success();
    }

    @PostMapping("/{no}/confirm")
    public AjaxResult<Void> confirm(@PathVariable String no) {
        orderService.confirm(getMemberId(), no);
        return success();
    }

    /** 自提核销码 */
    @GetMapping("/{no}/verify-code")
    public AjaxResult<Map<String, Object>> verifyCode(@PathVariable String no) {
        return success(orderService.verifyCode(no));
    }
}
