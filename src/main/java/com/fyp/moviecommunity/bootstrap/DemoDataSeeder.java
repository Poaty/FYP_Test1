package com.fyp.moviecommunity.bootstrap;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.MovieRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.service.MovieService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data on startup. Split into two independent phases:
 *
 *   Core  -- 5 users, 10 movies, 16 posts, 20 comments.
 *             Idempotent: skipped if user "alice" already exists.
 *   Extra -- +5 movies, +15 posts, +30 comments (heavier engagement skew,
 *             so the popular-threshold kicks in meaningfully).
 *             Idempotent: skipped if the Matrix movie (tt0133093) is already cached.
 *
 * Two phases rather than one so you can re-run the app after the first seed
 * and still pick up the second round without having to truncate tables.
 *
 * Controlled by `app.demo-data.enabled` -- flip to false to skip entirely.
 * All seeded users share password "demo1234".
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String SEED_PASSWORD = "demo1234";

    // Core movies (seeded in phase 1)
    private static final String INTERSTELLAR   = "tt0816692";
    private static final String SHAWSHANK      = "tt0111161";
    private static final String DARK_KNIGHT    = "tt0468569";
    private static final String PULP_FICTION   = "tt0110912";
    private static final String INCEPTION      = "tt1375666";
    private static final String ETERNAL_SUN    = "tt0338013";
    private static final String PANS_LABYRINTH = "tt0457430";
    private static final String MONONOKE       = "tt0119698";
    private static final String GHOSTBUSTERS   = "tt0087332";
    private static final String SORRY_TO_BOTHER= "tt5294550";

    // Extra movies (seeded in phase 2)
    private static final String MATRIX         = "tt0133093";
    private static final String PARASITE       = "tt6751668";
    private static final String GROUNDHOG      = "tt0107048";
    private static final String SHINING        = "tt0081505";
    private static final String SEVEN          = "tt0114369";

    private final UserRepository users;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final MovieRepository movieRepo;
    private final MovieService movieService;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DemoDataSeeder(UserRepository users,
                          PostRepository posts,
                          CommentRepository comments,
                          MovieRepository movieRepo,
                          MovieService movieService,
                          PasswordEncoder encoder,
                          @Value("${app.demo-data.enabled:true}") boolean enabled) {
        this.users = users;
        this.posts = posts;
        this.comments = comments;
        this.movieRepo = movieRepo;
        this.movieService = movieService;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Demo seeding disabled via config.");
            return;
        }

        if (!users.existsByUsername("alice")) {
            seedCore();
        } else {
            log.info("Core seed already present (alice exists). Skipping phase 1.");
        }

        if (!movieRepo.existsById(MATRIX)) {
            seedExtra();
        } else {
            log.info("Extra seed already present (Matrix cached). Skipping phase 2.");
        }
    }

    // ========================================================================
    // Phase 1 -- Core seed
    // ========================================================================
    private void seedCore() {
        log.info("Phase 1: seeding core demo data...");

        User alice = saveUser("alice", "alice@demo.local", "Watches everything, opinions for days.");
        User bob   = saveUser("bob",   "bob@demo.local",   "Eclectic. Loves arthouse, tolerates blockbusters.");
        User carla = saveUser("carla", "carla@demo.local", "Quality over quantity -- posts when something really hits.");
        User dave  = saveUser("dave",  "dave@demo.local",  "Contrarian by default. If you loved it, I probably didn't.");
        User eve   = saveUser("eve",   "eve@demo.local",   "New here. Just getting started.");

        for (String id : List.of(INTERSTELLAR, SHAWSHANK, DARK_KNIGHT, PULP_FICTION, INCEPTION,
                                  ETERNAL_SUN, PANS_LABYRINTH, MONONOKE, GHOSTBUSTERS, SORRY_TO_BOTHER)) {
            movieService.findOrFetch(id);
        }

        Post p1 = savePost(alice, INTERSTELLAR,
                "Finally rewatched this. The docking scene still makes my jaw drop. " +
                "Some of the science is handwavey but Nolan earns every second of the runtime.");
        Post p2 = savePost(bob, INTERSTELLAR,
                "Hot take: the emotional beats carry the film harder than the physics. " +
                "Cooper-Murph scenes made me ugly-cry in a way I wasn't expecting.");
        Post p3 = savePost(dave, INTERSTELLAR,
                "I'll be honest -- bored me senseless. The ending felt like a cheat code. " +
                "Everyone on Letterboxd disagrees. That's fine.");

        Post p4 = savePost(alice, SHAWSHANK,
                "Holds up in a way most 'best of all time' films don't. " +
                "Rewatched with my flatmates, we all forgot dinner.");
        savePost(carla, SHAWSHANK,
                "The pacing is the secret. Never rushes, never drags. I keep coming back.");

        Post p6 = savePost(alice, DARK_KNIGHT,
                "Ledger carries a whole franchise on his shoulders. I don't care that it's been said, it's true.");
        savePost(bob, DARK_KNIGHT,
                "Rewatched this expecting it had aged. Still phenomenal. " +
                "Though the interrogation scene hits different now.");

        Post p8 = savePost(carla, PULP_FICTION,
                "I taught this to a friend who'd never seen it. Her reactions reminded me why it's so good.");
        savePost(dave, PULP_FICTION,
                "Tarantino at his most self-indulgent. I get why people love it. I find it tiring.");

        Post p10 = savePost(alice, INCEPTION,
                "Best rewatch value of any Nolan. You always catch something new.");
        savePost(bob, INCEPTION,
                "Limbo sequence is the closest cinema has got to a recurring dream.");

        // Niche films: one post each, stay in long tail
        savePost(carla, ETERNAL_SUN,
                "Nobody I know has seen this and it breaks my heart every time. " +
                "The beach-house sequence. Please watch it.");
        savePost(bob, PANS_LABYRINTH,
                "Del Toro at his cruellest and most beautiful. Can't believe this is 20 years old.");
        savePost(bob, MONONOKE,
                "Easily Miyazaki's best for me. Ashitaka is the kind of protagonist we need more of.");
        savePost(eve, GHOSTBUSTERS,
                "Just watched this for the first time?? why did nobody tell me it's funny-funny not nostalgic-funny");
        savePost(dave, SORRY_TO_BOTHER,
                "Feels like a film made by someone who actually had things to say. " +
                "Swings hard, misses sometimes, I respect it more than anything polished.");

        // Comments -- heavy on a handful of posts, zero on niche
        saveComment(p1, bob,   "Agreed on Nolan earning the runtime. Some docs say the science is tighter than people give it credit for.");
        saveComment(p1, carla, "The docking scene uses real footage of Saturn from Cassini for some shots -- blew my mind when I learned that.");
        saveComment(p1, dave,  "Runtime is exactly the problem for me. Not every film needs to be 3 hours.");
        saveComment(p1, eve,   "ok I'm convinced, adding to watchlist");

        saveComment(p2, alice, "Ugly-cry is the right description. Murph's final scene gets me every time.");
        saveComment(p2, carla, "I think it's a film about love dressed up as a film about space, which is why it works.");

        saveComment(p3, alice, "Fair. I think it's one of those where you had to see it on an IMAX screen for the physicality to land.");
        saveComment(p3, bob,   "Respect the honesty. It's refreshing to see a not-everyone-loves-it post.");
        saveComment(p3, carla, "The ending as cheat code is a take I haven't heard before and I might agree.");

        saveComment(p4, bob,   "The tunnel shot. That's it, that's the comment.");
        saveComment(p4, carla, "Andy's patience is what kills me on every rewatch.");

        saveComment(p6, bob,   "Ledger's interrogation scene -- no CGI, no score, just two actors. Masterclass.");
        saveComment(p6, carla, "I think about that scene way too often.");

        saveComment(p8, alice, "The needle scene still makes me physically tense.");
        saveComment(p8, bob,   "Tarantino has never recaptured this energy IMO.");

        saveComment(p10, carla, "The van shot in slow motion while everything else happens fast. Cinema.");
        saveComment(p10, bob,   "And you still can't explain what was real at the end. Best part.");

        log.info("Phase 1 done: {} users, {} posts, {} comments.",
                users.count(), posts.count(), comments.count());
    }

    // ========================================================================
    // Phase 2 -- Extra seed
    //
    // Adds new movies + richer engagement. Peak comment count after phase 2
    // reaches 10+, so the popular-threshold (20% of peak) becomes ~2, which
    // is a proper threshold rather than the degenerate "anything with 1+ comment".
    // ========================================================================
    private void seedExtra() {
        log.info("Phase 2: seeding extra demo data (more movies, heavier engagement)...");

        // Pull user references -- users were already persisted in phase 1
        // (or in a previous startup). Loading by username works either way.
        User alice = users.findByUsername("alice").orElseThrow();
        User bob   = users.findByUsername("bob").orElseThrow();
        User carla = users.findByUsername("carla").orElseThrow();
        User dave  = users.findByUsername("dave").orElseThrow();
        User eve   = users.findByUsername("eve").orElseThrow();

        // New movies -- mix of mainstream sci-fi (Matrix), foreign-language (Parasite),
        // cult comedy (Groundhog), horror (Shining), thriller (Seven).
        // Adds genre diversity so the Shannon-entropy metric has something to measure.
        for (String id : List.of(MATRIX, PARASITE, GROUNDHOG, SHINING, SEVEN)) {
            movieService.findOrFetch(id);
        }

        // Bump comment counts on existing popular posts so the peak rises.
        // These reference posts created in phase 1 -- load by user + movie.
        Post p1Interstellar = firstPost(alice, INTERSTELLAR);
        if (p1Interstellar != null) {
            saveComment(p1Interstellar, carla, "Re-reading this thread 3 weeks later and still finding new takes I agree with.");
            saveComment(p1Interstellar, bob,   "Worth mentioning: the IMAX run earlier this year was sold out every night here.");
            saveComment(p1Interstellar, dave,  "Fine. If we're doing rewatches I'll give the tesseract scene one more try.");
            saveComment(p1Interstellar, eve,   "watched it last night, cried three times, sending flowers to my past self");
            saveComment(p1Interstellar, carla, "Hans Zimmer's score is doing half the emotional work here and I will not be taking questions.");
            saveComment(p1Interstellar, alice, "^ Zimmer. Yes. Every time.");
        }

        Post p6DarkKnight = firstPost(alice, DARK_KNIGHT);
        if (p6DarkKnight != null) {
            saveComment(p6DarkKnight, dave,  "Ledger genuinely deserved that Oscar. No caveats.");
            saveComment(p6DarkKnight, eve,   "I'm 19 and this is still the best Joker. Prove me wrong.");
            saveComment(p6DarkKnight, carla, "Heath's magic trick with the pencil was improvised, which is insane.");
            saveComment(p6DarkKnight, bob,   "The bank heist opening is better than most entire films.");
            saveComment(p6DarkKnight, alice, "Agreed with all of the above and raising you: Harvey Dent's arc is underrated.");
            saveComment(p6DarkKnight, dave,  "Disagree on Harvey. Feels rushed.");
        }

        Post p10Inception = firstPost(alice, INCEPTION);
        if (p10Inception != null) {
            saveComment(p10Inception, dave,  "The spinning top is a red herring. The ending doesn't matter and that's the whole point.");
            saveComment(p10Inception, eve,   "I had no idea Arthur's zero-gravity fight was done with practical effects. Rewatched today.");
            saveComment(p10Inception, bob,   "The kick sequence editing across all three dream levels is basically the film's thesis.");
            saveComment(p10Inception, carla, "Nolan should make smaller films more often; this is him at his tightest.");
            saveComment(p10Inception, alice, "Tight is the word. Every scene earns its place.");
        }

        // New posts on the new movies -- engagement varies widely.
        Post matrixAlice = savePost(alice, MATRIX,
                "Holds up in a way I really wasn't expecting on a 2025 rewatch. " +
                "The bullet-time has been so memed that it's easy to forget how genuinely innovative it was.");
        Post matrixBob = savePost(bob, MATRIX,
                "The philosophy is half-baked, the action is flawless, and the combination somehow works. " +
                "Films that try to be this ambitious usually embarrass themselves.");
        savePost(dave, MATRIX,
                "Overrated and overreferenced. Fight me.");

        Post parasiteCarla = savePost(carla, PARASITE,
                "Every shot is composed like a still from a better film. " +
                "The scene where the rain starts flooding the basement might be the best sequence of the decade.");
        savePost(bob, PARASITE,
                "Bong Joon-ho's control of tone here is wild. Laughing one minute, terrified the next.");
        savePost(eve, PARASITE,
                "I was told not to read anything about this before watching. Best advice anyone ever gave me.");

        savePost(alice, GROUNDHOG,
                "Genuinely one of the deepest films ever dressed up as a comedy. " +
                "Bill Murray's performance sneaks up on you -- you don't notice the change until you do.");
        savePost(bob, GROUNDHOG,
                "The fact that we never learn WHY the loop happens is the best decision they made.");

        savePost(carla, SHINING,
                "Kubrick's framing is doing things I still can't fully explain. " +
                "The twins shot is a cliche for a reason -- it shouldn't still work, and yet.");
        savePost(dave, SHINING,
                "I've tried three times. I don't get the hype. Beautiful to look at, exhausting to sit through.");

        Post sevenEve = savePost(eve, SEVEN,
                "watched this alone at night and could not sleep. the final scene. the BOX.");
        savePost(bob, SEVEN,
                "Fincher's best? I keep flipping between this and Zodiac. Both feel like the same filmmaker at different stages of certainty.");
        savePost(alice, SEVEN,
                "Rewatched and Freeman's performance is so much quieter than I remembered. He's the real centre of the film.");

        // Extra comments concentrated on matrixAlice, parasiteCarla, sevenEve
        // to push the peak comment count up and validate the threshold logic.
        saveComment(matrixAlice, bob,   "Bullet-time still doesn't look dated and that's genuinely miraculous.");
        saveComment(matrixAlice, carla, "The way the Wachowskis layer cyberpunk, kung-fu, and gnostic theology into a single film... we don't see this kind of synthesis anymore.");
        saveComment(matrixAlice, dave,  "Fine, the action IS good. I'll give you that.");
        saveComment(matrixAlice, eve,   "first watch tonight, i finally get it");
        saveComment(matrixAlice, alice, "Welcome to the club.");
        saveComment(matrixAlice, bob,   "Neo dodging bullets on the rooftop is still my benchmark for 'action scene that makes you sit forward'.");
        saveComment(matrixAlice, carla, "Also: Trinity. Carrie-Anne Moss deserved more films this good.");
        saveComment(matrixAlice, dave,  "Trinity I'll grant. Morpheus too.");

        saveComment(parasiteCarla, alice, "The flooding scene is the one I keep coming back to when people ask why foreign films matter.");
        saveComment(parasiteCarla, bob,   "Bong Joon-ho filming stairs. That's the whole film.");
        saveComment(parasiteCarla, eve,   "I looked up the house afterwards and it's entirely a set? That broke my brain.");
        saveComment(parasiteCarla, dave,  "I'll say it: Okja was better. But Parasite is incredible, no argument.");
        saveComment(parasiteCarla, carla, "Okja is fantastic. Different film, different weapon.");
        saveComment(parasiteCarla, bob,   "The stone. The scholar's rock. That object is the most loaded prop of the decade.");

        saveComment(sevenEve, bob,   "The box. Yeah. Nothing else to say.");
        saveComment(sevenEve, alice, "Fincher had to fight the studio to keep that ending. He was right.");
        saveComment(sevenEve, carla, "The sound design in the final scene is doing so much work. No music until the exact right moment.");
        saveComment(sevenEve, dave,  "Fincher at his most controlled. Zodiac is looser but I think this is tighter.");
        saveComment(sevenEve, eve,   "I'm never watching this alone again. amazing film though");

        log.info("Phase 2 done: {} users, {} posts, {} comments total.",
                users.count(), posts.count(), comments.count());
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private User saveUser(String username, String email, String bio) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setBio(bio);
        u.setPasswordHash(encoder.encode(SEED_PASSWORD));
        return users.save(u);
    }

    private Post savePost(User author, String imdbId, String content) {
        Movie movie = movieService.findOrFetch(imdbId)
                .orElseThrow(() -> new IllegalStateException("OMDb doesn't know " + imdbId));
        Post p = new Post();
        p.setUser(author);
        p.setMovie(movie);
        p.setContent(content);
        return posts.save(p);
    }

    private void saveComment(Post post, User author, String content) {
        Comment c = new Comment();
        c.setPost(post);
        c.setUser(author);
        c.setContent(content);
        comments.save(c);
    }

    /** Find the first post by a given user about a given movie. Null if none. */
    private Post firstPost(User author, String imdbId) {
        return posts.findByUserOrderByCreatedAtDesc(author).stream()
                .filter(p -> p.getMovie().getImdbId().equals(imdbId))
                .findFirst()
                .orElse(null);
    }
}
