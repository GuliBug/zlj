package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Stocktake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {

    List<Stocktake> findAllByOrderByIdDesc();
}
