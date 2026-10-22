package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.AfterSale;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AfterSaleRepository extends JpaRepository<AfterSale, Long> {

    Optional<AfterSale> findByAfterSaleNo(String afterSaleNo);

    List<AfterSale> findByOrderNo(String orderNo);

    long countByOrderNoAndSkuId(String orderNo, Long skuId);
}
