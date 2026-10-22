package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Alert;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByIdDesc();

    Optional<Alert> findTopBySkuIdAndType(Long skuId, String type);

    Optional<Alert> findTopBySkuIdAndTypeOrderByIdDesc(Long skuId, String type);

    Optional<Alert> findTopBySkuIdAndTypeAndStatusOrderByIdDesc(Long skuId, String type, String status);
}
