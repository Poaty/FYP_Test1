package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ForYouService {

    private static final Logger log = LoggerFactory.getLogger(ForYouService.class);

    public static final String LABEL_POPULAR = "popular";
    public static final String LABEL_QUIET_PICK = "quiet pick";

    private final PostRepository posts;
    private final CommentRepository comments;

    private final int diversityRatio;
    private final int poolSize;
    private final double wComment;
    private final double wMoviePopularity;
    private final double wAgePenalty;
    private final double popularThresholdRatio;
    private final int popularWindowDays;

    public ForYouService(PostRepository posts,
                         CommentRepository comments,
                         @Value("${foryou.diversity-ratio:4}") int diversityRatio,
                         @Value("${foryou.pool-size:100}") int poolSize,
                         @Value("${foryou.weight.comment:3.0}") double wComment,
                         @Value("${foryou.weight.movie-popularity:1.0}") double wMoviePopularity,
                         @Value("${foryou.weight.age-penalty:0.2}") double wAgePenalty,
                         @Value("${foryou.popular-threshold.ratio:0.2}") double popularThresholdRatio,
                         @Value("${foryou.popular-threshold.window-days:7}") int popularWindowDays) {
        this.posts = posts;
        this.comments = comments;
        this.diversityRatio = diversityRatio;
        this.poolSize = poolSize;
        this.wComment = wComment;
        this.wMoviePopularity = wMoviePopularity;
        this.wAgePenalty = wAgePenalty;
        this.popularThresholdRatio = popularThresholdRatio;
        this.popularWindowDays = popularWindowDays;
    }

    public record FeedSlot(Post post, boolean unconventional, long commentCount, String label) {}

    public record Diagnostics(
            List<Post> pool,
            Map<Long, Long> commentCounts,
            List<Post> baselineFeed,
            List<FeedSlot> diverseFeed
    ) {}

    public List<FeedSlot> buildFeed(int size) {
        return diagnostics(size).diverseFeed();
    }

    public Diagnostics diagnostics(int size) {
        if (size <= 0) {
            return new Diagnostics(List.of(), Map.of(), List.of(), List.of());
        }

        List<Post> pool = posts.findRecentWithAuthors(PageRequest.of(0, poolSize));
        if (pool.isEmpty()) {
            return new Diagnostics(List.of(), Map.of(), List.of(), List.of());
        }

        // ids needed for the batched count queries
        List<Long> postIds = pool.stream().map(Post::getId).toList();
        List<String> imdbIds = pool.stream()
                .map(p -> p.getMovie().getImdbId())
                .distinct()
                .toList();

        Map<Long, Long> commentCounts = toMap(comments.countByPostIdIn(postIds));
        Map<String, Long> moviePostCounts = toMap(posts.countByMovieImdbIdIn(imdbIds));

        // score every post once and reuse it
        Instant now = Instant.now();
        Map<Post, Double> scores = pool.stream().collect(Collectors.toMap(
                Function.identity(),
                p -> score(p, commentCounts, moviePostCounts, now)));

        List<Post> baseline = pool.stream()
                .sorted(Comparator.<Post>comparingDouble(scores::get).reversed())
                .limit(size)
                .toList();

        List<FeedSlot> diverse = buildDiverseFeed(pool, commentCounts, scores, size);
        return new Diagnostics(pool, commentCounts, baseline, diverse);
    }

    private List<FeedSlot> buildDiverseFeed(List<Post> pool,
                                            Map<Long, Long> commentCounts,
                                            Map<Post, Double> scores,
                                            int size) {
        long popularThreshold = computePopularThreshold(pool, commentCounts);
        log.debug("For You: popular threshold = {} comments", popularThreshold);

        // most engaged posts first
        List<Post> byPopular = pool.stream()
                .sorted(Comparator.<Post>comparingDouble(scores::get).reversed())
                .toList();

        int quietSlots = size / (diversityRatio + 1);
        int popularSlots = size - quietSlots;

        List<Post> popularBucket = byPopular.stream().limit(popularSlots).toList();
        Set<Long> popularIds = popularBucket.stream().map(Post::getId).collect(Collectors.toSet());

        // lowest scored posts that are not already in the popular bucket
        List<Post> quietBucket = pool.stream()
                .sorted(Comparator.comparingDouble(scores::get))
                .filter(p -> !popularIds.contains(p.getId()))
                .limit(quietSlots)
                .toList();

        log.debug("For You: pool={} popularBucket={} quietBucket={}",
                pool.size(), popularBucket.size(), quietBucket.size());

        List<FeedSlot> feed = new ArrayList<>(size);
        int pIdx = 0;
        int qIdx = 0;

        for (int slot = 0; slot < size; slot++) {
            boolean wantsQuiet = ((slot + 1) % (diversityRatio + 1)) == 0;

            if (wantsQuiet && qIdx < quietBucket.size()) {
                feed.add(makeSlot(quietBucket.get(qIdx++), true, commentCounts, popularThreshold));
            } else if (pIdx < popularBucket.size()) {
                feed.add(makeSlot(popularBucket.get(pIdx++), false, commentCounts, popularThreshold));
            } else if (qIdx < quietBucket.size()) {
                feed.add(makeSlot(quietBucket.get(qIdx++), true, commentCounts, popularThreshold));
            }
        }

        return feed;
    }

    private FeedSlot makeSlot(Post p, boolean unconventional,
                              Map<Long, Long> commentCounts, long threshold) {
        long count = commentCounts.getOrDefault(p.getId(), 0L);
        String label;

        if (count >= threshold) {
            label = LABEL_POPULAR;
        } else if (unconventional) {
            label = LABEL_QUIET_PICK;
        } else {
            label = null;
        }

        return new FeedSlot(p, unconventional, count, label);
    }

    private long computePopularThreshold(List<Post> pool, Map<Long, Long> commentCounts) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(popularWindowDays);
        long peak = pool.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(cutoff))
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .max()
                .orElse(0L);

        // at least one comment needed before something is treated as popular
        return Math.max(1L, Math.round(peak * popularThresholdRatio));
    }

    private double score(Post p,
                         Map<Long, Long> commentCounts,
                         Map<String, Long> moviePostCounts,
                         Instant now) {
        long commentCount = commentCounts.getOrDefault(p.getId(), 0L);
        long otherPosts = Math.max(0,
                moviePostCounts.getOrDefault(p.getMovie().getImdbId(), 1L) - 1);
        long daysOld = ChronoUnit.DAYS.between(p.getCreatedAt().toInstant(), now);

        return commentCount * wComment
                + otherPosts * wMoviePopularity
                - daysOld * wAgePenalty;
    }

    @SuppressWarnings("unchecked")
    private static <K> Map<K, Long> toMap(List<Object[]> rows) {
        Map<K, Long> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            K key = (K) row[0];
            map.put(key, ((Number) row[1]).longValue());
        }
        return map;
    }
}