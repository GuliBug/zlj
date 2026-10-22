package com.zhenglongji.mall.common.exception;

import com.zhenglongji.mall.common.constant.HttpStatus;
import com.zhenglongji.mall.common.core.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：HTTP 状态保持 200，由 code 区分成败
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public AjaxResult<Void> handleServiceException(ServiceException e) {
        log.warn("业务异常：{}", e.getMessage());
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleValid(MethodArgumentNotValidException e) {
        var fe = e.getBindingResult().getFieldError();
        return AjaxResult.error(fe == null ? "参数校验失败" : fe.getField() + ": " + fe.getDefaultMessage());
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult<Void> handleBind(BindException e) {
        return AjaxResult.error(
                e.getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .orElse("参数校验失败"));
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return AjaxResult.error(HttpStatus.ERROR, "系统繁忙，请稍后重试");
    }
}
