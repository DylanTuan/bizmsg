package com.duanjiajun.bizmsg.message.mq;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.duanjiajun.bizmsg.message.config.MessageMqProperties;
import com.duanjiajun.bizmsg.message.config.UploadProperties;
import com.duanjiajun.bizmsg.message.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.message.dto.UploadReceiptRequest;
import com.duanjiajun.bizmsg.message.service.ReceiptStorageService;
import com.duanjiajun.bizmsg.message.xml.ReportXmlBuilder;
import com.rabbitmq.client.Channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消费端可靠性单测：不连真实 RabbitMQ，用桩件替换 Channel 与 RabbitTemplate。
 * 覆盖「成功 ack / 幂等跳过 / 失败转重试队列 / 重试耗尽进死信」四条路径。
 */
class TransferReportConsumerTest {

    private static final long DELIVERY_TAG = 7L;

    @TempDir
    Path tempDir;

    private RabbitTemplate rabbitTemplate;
    private Channel channel;
    private MessageMqProperties properties;
    private UploadProperties uploadProperties;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        channel = mock(Channel.class);
        properties = new MessageMqProperties();
        uploadProperties = new UploadProperties();
        uploadProperties.setStorageDir(tempDir.toString());
        uploadProperties.setReceiptPrefix("RCPT");
    }

    @Test
    @DisplayName("消费成功：报文落盘 + basicAck，且不产生任何重投")
    void consumeSuccessfullyStoresReportAndAcks() throws IOException {
        TransferReportConsumer consumer = consumerWith(new ReceiptStorageService(uploadProperties));

        consumer.onTransferCompleted(event("TRF-OK-1"), message(null), channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));
        assertThat(storedFileCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("幂等：同一业务号被重复投递时直接 ack，不会重复落盘")
    void skipDuplicateDelivery() throws IOException {
        TransferReportConsumer consumer = consumerWith(new ReceiptStorageService(uploadProperties));
        TransferCompletedEvent event = event("TRF-IDEMPOTENT");

        consumer.onTransferCompleted(event, message(null), channel);
        consumer.onTransferCompleted(event, message(null), channel);

        verify(channel, org.mockito.Mockito.times(2)).basicAck(DELIVERY_TAG, false);
        // 只有一份报文——这正是幂等守卫要防的场景（at-least-once 语义下消息可能被投递多次）
        assertThat(storedFileCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("首次失败：原消息 ack，并带重试计数投到重试队列（延迟重试）")
    void republishToRetryQueueOnFirstFailure() throws IOException {
        TransferReportConsumer consumer = consumerWith(failingStorage());

        consumer.onTransferCompleted(event("TRF-RETRY"), message(null), channel);

        // 原消息已成功转投重试队列，可以确认掉
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(any(Long.class), any(Boolean.class), any(Boolean.class));
        verify(rabbitTemplate).convertAndSend(eq(properties.getRetryExchange()),
                eq(properties.getRetryRoutingKey()), any(), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("重试耗尽（第 3 次仍失败）：basicNack(requeue=false) 转死信队列，不再重投")
    void nackToDeadLetterWhenRetriesExhausted() throws IOException {
        TransferReportConsumer consumer = consumerWith(failingStorage());
        // header 为 2 表示已投递过两次，本次是第 3 次（max-attempts=3）→ 直接进死信
        Message message = message(properties.getMaxAttempts() - 1);

        consumer.onTransferCompleted(event("TRF-DLQ"), message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(any(Long.class), any(Boolean.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));
    }

    private TransferReportConsumer consumerWith(ReceiptStorageService storage) {
        return new TransferReportConsumer(new ReportXmlBuilder(), storage, new ProcessedMessageGuard(),
                rabbitTemplate, properties);
    }

    /** 模拟落盘失败（磁盘只读/权限不足），触发重试链路。 */
    private ReceiptStorageService failingStorage() {
        ReceiptStorageService storage = mock(ReceiptStorageService.class);
        when(storage.store(any(UploadReceiptRequest.class)))
                .thenThrow(new IllegalStateException("报文落盘失败：磁盘只读"));
        return storage;
    }

    private Message message(Integer retryCount) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        if (retryCount != null) {
            messageProperties.setHeader(TransferReportConsumer.RETRY_COUNT_HEADER, retryCount);
        }
        return new Message("{}".getBytes(StandardCharsets.UTF_8), messageProperties);
    }

    private TransferCompletedEvent event(String businessId) {
        return new TransferCompletedEvent("msg-" + businessId, businessId, "HOUSE-TRANSFER", "2026-10-05 10:00:00",
                new TransferCompletedEvent.Party("张三", "110101199001011234"),
                new TransferCompletedEvent.Party("李四", "110202199003034567"),
                new TransferCompletedEvent.House("北京市朝阳区某小区 1 号楼 101",
                        new BigDecimal("88.50"), new BigDecimal("3200000.00")));
    }

    private long storedFileCount() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.count();
        }
    }
}
