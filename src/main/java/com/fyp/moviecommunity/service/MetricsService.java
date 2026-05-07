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

@Service
public class MetricsService {

    private final ForYouService forYou;

    public MetricsService(ForYouService forYou) {
        this.forYou = forYou;
    }

    public record FeedMetrics(
            int feedSize,
            int poolSize,
            double shannonEntropy,
            double shannonMax,
            double shannonNormalised,
            int uniqueAuthors,
            double uniqueAuthorRatio,
            double longTailPct,
            long poolMedianComments,
            double meanCommentsPerSlot
    ) {}

    public record Comparison(FeedMetrics baseline, FeedMetrics diverse) {}

    public Comparison compareFeeds(int size) {
        Diagnostics d = forYou.diagnostics(size);
        long poolMedian = medianComments(d.pool(), d.commentCounts());

        FeedMetrics baseline = metricsFor(d.baselineFeed(), d.pool().size(),
                d.commentCounts(), poolMedian);
        FeedMetrics diverse = metricsFor(slotsToPosts(d.diverseFeed()), d.pool().size(),
                d.commentCounts(), poolMedian);

        return new Comparison(baseline, diverse);
    }

    private FeedMetrics metricsFor(List<Post> feed, int poolSize,
                                   Map<Long, Long> commentCounts, long poolMedian) {
        if (feed.isEmpty()) {
            return new FeedMetrics(0, poolSize, 0, 0, 0, 0, 0, 0, poolMedian, 0);
        }

        double[] entropy = shannonEntropy(feed);
        Set<Long> authors = feed.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());

        // posts at or below the pool median are counted as long tail
        long longTail = feed.stream()
                .filter(p -> commentCounts.getOrDefault(p.getId(), 0L) <= poolMedian)
                .count();

        double meanComments = feed.stream()
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .average()
                .orElse(0.0);

        return new FeedMetrics(
                feed.size(),
                poolSize,
                entropy[0],
                entropy[1],
                entropy[1] == 0 ? 0 : entropy[0] / entropy[1],
                authors.size(),
                (double) authors.size() / feed.size(),
                100.0 * longTail / feed.size(),
                poolMedian,
                meanComments);
    }

    private static double[] shannonEntropy(List<Post> feed) {
        Map<String, Long> counts = new HashMap<>();
        long total = 0;

        // split omdb's comma separated genre field
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

    private static long medianComments(List<Post> pool, Map<Long, Long> commentCounts) {
        if (pool.isEmpty()) return 0;

        long[] sorted = pool.stream()
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .sorted()
                .toArray();

        int n = sorted.length;
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }

    private static List<Post> slotsToPosts(List<FeedSlot> slots) {
        return slots.stream().map(FeedSlot::post).toList();
    }
}