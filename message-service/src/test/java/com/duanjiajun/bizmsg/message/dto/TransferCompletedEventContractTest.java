package com.duanjiajun.bizmsg.message.dto;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.duanjiajun.bizmsg.message.config.RabbitMqConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨服务消息契约测试：用「business-service 生产端实际发出的报文」反序列化，
 * 验证消费端的独立副本能完整还原。
 * 生产端加/改字段而消费端没跟上、或消费端手滑改了字段名，这里会直接失败，
 * 把 README 里「改字段必须两边同步」从口头约定变成可执行的兜底。
 */
class TransferCompletedEventContractTest {

    /** 字段集合与 business-service 的 TransferCompletedEventJsonTest 保持一致。 */
    private static final String PRODUCER_PAYLOAD = """
            {
              "messageId": "msg-TRF-1",
              "businessId": "TRF-20261005100000000-0001",
              "messageType": "HOUSE-TRANSFER",
              "completedAt": "2026-10-05 10:00:00",
              "seller": {"name": "张三", "idNo": "110101199001011234"},
              "buyer": {"name": "李四", "idNo": "110202199003034567"},
              "house": {"address": "北京市朝阳区某小区 1 号楼 101", "area": 88.50, "price": 3200000.00}
            }
            """;

    @Test
    @DisplayName("消费端副本可反序列化生产端报文（__TypeId__ 指向生产端 FQCN 也能忽略）")
    void consumerCopyDeserializesProducerPayload() {
        Jackson2JsonMessageConverter converter = new RabbitMqConfig().jsonMessageConverter();
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // 生产端消息头里带着自己的全限定类名；消费端靠监听方法参数类型（inferredArgumentType）忽略它
        messageProperties.setHeader("__TypeId__", "com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent");
        messageProperties.setInferredArgumentType(TransferCompletedEvent.class);

        Message message = new Message(PRODUCER_PAYLOAD.getBytes(StandardCharsets.UTF_8), messageProperties);

        Object payload = converter.fromMessage(message);

        assertThat(payload).isInstanceOf(TransferCompletedEvent.class);
        TransferCompletedEvent event = (TransferCompletedEvent) payload;
        assertThat(event.messageId()).isEqualTo("msg-TRF-1");
        assertThat(event.businessId()).isEqualTo("TRF-20261005100000000-0001");
        assertThat(event.messageType()).isEqualTo("HOUSE-TRANSFER");
        assertThat(event.completedAt()).isEqualTo("2026-10-05 10:00:00");
        assertThat(event.seller().name()).isEqualTo("张三");
        assertThat(event.seller().idNo()).isEqualTo("110101199001011234");
        assertThat(event.buyer().name()).isEqualTo("李四");
        assertThat(event.buyer().idNo()).isEqualTo("110202199003034567");
        assertThat(event.house().address()).isEqualTo("北京市朝阳区某小区 1 号楼 101");
        assertThat(event.house().area()).isEqualByComparingTo("88.50");
        assertThat(event.house().price()).isEqualByComparingTo("3200000.00");
    }
}
