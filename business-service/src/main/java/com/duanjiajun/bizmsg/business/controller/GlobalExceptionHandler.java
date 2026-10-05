package com.duanjiajun.bizmsg.business.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.duanjiajun.bizmsg.business.exception.MqPublishException;
import com.duanjiajun.bizmsg.business.exception.TransferNotFoundException;

/**
 * 统一异常出口：参数 400、业务不存在 404、MQ 不可用 503、其余 500。
 * 503 表示状态未推进、可直接重试办结，前端据此提示重试而不是报错。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 字段校验失败，把各字段提示拼起来返回。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("业务受理参数不合法：{}", message);
        return Map.of("success", "false", "message", message.isBlank() ? "请求参数不合法" : message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        log.warn("请求参数不合法：{}", ex.getMessage());
        return Map.of("success", "false", "message", String.valueOf(ex.getMessage()));
    }

    @ExceptionHandler(TransferNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(TransferNotFoundException ex) {
        log.warn("业务不存在：{}", ex.getMessage());
        return Map.of("success", "false", "message", ex.getMessage());
    }

    /** MQ 投递失败；业务状态保持「已受理」，重试即可。 */
    @ExceptionHandler(MqPublishException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleMqUnavailable(MqPublishException ex) {
        log.error("办结事件投递失败，业务状态未推进：{}", ex.getMessage(), ex);
        return Map.of("success", "false",
                "message", "消息队列不可用，业务状态未变更，请稍后重试办结：" + ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(IllegalStateException ex) {
        log.error("业务处理失败", ex);
        return Map.of("success", "false", "message", String.valueOf(ex.getMessage()));
    }
}
