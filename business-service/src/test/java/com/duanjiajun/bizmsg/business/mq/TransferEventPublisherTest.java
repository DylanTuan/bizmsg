package com.duanjiajun.bizmsg.business.mq;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.duanjiajun.bizmsg.business.config.BusinessMqProperties;
import com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent;
import com.duanjiajun.bizmsg.business.exception.MqPublishException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * 投递可靠性单测：不连真实 broker，用桩件 RabbitTemplate + 手工完成 CorrelationData，
 * 覆盖「ack / nack / confirm 超时 / 不可路由退回 / 发送异常」五条路径。
 * 这些正是「业务状态绝不在消息未落稳时推进」的断言，单靠 mock 掉 publisher 的 TransferServiceTest 覆盖不到。
 */
class TransferEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private BusinessMqProperties properties;
    private TransferEventPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        properties = new BusinessMqProperties();
        publisher = new TransferEventPublisher(rabbitTemplate, properties);
    }

    @Test
    @DisplayName("broker ack：投递成功，不抛异常")
    void ackMeansSuccess() {
        onSend(data -> data.getFuture().complete(new CorrelationData.Confirm(true, null)));

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("broker nack：抛 MqPublishException，调用方保持 ACCEPTED 并可重试")
    void nackThrows() {
        onSend(data -> data.getFuture().complete(new CorrelationData.Confirm(false, "队列已满")));

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(MqPublishException.class)
                .hasMessageContaining("队列已满");
    }

    @Test
    @DisplayName("confirm 超时：抛 MqPublishException，宁可重试也不单方面推进状态")
    void timeoutThrows() {
        properties.setConfirmTimeoutSeconds(1L);
        // 收到发送动作但永不完结 confirm future，模拟 broker 未在超时内确认
        onSend(data -> { });

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(MqPublishException.class)
                .hasMessageContaining("confirm");
    }

    @Test
    @DisplayName("不可路由（mandatory）：broker 先 basic.return 再 ack，只看 isAck 会误判成功")
    void returnedMessageThrowsEvenIfAcked() {
        onSend(data -> {
            data.setReturned(new ReturnedMessage(
                    new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()),
                    312, "NO_ROUTE", properties.getExchange(), properties.getRoutingKey()));
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(MqPublishException.class)
                .hasMessageContaining("不可路由");
    }

    @Test
    @DisplayName("发送阶段异常（序列化失败/连接断开等）：统一包装为 MqPublishException")
    void sendFailureIsWrapped() {
        doThrow(new IllegalArgumentException("SimpleMessageConverter only supports String, byte[] and Serializable payloads"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publish(event()))
                .isInstanceOf(MqPublishException.class)
                .hasMessageContaining("businessId");
    }

    /** 用桩件接管 convertAndSend，把发布者内部创建的 CorrelationData 交给测试代码操纵。 */
    private void onSend(Consumer<CorrelationData> behaviour) {
        doAnswer(invocation -> {
            behaviour.accept(invocation.getArgument(3, CorrelationData.class));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }

    private TransferCompletedEvent event() {
        return new TransferCompletedEvent("msg-TRF-1", "TRF-1", "HOUSE-TRANSFER", "2026-10-05 10:00:00",
                new TransferCompletedEvent.Party("张三", "110101199001011234"),
                new TransferCompletedEvent.Party("李四", "110202199003034567"),
                new TransferCompletedEvent.House("北京市朝阳区某小区 1 号楼 101",
                        new BigDecimal("88.50"), new BigDecimal("3200000.00")));
    }
}
