package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.ProductSku;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * SKU 数据访问：库存原子条件更新（防超卖），返回受影响行数
 */
@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    List<ProductSku> findByProductId(Long productId);

    Optional<ProductSku> findByBarcode(String barcode);

    List<ProductSku> findByStatus(String status);

    /** 下单预占：available -= n, reserved += n（可用不足即失败） */
    @Modifying
    @Query("UPDATE ProductSku s SET s.available = s.available - :qty, s.reserved = s.reserved + :qty "
            + "WHERE s.id = :skuId AND s.available >= :qty AND s.status = 'NORMAL'")
    int reserve(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 支付实扣 / 核销实扣：reserved -= n（实物减少） */
    @Modifying
    @Query("UPDATE ProductSku s SET s.reserved = s.reserved - :qty "
            + "WHERE s.id = :skuId AND s.reserved >= :qty")
    int deductReserved(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 取消 / 超时释放：reserved -= n, available += n */
    @Modifying
    @Query("UPDATE ProductSku s SET s.reserved = s.reserved - :qty, s.available = s.available + :qty "
            + "WHERE s.id = :skuId AND s.reserved >= :qty")
    int release(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 退款回补：available += n */
    @Modifying
    @Query("UPDATE ProductSku s SET s.available = s.available + :qty WHERE s.id = :skuId")
    int refundIn(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 盘点修正：按差异调整可用库存（diff 可正可负） */
    @Modifying
    @Query("UPDATE ProductSku s SET s.available = s.available + :diff "
            + "WHERE s.id = :skuId AND s.available + :diff >= 0")
    int adjust(@Param("skuId") Long skuId, @Param("diff") int diff);

    /** 冻结：可用全部转冻结 */
    @Modifying
    @Query("UPDATE ProductSku s SET s.frozen = s.frozen + s.available, s.available = 0, s.status = 'FROZEN' "
            + "WHERE s.id = :skuId AND s.status = 'NORMAL'")
    int freeze(@Param("skuId") Long skuId);

    /** 释放冻结：冻结全部转可用 */
    @Modifying
    @Query("UPDATE ProductSku s SET s.available = s.available + s.frozen, s.frozen = 0, s.status = 'NORMAL' "
            + "WHERE s.id = :skuId AND s.status = 'FROZEN'")
    int unfreeze(@Param("skuId") Long skuId);

    /** 入库 */
    @Modifying
    @Query("UPDATE ProductSku s SET s.available = s.available + :qty WHERE s.id = :skuId AND s.status = 'NORMAL'")
    int inbound(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 成交累计已售：实扣时同步增加（FR-02-6） */
    @Modifying
    @Query("UPDATE ProductSku s SET s.sales = COALESCE(s.sales, 0) + :qty WHERE s.id = :skuId")
    int increaseSales(@Param("skuId") Long skuId, @Param("qty") int qty);
}
