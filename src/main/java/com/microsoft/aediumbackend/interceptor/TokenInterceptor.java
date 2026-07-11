package com.microsoft.aediumbackend.interceptor;

import com.microsoft.aediumbackend.utils.CurrentHold;
import com.microsoft.aediumbackend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.microsoft.aediumbackend.constant.CommonConstant.*;

@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        CurrentHold.removeId();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("进入拦截器");
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (TOKEN_COOKIE_FIELD.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null || token.isEmpty()) {
            log.error("没有token，拒绝访问接口: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        // 拿到token 进行解析
        try {
            Claims claims = JwtUtils.parseToken(token);
            Long userId = Long.valueOf(claims.get(TOKEN_PAYLOAD_KEY_1).toString());
            long tokenActiveTimestamp = Long.parseLong(claims.get(TOKEN_PAYLOAD_KEY_3).toString());

            String validTimestamp = stringRedisTemplate.opsForValue().get("user:valid_time:" + userId);

            if (validTimestamp != null) {
                long latestValidTimestamp = Long.parseLong(validTimestamp);
                if (tokenActiveTimestamp < latestValidTimestamp) {
                    log.error("token无效");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
            }

            CurrentHold.setCurrentId(userId);
        } catch (Exception e) {
            log.error("token无效，拒绝访问接口");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}
