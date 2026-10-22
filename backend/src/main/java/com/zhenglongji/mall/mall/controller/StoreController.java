package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.mall.domain.Store;
import com.zhenglongji.mall.mall.service.StoreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门店接口（AC-02）
 */
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController extends BaseController {

    private final StoreService storeService;

    @GetMapping
    public AjaxResult<List<Store>> list() {
        return success(storeService.list());
    }
}
