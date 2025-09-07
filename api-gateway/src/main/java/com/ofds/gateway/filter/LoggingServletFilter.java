package com.ofds.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingServletFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingServletFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().toString();

        log.info("Incoming Request: [{}] {}", method, path);

        return chain.filter(exchange).doFinally(signalType -> {
            Integer statusCode = null;
            if (response.getStatusCode() != null) {
                statusCode = response.getStatusCode().value();
            }
            log.info("Response Status: {}", statusCode != null ? statusCode : "UNKNOWN");
        });
    }

    @Override
    public int getOrder() {
        return -1; // Run early
    }
}
