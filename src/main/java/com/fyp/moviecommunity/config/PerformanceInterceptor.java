package com.fyp.moviecommunity.config;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Times every HTTP request and groups results by route pattern (so /posts/123
 * and /posts/456 both bucket under "/posts/{id}").
 *
 * Skips the admin perf pages themselves so reading the dashboard doesn't
 * pollute the numbers.
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
        if (start == null) return;
        long elapsed = System.nanoTime() - (long) start;

        String route = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route == null) route = req.getRequestURI();

        // Skip our own pages -- viewing perf shouldn't be a metric we report on.
        if (route.startsWith("/admin/perf")) return;
        // Static assets aren't interesting either.
        if (route.startsWith("/css") || route.startsWith("/js")
                || route.startsWith("/images") || route.startsWith("/webjars")
                || route.equals("/error") || route.equals("/favicon.ico")) return;

        metrics.recordRequest(route, elapsed);
    }
}
