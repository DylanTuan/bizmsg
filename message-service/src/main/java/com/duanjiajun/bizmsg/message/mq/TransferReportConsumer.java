package com.duanjiajun.bizmsg.message.mq;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.duanjiajun.bizmsg.message.config.MessageMqProperties;
import com.duanjiajun.bizmsg.message.dto.ReceiptResponse;
import com.duanjiajun.bizmsg.message.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.service.ReceiptStorageService;
import com.duanjiajun.bizmsg.message.xml.ReportXmlBuilder;
import com.duanjiajun.bizmsg.message.xml.model.TransferReport;
import com.rabbitmq.client.Channel;

/**
 * 消费办结事件，生成商品房转移报文并落盘。
 * 手动 ack、TTL 延迟重试、重试耗尽进死信、businessId 幂等，四条都在这个类里。
 */
@Component
public class TransferReportConsumer {

    /** 重投次数，每次 +1，用来判断是否到上限。 */
    public static final String RETRY_COUNT_HEADER = "x-retry-count";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(TransferReportConsumer.class);

    private final ReportXmlBuilder xmlBuilder;
    private final ReceiptStorageService receiptStorageService;
    private final ProcessedMessageGuard processedGuard;
    private final RabbitTemplate rabbitTemplate;
    private final MessageMqProperties properties;

    public TransferReportConsumer(ReportXmlBuilder xmlBuilder,
                                  ReceiptStorageService receiptStorageService,
                                  ProcessedMessageGuard processedGuard,
                                  RabbitTemplate rabbitTemplate,
                                  MessageMqProperties properties) {
        this.xmlBuilder = xmlBuilder;
        this.receiptStorageService = receiptStorageService;
        this.processedGuard = processedGuard;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(queues = "${bizmsg.mq.transfer-queue:" + MessageMqProperties.DEFAULT_TRANSFER_QUEUE + "}")
    public void onTransferCompleted(TransferCompletedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String businessId = event.businessId();
        // 1-based：首次投递时 header 不存在，算第 1 次尝试
        int attempt = readRetryCount(message) + 1;
        try {
            if (processedGuard.isProcessed(businessId)) {
                // 已经生成过，直接 ack
                log.info("报文已存在，跳过重复投递 businessId={} attempt={}", businessId, attempt);
                channel.basicAck(deliveryTag, false);
                return;
            }

            String xml = xmlBuilder.toXml(buildReport(event));
            ReceiptResponse receipt = receiptStorageService.store(
                    new UploadReceiptRequest(businessId, event.messageType(), xml));
            processedGuard.markProcessed(businessId);

            log.info("办结事件消费成功 businessId={} attempt={} receiptNo={}",
                    businessId, attempt, receipt.receiptNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            handleFailure(event, channel, deliveryTag, businessId, attempt, ex);
        }
    }

    /** 未到上限就延迟重投，到了就转死信。 */
    private void handleFailure(TransferCompletedEvent event, Channel channel, long deliveryTag,
                               String businessId, int attempt, Exception ex) throws IOException {
        if (attempt < properties.getMaxAttempts()) {
            log.warn("第 {} 次消费失败，约 {}ms 后重试 businessId={} 原因={}",
                    attempt, properties.getRetryDelayMillis(), businessId, ex.toString());
            try {
                republishToRetry(event, attempt);
                // 已经转投成功，原消息可以确认
                channel.basicAck(deliveryTag, false);
            } catch (Exception republishEx) {
                // 重投也失败就保持未确认，等 broker 重投，不能静默丢
                log.error("重试投递失败，保持消息未确认等待 broker 重投 businessId={}", businessId, republishEx);
            }
            return;
        }

        log.error("已尝试 {} 次仍未成功，转入死信队列 businessId={} 原因={}", attempt, businessId, ex.toString());
        // requeue=false → broker 按主队列的 x-dead-letter-exchange / x-dead-letter-routing-key 投到 DLQ
        channel.basicNack(deliveryTag, false, false);
    }

    /** 带重试计数投回重试队列，TTL 到期后自动回主队列。 */
    private void republishToRetry(TransferCompletedEvent event, int retryCount) {
        rabbitTemplate.convertAndSend(properties.getRetryExchange(), properties.getRetryRoutingKey(), event,
                message -> {
                    message.getMessageProperties().setHeader(RETRY_COUNT_HEADER, retryCount);
                    message.getMessageProperties().setMessageId(event.messageId());
                    return message;
                });
    }

    /** 事件转报文模型。生成时间取当前时间，和业务办结时间区分开。 */
    private TransferReport buildReport(TransferCompletedEvent event) {
        return new TransferReport(event.businessId(), event.messageType(),
                LocalDateTime.now().format(TIME_FORMATTER),
                new TransferReport.Party(event.seller().name(), event.seller().idNo()),
                new TransferReport.Party(event.buyer().name(), event.buyer().idNo()),
                new TransferReport.House(event.house().address(), event.house().area(), event.house().price()));
    }

    private int readRetryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("重试计数 header 非法，按首次投递处理 value={}", value);
            return 0;
        }
    }
}
