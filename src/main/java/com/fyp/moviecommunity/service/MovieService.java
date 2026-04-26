package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.omdb.OmdbClient;
import com.fyp.moviecommunity.omdb.OmdbSearchItem;
import com.fyp.moviecommunity.omdb.OmdbSearchResponse;
import com.fyp.moviecommunity.repository.MovieRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Note: PerformanceMetrics import is in the same package, no import needed.

/**
 * Finds movies. Caches OMDb hits in our DB.
 *
 *   search("Inception")     -> always hits OMDb, returns lightweight summaries
 *   findOrFetch("tt...")    -> DB first, OMDb if missing, saves to DB on fetch
 *
 * Why cache (we're on the paid OMDb tier so it's not about rate limits):
 *   - latency: DB lookup is ~10ms, OMDb round-trip is 100-500ms. Feeds that
 *     show many posts would feel slow without caching.
 *   - resilience: if OMDb blips, cached movies still render.
 *   - queryability: once movies are in Postgres we can JOIN them with posts
 *     for things like "most discussed films this week".
 */
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

    /** Live search -- we don't cache search results, users type free text. */
    public List<OmdbSearchItem> search(String query) {
        return omdb.search(query);
    }

    /** A page of search results (10 per page, OMDb's fixed page size).
     *  Includes total result count so the UI can render pagination. */
    public SearchPage searchPaged(String query, int page) {
        var resp = omdb.search(query, Math.max(1, page));
        int total = parseInt(resp.getTotalResults());
        int totalPages = (int) Math.ceil(total / 10.0);
        List<OmdbSearchItem> items = resp.getSearch() == null ? List.of() : resp.getSearch();
        return new SearchPage(items, total, Math.max(1, page), Math.max(1, totalPages));
    }

    /** Returned by searchPaged -- everything the template needs to show pagination. */
    public record SearchPage(
            List<OmdbSearchItem> items,
            int totalResults,
            int currentPage,
            int totalPages
    ) {
        public boolean hasPrevious() { return currentPage > 1; }
        public boolean hasNext()     { return currentPage < totalPages; }
        public boolean isEmpty()     { return items.isEmpty(); }
    }

    private static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Get a movie by IMDb ID. Cheap if already cached; one OMDb call if not.
     * Empty Optional only if OMDb doesn't know the ID either.
     */
    @Transactional
    public Optional<Movie> findOrFetch(String imdbId) {
        Optional<Movie> cached = movies.findById(imdbId);
        if (cached.isPresent()) {
            perf.recordCacheHit();
            return cached;
        }
        perf.recordCacheMiss();

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

    /**
     * OMDb sends years like "2014", "2014–", "2014–2017". Take the first 4
     * digits, null if we can't parse anything sensible.
     */
    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) return null;
        try {
            return Integer.parseInt(year.replaceAll("\\D.*", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
