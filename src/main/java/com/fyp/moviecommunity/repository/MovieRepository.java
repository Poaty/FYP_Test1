package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, String> {
}