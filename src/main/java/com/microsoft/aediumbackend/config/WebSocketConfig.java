package com.microsoft.aediumbackend.config;

import com.microsoft.aediumbackend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

import static com.microsoft.aediumbackend.constant.CommonConstant.TOKEN_PAYLOAD_KEY_1;
import static com.microsoft.aediumbackend.constant.CommonConstant.TOKEN_PAYLOAD_KEY_3;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://starter-29b.pages.dev", "http://localhost:5173")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
                            String token = JwtUtils.extractTokenFromCookie(cookies);
                            if (token == null || token.isEmpty()) {
                                log.error("WebSocketConfig : 没有token");
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;
                            }
                            try {
                                Claims claims = JwtUtils.parseToken(token);
                                long userId = Long.parseLong(claims.get(TOKEN_PAYLOAD_KEY_1).toString());
                                long tokenActiveTimestamp = Long.parseLong(claims.get(TOKEN_PAYLOAD_KEY_3).toString());

                                String validTimestamp = stringRedisTemplate.opsForValue().get("user:valid_time:" + userId);

                                if (validTimestamp != null) {
                                    long latestValidTimestamp = Long.parseLong(validTimestamp);
                                    if (tokenActiveTimestamp < latestValidTimestamp) {
                                        log.error("WebSocket Config : token过期");
                                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                        return false;
                                    }
                                }
                                attributes.put("principal", userId);
                            } catch (Exception e) {
                                log.error("WebSocket Config : token无效，拒绝访问");
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                return false;
                            }
                        }
                        log.info("WebSocket Config : token 有效");
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
                })
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        Object userId = attributes.get("principal");
                        if (userId == null) return null;
                        String name = String.valueOf(userId);
                        return () -> name;
                    }
                });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
