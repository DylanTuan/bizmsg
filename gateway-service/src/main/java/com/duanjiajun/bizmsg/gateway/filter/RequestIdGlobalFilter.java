package com.duanjiajun.bizmsg.gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/** 给每个入口请求生成或透传 X-Request-Id，并打一条访问日志，用于跨服务串联日志。 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 上游带了就透传，否则生成 32 位无连字符 UUID
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        final String traceId = requestId;
        final long startTime = System.currentTimeMillis();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(REQUEST_ID_HEADER, traceId))
                .build();
        // 回写响应头，方便调用方按 ID 反查日志
        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, traceId);

        return chain.filter(mutatedExchange)
                .doFinally(signal -> log.info("网关请求结束 method={} path={} status={} 耗时={}ms requestId={}",
                        mutatedExchange.getRequest().getMethod(),
                        mutatedExchange.getRequest().getURI().getRawPath(),
                        mutatedExchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - startTime,
                        traceId));
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保转发的请求上已经带上 Header
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
