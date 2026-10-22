package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.MallOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<MallOrder, Long> {

    Optional<MallOrder> findByOrderNo(String orderNo);

    /** 超时未支付订单（定时关单） */
    List<MallOrder> findByStatusAndPayDeadlineBefore(String status, LocalDateTime time);

    List<MallOrder> findByMemberIdOrderByIdDesc(Long memberId);

    List<MallOrder> findByMemberIdAndStatusOrderByIdDesc(Long memberId, String status);
}
