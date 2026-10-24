package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.constant.Constants;
import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.mall.domain.CartItem;
import com.zhenglongji.mall.mall.domain.Product;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.repository.CartItemRepository;
import com.zhenglongji.mall.mall.repository.ProductRepository;
import com.zhenglongji.mall.mall.repository.ProductSkuRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车服务：同 SKU 合并、上限、失效标识
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository skuRepository;
    private final ProductRepository productRepository;

    public Map<String, Object> list(Long memberId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int totalQuantity = 0;
        for (CartItem item : cartItemRepository.findByMemberIdOrderByIdAsc(memberId)) {
            ProductSku sku = skuRepository.findById(item.getSkuId()).orElse(null);
            if (sku == null) {
                continue;
            }
            Product product = productRepository.findById(sku.getProductId()).orElse(null);
            boolean invalid = product == null || !"ON".equals(product.getStatus());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("skuId", sku.getId());
            row.put("productId", sku.getProductId());
            row.put("name", product == null ? "未知商品" : product.getName());
            row.put("image", product == null ? null : first(product.getImages()));
            row.put("flavor", sku.getFlavor());
            row.put("spec", sku.getSpec());
            row.put("price", sku.getPrice());
            row.put("quantity", item.getQuantity());
            row.put("stock", ProductSku.n(sku.getAvailable()));
            row.put("checked", Boolean.TRUE.equals(item.getChecked()));
            row.put("invalid", invalid);
            totalQuantity += item.getQuantity() == null ? 0 : item.getQuantity();
            rows.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("totalQuantity", totalQuantity);
        return data;
    }

    @Transactional
    public void add(Long memberId, Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            throw new ServiceException("参数错误");
        }
        ProductSku sku = skuRepository.findById(skuId).orElseThrow(() -> new ServiceException("SKU 不存在"));
        CartItem item = cartItemRepository.findByMemberIdAndSkuId(memberId, skuId).orElse(null);
        if (item == null) {
            if (cartItemRepository.countByMemberId(memberId) >= Constants.CART_MAX_KINDS) {
                throw new ServiceException("购物车已满");
            }
            item = new CartItem();
            item.setMemberId(memberId);
            item.setSkuId(sku.getId());
            item.setQuantity(0);
            item.setChecked(true);
        }
        int qty = item.getQuantity() + quantity;
        if (qty > Constants.CART_SKU_MAX_QUANTITY) {
            throw new ServiceException("单 SKU 最多购买 " + Constants.CART_SKU_MAX_QUANTITY + " 件");
        }
        item.setQuantity(qty);
        cartItemRepository.save(item);
    }

    @Transactional
    public void update(Long memberId, Long id, Integer quantity, Boolean checked) {
        CartItem item = getOwned(memberId, id);
        if (quantity != null) {
            if (quantity <= 0 || quantity > Constants.CART_SKU_MAX_QUANTITY) {
                throw new ServiceException("数量须在 1~" + Constants.CART_SKU_MAX_QUANTITY + " 之间");
            }
            item.setQuantity(quantity);
        }
        if (checked != null) {
            item.setChecked(checked);
        }
        cartItemRepository.save(item);
    }

    @Transactional
    public void delete(Long memberId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            cartItemRepository.delete(getOwned(memberId, id));
        }
    }

    @Transactional
    public void checkAll(Long memberId, Boolean checked) {
        for (CartItem item : cartItemRepository.findByMemberIdOrderByIdAsc(memberId)) {
            item.setChecked(Boolean.TRUE.equals(checked));
            cartItemRepository.save(item);
        }
    }

    /** 清除失效商品（已下架 / SKU 已删除） */
    @Transactional
    public void clearInvalid(Long memberId) {
        for (CartItem item : cartItemRepository.findByMemberIdOrderByIdAsc(memberId)) {
            ProductSku sku = skuRepository.findById(item.getSkuId()).orElse(null);
            Product product = sku == null ? null : productRepository.findById(sku.getProductId()).orElse(null);
            if (product == null || !"ON".equals(product.getStatus())) {
                cartItemRepository.delete(item);
            }
        }
    }

    private CartItem getOwned(Long memberId, Long id) {
        CartItem item = cartItemRepository.findById(id).orElseThrow(() -> new ServiceException("购物车项不存在"));
        if (!item.getMemberId().equals(memberId)) {
            throw new ServiceException("无权操作该购物车项");
        }
        return item;
    }

    private String first(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        return images.split("\\|", -1)[0];
    }
}
