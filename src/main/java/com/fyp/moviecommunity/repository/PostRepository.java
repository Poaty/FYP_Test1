package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.model.User;
import java.util.List;
import java.util.Optional;
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
     * lazy-load queries per row. Used on hot paths like the For You pool.
     */
    @Query("""
        select p from Post p
        join fetch p.user
        join fetch p.movie
        order by p.createdAt desc
        """)
    List<Post> findRecentWithAuthors(Pageable pageable);

    /**
     * Same as above but returns a Page (with total count) so /feed can
     * render pagination controls. Explicit countQuery avoids Hibernate
     * generating a bad one due to the JOIN FETCH.
     */
    @Query(value = """
            select p from Post p
            join fetch p.user
            join fetch p.movie
            order by p.createdAt desc
            """,
           countQuery = "select count(p) from Post p")
    Page<Post> findPageWithAuthors(Pageable pageable);

    /**
     * Single post with user + movie already loaded. Used by the /posts/{id}
     * show page -- without this, Thymeleaf tries to lazy-load movie/user
     * after the Hibernate session is closed (we have open-in-view off).
     */
    @Query("""
        select p from Post p
        join fetch p.user
        join fetch p.movie
        where p.id = :id
        """)
    Optional<Post> findByIdWithAuthor(Long id);

    /**
     * Batch count for the For You algorithm: how many posts exist about
     * each of these movies. Returns (imdbId, count) rows.
     */
    @Query("""
        select p.movie.imdbId, count(p)
        from Post p
        where p.movie.imdbId in :imdbIds
        group by p.movie.imdbId
        """)
    List<Object[]> countByMovieImdbIdIn(java.util.Collection<String> imdbIds);
}
