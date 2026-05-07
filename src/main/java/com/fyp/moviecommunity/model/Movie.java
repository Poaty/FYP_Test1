package com.fyp.moviecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    // omdb id is used as the primary key
    @Id
    @Column(name = "imdb_id")
    private String imdbId;

    @Column(nullable = false)
    private String title;

    private Integer year;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(columnDefinition = "text")
    private String plot;

    private String genre;

    private String director;

    // when this movie was cached locally
    @Column(name = "cached_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime cachedAt;
}