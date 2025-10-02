package com.hanserwei.hannote.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AddUserId2HeaderFilter implements GlobalFilter {
    /**
     * 请求头中，用户 ID 的键
     */
    private static final String HEADER_USER_ID = "userId";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("==================> TokenConvertFilter");
        // 用户 ID
        Long userId;
        SaReactorSyncHolder.setContext(exchange);
        try {
            // 获取当前登录用户的 ID
            userId = StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            log.debug("==> 用户未登录, 不追加 userId 请求头");
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("==> 获取用户 ID 失败", e);
            return chain.filter(exchange);
        } finally {
            SaReactorSyncHolder.clearContext();
        }

        log.info("## 当前登录的用户 ID: {}", userId);

        ServerWebExchange newExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(HEADER_USER_ID, String.valueOf(userId)))) // 将用户 ID 设置到请求头中
                .build();
        return chain.filter(newExchange);
    }
}
