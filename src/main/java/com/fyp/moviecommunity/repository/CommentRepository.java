package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Comments on a post, oldest first. JOIN FETCH on the author so the
     * show page can render `c.user.username` without triggering a
     * lazy-load after the session closes (open-in-view is off).
     */
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post = :post
        order by c.createdAt asc
        """)
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    /** Used by feed cards to show a comment count under each post. */
    long countByPost(Post post);

    /**
     * Batch count for the For You algorithm -- one query returns
     * (postId, count) rows for every post id we care about. Avoids
     * N separate countByPost calls per feed render.
     *
     * Returns List<Object[]> because JPQL can't emit record projections
     * cleanly without a bit of plumbing. Converted to Map<Long, Long>
     * in the service layer.
     */
    @Query("""
        select c.post.id, count(c)
        from Comment c
        where c.post.id in :postIds
        group by c.post.id
        """)
    List<Object[]> countByPostIdIn(Collection<Long> postIds);
}
