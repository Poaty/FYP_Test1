package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // top level comments on a post
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post = :post and c.parent is null
        order by c.createdAt asc
        """)
    List<Comment> findTopLevelByPost(Post post);

    // replies for the visible top level comments
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.parent.id in :parentIds
        order by c.createdAt asc
        """)
    List<Comment> findRepliesByParentIds(Collection<Long> parentIds);

    // total comments per post including replies
    @Query("""
        select c.post.id, count(c)
        from Comment c
        where c.post.id in :postIds
        group by c.post.id
        """)
    List<Object[]> countByPostIdIn(Collection<Long> postIds);

    // top level comments across feed cards
    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post.id in :postIds and c.parent is null
        """)
    List<Comment> findTopLevelByPostIds(Collection<Long> postIds);

    // reply counts for picking the preview comment
    @Query("""
        select c.parent.id, count(c)
        from Comment c
        where c.parent.id in :parentIds
        group by c.parent.id
        """)
    List<Object[]> countRepliesByParentIds(Collection<Long> parentIds);
}