package com.fyp.moviecommunity.service;

import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.omdb.OmdbClient;
import com.fyp.moviecommunity.omdb.OmdbSearchItem;
import com.fyp.moviecommunity.repository.MovieRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public MovieService(MovieRepository movies, OmdbClient omdb) {
        this.movies = movies;
        this.omdb = omdb;
    }

    /** Live search -- we don't cache search results, users type free text. */
    public List<OmdbSearchItem> search(String query) {
        return omdb.search(query);
    }

    /**
     * Get a movie by IMDb ID. Cheap if already cached; one OMDb call if not.
     * Empty Optional only if OMDb doesn't know the ID either.
     */
    @Transactional
    public Optional<Movie> findOrFetch(String imdbId) {
        Optional<Movie> cached = movies.findById(imdbId);
        if (cached.isPresent()) return cached;

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
