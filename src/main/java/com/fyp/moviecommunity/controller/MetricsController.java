package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.MetricsService;
import com.fyp.moviecommunity.service.MetricsService.Comparison;
import com.fyp.moviecommunity.service.MetricsService.FeedMetrics;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin-only quantitative-evaluation endpoints. Both routes are gated to
 * ROLE_ADMIN by the /admin/** rule in SecurityConfig.
 *
 *   GET /admin/metrics            -- HTML comparison table
 *   GET /admin/metrics.csv        -- raw numbers, paste straight into the report
 *
 * Default feed size = 20 (the For You page default). Override with ?size=N
 * for sensitivity analysis (run at 10, 20, 30 to show the metrics scale).
 */
@Controller
@RequestMapping("/admin")
public class MetricsController {

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public String metrics(@RequestParam(defaultValue = "20") int size, Model model) {
        Comparison c = metrics.compareFeeds(Math.max(1, Math.min(size, 100)));
        model.addAttribute("comparison", c);
        model.addAttribute("size", size);
        // Pre-computed deltas for the template (avoids math in Thymeleaf).
        model.addAttribute("deltas", new Deltas(c));
        return "admin/metrics";
    }

    @GetMapping(value = "/metrics.csv", produces = "text/csv")
    public ResponseEntity<String> metricsCsv(@RequestParam(defaultValue = "20") int size) {
        Comparison c = metrics.compareFeeds(Math.max(1, Math.min(size, 100)));
        FeedMetrics b = c.baseline();
        FeedMetrics d = c.diverse();

        StringBuilder out = new StringBuilder();
        out.append("metric,baseline,foryou,delta,pct_change\n");
        row(out, "feed_size", b.feedSize(), d.feedSize());
        row(out, "pool_size", b.poolSize(), d.poolSize());
        row(out, "pool_median_comments", b.poolMedianComments(), d.poolMedianComments());
        row(out, "shannon_entropy_bits", b.shannonEntropy(), d.shannonEntropy());
        row(out, "shannon_entropy_max_bits", b.shannonMax(), d.shannonMax());
        row(out, "shannon_normalised", b.shannonNormalised(), d.shannonNormalised());
        row(out, "unique_authors", b.uniqueAuthors(), d.uniqueAuthors());
        row(out, "unique_author_ratio", b.uniqueAuthorRatio(), d.uniqueAuthorRatio());
        row(out, "long_tail_pct", b.longTailPct(), d.longTailPct());
        row(out, "mean_comments_per_slot", b.meanCommentsPerSlot(), d.meanCommentsPerSlot());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"feed_metrics.csv\"")
                .body(out.toString());
    }

    // ---- CSV helpers ----

    private static void row(StringBuilder sb, String name, double base, double diverse) {
        double delta = diverse - base;
        double pct = base == 0 ? 0 : (delta / base) * 100.0;
        sb.append(name).append(',')
                .append(fmt(base)).append(',')
                .append(fmt(diverse)).append(',')
                .append(fmt(delta)).append(',')
                .append(fmt(pct)).append('\n');
    }

    private static String fmt(double v) {
        // Strip trailing zeros for clean CSV.
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.4f", v);
    }

    /**
     * Simple holder of pre-computed deltas + percentage changes so the
     * template doesn't have to do arithmetic in Thymeleaf expressions.
     */
    public record Deltas(double shannonDelta,    double shannonPct,
                         int    authorsDelta,    double authorsPct,
                         double longTailDelta,   double longTailPct,
                         double meanCommentsDelta, double meanCommentsPct) {

        public Deltas(Comparison c) {
            this(
                c.diverse().shannonEntropy() - c.baseline().shannonEntropy(),
                pct(c.baseline().shannonEntropy(),    c.diverse().shannonEntropy()),
                c.diverse().uniqueAuthors() - c.baseline().uniqueAuthors(),
                pct(c.baseline().uniqueAuthors(),     c.diverse().uniqueAuthors()),
                c.diverse().longTailPct() - c.baseline().longTailPct(),
                pct(c.baseline().longTailPct(),       c.diverse().longTailPct()),
                c.diverse().meanCommentsPerSlot() - c.baseline().meanCommentsPerSlot(),
                pct(c.baseline().meanCommentsPerSlot(), c.diverse().meanCommentsPerSlot())
            );
        }

        private static double pct(double a, double b) {
            return a == 0 ? 0 : ((b - a) / a) * 100.0;
        }
    }
}
