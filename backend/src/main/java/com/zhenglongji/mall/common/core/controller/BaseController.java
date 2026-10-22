package com.zhenglongji.mall.common.core.controller;

import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.LoginHelper;

/**
 * 控制器基类：统一响应封装与登录上下文快捷方法
 */
public class BaseController {

    protected <T> AjaxResult<T> success(T data) {
        return AjaxResult.success(data);
    }

    protected <T> AjaxResult<T> success() {
        return AjaxResult.success();
    }

    protected Long getMemberId() {
        return LoginHelper.getMemberId();
    }

    protected String getAdminName() {
        return LoginHelper.getAdminName();
    }
}
