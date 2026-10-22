package com.zhenglongji.mall.mall.controller;

import com.zhenglongji.mall.common.core.controller.BaseController;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.mall.domain.Address;
import com.zhenglongji.mall.mall.domain.dto.AddressBody;
import com.zhenglongji.mall.mall.service.AddressService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收货地址接口（AC-05）
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@RequireLogin(LoginType.MEMBER)
public class AddressController extends BaseController {

    private final AddressService addressService;

    @GetMapping
    public AjaxResult<List<Address>> list() {
        return success(addressService.list(getMemberId()));
    }

    @PostMapping
    public AjaxResult<Address> create(@RequestBody AddressBody body) {
        Address address = new Address();
        address.setReceiver(body.receiver());
        address.setPhone(body.phone());
        address.setRegion(body.region());
        address.setDetail(body.detail());
        address.setIsDefault(body.isDefault());
        return success(addressService.create(getMemberId(), address));
    }

    @PutMapping("/{id}")
    public AjaxResult<Address> update(@PathVariable Long id, @RequestBody AddressBody body) {
        Address address = new Address();
        address.setReceiver(body.receiver());
        address.setPhone(body.phone());
        address.setRegion(body.region());
        address.setDetail(body.detail());
        address.setIsDefault(body.isDefault());
        return success(addressService.update(getMemberId(), id, address));
    }

    @DeleteMapping("/{id}")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        addressService.delete(getMemberId(), id);
        return success();
    }

    @PostMapping("/{id}/default")
    public AjaxResult<Address> setDefault(@PathVariable Long id) {
        return success(addressService.setDefault(getMemberId(), id));
    }
}
