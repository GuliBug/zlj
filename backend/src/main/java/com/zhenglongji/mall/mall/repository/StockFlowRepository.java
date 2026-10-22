package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.StockFlow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockFlowRepository extends JpaRepository<StockFlow, Long> {

    List<StockFlow> findBySkuIdOrderByIdAsc(Long skuId);

    /** 全部流水净和（实物口径） */
    @Query("SELECT COALESCE(SUM(f.quantity), 0) FROM StockFlow f WHERE f.skuId = :skuId")
    int sumQuantityBySkuId(@Param("skuId") Long skuId);
}
