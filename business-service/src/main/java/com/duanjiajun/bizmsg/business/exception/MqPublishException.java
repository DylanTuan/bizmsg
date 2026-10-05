package com.duanjiajun.bizmsg.business.exception;

/**
 * MQ 投递失败（broker nack、confirm 超时、消息不可路由被退回）。
 * 抛出时业务状态保持 ACCEPTED，调用方直接重试办结即可，映射为 503。
 */
public class MqPublishException extends RuntimeException {

    public MqPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqPublishException(String message) {
        super(message);
    }
}
