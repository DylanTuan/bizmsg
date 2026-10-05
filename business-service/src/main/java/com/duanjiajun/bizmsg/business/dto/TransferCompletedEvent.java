package com.duanjiajun.bizmsg.business.dto;

import java.math.BigDecimal;

/**
 * 业务办结事件，两个服务之间的消息契约（JSON 即契约）。
 * 两边各持一份字段一致的副本，改字段必须同步，否则消费端反序列化失败。
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
