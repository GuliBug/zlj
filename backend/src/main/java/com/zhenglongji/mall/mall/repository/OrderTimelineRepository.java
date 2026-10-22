package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.OrderTimeline;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, Long> {

    List<OrderTimeline> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
