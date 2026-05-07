# ReelRooms

ReelRooms is a film-discussion platform I built to test whether a recommender feed can resist algorithmic conformity. It's the codebase behind my final-year project at NTU. The goal isn't to compete with Letterboxd or Reddit. It's to put a different kind of feed in front of real users and see what actually changes when the algorithm doesn't reward consensus by default.

## The thesis in plain terms

Mainstream platforms reward consensus. The more comments a post gets, the more visible it becomes, and that loop drives the conversation toward whatever the majority already thinks. ReelRooms tries the opposite. The For You feed is diversity-weighted: every few slots it forces in a quiet pick, meaning a post with low engagement that wouldn't surface on a popularity ranking. The rest of the design follows the same idea. No like counts on cards, no follower counts, no streaks. The point is to remove the easy signals that nudge people toward the consensus take.

## The three feeds

- `/feed`: chronological. The baseline. Newest posts first.
- `/for-you`: diversity-weighted. The contribution. Mixes popular posts with quiet picks.
- `/popular`: engagement-ranked. The comparison baseline used in the evaluation chapter.

## How the For You algorithm works

The feed has 20 slots. The diversity ratio is 1:4 by default, which means one slot in every five is a quiet pick. Both numbers live in `application.properties` so I can change them without touching code.

Posts in the pool are scored with this formula:

```
score = comments * 3.0 + movies * 1.0 - days * 0.2
```

Where `comments` is the comment count, `movies` is how many other recent posts exist on the same film (a rough crowd-interest signal), and `days` is the post's age. All three weights are configurable.

One thing I tried to be careful about: the badge on a card is decoupled from the slot it lands in. A post only gets the "popular" badge if its comment count is high enough on its own merits, not because it landed in a popular slot. Otherwise you'd end up labelling a post "popular" with zero comments, which is just lying to the user.

## Why each major feature exists

- Authentication: needed for posting and so moderation actions have someone accountable
- Posts, comments, threading: the actual discussion content
- Watch parties: a way for the community to form around shared viewing
- Admin moderation with audit log: required for LSEPI compliance with the Computer Misuse Act 1990
- OMDb integration with cache-first design: stops the app hammering the external API; cache hit rate measured at 90.03% during testing
- `/admin/metrics` dashboard: evaluation tool, computes the diversity comparison reported in Chapter 5
- `/admin/perf` dashboard: evaluation tool, captures the real instrumentation latencies reported in Chapter 5
- Static pages (privacy, terms, guidelines): UK GDPR and DPA 2018 compliance

## How to run

You'll need Java 17 and an OMDb API key.

Set these environment variables:

- `SUPABASE_DB_HOST`
- `SUPABASE_DB_USER`
- `SUPABASE_DB_PASSWORD`
- `OMDB_API_KEY`

Run the schema in your Supabase project — paste `sql/schema.sql` into the SQL Editor and run it. Then:

```
./gradlew bootRun
```

The app comes up at `http://localhost:8080`.

## Tech stack

- Spring Boot 4
- Java 17
- Hibernate 7
- PostgreSQL (hosted on Supabase)
- Thymeleaf
- Bootstrap 5
- OMDb API

## Known limitations

- User testing was n=10, in person. The numbers are descriptive, not statistical.
- The 1:4 ratio is a starting point. I don't claim it's optimal.
- The 15-minute test sessions can't show whether user preferences hold up over weeks.
- The UI is mobile-responsive but not mobile-optimised. A tablet works fine, a small phone is awkward.
- Real-time chat in watch parties was cut from scope.
- A profanity filter was cut from scope too.

## Where to read more

The full design rationale, the evaluation results, and the LSEPI discussion are all in the dissertation. The interesting parts are Chapter 3 (why the algorithm is shaped the way it is) and Chapter 5 (whether it actually achieved what I said it would).
