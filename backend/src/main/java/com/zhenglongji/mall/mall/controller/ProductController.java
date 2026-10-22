package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.service.ProductService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（AC-03）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController extends BaseController {

    private final ProductService productService;

    @GetMapping("/categories")
    public AjaxResult<Object> categories() {
        return success(productService.categoryTree());
    }

    @GetMapping("/products")
    public AjaxResult<Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(productService.buyerList(keyword, categoryId, page, size));
    }

    @GetMapping("/products/{id}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable Long id) {
        return success(productService.detail(id));
    }

    /** 到货提醒登记 */
    @PostMapping("/products/{id}/remind")
    @RequireLogin(LoginType.MEMBER)
    public AjaxResult<String> remind(@PathVariable Long id) {
        productService.saveArrivalRemind(getMemberId(), id);
        return AjaxResult.success("ok", "ok");
    }
}
