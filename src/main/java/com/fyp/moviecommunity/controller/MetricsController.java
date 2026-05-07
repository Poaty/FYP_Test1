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

@Controller
@RequestMapping("/admin")
public class MetricsController {

    private static final String DEFAULT_SIZE = "20";
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public String metrics(@RequestParam(defaultValue = DEFAULT_SIZE) int size, Model model) {
        // compare the normal feed against for you
        Comparison comparison = metrics.compareFeeds(clampSize(size));

        model.addAttribute("comparison", comparison);
        model.addAttribute("size", size);
        model.addAttribute("deltas", new Deltas(comparison));
        return "admin/metrics";
    }

    @GetMapping(value = "/metrics.csv", produces = "text/csv")
    public ResponseEntity<String> metricsCsv(@RequestParam(defaultValue = DEFAULT_SIZE) int size) {
        Comparison comparison = metrics.compareFeeds(clampSize(size));
        FeedMetrics baseline = comparison.baseline();
        FeedMetrics forYou = comparison.diverse();

        // csv version used for the report tables
        StringBuilder out = new StringBuilder();
        out.append("metric,baseline,foryou,delta,pct_change\n");
        appendRow(out, "feed_size", baseline.feedSize(), forYou.feedSize());
        appendRow(out, "pool_size", baseline.poolSize(), forYou.poolSize());
        appendRow(out, "pool_median_comments", baseline.poolMedianComments(), forYou.poolMedianComments());
        appendRow(out, "shannon_entropy_bits", baseline.shannonEntropy(), forYou.shannonEntropy());
        appendRow(out, "shannon_entropy_max_bits", baseline.shannonMax(), forYou.shannonMax());
        appendRow(out, "shannon_normalised", baseline.shannonNormalised(), forYou.shannonNormalised());
        appendRow(out, "unique_authors", baseline.uniqueAuthors(), forYou.uniqueAuthors());
        appendRow(out, "unique_author_ratio", baseline.uniqueAuthorRatio(), forYou.uniqueAuthorRatio());
        appendRow(out, "long_tail_pct", baseline.longTailPct(), forYou.longTailPct());
        appendRow(out, "mean_comments_per_slot", baseline.meanCommentsPerSlot(), forYou.meanCommentsPerSlot());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"feed_metrics.csv\"")
                .body(out.toString());
    }

    private static int clampSize(int requested) {
        // stop silly values from being passed in the url
        if (requested < MIN_SIZE) return MIN_SIZE;
        if (requested > MAX_SIZE) return MAX_SIZE;
        return requested;
    }

    private static void appendRow(StringBuilder sb, String name, double base, double diverse) {
        double delta = diverse - base;
        double pct = (base == 0) ? 0 : (delta / base) * 100.0;

        sb.append(name).append(',')
                .append(formatNumber(base)).append(',')
                .append(formatNumber(diverse)).append(',')
                .append(formatNumber(delta)).append(',')
                .append(formatNumber(pct)).append('\n');
    }

    private static String formatNumber(double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return String.valueOf(value);
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format("%.4f", value);
    }

    public record Deltas(double shannonDelta, double shannonPct,
                         int authorsDelta, double authorsPct,
                         double longTailDelta, double longTailPct,
                         double meanCommentsDelta, double meanCommentsPct) {

        public Deltas(Comparison comparison) {
            this(
                    comparison.diverse().shannonEntropy() - comparison.baseline().shannonEntropy(),
                    pct(comparison.baseline().shannonEntropy(), comparison.diverse().shannonEntropy()),
                    comparison.diverse().uniqueAuthors() - comparison.baseline().uniqueAuthors(),
                    pct(comparison.baseline().uniqueAuthors(), comparison.diverse().uniqueAuthors()),
                    comparison.diverse().longTailPct() - comparison.baseline().longTailPct(),
                    pct(comparison.baseline().longTailPct(), comparison.diverse().longTailPct()),
                    comparison.diverse().meanCommentsPerSlot() - comparison.baseline().meanCommentsPerSlot(),
                    pct(comparison.baseline().meanCommentsPerSlot(), comparison.diverse().meanCommentsPerSlot())
            );
        }

        private static double pct(double base, double revised) {
            return (base == 0) ? 0 : ((revised - base) / base) * 100.0;
        }
    }
}