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

@Service
public class CommentService {

    private final CommentRepository comments;

    public CommentService(CommentRepository comments) {
        this.comments = comments;
    }

    public record TopCommentPreview(String authorUsername, String content, long replyCount) {}

    public Map<Long, TopCommentPreview> topCommentByPost(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();

        // get top level comments for these posts
        List<Comment> topLevel = comments.findTopLevelByPostIds(postIds);
        if (topLevel.isEmpty()) return Map.of();

        // count replies against each top level comment
        List<Long> topLevelIds = topLevel.stream().map(Comment::getId).toList();
        Map<Long, Long> replyCounts = new HashMap<>();
        for (Object[] row : comments.countRepliesByParentIds(topLevelIds)) {
            replyCounts.put((Long) row[0], ((Number) row[1]).longValue());
        }

        Map<Long, List<Comment>> byPost = topLevel.stream()
                .collect(Collectors.groupingBy(c -> c.getPost().getId()));

        Map<Long, TopCommentPreview> result = new HashMap<>();
        for (Map.Entry<Long, List<Comment>> entry : byPost.entrySet()) {
            Long postId = entry.getKey();

            // most replied comment wins, newer comment breaks the tie
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