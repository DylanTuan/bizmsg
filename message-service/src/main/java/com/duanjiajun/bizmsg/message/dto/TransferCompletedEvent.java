package com.duanjiajun.bizmsg.message.dto;

import java.math.BigDecimal;

/**
 * 业务办结事件，消费端的副本（JSON 即契约）。
 * 字段改动必须与 business-service 侧同步；不可反序列化时消息会直接进死信队列。
 */
public record TransferCompletedEvent(String messageId,
                                     String businessId,
                                     String messageType,
                                     String completedAt,
                                     Party seller,
                                     Party buyer,
                                     House house) {

    public record Party(String name, String idNo) {
    }

    public record House(String address, BigDecimal area, BigDecimal price) {
    }
}
