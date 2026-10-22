package com.zhenglongji.mall.mall.controller.admin;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.common.core.page.PageUtils;
import com.zhenglongji.mall.common.core.page.TableDataInfo;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.Store;
import com.zhenglongji.mall.mall.domain.dto.StoreBody;
import com.zhenglongji.mall.mall.domain.dto.VerifyBody;
import com.zhenglongji.mall.mall.service.AdminService;
import com.zhenglongji.mall.mall.service.OrderService;
import com.zhenglongji.mall.mall.service.StoreService;
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
 * 管理端门店与核销（AC-12）
 */
@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
@RequireLogin(LoginType.ADMIN)
public class AdminStoreController extends BaseController {

    private final StoreService storeService;
    private final OrderService orderService;
    private final AdminService adminService;

    @GetMapping("/stores")
    public AjaxResult<List<Store>> list() {
        return success(storeService.list());
    }

    @PostMapping("/stores")
    public AjaxResult<Store> create(@RequestBody StoreBody body) {
        Store store = new Store();
        store.setName(body.name());
        store.setAddress(body.address());
        store.setPhone(body.phone());
        store.setStatus(body.status() == null ? "OPEN" : body.status());
        return success(storeService.create(store));
    }

    @PutMapping("/stores/{id}")
    public AjaxResult<Store> update(@PathVariable Long id, @RequestBody StoreBody body) {
        Store store = new Store();
        store.setName(body.name());
        store.setAddress(body.address());
        store.setPhone(body.phone());
        store.setStatus(body.status());
        return success(storeService.update(id, store));
    }

    @GetMapping("/verify-records")
    public AjaxResult<TableDataInfo<Map<String, Object>>> verifyRecords(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return success(adminService.verifyRecords(storeId, page, size));
    }

    /** 到店核销 */
    @PostMapping("/verify")
    public AjaxResult<Map<String, Object>> verify(@RequestBody VerifyBody body) {
        return success(orderService.verify(getAdminName(), body));
    }
}
