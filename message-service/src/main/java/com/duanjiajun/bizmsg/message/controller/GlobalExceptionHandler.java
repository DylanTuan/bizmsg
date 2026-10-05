package com.duanjiajun.bizmsg.message.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.duanjiajun.bizmsg.message.exception.ReceiptNotFoundException;

/**
 * 统一异常出口：参数/报文问题 400、回执不存在 404、服务端内部错误 500，避免把堆栈直接暴露给调用方。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReceiptNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ReceiptNotFoundException ex) {
        log.warn("回执查询未命中：{}", ex.getMessage());
        return Map.of("success", "false", "message", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        log.warn("请求参数不合法：{}", ex.getMessage());
        return Map.of("success", "false", "message", String.valueOf(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(IllegalStateException ex) {
        log.error("上传处理失败", ex);
        return Map.of("success", "false", "message", String.valueOf(ex.getMessage()));
    }
}
