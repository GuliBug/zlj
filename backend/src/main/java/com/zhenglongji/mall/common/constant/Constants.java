package com.zhenglongji.mall.common.constant;

/**
 * 全局常量
 */
public class Constants {

    private Constants() {}

    /** 登录令牌请求头 */
    public static final String TOKEN_HEADER = "X-Token";

    /** 买家令牌前缀 */
    public static final String MEMBER_TOKEN_PREFIX = "MBR-";

    /** 管理员令牌前缀 */
    public static final String ADMIN_TOKEN_PREFIX = "ADM-";

    /** 令牌有效期 7 天 */
    public static final long TOKEN_TTL_MILLIS = 7 * 24 * 3600 * 1000L;

    /** 模拟短信验证码 */
    public static final String SMS_CODE = "123456";

    /** 待支付超时分钟数 */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    /** 快递包邮门槛（分） */
    public static final int FREE_FREIGHT_THRESHOLD = 9900;

    /** 快递运费（分） */
    public static final int EXPRESS_FREIGHT = 1000;

    /** 单笔应付上限（分），即 5000 元 */
    public static final long MAX_PAY_AMOUNT = 500000L;

    /** SKU 默认库存预警阈值 */
    public static final int DEFAULT_THRESHOLD = 50;

    /** 单 SKU 购物车数量上限 */
    public static final int CART_SKU_MAX_QUANTITY = 999;

    /** 购物车 SKU 种类上限 */
    public static final int CART_MAX_KINDS = 100;

    /** 收货地址上限 */
    public static final int ADDRESS_MAX = 20;

    /** 商品详情固定警示语 */
    public static final String WARN_TEXT = "长期过量咀嚼槟榔，有害口腔健康";

    /** 未成年人提示固定文案 */
    public static final String MINOR_TEXT = "不向未成年人销售";
}
