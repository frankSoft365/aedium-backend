package com.microsoft.aediumbackend.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Controller日志生成器
 */
@Slf4j
@Aspect
@Component
public class LogInterceptor {

    @Around("execution(* com.microsoft.aediumbackend.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String uuid = UUID.randomUUID().toString();
        Object[] args = joinPoint.getArgs();
        String params = "[ " + StringUtils.join(args, ", ") + " ]";
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String url = request.getRequestURI();
        String remoteHost = request.getRemoteAddr();
        log.info("request start : uuid : {} | requestUrl : {} | hostUrl : {} | params : {}", uuid, url, remoteHost, params);
        Object result = joinPoint.proceed();
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        log.info("request end : uuid : {} | costTime : {}ms", uuid, totalTimeMillis);
        return result;
    }
}
