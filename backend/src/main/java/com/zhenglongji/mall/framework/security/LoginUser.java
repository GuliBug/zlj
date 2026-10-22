package com.zhenglongji.mall.framework.security;

import lombok.Getter;

/**
 * 当前登录用户：memberId 仅买家有值，adminName 仅管理端有值
 */
@Getter
public class LoginUser {

    private final Long memberId;

    private final String adminName;

    private final boolean admin;

    private LoginUser(Long memberId, String adminName, boolean admin) {
        this.memberId = memberId;
        this.adminName = adminName;
        this.admin = admin;
    }

    public static LoginUser member(Long memberId) {
        return new LoginUser(memberId, null, false);
    }

    public static LoginUser admin(String adminName) {
        return new LoginUser(null, adminName, true);
    }
}
