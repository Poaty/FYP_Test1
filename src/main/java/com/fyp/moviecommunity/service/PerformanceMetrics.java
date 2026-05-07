package com.fyp.moviecommunity.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PerformanceMetrics {

    private static final int RECENT_CAP = 200;

    private final AtomicLong omdbCalls = new AtomicLong();
    private final AtomicLong omdbTotalNanos = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();

    private final Map<String, RouteTimings> routes = new ConcurrentHashMap<>();

    public void recordOmdbCall(long nanos) {
        omdbCalls.incrementAndGet();
        omdbTotalNanos.addAndGet(nanos);
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordRequest(String route, long nanos) {
        routes.computeIfAbsent(route, k -> new RouteTimings()).record(nanos);
    }

    public Snapshot snapshot() {
        long calls = omdbCalls.get();
        double meanOmdbMs = calls == 0 ? 0
                : TimeUnit.NANOSECONDS.toMillis(omdbTotalNanos.get()) / (double) calls;

        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long lookups = hits + misses;
        double hitRate = lookups == 0 ? 0 : 100.0 * hits / lookups;

        Map<String, RouteSnapshot> routeSnapshots = new LinkedHashMap<>();
        routes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> routeSnapshots.put(e.getKey(), e.getValue().snapshot()));

        return new Snapshot(calls, meanOmdbMs, hits, misses, lookups, hitRate, routeSnapshots);
    }

    public record Snapshot(
            long omdbCalls,
            double omdbMeanMs,
            long cacheHits,
            long cacheMisses,
            long cacheLookups,
            double cacheHitRatePct,
            Map<String, RouteSnapshot> routes) {}

    public record RouteSnapshot(
            long requestCount,
            long sampleCount,
            double meanMs,
            long p50Ms,
            long p95Ms,
            long minMs,
            long maxMs) {}

    private static final class RouteTimings {
        private final Deque<Long> recent = new ArrayDeque<>(RECENT_CAP);
        private final AtomicLong totalCount = new AtomicLong();

        synchronized void record(long nanos) {
            totalCount.incrementAndGet();

            // keep the latest request timings only
            long ms = Math.max(0, TimeUnit.NANOSECONDS.toMillis(nanos));
            recent.addLast(ms);
            while (recent.size() > RECENT_CAP) {
                recent.pollFirst();
            }
        }

        synchronized RouteSnapshot snapshot() {
            long count = totalCount.get();
            if (recent.isEmpty()) {
                return new RouteSnapshot(count, 0, 0, 0, 0, 0, 0);
            }

            long[] arr = recent.stream().mapToLong(Long::longValue).sorted().toArray();

            long sum = 0;
            for (long v : arr) {
                sum += v;
            }

            double mean = sum / (double) arr.length;
            long p50 = arr[arr.length / 2];
            long p95 = arr[Math.min(arr.length - 1, (int) Math.floor(arr.length * 0.95))];

            return new RouteSnapshot(count, arr.length, mean, p50, p95, arr[0], arr[arr.length - 1]);
        }
    }
}