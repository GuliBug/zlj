package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.ArrivalRemind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArrivalRemindRepository extends JpaRepository<ArrivalRemind, Long> {}
