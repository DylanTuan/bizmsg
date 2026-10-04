package com.duanjiajun.bizmsg.gateway.filter;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdGlobalFilterTest {

    private final RequestIdGlobalFilter filter = new RequestIdGlobalFilter();

    @Test
    @DisplayName("请求未携带 X-Request-Id 时自动生成 32 位 ID 并透传给下游")
    void generateRequestIdWhenAbsent() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/report/echo").build());
        ServerWebExchange forwarded = forward(exchange);

        String requestId = forwarded.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank().hasSize(32);
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestId);
    }

    @Test
    @DisplayName("请求已携带 X-Request-Id 时原样透传，不重复生成")
    void keepExistingRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/upload/receipt")
                        .header(RequestIdGlobalFilter.REQUEST_ID_HEADER, "trace-0001")
                        .build());
        ServerWebExchange forwarded = forward(exchange);

        assertThat(forwarded.getRequest().getHeaders().getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo("trace-0001");
    }

    /** 执行过滤器并返回传递给下游的 exchange。 */
    private ServerWebExchange forward(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(exchange, current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();
        return forwarded.get();
    }
}
