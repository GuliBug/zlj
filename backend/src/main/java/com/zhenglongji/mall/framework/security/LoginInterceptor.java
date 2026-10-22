package com.zhenglongji.mall.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhenglongji.mall.common.constant.Constants;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import com.zhenglongji.mall.framework.security.annotation.LoginType;
import com.zhenglongji.mall.framework.security.annotation.RequireLogin;
import com.zhenglongji.mall.framework.web.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：只对标注 @RequireLogin 的方法/类生效
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireLogin requireLogin = findAnnotation(handlerMethod);
        if (requireLogin == null) {
            return true;
        }

        String token = request.getHeader(Constants.TOKEN_HEADER);
        if (requireLogin.value() == LoginType.ADMIN) {
            String adminName = tokenService.getAdminName(token);
            if (adminName == null) {
                writeUnauthorized(response, "管理员未登录或登录已过期");
                return false;
            }
            LoginHelper.setLoginUser(LoginUser.admin(adminName));
            return true;
        }

        Long memberId = tokenService.getMemberId(token);
        if (memberId == null) {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        LoginHelper.setLoginUser(LoginUser.member(memberId));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginHelper.clear();
    }

    private RequireLogin findAnnotation(HandlerMethod handlerMethod) {
        RequireLogin annotation = handlerMethod.getMethodAnnotation(RequireLogin.class);
        return annotation != null ? annotation : handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(AjaxResult.unauthorized(message)));
    }
}
