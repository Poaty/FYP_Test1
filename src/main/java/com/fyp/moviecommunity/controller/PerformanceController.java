package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import com.fyp.moviecommunity.service.PerformanceMetrics.RouteSnapshot;
import com.fyp.moviecommunity.service.PerformanceMetrics.Snapshot;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin-only performance dashboard. Live counters since app startup.
 *
 *   GET /admin/perf       -- HTML dashboard
 *   GET /admin/perf.csv   -- raw numbers, paste into the report
 */
@Controller
@RequestMapping("/admin")
public class PerformanceController {

    private final PerformanceMetrics metrics;

    public PerformanceController(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/perf")
    public String perf(Model model) {
        model.addAttribute("snapshot", metrics.snapshot());
        return "admin/perf";
    }

    @GetMapping(value = "/perf.csv", produces = "text/csv")
    public ResponseEntity<String> perfCsv() {
        Snapshot s = metrics.snapshot();
        StringBuilder sb = new StringBuilder();

        sb.append("# OMDb counters\n");
        sb.append("metric,value\n");
        sb.append("omdb_calls,").append(s.omdbCalls()).append('\n');
        sb.append("omdb_mean_ms,").append(fmt(s.omdbMeanMs())).append('\n');
        sb.append("cache_hits,").append(s.cacheHits()).append('\n');
        sb.append("cache_misses,").append(s.cacheMisses()).append('\n');
        sb.append("cache_lookups,").append(s.cacheLookups()).append('\n');
        sb.append("cache_hit_rate_pct,").append(fmt(s.cacheHitRatePct())).append('\n');
        sb.append('\n');

        sb.append("# Per-route timing (rolling window of last 200 requests)\n");
        sb.append("route,total_count,sample_count,mean_ms,p50_ms,p95_ms,min_ms,max_ms\n");
        for (var e : s.routes().entrySet()) {
            RouteSnapshot r = e.getValue();
            sb.append('"').append(e.getKey()).append('"').append(',')
                    .append(r.requestCount()).append(',')
                    .append(r.sampleCount()).append(',')
                    .append(fmt(r.meanMs())).append(',')
                    .append(r.p50Ms()).append(',')
                    .append(r.p95Ms()).append(',')
                    .append(r.minMs()).append(',')
                    .append(r.maxMs()).append('\n');
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"perf.csv\"")
                .body(sb.toString());
    }

    private static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
