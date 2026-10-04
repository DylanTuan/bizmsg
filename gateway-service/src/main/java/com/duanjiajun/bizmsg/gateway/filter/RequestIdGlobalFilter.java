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

/**
 * 全局请求 ID 过滤器：为每个入口请求生成或透传 X-Request-Id，并记录一条访问日志。
 * 作用：跨服务串联日志、快速定位问题；下游服务也可把它当作幂等键的种子。
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    /** 请求 ID 的 Header 名称，网关与下游服务统一使用该常量。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 上游已带请求 ID 则透传（便于链路重试复用），否则生成 32 位无连字符 UUID
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        final String traceId = requestId;
        final long startTime = System.currentTimeMillis();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(REQUEST_ID_HEADER, traceId))
                .build();
        // 回写响应头，调用方可凭该 ID 反查全链路日志
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
        // 最高优先级：确保 Header 在路由转发前写入
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
