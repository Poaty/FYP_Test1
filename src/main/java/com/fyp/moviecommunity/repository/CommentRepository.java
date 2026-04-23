package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Comments on a post, oldest first (natural reading order). */
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    /** Used by feed cards to show a comment count under each post. */
    long countByPost(Post post);
}
