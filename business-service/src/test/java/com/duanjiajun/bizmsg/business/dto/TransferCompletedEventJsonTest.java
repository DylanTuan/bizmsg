package com.duanjiajun.bizmsg.business.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import com.duanjiajun.bizmsg.business.config.BusinessRabbitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生产端 JSON 契约测试：用真实的 {@link BusinessRabbitConfig#jsonMessageConverter()} 把事件序列化，
 * 把「线上报文长什么样」的字段集合钉死。
 * 消费端有一份字段一致的独立副本（message-service 的 TransferCompletedEvent），
 * 任意一侧改字段，都要同步改这里与 message-service 的 TransferCompletedEventContractTest。
 */
class TransferCompletedEventJsonTest {

    private static final Set<String> TOP_LEVEL_KEYS =
            Set.of("messageId", "businessId", "messageType", "completedAt", "seller", "buyer", "house");
    private static final Set<String> PARTY_KEYS = Set.of("name", "idNo");
    private static final Set<String> HOUSE_KEYS = Set.of("address", "area", "price");

    @Test
    @DisplayName("序列化字段与 __TypeId__ 稳定，消费端副本据此反序列化")
    void producerWireFormatIsStable() throws Exception {
        Jackson2JsonMessageConverter converter = new BusinessRabbitConfig().jsonMessageConverter();

        Message message = converter.toMessage(event(), new MessageProperties());
        JsonNode body = new ObjectMapper().readTree(message.getBody());

        assertThat(keysOf(body)).containsExactlyInAnyOrderElementsOf(TOP_LEVEL_KEYS);
        assertThat(keysOf(body.get("seller"))).containsExactlyInAnyOrderElementsOf(PARTY_KEYS);
        assertThat(keysOf(body.get("buyer"))).containsExactlyInAnyOrderElementsOf(PARTY_KEYS);
        assertThat(keysOf(body.get("house"))).containsExactlyInAnyOrderElementsOf(HOUSE_KEYS);
        // 消费端 TypePrecedence.INFERRED 会忽略这个头，但消息体里确实带着生产端 FQCN
        assertThat((String) message.getMessageProperties().getHeader("__TypeId__"))
                .isEqualTo("com.duanjiajun.bizmsg.business.dto.TransferCompletedEvent");
    }

    private List<String> keysOf(JsonNode node) {
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private TransferCompletedEvent event() {
        return new TransferCompletedEvent("msg-TRF-1", "TRF-20261005100000000-0001", "HOUSE-TRANSFER",
                "2026-10-05 10:00:00",
                new TransferCompletedEvent.Party("张三", "110101199001011234"),
                new TransferCompletedEvent.Party("李四", "110202199003034567"),
                new TransferCompletedEvent.House("北京市朝阳区某小区 1 号楼 101",
                        new BigDecimal("88.50"), new BigDecimal("3200000.00")));
    }
}
