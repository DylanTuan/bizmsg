package com.duanjiajun.bizmsg.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务消息配置，绑定 Nacos 中 business-service.yml 的 bizmsg.mq.* 节点。
 * exchange / routing-key 必须与 message-service 侧声明的一致。
 */
@ConfigurationProperties(prefix = "bizmsg.mq")
public class BusinessMqProperties {

    /** 业务事件交换机，topic。 */
    private String exchange = "bizmsg.business";

    private String routingKey = "business.transfer.completed";

    /** 超时视为投递失败，业务状态不推进。 */
    private long confirmTimeoutSeconds = 5L;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public long getConfirmTimeoutSeconds() {
        return confirmTimeoutSeconds;
    }

    public void setConfirmTimeoutSeconds(long confirmTimeoutSeconds) {
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }
}
