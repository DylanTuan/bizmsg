package com.duanjiajun.bizmsg.message.mq;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 消费幂等守卫。MQ 是 at-least-once，抖动、ack 丢失、重投、重启都可能重复投递，
 * 不拦的话同一笔业务会生成多份报文。以 businessId 为幂等键，落盘成功后标记。
 * 内存实现，重启失效；生产该换 Redis SETNX 或数据库唯一索引。
 */
@Component
public class ProcessedMessageGuard {

    private final Set<String> processedBusinessIds = ConcurrentHashMap.newKeySet();

    /** 该业务是否已成功生成过报文。 */
    public boolean isProcessed(String businessId) {
        return businessId != null && processedBusinessIds.contains(businessId);
    }

    /** 标记业务已成功处理；必须在报文真正落盘之后调用。 */
    public void markProcessed(String businessId) {
        if (businessId != null) {
            processedBusinessIds.add(businessId);
        }
    }
}
