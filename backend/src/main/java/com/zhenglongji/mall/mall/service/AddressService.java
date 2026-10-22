package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.constant.Constants;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.mall.domain.Address;
import com.zhenglongji.mall.mall.repository.AddressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收货地址服务
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<Address> list(Long memberId) {
        return addressRepository.findByMemberIdOrderByIdDesc(memberId);
    }

    public Address getById(Long memberId, Long id) {
        Address address =
                addressRepository.findById(id).orElseThrow(() -> new ServiceException("地址不存在"));
        if (!address.getMemberId().equals(memberId)) {
            throw new ServiceException("无权操作该地址");
        }
        return address;
    }

    @Transactional
    public Address create(Long memberId, Address body) {
        validate(body, true);
        if (addressRepository.findByMemberIdOrderByIdDesc(memberId).size() >= Constants.ADDRESS_MAX) {
            throw new ServiceException("收货地址最多 " + Constants.ADDRESS_MAX + " 条");
        }
        Address address = new Address();
        address.setMemberId(memberId);
        address.setReceiver(body.getReceiver());
        address.setPhone(body.getPhone());
        address.setRegion(body.getRegion());
        address.setDetail(body.getDetail());
        address.setIsDefault(Boolean.TRUE.equals(body.getIsDefault()));
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            clearDefault(memberId);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public Address update(Long memberId, Long id, Address body) {
        validate(body, false);
        Address address = getById(memberId, id);
        if (body.getReceiver() != null) {
            address.setReceiver(body.getReceiver());
        }
        if (body.getPhone() != null) {
            address.setPhone(body.getPhone());
        }
        if (body.getRegion() != null) {
            address.setRegion(body.getRegion());
        }
        if (body.getDetail() != null) {
            address.setDetail(body.getDetail());
        }
        if (body.getIsDefault() != null) {
            address.setIsDefault(body.getIsDefault());
            if (body.getIsDefault()) {
                clearDefault(memberId);
            }
        }
        return addressRepository.save(address);
    }

    public void delete(Long memberId, Long id) {
        addressRepository.delete(getById(memberId, id));
    }

    @Transactional
    public Address setDefault(Long memberId, Long id) {
        Address address = getById(memberId, id);
        clearDefault(memberId);
        address.setIsDefault(true);
        return addressRepository.save(address);
    }

    private void clearDefault(Long memberId) {
        addressRepository.findByMemberIdOrderByIdDesc(memberId).forEach(a -> {
            if (Boolean.TRUE.equals(a.getIsDefault())) {
                a.setIsDefault(false);
                addressRepository.save(a);
            }
        });
    }

    /** 地址字段校验：手机号 11 位、必填项非空（FR-08-1） */
    private void validate(Address body, boolean requireAll) {
        if (body == null) {
            throw new ServiceException("地址信息不能为空");
        }
        if (requireAll || body.getReceiver() != null) {
            if (body.getReceiver() == null || body.getReceiver().isBlank()) {
                throw new ServiceException("收货人不能为空");
            }
        }
        if (requireAll || body.getPhone() != null) {
            if (body.getPhone() == null || !body.getPhone().matches("^1\\d{10}$")) {
                throw new ServiceException("手机号格式不正确（11 位，1 开头）");
            }
        }
        if (requireAll || body.getRegion() != null) {
            if (body.getRegion() == null || body.getRegion().isBlank()) {
                throw new ServiceException("所在地区不能为空");
            }
        }
        if (requireAll || body.getDetail() != null) {
            if (body.getDetail() == null || body.getDetail().isBlank()) {
                throw new ServiceException("详细地址不能为空");
            }
        }
    }
}
