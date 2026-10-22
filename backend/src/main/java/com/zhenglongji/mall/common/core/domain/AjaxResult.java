package com.zhenglongji.mall.common.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhenglongji.mall.common.constant.HttpStatus;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 统一响应体 {@code {code, msg, data}}
 */
@Data
public class AjaxResult<T> implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private int code;

    private String msg;

    private T data;

    public AjaxResult() {}

    public AjaxResult(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public AjaxResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> AjaxResult<T> success() {
        return new AjaxResult<>(HttpStatus.SUCCESS, "操作成功");
    }

    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(HttpStatus.SUCCESS, "操作成功", data);
    }

    public static <T> AjaxResult<T> success(String msg, T data) {
        return new AjaxResult<>(HttpStatus.SUCCESS, msg, data);
    }

    public static <T> AjaxResult<T> error(String msg) {
        return new AjaxResult<>(HttpStatus.ERROR, msg);
    }

    public static <T> AjaxResult<T> error(int code, String msg) {
        return new AjaxResult<>(code, msg);
    }

    public static <T> AjaxResult<T> unauthorized(String msg) {
        return new AjaxResult<>(HttpStatus.UNAUTHORIZED, msg);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return code == HttpStatus.SUCCESS;
    }
}
