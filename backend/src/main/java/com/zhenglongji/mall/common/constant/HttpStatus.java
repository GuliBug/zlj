package com.zhenglongji.mall.common.constant;

/**
 * 返回状态码
 */
public class HttpStatus {

    /** 操作成功 */
    public static final int SUCCESS = 200;

    /** 操作失败 */
    public static final int ERROR = 500;

    /** 未登录或登录已过期 */
    public static final int UNAUTHORIZED = 401;

    private HttpStatus() {}
}
