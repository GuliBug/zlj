package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.StocktakeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {

    List<StocktakeItem> findByStocktakeId(Long stocktakeId);
}
