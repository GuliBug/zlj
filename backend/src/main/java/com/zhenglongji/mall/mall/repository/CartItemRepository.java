package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByMemberIdOrderByIdAsc(Long memberId);

    Optional<CartItem> findByMemberIdAndSkuId(Long memberId, Long skuId);

    long countByMemberId(Long memberId);
}
