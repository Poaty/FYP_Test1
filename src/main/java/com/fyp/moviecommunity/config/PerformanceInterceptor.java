package com.fyp.moviecommunity.config;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Records request timings for the local performance dashboard.
 * Static assets and the dashboard route are ignored.
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_NANOS = "perf.start";

    private final PerformanceMetrics metrics;

    public PerformanceInterceptor(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        req.setAttribute(START_NANOS, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                                Object handler, Exception ex) {
        Object start = req.getAttribute(START_NANOS);
        if (!(start instanceof Long startNanos)) return;

        long elapsed = System.nanoTime() - startNanos;

        String route = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route == null) route = req.getRequestURI();

        if (route.startsWith("/admin/perf")) return;
        if (route.startsWith("/css") || route.startsWith("/js")
                || route.startsWith("/images") || route.startsWith("/webjars")
                || route.equals("/error") || route.equals("/favicon.ico")) return;

        metrics.recordRequest(route, elapsed);
    }
}