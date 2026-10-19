    package com.customcharge.persistencelayer.web;

import com.customcharge.persistencelayer.context.TimeZoneContext;
import com.customcharge.persistencelayer.util.TimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;

public class TimeZoneHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String header = request.getHeader(TimeZoneContext.HEADER_NAME);
        ZoneId zone = TimeUtil.parseZoneIdOrUtc(header);
        TimeZoneContext.set(zone);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        TimeZoneContext.clear();
    }
}
