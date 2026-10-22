package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Inbound;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundRepository extends JpaRepository<Inbound, Long> {

    List<Inbound> findAllByOrderByIdDesc();
}
