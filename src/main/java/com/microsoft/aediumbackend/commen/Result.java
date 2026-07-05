package com.microsoft.aediumbackend.commen;

import lombok.Data;

/**
 * 统一返回封装
 */
@Data
public class Result<T> {
    private Integer code;// 0 成功
    private T data;// 返回前端的数据
    private String message;// 状态码的描述
    private String description;// 错误具体描述

    /**
     * 成功响应 无数据
     */
    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.setCode(0);
        result.setData(null);
        result.setMessage("success");
        result.setDescription(null);
        return result;
    }

    /**
     * 成功响应 返回数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setData(data);
        result.setMessage("success");
        result.setDescription(null);
        return result;
    }

    /**
     * 失败响应 含特定错误码与错误描述信息
     */
    public static Result<Void> error(Integer code, String message, String description) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setData(null);
        result.setMessage(message);
        result.setDescription(description);
        return result;
    }

    /**
     * 失败响应 含特定错误码与错误描述信息
     */
    public static Result<Void> error(ErrorCode errorCode, String description) {
        Result<Void> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setData(null);
        result.setMessage(errorCode.getMessage());
        result.setDescription(description);
        return result;
    }
}
