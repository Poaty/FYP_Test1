package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.service.ForYouService.Diagnostics;
import com.fyp.moviecommunity.service.ForYouService.FeedSlot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Quantitative comparison: popularity-only baseline vs diversity-weighted feed.
 *
 * Same input pool, same scoring formula, only difference is whether the
 * 1:4 quiet-pick interleaving fires. Lets Chapter 5 say things like
 * "Shannon entropy was X% higher" with actual numbers behind it.
 *
 * Four metrics computed on each feed:
 *   - Shannon entropy of genre distribution (diversity in bits)
 *   - Unique-author ratio (distinct authors / feed size)
 *   - Long-tail share (% of slots with comment count <= pool median)
 *   - Mean comment count per slot
 */
@Service
public class MetricsService {

    private final ForYouService forYou;

    public MetricsService(ForYouService forYou) {
        this.forYou = forYou;
    }

    /** Numbers for one feed. All defaults sensible if the feed is empty. */
    public record FeedMetrics(
            int    feedSize,
            int    poolSize,
            double shannonEntropy,    // bits, 0 if no data
            double shannonMax,        // log2(distinct genres) -- the theoretical ceiling
            double shannonNormalised, // entropy / max, 0..1 (1.0 = perfectly even)
            int    uniqueAuthors,
            double uniqueAuthorRatio,
            double longTailPct,       // % of feed slots with commentCount <= pool median
            long   poolMedianComments,
            double meanCommentsPerSlot
    ) {}

    /** Side-by-side comparison shown on /admin/metrics. */
    public record Comparison(FeedMetrics baseline, FeedMetrics diverse) {}

    /** Run the algorithm + baseline at the given feed size and compute metrics. */
    public Comparison compareFeeds(int size) {
        Diagnostics d = forYou.diagnostics(size);
        long poolMedian = medianComments(d.pool(), d.commentCounts());
        FeedMetrics baseline = metricsFor(d.baselineFeed(), d.pool().size(),
                d.commentCounts(), poolMedian);
        FeedMetrics diverse = metricsFor(slotsToPosts(d.diverseFeed()), d.pool().size(),
                d.commentCounts(), poolMedian);
        return new Comparison(baseline, diverse);
    }

    // -------- per-feed metrics --------

    private FeedMetrics metricsFor(List<Post> feed, int poolSize,
                                   Map<Long, Long> commentCounts, long poolMedian) {
        if (feed.isEmpty()) {
            return new FeedMetrics(0, poolSize, 0, 0, 0, 0, 0, 0, poolMedian, 0);
        }
        double[] entropy = shannonEntropy(feed);
        Set<Long> authors = feed.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());
        long longTail = feed.stream()
                .filter(p -> commentCounts.getOrDefault(p.getId(), 0L) <= poolMedian)
                .count();
        double meanComments = feed.stream()
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .average().orElse(0.0);
        return new FeedMetrics(
                feed.size(),
                poolSize,
                entropy[0],                                // shannon
                entropy[1],                                // shannon max
                entropy[1] == 0 ? 0 : entropy[0] / entropy[1], // normalised
                authors.size(),
                (double) authors.size() / feed.size(),
                100.0 * longTail / feed.size(),
                poolMedian,
                meanComments);
    }

    /**
     * Returns [entropy_in_bits, max_entropy_for_this_corpus].
     * Genres are split on commas (OMDb returns "Drama, Crime, Thriller").
     * Each individual genre token contributes 1 to the count.
     */
    private static double[] shannonEntropy(List<Post> feed) {
        Map<String, Long> counts = new HashMap<>();
        long total = 0;
        for (Post p : feed) {
            String g = p.getMovie().getGenre();
            if (g == null) continue;
            for (String tok : g.split(",")) {
                String genre = tok.trim();
                if (!genre.isEmpty()) {
                    counts.merge(genre, 1L, Long::sum);
                    total++;
                }
            }
        }
        if (total == 0 || counts.size() <= 1) {
            return new double[]{0, 0};
        }
        double h = 0;
        for (long c : counts.values()) {
            double p = (double) c / total;
            h -= p * (Math.log(p) / Math.log(2));
        }
        double max = Math.log(counts.size()) / Math.log(2);
        return new double[]{h, max};
    }

    /** Median comment count across the entire pool (the long-tail threshold). */
    private static long medianComments(List<Post> pool, Map<Long, Long> commentCounts) {
        if (pool.isEmpty()) return 0;
        long[] sorted = pool.stream()
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .sorted()
                .toArray();
        int n = sorted.length;
        // Even N -> mean of the two middle values, but we want a long, so floor.
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }

    private static List<Post> slotsToPosts(List<FeedSlot> slots) {
        return slots.stream().map(FeedSlot::post).toList();
    }
}
