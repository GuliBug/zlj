package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.PointsLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointsLogRepository extends JpaRepository<PointsLog, Long> {

    List<PointsLog> findByMemberIdOrderByIdDesc(Long memberId);
}
