package com.fyp.moviecommunity.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * In-memory counters for the performance dashboard.
 *
 * Resets on app restart -- this is observability, not analytics. Tracks two
 * things:
 *   1. OMDb API behaviour (call count, mean latency, cache hit rate)
 *   2. Per-route request timing (recent 200 samples per route)
 *
 * Thread-safe: AtomicLong + ConcurrentHashMap. The per-route deque uses
 * a synchronised method to keep "add then evict-oldest" atomic.
 *
 * Exposed via /admin/perf for live viewing and /admin/perf.csv for export.
 */
@Component
public class PerformanceMetrics {

    private static final int RECENT_CAP = 200;

    // OMDb counters
    private final AtomicLong omdbCalls       = new AtomicLong();
    private final AtomicLong omdbTotalNanos  = new AtomicLong();
    private final AtomicLong cacheHits       = new AtomicLong();
    private final AtomicLong cacheMisses     = new AtomicLong();

    // Per-route request timings (route pattern -> rolling window of millis)
    private final Map<String, RouteTimings> routes = new ConcurrentHashMap<>();

    // ---------- recording ----------

    /** OmdbClient calls this when an HTTP request to OMDb completes. */
    public void recordOmdbCall(long nanos) {
        omdbCalls.incrementAndGet();
        omdbTotalNanos.addAndGet(nanos);
    }

    /** MovieService calls this when findOrFetch finds the movie cached. */
    public void recordCacheHit()  { cacheHits.incrementAndGet(); }
    /** MovieService calls this when findOrFetch had to call OMDb. */
    public void recordCacheMiss() { cacheMisses.incrementAndGet(); }

    /** Interceptor calls this once per HTTP request. */
    public void recordRequest(String route, long nanos) {
        routes.computeIfAbsent(route, k -> new RouteTimings()).record(nanos);
    }

    // ---------- snapshots ----------

    public Snapshot snapshot() {
        long calls = omdbCalls.get();
        double meanOmdbMs = calls == 0 ? 0
                : TimeUnit.NANOSECONDS.toMillis(omdbTotalNanos.get()) / (double) calls;
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long lookups = hits + misses;
        double hitRate = lookups == 0 ? 0 : 100.0 * hits / lookups;

        // Stable order by route name for the dashboard
        Map<String, RouteSnapshot> rs = new LinkedHashMap<>();
        routes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> rs.put(e.getKey(), e.getValue().snapshot()));

        return new Snapshot(calls, meanOmdbMs, hits, misses, lookups, hitRate, rs);
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

    // ---------- per-route rolling stats ----------

    private static final class RouteTimings {
        private final Deque<Long> recent = new ArrayDeque<>(RECENT_CAP);
        private final AtomicLong totalCount = new AtomicLong();

        synchronized void record(long nanos) {
            totalCount.incrementAndGet();
            long ms = Math.max(0, TimeUnit.NANOSECONDS.toMillis(nanos));
            recent.addLast(ms);
            while (recent.size() > RECENT_CAP) recent.pollFirst();
        }

        synchronized RouteSnapshot snapshot() {
            long count = totalCount.get();
            if (recent.isEmpty()) {
                return new RouteSnapshot(count, 0, 0, 0, 0, 0, 0);
            }
            long[] arr = recent.stream().mapToLong(Long::longValue).sorted().toArray();
            long sum = 0;
            for (long v : arr) sum += v;
            double mean = sum / (double) arr.length;
            long p50 = arr[arr.length / 2];
            long p95 = arr[Math.min(arr.length - 1, (int) Math.floor(arr.length * 0.95))];
            return new RouteSnapshot(count, arr.length, mean, p50, p95, arr[0], arr[arr.length - 1]);
        }
    }
}
