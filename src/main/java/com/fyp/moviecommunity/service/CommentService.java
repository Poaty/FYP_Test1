package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.repository.CommentRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Comment-related orchestration that's used in more than one place.
 *
 * Right now: building "top comment preview" for feed cards. Could grow to
 * include reply-tree assembly for show pages if that gets duplicated too.
 */
@Service
public class CommentService {

    private final CommentRepository comments;

    public CommentService(CommentRepository comments) {
        this.comments = comments;
    }

    /** A snippet shown on a feed card under the post body. */
    public record TopCommentPreview(String authorUsername, String content, long replyCount) {}

    /**
     * For each post in the input, find the "top" top-level comment -- the
     * one with the most replies. Ties broken by recency (newer wins).
     *
     * Posts with no top-level comments are simply absent from the returned map.
     *
     * Cost: 2 batched DB queries regardless of input size. We avoid an
     * N+1 trap that would have been easy to fall into here.
     */
    public Map<Long, TopCommentPreview> topCommentByPost(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();

        // Query 1: every top-level comment for the visible posts (with author).
        List<Comment> topLevel = comments.findTopLevelByPostIds(postIds);
        if (topLevel.isEmpty()) return Map.of();

        // Query 2: reply counts per top-level comment.
        List<Long> topLevelIds = topLevel.stream().map(Comment::getId).toList();
        Map<Long, Long> replyCounts = new HashMap<>();
        for (Object[] row : comments.countRepliesByParentIds(topLevelIds)) {
            replyCounts.put((Long) row[0], ((Number) row[1]).longValue());
        }

        // Group by post; pick the comment with the most replies, ties to newest.
        Map<Long, List<Comment>> byPost = topLevel.stream()
                .collect(Collectors.groupingBy(c -> c.getPost().getId()));

        Map<Long, TopCommentPreview> result = new HashMap<>();
        for (Map.Entry<Long, List<Comment>> entry : byPost.entrySet()) {
            Long postId = entry.getKey();
            Comment best = entry.getValue().stream()
                    .max(Comparator
                            .<Comment>comparingLong(c -> replyCounts.getOrDefault(c.getId(), 0L))
                            .thenComparing(Comment::getCreatedAt))
                    .orElse(null);
            if (best != null) {
                long replies = replyCounts.getOrDefault(best.getId(), 0L);
                result.put(postId, new TopCommentPreview(
                        best.getUser().getUsername(),
                        best.getContent(),
                        replies));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
