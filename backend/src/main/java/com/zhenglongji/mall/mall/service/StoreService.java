package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.mall.domain.Store;
import com.zhenglongji.mall.mall.repository.StoreRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 门店服务
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    public List<Store> list() {
        return storeRepository.findAll();
    }

    public Store getById(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new ServiceException("门店不存在"));
    }

    public Store create(Store store) {
        return storeRepository.save(store);
    }

    public Store update(Long id, Store body) {
        Store store = getById(id);
        if (body.getName() != null) {
            store.setName(body.getName());
        }
        if (body.getAddress() != null) {
            store.setAddress(body.getAddress());
        }
        if (body.getPhone() != null) {
            store.setPhone(body.getPhone());
        }
        if (body.getOperatingHours() != null) {
            store.setOperatingHours(body.getOperatingHours());
        }
        if (body.getStatus() != null) {
            store.setStatus(body.getStatus());
        }
        return storeRepository.save(store);
    }
}
