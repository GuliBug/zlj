package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.CartAddBody;
import com.zhenglongji.mall.mall.domain.dto.CartUpdateBody;
import com.zhenglongji.mall.mall.domain.dto.CheckedBody;
import com.zhenglongji.mall.mall.domain.dto.IdsBody;
import com.zhenglongji.mall.mall.service.CartService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口（AC-04）
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@RequireLogin(LoginType.MEMBER)
public class CartController extends BaseController {

    private final CartService cartService;

    @GetMapping
    public AjaxResult<Map<String, Object>> list() {
        return success(cartService.list(getMemberId()));
    }

    @PostMapping
    public AjaxResult<Void> add(@RequestBody CartAddBody body) {
        cartService.add(getMemberId(), body.skuId(), body.quantity());
        return success();
    }

    @PostMapping("/update")
    public AjaxResult<Void> update(@RequestBody CartUpdateBody body) {
        cartService.update(getMemberId(), body.id(), body.quantity(), body.checked());
        return success();
    }

    @PostMapping("/delete")
    public AjaxResult<Void> delete(@RequestBody IdsBody body) {
        cartService.delete(getMemberId(), body.ids());
        return success();
    }

    @PostMapping("/check-all")
    public AjaxResult<Void> checkAll(@RequestBody CheckedBody body) {
        cartService.checkAll(getMemberId(), body.checked());
        return success();
    }

    @PostMapping("/clear-invalid")
    public AjaxResult<Void> clearInvalid() {
        cartService.clearInvalid(getMemberId());
        return success();
    }
}
