package com.fyp.moviecommunity.bootstrap;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory performance metrics with synthetic samples when
 * demo data is enabled. This makes the /admin/perf dashboard non-empty
 * during local testing.
 *
 * The values are synthetic and should not be treated as real measurements.
 * Production figures come from the live HandlerInterceptor recording
 * actual request latencies.
 *
 * Disable with app.demo-data.enabled=false. Real requests record on top
 * of these samples; nothing is overwritten.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // run AFTER DemoDataSeeder
public class PerformanceSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceSeeder.class);

    private final PerformanceMetrics metrics;
    private final boolean enabled;

    public PerformanceSeeder(PerformanceMetrics metrics,
                             @Value("${app.demo-data.enabled:true}") boolean enabled) {
        this.metrics = metrics;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Performance seeding disabled (app.demo-data.enabled=false).");
            return;
        }
        log.info("Seeding performance metrics with synthetic samples...");

        // Fixed seed for reproducibility across restarts.
        Random r = new Random(20260426L);

        // ---- OMDb counters --------------------------------------------------
        for (int i = 0; i < 78; i++) {
            long ms = sample(r, 285, 70);
            metrics.recordOmdbCall(ms * 1_000_000L);
        }

        for (int i = 0; i < 712; i++) metrics.recordCacheHit();
        for (int i = 0; i < 78; i++)  metrics.recordCacheMiss();

        // ---- Per-route timings ----------------------------------------------
        seedRoute(r, "/feed",                184,  92, 38);
        seedRoute(r, "/for-you",             142, 138, 55);
        seedRoute(r, "/events",               64,  84, 28);
        seedRoute(r, "/events/{id}",          41, 121, 42);
        seedRoute(r, "/posts/{id}",          153, 109, 41);
        seedRoute(r, "/posts/new",            28,  76, 24);
        seedRoute(r, "/posts/new/write",      19,  88, 30);
        seedRoute(r, "/login",                17,  43, 18);
        seedRoute(r, "/signup",                6,  61, 22);
        seedRoute(r, "/admin/log",            14,  51, 19);
        seedRoute(r, "/admin/metrics",        12, 312, 110);
        seedRoute(r, "/posts",                34,  97, 35);  // POST endpoint
        seedRoute(r, "/posts/{postId}/comments", 47, 88, 32);
        seedRoute(r, "/events/{id}/rsvp",     22,  61, 24);

        log.info("Performance seed complete.");
    }

    private void seedRoute(Random r, String route, int count, int meanMs, int stdMs) {
        for (int i = 0; i < count; i++) {
            long ms = sample(r, meanMs, stdMs);
            metrics.recordRequest(route, ms * 1_000_000L);
        }
    }

    /**
     * Single synthetic latency sample: Gaussian around the mean with a 5%
     * chance of being multiplied 1.5x-3x to introduce tail-end outliers.
     * Floor of 15 ms so values stay positive.
     */
    private static long sample(Random r, int meanMs, int stdMs) {
        double base = meanMs + r.nextGaussian() * stdMs;
        if (r.nextDouble() < 0.05) {
            base *= 1.5 + r.nextDouble() * 1.5;
        }
        return Math.max(15, (long) base);
    }
}
