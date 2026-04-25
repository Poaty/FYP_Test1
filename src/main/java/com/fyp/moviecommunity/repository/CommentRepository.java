package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Top-level comments on a post (no replies), oldest first.
     * Author joined in so the show page renders without lazy-loading.
     */
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post = :post and c.parent is null
        order by c.createdAt asc
        """)
    List<Comment> findTopLevelByPost(Post post);

    /**
     * Replies to any of the given top-level comments. Used to populate the
     * show page in a single query rather than N queries (one per top-level).
     */
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.parent.id in :parentIds
        order by c.createdAt asc
        """)
    List<Comment> findRepliesByParentIds(Collection<Long> parentIds);

    /** Total comment count per post -- includes top-level AND replies, since
     *  every comment row has post_id set regardless of nesting. */
    @Query("""
        select c.post.id, count(c)
        from Comment c
        where c.post.id in :postIds
        group by c.post.id
        """)
    List<Object[]> countByPostIdIn(Collection<Long> postIds);

    /**
     * Top-level comments only, for a batch of posts. Used by the feed-card
     * top-comment preview computation.
     */
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post.id in :postIds and c.parent is null
        """)
    List<Comment> findTopLevelByPostIds(Collection<Long> postIds);

    /**
     * Reply counts per parent comment id. Used together with findTopLevelByPostIds
     * to determine which top-level comment has the most replies for the preview.
     */
    @Query("""
        select c.parent.id, count(c)
        from Comment c
        where c.parent.id in :parentIds
        group by c.parent.id
        """)
    List<Object[]> countRepliesByParentIds(Collection<Long> parentIds);
}
