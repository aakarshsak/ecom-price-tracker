package com.sinha.ecom_system.api_gateway.filter;

import com.sinha.ecom_system.common.contants.CommonConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(1)
public class AppGlobalFIlter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        UUID requestId = UUID.randomUUID();

        ServerHttpRequest request = exchange.getRequest();

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CommonConstants.HEADER_REQUEST_ID, requestId.toString())
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }
}
