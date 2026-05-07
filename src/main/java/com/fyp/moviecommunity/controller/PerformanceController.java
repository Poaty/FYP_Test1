package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.PerformanceMetrics;
import com.fyp.moviecommunity.service.PerformanceMetrics.RouteSnapshot;
import com.fyp.moviecommunity.service.PerformanceMetrics.Snapshot;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class PerformanceController {

    private final PerformanceMetrics metrics;

    public PerformanceController(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/perf")
    public String perf(Model model) {
        // current runtime snapshot
        model.addAttribute("snapshot", metrics.snapshot());
        return "admin/perf";
    }

    @GetMapping(value = "/perf.csv", produces = "text/csv")
    public ResponseEntity<String> perfCsv() {
        Snapshot snapshot = metrics.snapshot();
        StringBuilder csv = new StringBuilder();

        // csv export for report evidence
        writeOmdbCounters(csv, snapshot);
        csv.append('\n');
        writeRouteTimings(csv, snapshot.routes());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"perf.csv\"")
                .body(csv.toString());
    }

    private static void writeOmdbCounters(StringBuilder csv, Snapshot snapshot) {
        csv.append("# OMDb counters\n");
        csv.append("metric,value\n");
        csv.append("omdb_calls,").append(snapshot.omdbCalls()).append('\n');
        csv.append("omdb_mean_ms,").append(formatNumber(snapshot.omdbMeanMs())).append('\n');
        csv.append("cache_hits,").append(snapshot.cacheHits()).append('\n');
        csv.append("cache_misses,").append(snapshot.cacheMisses()).append('\n');
        csv.append("cache_lookups,").append(snapshot.cacheLookups()).append('\n');
        csv.append("cache_hit_rate_pct,").append(formatNumber(snapshot.cacheHitRatePct())).append('\n');
    }

    private static void writeRouteTimings(StringBuilder csv, Map<String, RouteSnapshot> routes) {
        csv.append("# Per-route timing (rolling window of last 200 requests)\n");
        csv.append("route,total_count,sample_count,mean_ms,p50_ms,p95_ms,min_ms,max_ms\n");

        for (Map.Entry<String, RouteSnapshot> entry : routes.entrySet()) {
            RouteSnapshot route = entry.getValue();
            csv.append('"').append(entry.getKey()).append('"').append(',')
                    .append(route.requestCount()).append(',')
                    .append(route.sampleCount()).append(',')
                    .append(formatNumber(route.meanMs())).append(',')
                    .append(route.p50Ms()).append(',')
                    .append(route.p95Ms()).append(',')
                    .append(route.minMs()).append(',')
                    .append(route.maxMs()).append('\n');
        }
    }

    private static String formatNumber(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return String.valueOf(value);
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}