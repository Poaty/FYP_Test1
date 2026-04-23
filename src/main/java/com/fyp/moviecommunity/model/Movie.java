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

/**
 * A movie cached from the OMDb API.
 *
 * <p>The primary key is OMDb's {@code imdb_id} (e.g. "tt3896198"),
 * so we avoid duplicating the same movie across posts. Whenever a
 * user writes a post about a movie we haven't seen, we fetch from
 * OMDb and upsert into this table.
 */
@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

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

    @Column(name = "cached_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime cachedAt;
}
