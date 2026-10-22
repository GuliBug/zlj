package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(String status);

    List<Product> findByCategoryIdIn(List<Long> categoryIds);
}
