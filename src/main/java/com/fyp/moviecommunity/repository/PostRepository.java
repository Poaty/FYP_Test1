package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** Chronological feed, newest first. For the plain /feed page. */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** All posts by a given user, newest first (for profile pages). */
    List<Post> findByUserOrderByCreatedAtDesc(User user);

    /** All posts about a specific movie, newest first. */
    List<Post> findByMovieImdbIdOrderByCreatedAtDesc(String imdbId);

    /**
     * Feed eagerly loaded -- joins user + movie so templates don't fire
     * lazy-load queries per row. Used on hot paths like the main feed.
     */
    @Query("""
        select p from Post p
        join fetch p.user
        join fetch p.movie
        order by p.createdAt desc
        """)
    List<Post> findRecentWithAuthors(Pageable pageable);
}
