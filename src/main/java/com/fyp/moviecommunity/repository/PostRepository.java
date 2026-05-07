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

    // normal chronological feed
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // posts by one user
    List<Post> findByUserOrderByCreatedAtDesc(User user);

    // posts for a movie page or lookup
    List<Post> findByMovieImdbIdOrderByCreatedAtDesc(String imdbId);

    // recent posts with user and movie ready for the feed
    @Query("""
        select p from Post p
        join fetch p.user
        join fetch p.movie
        order by p.createdAt desc
        """)
    List<Post> findRecentWithAuthors(Pageable pageable);

    // paged version used by /feed
    @Query(value = """
            select p from Post p
            join fetch p.user
            join fetch p.movie
            order by p.createdAt desc
            """,
            countQuery = "select count(p) from Post p")
    Page<Post> findPageWithAuthors(Pageable pageable);

    // single post for the show page
    @Query("""
        select p from Post p
        join fetch p.user
        join fetch p.movie
        where p.id = :id
        """)
    Optional<Post> findByIdWithAuthor(Long id);

    // post counts per movie for scoring
    @Query("""
        select p.movie.imdbId, count(p)
        from Post p
        where p.movie.imdbId in :imdbIds
        group by p.movie.imdbId
        """)
    List<Object[]> countByMovieImdbIdIn(java.util.Collection<String> imdbIds);
}