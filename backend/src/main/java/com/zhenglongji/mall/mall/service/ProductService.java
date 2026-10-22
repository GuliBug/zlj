package com.zhenglongji.mall.mall.service;

import com.zhenglongji.mall.common.exception.ServiceException;
import com.zhenglongji.mall.mall.domain.BannedWord;
import com.zhenglongji.mall.mall.domain.Category;
import com.zhenglongji.mall.mall.domain.Product;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.domain.dto.ProductBody;
import com.zhenglongji.mall.mall.domain.dto.ProductSkuBody;
import com.zhenglongji.mall.mall.repository.BannedWordRepository;
import com.zhenglongji.mall.mall.repository.CategoryRepository;
import com.zhenglongji.mall.mall.repository.ProductRepository;
import com.zhenglongji.mall.mall.repository.ProductSkuRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品服务：浏览 / 搜索 / 上下架 / 违禁词校验 / 审计
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final CategoryRepository categoryRepository;
    private final BannedWordRepository bannedWordRepository;
    private final com.zhenglongji.mall.mall.repository.StockFlowRepository flowRepository;
    private final com.zhenglongji.mall.mall.repository.ArrivalRemindRepository arrivalRemindRepository;
    private final AuditService auditService;
    private final AlertService alertService;

    /** 到货提醒登记（下架商品补货通知） */
    public void saveArrivalRemind(Long memberId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ServiceException("商品不存在"));
        if ("ON".equals(product.getStatus())) {
            throw new ServiceException("商品在售中，无需登记到货提醒");
        }
        com.zhenglongji.mall.mall.domain.ArrivalRemind remind = new com.zhenglongji.mall.mall.domain.ArrivalRemind();
        remind.setMemberId(memberId);
        remind.setProductId(productId);
        remind.setCreatedAt(LocalDateTime.now());
        arrivalRemindRepository.save(remind);
    }

    // ---------- 买家端 ----------

    /** 商品列表（仅上架），keyword 支持名称 / 条码 */
    public Map<String, Object> buyerList(String keyword, Long categoryId, int page, int size) {
        List<Product> products = productRepository.findByStatus("ON");
        if (categoryId != null) {
            List<Long> ids = new ArrayList<>();
            ids.add(categoryId);
            categoryRepository.findByParentId(categoryId).forEach(c -> ids.add(c.getId()));
            products = products.stream().filter(p -> ids.contains(p.getCategoryId())).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            Set<Long> skuMatchProductIds = new HashSet<>();
            skuRepository.findAll().forEach(sku -> {
                if (sku.getBarcode() != null && sku.getBarcode().contains(kw)) {
                    skuMatchProductIds.add(sku.getProductId());
                }
            });
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().contains(kw))
                            || (p.getBrand() != null && p.getBrand().contains(kw))
                            || skuMatchProductIds.contains(p.getId()))
                    .toList();
        }
        products = products.stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList();
        List<Map<String, Object>> rows = products.stream().map(this::buyerRow).toList();
        var table = com.zhenglongji.mall.common.core.page.PageUtils.page(rows, page, size);
        return Map.of("total", table.getTotal(), "rows", table.getRows());
    }

    private Map<String, Object> buyerRow(Product product) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", product.getId());
        row.put("name", product.getName());
        row.put("image", firstImage(product.getImages()));
        List<ProductSku> skus = skuRepository.findByProductId(product.getId());
        row.put("priceMin", skus.stream().mapToInt(ProductSku::getPrice).min().orElse(0));
        row.put("priceMax", skus.stream().mapToInt(ProductSku::getPrice).max().orElse(0));
        row.put("sales", product.getSales());
        row.put("status", product.getStatus());
        return row;
    }

    /** 商品详情（下架可看但禁止下单） */
    public Map<String, Object> detail(Long id) {
        Product product =
                productRepository.findById(id).orElseThrow(() -> new ServiceException("商品不存在"));
        Category category = product.getCategoryId() == null
                ? null
                : categoryRepository.findById(product.getCategoryId()).orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("brand", product.getBrand());
        data.put("origin", product.getOrigin());
        data.put("description", product.getDescription());
        data.put("images", split(product.getImages()));
        data.put("detailImages", split(product.getDetailImages()));
        data.put("categoryId", product.getCategoryId());
        data.put("categoryName", category == null ? null : category.getName());
        data.put("status", product.getStatus());
        data.put("warnText", com.zhenglongji.mall.common.constant.Constants.WARN_TEXT);
        data.put("minorText", com.zhenglongji.mall.common.constant.Constants.MINOR_TEXT);
        data.put("sales", product.getSales());
        List<Map<String, Object>> skus = new ArrayList<>();
        for (ProductSku sku : skuRepository.findByProductId(product.getId())) {
            Map<String, Object> skuRow = new LinkedHashMap<>();
            skuRow.put("id", sku.getId());
            skuRow.put("flavor", sku.getFlavor());
            skuRow.put("spec", sku.getSpec());
            skuRow.put("barcode", sku.getBarcode());
            skuRow.put("price", sku.getPrice());
            skuRow.put("stock", ProductSku.n(sku.getAvailable()));
            skuRow.put("sales", sku.getSales());
            skus.add(skuRow);
        }
        data.put("skus", skus);
        return data;
    }

    // ---------- 管理端 ----------

    public List<Map<String, Object>> adminList(String keyword, String status, Long categoryId) {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .filter(p -> status == null || status.isBlank() || status.equals(p.getStatus()))
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .filter(p -> keyword == null
                        || keyword.isBlank()
                        || (p.getName() != null && p.getName().contains(keyword.trim())))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(p -> {
                    Map<String, Object> row = buyerRow(p);
                    row.put("brand", p.getBrand());
                    row.put("origin", p.getOrigin());
                    row.put("description", p.getDescription());
                    row.put("images", split(p.getImages()));
                    row.put("detailImages", split(p.getDetailImages()));
                    row.put("categoryId", p.getCategoryId());
                    Category category =
                            p.getCategoryId() == null ? null : categoryRepository.findById(p.getCategoryId()).orElse(null);
                    row.put("categoryName", category == null ? null : category.getName());
                    List<Map<String, Object>> skus = new ArrayList<>();
                    for (ProductSku sku : skuRepository.findByProductId(p.getId())) {
                        Map<String, Object> skuRow = new LinkedHashMap<>();
                        skuRow.put("id", sku.getId());
                        skuRow.put("flavor", sku.getFlavor());
                        skuRow.put("spec", sku.getSpec());
                        skuRow.put("barcode", sku.getBarcode());
                        skuRow.put("price", sku.getPrice());
                        skuRow.put("costPrice", sku.getCostPrice());
                        skuRow.put("stock", ProductSku.n(sku.getAvailable()));
                        skuRow.put("available", ProductSku.n(sku.getAvailable()));
                        skuRow.put("reserved", ProductSku.n(sku.getReserved()));
                        skuRow.put("frozen", ProductSku.n(sku.getFrozen()));
                        skus.add(skuRow);
                    }
                    row.put("skus", skus);
                    return row;
                })
                .toList();
    }

    @Transactional
    public Product create(ProductBody body) {
        checkBannedWordsThrow(body.name(), body.description());
        if (body.name() == null || body.name().isBlank()) {
            throw new ServiceException("商品名称不能为空");
        }
        if (body.categoryId() == null) {
            throw new ServiceException("分类不能为空");
        }
        Product product = new Product();
        product.setName(body.name());
        product.setCategoryId(body.categoryId());
        product.setBrand(body.brand());
        product.setOrigin(body.origin());
        product.setDescription(body.description());
        product.setImages(join(body.images()));
        product.setDetailImages(join(body.detailImages()));
        product.setSales(0);
        product.setStatus("OFF");
        product.setCreatedAt(LocalDateTime.now());
        product = productRepository.save(product);
        saveSkus(product, body.skus());
        auditService.log("product", "新增商品", "商品[" + product.getId() + "] " + product.getName());
        return product;
    }

    @Transactional
    public Product update(Long id, ProductBody body) {
        Product product =
                productRepository.findById(id).orElseThrow(() -> new ServiceException("商品不存在"));
        checkBannedWordsThrow(body.name(), body.description());
        StringBuilder detail = new StringBuilder("商品[" + id + "] 变更:");
        if (body.name() != null && !body.name().equals(product.getName())) {
            detail.append(" 名称:").append(product.getName()).append("->").append(body.name()).append(";");
            product.setName(body.name());
        }
        if (body.categoryId() != null) {
            product.setCategoryId(body.categoryId());
        }
        if (body.brand() != null) {
            product.setBrand(body.brand());
        }
        if (body.origin() != null) {
            product.setOrigin(body.origin());
        }
        if (body.description() != null) {
            product.setDescription(body.description());
        }
        if (body.images() != null) {
            product.setImages(join(body.images()));
        }
        if (body.detailImages() != null) {
            product.setDetailImages(join(body.detailImages()));
        }
        productRepository.save(product);
        if (body.skus() != null && !body.skus().isEmpty()) {
            applySkuChanges(product, body.skus(), detail);
        }
        auditService.log("product", "编辑商品", detail.toString());
        return product;
    }

    private void applySkuChanges(Product product, List<ProductSkuBody> skus, StringBuilder detail) {
        for (ProductSkuBody body : skus) {
            ProductSku sku = body.barcode() == null ? null : skuRepository.findByBarcode(body.barcode()).orElse(null);
            boolean isNew = sku == null || !sku.getProductId().equals(product.getId());
            if (isNew) {
                if (body.barcode() != null && sku != null) {
                    throw new ServiceException("条码已存在: " + body.barcode());
                }
                sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setBarcode(body.barcode());
                sku.setAvailable(0);
                sku.setReserved(0);
                sku.setFrozen(0);
                sku.setThreshold(com.zhenglongji.mall.common.constant.Constants.DEFAULT_THRESHOLD);
                sku.setSales(0);
                sku.setStatus("NORMAL");
                sku.setCreatedAt(LocalDateTime.now());
                if (body.stock() != null && body.stock() > 0) {
                    sku.setAvailable(body.stock());
                }
            }
            if (body.price() != null && !body.price().equals(sku.getPrice())) {
                detail.append(" SKU[").append(sku.getId()).append("] 价格:")
                        .append(sku.getPrice()).append("->").append(body.price()).append(";");
                sku.setPrice(body.price());
            }
            if (body.costPrice() != null) {
                sku.setCostPrice(body.costPrice());
            }
            if (body.flavor() != null) {
                sku.setFlavor(body.flavor());
            }
            if (body.spec() != null) {
                sku.setSpec(body.spec());
            }
            sku = skuRepository.save(sku);
            if (isNew && ProductSku.n(sku.getAvailable()) > 0) {
                com.zhenglongji.mall.mall.domain.StockFlow flow = new com.zhenglongji.mall.mall.domain.StockFlow();
                flow.setSkuId(sku.getId());
                flow.setType("IN");
                flow.setQuantity(sku.getAvailable());
                flow.setBalanceAfter(sku.getAvailable());
                flow.setRelatedNo("INIT-" + sku.getId());
                flow.setRemark("编辑商品新增 SKU 期初库存");
                flow.setCreatedAt(LocalDateTime.now());
                flowRepository.save(flow);
            }
        }
    }

    /** 批量上下架（上架前校验违禁词） */
    @Transactional
    public void updateStatus(List<Long> ids, String status) {
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new ServiceException("状态非法");
        }
        for (Long id : ids) {
            Product product = productRepository
                    .findById(id)
                    .orElseThrow(() -> new ServiceException("商品不存在: " + id));
            if ("ON".equals(status)) {
                checkBannedWordsThrow(product.getName(), product.getDescription());
            }
            String old = product.getStatus();
            product.setStatus(status);
            productRepository.save(product);
            auditService.log("product", "ON".equals(status) ? "上架商品" : "下架商品", "商品[" + id + "] " + old + "->" + status);
        }
    }

    public List<Map<String, Object>> categoryTree() {
        List<Category> all = categoryRepository.findAll();
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() != null) {
                continue;
            }
            Map<String, Object> node = categoryNode(c);
            List<Map<String, Object>> children = new ArrayList<>();
            for (Category child : all) {
                if (c.getId().equals(child.getParentId())) {
                    children.add(categoryNode(child));
                }
            }
            node.put("children", children);
            tree.add(node);
        }
        return tree;
    }

    private Map<String, Object> categoryNode(Category c) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", c.getId());
        node.put("name", c.getName());
        node.put("parentId", c.getParentId());
        return node;
    }

    @Transactional
    public Category createCategory(String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new ServiceException("分类名称不能为空");
        }
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    // ---------- 违禁词 ----------

    public List<String> checkBannedWords(String text) {
        List<String> hits = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return hits;
        }
        for (BannedWord word : bannedWordRepository.findAll()) {
            if (text.contains(word.getWord())) {
                hits.add(word.getWord());
            }
        }
        return hits;
    }

    public void checkBannedWordsThrow(String... texts) {
        List<String> hits = new ArrayList<>();
        for (String text : texts) {
            hits.addAll(checkBannedWords(text));
        }
        if (!hits.isEmpty()) {
            throw new ServiceException("内容命中违禁词: " + String.join("、", hits.stream().distinct().toList()));
        }
    }

    // ---------- helpers ----------

    @Transactional
    public void saveSkus(Product product, List<ProductSkuBody> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new ServiceException("SKU 列表不能为空");
        }
        for (ProductSkuBody body : skus) {
            if (body.barcode() != null && skuRepository.findByBarcode(body.barcode()).isPresent()) {
                throw new ServiceException("条码已存在: " + body.barcode());
            }
            if (body.price() == null || body.price() <= 0) {
                throw new ServiceException("售价必须大于 0");
            }
            if (body.stock() == null || body.stock() < 0) {
                throw new ServiceException("库存必须大于等于 0");
            }
            ProductSku sku = new ProductSku();
            sku.setProductId(product.getId());
            sku.setFlavor(body.flavor());
            sku.setSpec(body.spec());
            sku.setBarcode(body.barcode());
            sku.setPrice(body.price());
            sku.setCostPrice(body.costPrice());
            sku.setAvailable(body.stock());
            sku.setReserved(0);
            sku.setFrozen(0);
            sku.setThreshold(com.zhenglongji.mall.common.constant.Constants.DEFAULT_THRESHOLD);
            sku.setSales(0);
            sku.setStatus("NORMAL");
            sku.setCreatedAt(LocalDateTime.now());
            sku = skuRepository.save(sku);
            // 期初库存写 IN 流水，保证 实物=Σ流水净和
            if (body.stock() > 0) {
                com.zhenglongji.mall.mall.domain.StockFlow flow = new com.zhenglongji.mall.mall.domain.StockFlow();
                flow.setSkuId(sku.getId());
                flow.setType("IN");
                flow.setQuantity(body.stock());
                flow.setBalanceAfter(body.stock());
                flow.setRelatedNo("INIT-" + sku.getId());
                flow.setRemark("新建商品期初库存");
                flow.setCreatedAt(LocalDateTime.now());
                flowRepository.save(flow);
            }
        }
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ServiceException("商品不存在"));
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    private String firstImage(String images) {
        List<String> list = split(images);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<String> split(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return List.of(joined.split(","));
    }

    private String join(List<String> images) {
        return images == null ? null : String.join(",", images);
    }
}
