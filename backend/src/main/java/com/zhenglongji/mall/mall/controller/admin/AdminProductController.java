package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.dto.CategoryBody;
import com.zhenglongji.mall.mall.domain.dto.ProductBody;
import com.zhenglongji.mall.mall.domain.dto.ProductStatusBody;
import com.zhenglongji.mall.mall.domain.dto.TextBody;
import com.zhenglongji.mall.mall.service.ProductService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端商品与分类（AC-10）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminProductController extends BaseController {

    private final ProductService productService;

    @GetMapping("/products")
    public AjaxResult<TableDataInfo<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(PageUtils.page(productService.adminList(keyword, status, categoryId), page, size));
    }

    @GetMapping("/products/{id}")
    public AjaxResult<Map<String, Object>> detail(@PathVariable Long id) {
        return success(productService.detail(id));
    }

    @PostMapping("/products")
    public AjaxResult<Map<String, Object>> create(@RequestBody ProductBody body) {
        return success(Map.of("id", productService.create(body).getId()));
    }

    @PutMapping("/products/{id}")
    public AjaxResult<Map<String, Object>> update(@PathVariable Long id, @RequestBody ProductBody body) {
        productService.update(id, body);
        return success(Map.of("id", id));
    }

    @PostMapping("/products/status")
    public AjaxResult<Void> updateStatus(@RequestBody ProductStatusBody body) {
        productService.updateStatus(body.ids(), body.status());
        return success();
    }

    @PostMapping("/check-banned-words")
    public AjaxResult<Map<String, Object>> checkBannedWords(@RequestBody TextBody body) {
        return success(Map.of("hits", productService.checkBannedWords(body.text())));
    }

    @GetMapping("/categories")
    public AjaxResult<Object> categories() {
        return success(productService.categoryTree());
    }

    @PostMapping("/categories")
    public AjaxResult<Void> createCategory(@RequestBody CategoryBody body) {
        productService.createCategory(body.name(), body.parentId());
        return success();
    }
}
