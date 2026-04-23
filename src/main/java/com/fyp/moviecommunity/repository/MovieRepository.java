package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Primary key is {@code imdb_id} (String) -- Spring Data will give us
 * {@code findById(String)}, {@code existsById(String)}, etc. for free.
 */
public interface MovieRepository extends JpaRepository<Movie, String> {
}
