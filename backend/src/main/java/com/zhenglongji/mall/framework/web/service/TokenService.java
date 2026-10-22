package com.zhenglongji.mall.framework.web.service;

import com.zhenglongji.mall.common.constant.Constants;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 令牌服务：内存 token → 主体映射，7 天有效，重启即失效
 */
@Slf4j
@Service
public class TokenService {

    private record TokenInfo(Object principal, long expireAt) {}

    private final Map<String, TokenInfo> memberTokens = new ConcurrentHashMap<>();

    private final Map<String, TokenInfo> adminTokens = new ConcurrentHashMap<>();

    public String createMemberToken(Long memberId) {
        String token = Constants.MEMBER_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
        memberTokens.put(token, new TokenInfo(memberId, System.currentTimeMillis() + Constants.TOKEN_TTL_MILLIS));
        return token;
    }

    public Long getMemberId(String token) {
        TokenInfo info = valid(memberTokens, token, Constants.MEMBER_TOKEN_PREFIX);
        return info == null ? null : (Long) info.principal();
    }

    public String createAdminToken(String username) {
        String token = Constants.ADMIN_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
        adminTokens.put(token, new TokenInfo(username, System.currentTimeMillis() + Constants.TOKEN_TTL_MILLIS));
        return token;
    }

    public String getAdminName(String token) {
        TokenInfo info = valid(adminTokens, token, Constants.ADMIN_TOKEN_PREFIX);
        return info == null ? null : (String) info.principal();
    }

    private TokenInfo valid(Map<String, TokenInfo> store, String token, String prefix) {
        if (token == null || !token.startsWith(prefix)) {
            return null;
        }
        TokenInfo info = store.get(token);
        if (info == null) {
            return null;
        }
        if (info.expireAt() < System.currentTimeMillis()) {
            store.remove(token);
            return null;
        }
        return info;
    }
}
