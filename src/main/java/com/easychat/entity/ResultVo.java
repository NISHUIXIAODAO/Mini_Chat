package com.easychat.entity;

import com.easychat.enums.ErrorCodeEnum;
import lombok.Data;

@Data
public class ResultVo<T> {

    private Integer code;

    private String message;

    private T data;


    protected ResultVo(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //定义好的异常返回
    public static <T> ResultVo<T> success() {
        return new ResultVo(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMsg(), null);
    }
    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> success(T data) {
        return new ResultVo(ErrorCodeEnum.SUCCESS.getCode(), "success", data);
    }
    public static <T> ResultVo<T> success(String message) {
        return new ResultVo(ErrorCodeEnum.SUCCESS.getCode(), message, null);
    }
    //定义好的异常返回
    public static <T> ResultVo<T> failed() {
        return new ResultVo(ErrorCodeEnum.FAILED.getCode(), ErrorCodeEnum.FAILED.getMsg(), null);
    }

    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> failed(String message) {
        return new ResultVo(ErrorCodeEnum.FAILED.getCode(), message, null);
    }

    public static <T> ResultVo<T> failed(T data) {
        return new ResultVo(ErrorCodeEnum.FAILED.getCode(), "failed", null);
    }


    //定义好的异常返回
    public static <T> ResultVo<T> unauthorized() {
        return new ResultVo(ErrorCodeEnum.UNAUTHORIZED.getCode(), ErrorCodeEnum.UNAUTHORIZED.getMsg(), null);
    }

    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> unauthorized(String message) {
        return new ResultVo(ErrorCodeEnum.UNAUTHORIZED.getCode(), message, null);
    }

    //定义好的异常返回
    public static <T> ResultVo<T> forbidden() {
        return new ResultVo(ErrorCodeEnum.FORBIDDEN.getCode(), ErrorCodeEnum.FORBIDDEN.getMsg(), null);
    }

    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> forbidden(String massage) {
        return new ResultVo(ErrorCodeEnum.FORBIDDEN.getCode(), massage, null);
    }

    //定义好的异常返回
    public static <T> ResultVo<T> ACCOUNT() {
        return new ResultVo(ErrorCodeEnum.ACCOUNT_PWD_NOT_EXIST.getCode(), ErrorCodeEnum.ACCOUNT_PWD_NOT_EXIST.getMsg(), null);
    }

    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> ACCOUNT(String massage) {
        return new ResultVo(ErrorCodeEnum.ACCOUNT_PWD_NOT_EXIST.getCode(), massage, null);
    }

    //定义好的异常返回
    public static <T> ResultVo<T> PARAMS() {
        return new ResultVo(ErrorCodeEnum.PARAMS_ERROR.getCode(), ErrorCodeEnum.PARAMS_ERROR.getMsg(), null);
    }

    //可以自定义返回信息的异常返回
    public static <T> ResultVo<T> PARAMS(String massage) {
        return new ResultVo(ErrorCodeEnum.PARAMS_ERROR.getCode(), massage, null);
    }

//    public
}
