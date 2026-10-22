package com.zhenglongji.mall.framework.security;

import com.zhenglongji.mall.common.exception.ServiceException;

/**
 * 登录上下文工具：由 LoginInterceptor 写入 / 清理
 */
public class LoginHelper {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private LoginHelper() {}

    public static void setLoginUser(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public static LoginUser getLoginUser() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getMemberId() {
        LoginUser user = CONTEXT.get();
        if (user == null || user.getMemberId() == null) {
            throw new ServiceException("请先登录");
        }
        return user.getMemberId();
    }

    public static String getAdminName() {
        LoginUser user = CONTEXT.get();
        if (user == null || !user.isAdmin()) {
            throw new ServiceException("管理员未登录");
        }
        return user.getAdminName();
    }
}
