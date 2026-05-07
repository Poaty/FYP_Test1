package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.omdb.OmdbClient;
import com.fyp.moviecommunity.omdb.OmdbSearchItem;
import com.fyp.moviecommunity.repository.MovieRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {

    private final MovieRepository movies;
    private final OmdbClient omdb;
    private final PerformanceMetrics perf;

    public MovieService(MovieRepository movies, OmdbClient omdb, PerformanceMetrics perf) {
        this.movies = movies;
        this.omdb = omdb;
        this.perf = perf;
    }

    public SearchPage searchPaged(String query, int page) {
        var resp = omdb.search(query, Math.max(1, page));
        int total = parseInt(resp.getTotalResults());
        int totalPages = (int) Math.ceil(total / 10.0);
        List<OmdbSearchItem> items = resp.getSearch() == null ? List.of() : resp.getSearch();

        return new SearchPage(items, total, Math.max(1, page), Math.max(1, totalPages));
    }

    public record SearchPage(
            List<OmdbSearchItem> items,
            int totalResults,
            int currentPage,
            int totalPages
    ) {
        public boolean hasPrevious() {
            return currentPage > 1;
        }

        public boolean hasNext() {
            return currentPage < totalPages;
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    private static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    public Optional<Movie> findOrFetch(String imdbId) {
        Optional<Movie> cached = movies.findById(imdbId);
        if (cached.isPresent()) {
            perf.recordCacheHit();
            return cached;
        }

        perf.recordCacheMiss();

        // cache a full movie row if omdb has it
        return omdb.getByImdbId(imdbId).map(dto -> {
            Movie m = new Movie();
            m.setImdbId(dto.getImdbId());
            m.setTitle(dto.getTitle());
            m.setYear(parseYear(dto.getYear()));
            m.setPosterUrl("N/A".equals(dto.getPoster()) ? null : dto.getPoster());
            m.setPlot(dto.getPlot());
            m.setGenre(dto.getGenre());
            m.setDirector(dto.getDirector());
            return movies.save(m);
        });
    }

    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) return null;

        try {
            return Integer.parseInt(year.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}