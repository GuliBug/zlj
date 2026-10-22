package com.zhenglongji.mall.common.exception;

import java.io.Serial;

/**
 * 业务异常：库存不足、状态非法、参数校验失败等，由全局异常处理器转成 code=500 响应
 */
public class ServiceException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
