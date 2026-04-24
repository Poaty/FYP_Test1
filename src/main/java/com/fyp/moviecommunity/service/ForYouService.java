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

/**
 * The For You algorithm. The thesis centrepiece.
 *
 * Goal: push back against the echo-chamber effect (Lee, Park & Han 2008) by
 * forcing 1 in every N feed slots to be a LOW-engagement post. Mainstream
 * recommender systems surface the consensus; this one deliberately surfaces
 * quieter voices every few slots.
 *
 * Step by step:
 *   1. Grab a pool of recent posts (up to foryou.pool-size).
 *   2. For each, compute a popularity score:
 *        score = commentCount * w_comment
 *              + otherPostsOnSameMovie * w_movie
 *              - daysOld * w_age
 *   3. Rank by score DESC.
 *   4. Popular slots = top (ratio / (ratio+1)) * size.
 *   5. Quiet-pick slots = lowest-scoring posts NOT already picked.
 *   6. Interleave: every (ratio+1)th slot gets a quiet pick.
 *
 * --- Labels are a separate thing from slot placement. ---
 * The algorithm places posts in slots; the badge on the card describes what
 * the post ACTUALLY is, not what slot it landed in. A post only gets the
 * "popular" badge if its comment count is >= threshold, where threshold
 * is 20% (configurable) of the peak comment count on any post from the
 * last 7 days. A post in a quiet-pick slot below the threshold is labelled
 * "quiet pick". Anything else shows no badge. This prevents lying to the
 * user ("popular" with 0 comments makes no sense).
 */
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

    /**
     * A feed entry.
     *   post           -- the content
     *   unconventional -- did the algorithm use a quiet-pick slot for this?
     *   commentCount   -- how many comments on the post
     *   label          -- what badge to show: "popular", "quiet pick", or null
     */
    public record FeedSlot(Post post, boolean unconventional, long commentCount, String label) {}

    /** Build a feed of up to `size` posts, 1:N diversity-weighted. */
    public List<FeedSlot> buildFeed(int size) {
        if (size <= 0) return List.of();

        List<Post> pool = posts.findRecentWithAuthors(PageRequest.of(0, poolSize));
        if (pool.isEmpty()) return List.of();

        // Batch-fetch the two signals we need.
        List<Long> postIds = pool.stream().map(Post::getId).toList();
        List<String> imdbIds = pool.stream()
                .map(p -> p.getMovie().getImdbId())
                .distinct()
                .toList();

        Map<Long, Long> commentCounts = toMap(comments.countByPostIdIn(postIds));
        Map<String, Long> moviePostCounts = toMap(posts.countByMovieImdbIdIn(imdbIds));

        // Popularity threshold: 20% (default) of the peak comment count from the
        // last 7 days. max(1, ...) so even a sleepy DB has a sensible floor.
        long popularThreshold = computePopularThreshold(pool, commentCounts);
        log.debug("For You: popular threshold = {} comments", popularThreshold);

        // Score every post in the pool.
        Instant now = Instant.now();
        Map<Post, Double> scores = pool.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        p -> score(p, commentCounts, moviePostCounts, now)));

        // Rank DESC for popular slots, ASC for quiet picks.
        List<Post> byPopular = pool.stream()
                .sorted(Comparator.<Post>comparingDouble(scores::get).reversed())
                .toList();

        int quietSlots = size / (diversityRatio + 1);
        int popularSlots = size - quietSlots;

        List<Post> popularBucket = byPopular.stream().limit(popularSlots).toList();
        Set<Long> popularIds = popularBucket.stream().map(Post::getId).collect(Collectors.toSet());

        List<Post> quietBucket = pool.stream()
                .sorted(Comparator.comparingDouble(scores::get))
                .filter(p -> !popularIds.contains(p.getId()))
                .limit(quietSlots)
                .toList();

        log.debug("For You: pool={} popularBucket={} quietBucket={}",
                pool.size(), popularBucket.size(), quietBucket.size());

        // Interleave [P, P, P, P, Q, P, P, P, P, Q, ...].
        List<FeedSlot> feed = new ArrayList<>(size);
        int pIdx = 0, qIdx = 0;
        for (int slot = 0; slot < size; slot++) {
            boolean wantsQuiet = ((slot + 1) % (diversityRatio + 1)) == 0;
            if (wantsQuiet && qIdx < quietBucket.size()) {
                feed.add(makeSlot(quietBucket.get(qIdx++), true, commentCounts, popularThreshold));
            } else if (pIdx < popularBucket.size()) {
                feed.add(makeSlot(popularBucket.get(pIdx++), false, commentCounts, popularThreshold));
            } else if (qIdx < quietBucket.size()) {
                // Ran out of popular -- fill with whatever's left.
                feed.add(makeSlot(quietBucket.get(qIdx++), true, commentCounts, popularThreshold));
            }
        }
        return feed;
    }

    /**
     * Label policy:
     *   - commentCount >= threshold  -> "popular"
     *   - else if the algorithm used a quiet-pick slot -> "quiet pick"
     *   - else -> null (no badge; just a middle-of-pool post)
     *
     * This separates *what slot we placed it in* from *what it actually is*.
     * Stops us lying to the user (no more "popular" labels on 0-comment posts).
     */
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

    /**
     * Peak = max comments on any post created in the last N days.
     * Threshold = max(1, round(peak * ratio)).
     */
    private long computePopularThreshold(List<Post> pool, Map<Long, Long> commentCounts) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(popularWindowDays);
        long peak = pool.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(cutoff))
                .mapToLong(p -> commentCounts.getOrDefault(p.getId(), 0L))
                .max()
                .orElse(0L);
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
