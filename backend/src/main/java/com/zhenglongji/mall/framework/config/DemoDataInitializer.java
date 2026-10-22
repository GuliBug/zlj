package com.zhenglongji.mall.framework.config;

import com.zhenglongji.mall.mall.domain.AdminUser;
import com.zhenglongji.mall.mall.domain.BannedWord;
import com.zhenglongji.mall.mall.domain.Category;
import com.zhenglongji.mall.mall.domain.LevelRule;
import com.zhenglongji.mall.mall.domain.Product;
import com.zhenglongji.mall.mall.domain.ProductSku;
import com.zhenglongji.mall.mall.domain.StockFlow;
import com.zhenglongji.mall.mall.domain.Store;
import com.zhenglongji.mall.mall.repository.AdminUserRepository;
import com.zhenglongji.mall.mall.repository.BannedWordRepository;
import com.zhenglongji.mall.mall.repository.CategoryRepository;
import com.zhenglongji.mall.mall.repository.LevelRuleRepository;
import com.zhenglongji.mall.mall.repository.ProductRepository;
import com.zhenglongji.mall.mall.repository.ProductSkuRepository;
import com.zhenglongji.mall.mall.repository.StockFlowRepository;
import com.zhenglongji.mall.mall.repository.StoreRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 种子数据（H2 内存库每次重启重建）：门店、两级分类、6 个商品×2-4 SKU、管理员、等级规则、违禁词
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final StockFlowRepository flowRepository;
    private final AdminUserRepository adminUserRepository;
    private final LevelRuleRepository levelRuleRepository;
    private final BannedWordRepository bannedWordRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        // 门店（对齐原型：正隆记 3 家门店）
        Store xiangtan = store("正隆记总号", "湘潭市岳塘区岚霞路 6 号", "1737-321-0303", "09:00-22:00");
        store("正隆记熙春路店", "湘潭市雨湖区熙春路 152 号金荣广场", "1530-732-7888", "09:00-22:00");
        store("正隆记益阳领秀资江店", "益阳市资阳区五一东路领秀资江 9 栋", "1932-573-5988", "09:00-21:30");
        log.info("种子门店就绪，自提门店示例: {}", xiangtan.getName());

        // 档位分类（对齐原型 chips：口粮档/进阶档/待客档/礼盒典藏）
        Category staple = category("口粮档", null);
        Category advance = category("进阶档", null);
        Category guest = category("待客档", null);
        Category gift = category("礼盒典藏", null);

        // 商品 × SKU（对齐原型 6 大商品），最后一个默认下架
        product("三号究 · 正隆记门店同款", staple.getId(), "正隆记", "海南原果 · 湘潭卤制", "老口子的入门口粮 · 门店同款。以正立本，以究待客：海南原果，门店头道工艺，当日到店每日新鲜。",
                new Object[][] {{"三号究", "单包", "ZLJ1001", 3000, 300}, {"三号究", "5包装", "ZLJ1002", 15000, 120}});
        product("六号究 · 老客口粮", staple.getId(), "正隆记", "海南原果 · 湘潭卤制", "自己嚼 · 老客的口粮。精选六号究果，纹路分明。古法慢蒸杀青，老卤浸制，久嚼有味。",
                new Object[][] {{"六号究", "单包", "ZLJ2001", 6600, 128}, {"六号究", "5包装", "ZLJ2002", 33000, 64}});
        product("九号究 · 进阶之选", advance.getId(), "正隆记", "海南原果 · 湘潭卤制", "吃惯了 · 想要更好一口。金纹飞天究，籽长一寸六。四正本纪规矩所选，四等精品之列。",
                new Object[][] {{"九号究", "单包", "ZLJ3001", 9900, 86}, {"九号究", "礼袋装", "ZLJ3002", 29900, 42}});
        product("头道究 · 待客有场合", guest.getId(), "正隆记", "海南原果 · 湘潭卤制", "待客有场合 · 门店头道。头道究 · 1946 年留下的四等精品之首，以究待客。",
                new Object[][] {{"头道究", "单份", "ZLJ4001", 16800, 35}, {"头道究", "双份礼装", "ZLJ4002", 29800, 18}});
        product("正隆究 · 1000果 典藏", gift.getId(), "正隆记", "海南原果 · 湘潭卤制", "典藏 · 千果臻选（预售专享）。1000 果臻选品质，典藏之作，门店品鲜自提优先。",
                new Object[][] {{"正隆究·1000果", "标准装", "ZLJ5001", 50000, 22}});
        // 默认下架商品
        productOff("正隆究礼盒装 · 馈赠典藏", gift.getId(), "正隆记", "海南原果 · 湘潭卤制", "送亲友馈伙伴 · 内附隆记古铜钱。礼盒内附正隆元宝古铜钱一枚、四正本纪一本，一盒尽显心意。",
                new Object[][] {{"正隆究礼盒", "标准版", "ZLJ6001", 128000, 8}});

        // 管理员（演示明文比对）
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setNickname("平台管理员");
        admin.setFailCount(0);
        adminUserRepository.save(admin);

        // 第二管理员：用于盘点单二级确认（提交人≠确认人）
        AdminUser manager = new AdminUser();
        manager.setUsername("manager");
        manager.setPassword("manager123");
        manager.setNickname("库管主管");
        manager.setFailCount(0);
        adminUserRepository.save(manager);

        // 等级规则（累计实付分；500/2000/5000 元对齐原型：银卡掌柜/金卡掌柜/钻卡掌柜）
        level("V0", "普通会员", 0L);
        level("V1", "银卡掌柜", 50000L);
        level("V2", "金卡掌柜", 200000L);
        level("V3", "钻卡掌柜", 500000L);

        // 违禁词库
        for (String word : List.of("提神", "醒脑", "保健", "治疗", "功效", "抗癌", "戒瘾", "疗效", "药用", "康复")) {
            BannedWord bannedWord = new BannedWord();
            bannedWord.setWord(word);
            bannedWordRepository.save(bannedWord);
        }

        log.info("=== 正隆记演示数据就绪：门店 2 家，商品 6 个，管理员 admin/admin123 ===");
    }

    private Store store(String name, String address, String phone, String operatingHours) {
        Store store = new Store();
        store.setName(name);
        store.setAddress(address);
        store.setPhone(phone);
        store.setOperatingHours(operatingHours);
        store.setStatus("OPEN");
        return storeRepository.save(store);
    }

    private Category category(String name, Long parentId) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }

    private void level(String code, String name, long threshold) {
        LevelRule rule = new LevelRule();
        rule.setLevelCode(code);
        rule.setLevelName(name);
        rule.setThreshold(threshold);
        levelRuleRepository.save(rule);
    }

    /** rows: {口味, 规格, 条码, 售价(分), 库存} */
    private void product(String name, Long categoryId, String brand, String origin, String desc, Object[][] rows) {
        seedProduct(name, categoryId, brand, origin, desc, rows, "ON");
    }

    private void productOff(String name, Long categoryId, String brand, String origin, String desc, Object[][] rows) {
        seedProduct(name, categoryId, brand, origin, desc, rows, "OFF");
    }

    private void seedProduct(String name, Long categoryId, String brand, String origin, String desc, Object[][] rows, String status) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryId(categoryId);
        product.setBrand(brand);
        product.setOrigin(origin);
        product.setDescription(desc);
        product.setImages("/img/zhenglongji-" + productSeq() + "-1.jpg,/img/zhenglongji-" + productSeq() + "-2.jpg,/img/zhenglongji-" + productSeq() + "-3.jpg");
        product.setDetailImages("/img/zhenglongji-" + productSeq() + "-detail.jpg");
        product.setSales(0);
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now());
        product = productRepository.save(product);

        for (Object[] row : rows) {
            ProductSku sku = new ProductSku();
            sku.setProductId(product.getId());
            sku.setFlavor((String) row[0]);
            sku.setSpec((String) row[1]);
            sku.setBarcode((String) row[2]);
            sku.setPrice((Integer) row[3]);
            sku.setCostPrice((Integer) row[3] * 6 / 10);
            int stock = (Integer) row[4];
            sku.setAvailable(stock);
            sku.setReserved(0);
            sku.setFrozen(0);
            sku.setThreshold(50);
            sku.setSales(0);
            sku.setStatus("NORMAL");
            sku.setCreatedAt(LocalDateTime.now());
            sku = skuRepository.save(sku);

            // 初始库存写 IN 流水，保证日终核对 账实=Σ流水 成立
            StockFlow flow = new StockFlow();
            flow.setSkuId(sku.getId());
            flow.setType("IN");
            flow.setQuantity(stock);
            flow.setBalanceAfter(stock);
            flow.setRelatedNo("SEED-" + sku.getId());
            flow.setRemark("种子数据期初库存");
            flow.setCreatedAt(LocalDateTime.now());
            flowRepository.save(flow);
        }
    }

    private int productSeq() {
        return (int) (productRepository.count() + 1);
    }
}
