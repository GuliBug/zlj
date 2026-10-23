package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(String status);

    List<Product> findByCategoryIdIn(List<Long> categoryIds);

    /** 成交累计已售：实扣时同步增加（FR-02-6） */
    @Modifying
    @Query("UPDATE Product p SET p.sales = COALESCE(p.sales, 0) + :qty WHERE p.id = :productId")
    int increaseSales(@Param("productId") Long productId, @Param("qty") int qty);
}
