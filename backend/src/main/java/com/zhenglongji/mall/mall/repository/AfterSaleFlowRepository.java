package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.AfterSaleFlow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AfterSaleFlowRepository extends JpaRepository<AfterSaleFlow, Long> {

    List<AfterSaleFlow> findByAfterSaleIdOrderByCreatedAtAscIdAsc(Long afterSaleId);
}
