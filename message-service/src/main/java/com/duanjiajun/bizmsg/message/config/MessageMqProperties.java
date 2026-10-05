package com.duanjiajun.bizmsg.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 报文模块的 MQ 配置，绑定 Nacos 中 message-service.yml 的 bizmsg.mq.* 节点。
 * 只有队列与交换机名在这里兜底；acknowledge-mode 和连接信息在 Nacos 配置里，
 * 配置中心不可用时消费端会退回 auto ack，手动 ack 会报 channel 错误。
 */
@ConfigurationProperties(prefix = "bizmsg.mq")
public class MessageMqProperties {

    /** 主队列默认名，同时作为 @RabbitListener 占位符的兜底值。 */
    public static final String DEFAULT_TRANSFER_QUEUE = "bizmsg.message.transfer";

    /** 业务事件交换机，必须与 business-service 声明的同名同类型。 */
    private String exchange = "bizmsg.business";

    private String routingKey = "business.transfer.completed";

    private String transferQueue = DEFAULT_TRANSFER_QUEUE;

    private String deadLetterExchange = "bizmsg.dlx";

    /** 重试耗尽后的最终归宿，人工排查与补偿的入口。 */
    private String deadLetterQueue = "bizmsg.message.transfer.dlq";

    /** 同时作为主队列的 x-dead-letter-routing-key。 */
    private String deadLetterRoutingKey = "bizmsg.message.transfer.dlq";

    private String retryExchange = "bizmsg.retry";

    /** 靠 TTL + 死信回主队列实现延迟重试，避免失败后立刻热循环重投。 */
    private String retryQueue = "bizmsg.message.transfer.retry";

    private String retryRoutingKey = "business.transfer.retry";

    /** 即重试队列的消息 TTL。 */
    private long retryDelayMillis = 5000L;

    /** 含首次在内的最大尝试次数，超过后进死信队列。 */
    private int maxAttempts = 3;

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

    public String getTransferQueue() {
        return transferQueue;
    }

    public void setTransferQueue(String transferQueue) {
        this.transferQueue = transferQueue;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void setDeadLetterQueue(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    public String getDeadLetterRoutingKey() {
        return deadLetterRoutingKey;
    }

    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) {
        this.deadLetterRoutingKey = deadLetterRoutingKey;
    }

    public String getRetryExchange() {
        return retryExchange;
    }

    public void setRetryExchange(String retryExchange) {
        this.retryExchange = retryExchange;
    }

    public String getRetryQueue() {
        return retryQueue;
    }

    public void setRetryQueue(String retryQueue) {
        this.retryQueue = retryQueue;
    }

    public String getRetryRoutingKey() {
        return retryRoutingKey;
    }

    public void setRetryRoutingKey(String retryRoutingKey) {
        this.retryRoutingKey = retryRoutingKey;
    }

    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
