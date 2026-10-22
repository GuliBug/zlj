package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.LevelRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LevelRuleRepository extends JpaRepository<LevelRule, Long> {

    List<LevelRule> findAllByOrderByThresholdAsc();
}
