package com.duanjiajun.bizmsg.business.mq;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.duanjiajun.bizmsg.business.config.BusinessMqProperties;
import com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.business.exception.MqPublishException;

/**
 * 业务办结事件发布者。开了 publisher confirm，发完同步等 broker ack，确认落盘才算成功；
 * 失败就抛异常让业务保持 ACCEPTED，避免「已办结但消息丢了」。
 */
@Component
public class TransferEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final BusinessMqProperties properties;

    public TransferEventPublisher(RabbitTemplate rabbitTemplate, BusinessMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /** 投递并等待 broker 确认；失败抛 {@link MqPublishException}，调用方据此不推进状态。 */
    public void publish(TransferCompletedEvent event) {
        CorrelationData correlationData = new CorrelationData(event.messageId());
        try {
            rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), event, correlationData);
        } catch (RuntimeException ex) {
            // 序列化失败、连接断开、交换机不存在都按投递失败处理：上抛 503，状态保持 ACCEPTED
            throw new MqPublishException("投递办结事件失败 businessId=" + event.businessId(), ex);
        }

        CorrelationData.Confirm confirm = awaitConfirm(correlationData, event.messageId());
        if (!confirm.isAck()) {
            throw new MqPublishException("broker 拒绝消息 businessId=" + event.businessId()
                    + " 原因=" + confirm.getReason());
        }
        // 不可路由时 broker 先 basic.return 再 ack，此时 confirm 仍是 true，
        // 只看 isAck 会把「交换机在、绑定没了」当成投递成功，所以要单独检查 returned
        ReturnedMessage returned = correlationData.getReturned();
        if (returned != null) {
            throw new MqPublishException("消息不可路由，已被 broker 退回 businessId=" + event.businessId()
                    + " exchange=" + returned.getExchange() + " routingKey=" + returned.getRoutingKey()
                    + " 原因=" + returned.getReplyText());
        }
        log.info("业务办结事件已确认投递 businessId={} messageId={} exchange={} routingKey={}",
                event.businessId(), event.messageId(), properties.getExchange(), properties.getRoutingKey());
    }

    private CorrelationData.Confirm awaitConfirm(CorrelationData correlationData, String messageId) {
        try {
            return correlationData.getFuture().get(properties.getConfirmTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MqPublishException("等待 broker confirm 被中断 messageId=" + messageId, ex);
        } catch (ExecutionException | TimeoutException ex) {
            // 超时意味着 broker 未在约定时间内确认，宁可让调用方重试，也不能让业务状态单方面推进
            throw new MqPublishException("等待 broker confirm 失败 messageId=" + messageId, ex);
        }
    }
}
