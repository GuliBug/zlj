package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {}
