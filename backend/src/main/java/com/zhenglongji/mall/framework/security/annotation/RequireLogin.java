package com.zhenglongji.mall.framework.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录校验注解，由 LoginInterceptor 校验 X-Token；方法上的注解优先于类上
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireLogin {

    LoginType value() default LoginType.MEMBER;
}
