# Report Notes

Running log for the FYP dissertation. Every bullet here is a thing worth writing about in the report. When drafting the final doc, open this file and lift bullets into prose.

## Marking weightings (from the grid PDF)

- **Report Chapters — coverage and coherence: 50%**
- **Subject Matter and Project Achievement: 40%** (type of work, project size, achievement, initiative, demo)
- **Structure / Clarity / Presentation: 10%** (lucidity, analytical depth, English, references)

**Two recurring high-grade criteria in the grid** — keep these in mind while writing:
1. **Analytical, not descriptive.** "Reasoned argument" = 2.1; "critical skills applied in depth" = 1st; "purely descriptive" = 3rd.
2. **Quantitative evaluation.** Explicitly asked for at Ch5 and under Project Size. "No quantitative or measurable evidence" = Fail.

**Target word counts** (NTU Level 6 CS FYP, verify with supervisor):
- Total main body ≈ 12,000–15,000 words (≈50–60 pages).
- Chapter 2 and Chapter 4 are the biggest chapters; Chapter 5 is smaller but carries big marks.


**How to use:**
- Claude updates this after each meaningful decision or piece of work
- Bullets are grouped by report chapter so you can write chapters in order
- `Citation:` tags mark where a Harvard reference needs to slot in
- `Evidence:` tags mark where a screenshot, code listing, or log should be pasted

---

## PPD commitments — revised scope (agreed 2026-04-23)

**Building in the MVP:**
- Account creation (signup / login) — done
- Movie posting
- Commenting on posts
- For You algorithm with 1:4 diversity ratio (core thesis feature)
- Watch parties / events — reframed as scheduled movie events with RSVP + comment thread (no real-time streaming or chat)
- Admin moderation — simple `is_admin` flag + delete-post/comment/event endpoints
- Privacy page, TOS / guidelines page, survey consent page (static content, hits transparency + DPA + consent LSEPIs)
- User testing via Google Form (5–8 participants, anonymised)

**Intentional cuts (defended in Chapter 6 as design choices, not failures):**
- **Reactions / like button.** Visible popularity metrics are precisely the mechanism that drives the algorithmic conformity effect the thesis argues against (`Citation: Cheung & Thadani 2012`). Leaving them out is a design choice, not a shortfall.
- **Visible upvotes and numeric rating scores.** Same argument. Users never see a numeric popularity indicator they could conform to. The only engagement cue we expose is a comment count — and crucially, a *comment* is qualitative engagement (someone wrote a reply), not a reducible numeric judgement ("3 likes"). Thesis-safe: comment counts indicate "this sparked discussion," not "this is the approved opinion."

**Honest cuts (acknowledged in Chapter 6 as Future Work with timeline reasoning):**
- Real-time chatrooms (≤4 users) — would require WebSocket infrastructure, room management, reconnection handling; ~2–3 days of work on its own. Out of scope for the 14-day compressed timeline.
- Slur / bad-word filter — needs a wordlist + false-positive handling + test coverage. Roughly 1–2 days. Future work.
- Separate report / flag system — admin moderation above covers the same LSEPI commitment.
- In-app feedback survey (20-min unlock) — Google Form collects the same data with negligible functional loss.

**PPD-explicit out-of-scope (left out as originally planned):**
- Mobile client
- AI in the For You page

---

## Chapter 1 — Introduction

*Most of this chapter lifts straight from the PPD. See `FYP MISC/N1205202_PPD.docx`.*

- Problem: movie discussion platforms (Letterboxd, IMDb, Rotten Tomatoes) prioritise critic ratings and popularity metrics, which reinforces social conformity and silences emotional honesty.
- Thesis: a community-driven platform with a diversity-weighted feed can push back against algorithmic homogeneity.
- Scope statement (what's *in* the MVP built in these two weeks): account signup/login, movie posts, comments, For You feed with 1:4 diversity ratio. What's *out*: chatrooms, watch parties, events, slur filter, AI, mobile, reactions, in-app survey.
- Aims and objectives: see PPD section 2.

## Chapter 2 — Context / Literature Review

- **Social conformity and fear of social disapproval.** `Citation: Deutsch & Gerard (1955)` — normative and informational social influence explain why people hedge opinions when exposed to others'. Directly motivates why the platform hides global ratings until after you've posted.
- **Electronic word-of-mouth (eWOM).** `Citation: Cheung & Thadani (2012)` — online reviews and past accounts create signals that make readers drift toward socially accepted feedback even when their own experience differed.
- **Algorithmic echo chambers.** `Citation: Lee, Park & Han (2008)` — recommender systems that surface majority opinion generate conformity over time. Motivates the 1:4 diversity ratio in the For You algorithm.
- **Screen time and wellbeing.** `Citation: Madhav, Sherchand & Sherchan (2017)` — extended daily screen use correlates with higher depression levels in US adults. Motivates interactive features over endless scroll (LSEPI social).
- **Competitor landscape:** Letterboxd, IMDb, Rotten Tomatoes. TODO — write a short compare/contrast table once the MVP is done.

## Chapter 3 — New Ideas / Design

### Tech stack and rationale

- **Web app over desktop (JavaFX).** Chose Spring Boot + Thymeleaf over JavaFX. Tradeoffs discussed: JavaFX would have been faster to get running but less industry-relevant. Spring Boot is heavier setup but closer to real app-dev work and more convincing in a portfolio. `Evidence: the 14-day schedule from the planning section`
- **Spring Boot 4.0.6 on Java 17 (runtime 23).** Latest stable Spring Boot at time of writing. Java 17 LTS target for long-term compatibility; Gradle's toolchain happened to pick Java 23 for the actual runtime — fine, SB4 requires 17+.
- **Supabase (PostgreSQL) over Firebase.** Honoured the PPD commitment to PostgreSQL. Firebase is NoSQL; would have forced a data-model rewrite. From Spring Boot's side, Supabase is "just Postgres" via JDBC — zero integration friction.
- **Thymeleaf over an SPA framework.** Server-side rendering only. No React, no build step, no JS bundler. Keeps the scope small and the deploy story trivial.
- **Bootstrap 5 via CDN for styling.** Copy-paste decent UI. Skips writing CSS from scratch, which would have burned a day.
- **Gradle (Kotlin DSL) over Maven.** Slight preference; either would have worked.

### Data model

Four tables: `users`, `movies`, `posts`, `comments`.

- **`movies` keyed by `imdb_id` (text).** Primary key is the OMDb identifier, not a generated integer — prevents duplicates if two users post about the same film. Movies are essentially a cache of OMDb responses.
- **Foreign keys with `ON DELETE CASCADE`.** Removing a user cleanly removes their posts and comments. Keeps the database consistent without application-layer cleanup code.
- **Check constraints on content length** (`posts.content` 1–5000 chars, `comments.content` 1–2000, `users.username` 3–30). Makes the DB the source of truth for content limits, not just the form validator.
- **Timestamps set by Postgres (`default now()`).** Java doesn't control `created_at` — avoids clock-skew bugs and keeps the entity simpler.
- `Evidence: sql/schema.sql in the repo.`

### Java package layout

- `model/` — JPA entities
- `repository/` — Spring Data interfaces
- `dto/` — form-backing objects, never persisted
- `security/` — Spring Security adapters
- `config/` — `SecurityConfig` and future config beans
- `controller/` — Spring MVC controllers returning Thymeleaf views

Rationale: standard Spring layered structure. Keeps concerns separated without being over-abstracted (no `service/` layer yet — added only when controllers start duplicating logic).

### Entity design choices

- **Unidirectional relationships.** `Post` points at `User` and `Movie`; `Comment` points at `Post` and `User`. No reverse collections. Keeps feed queries cheap (no accidental N+1 when Jackson/Thymeleaf serialises).
- **`FetchType.LAZY` on all `@ManyToOne`.** Default is EAGER, which silently loads relations on every query. Lazy makes the cost explicit; hot paths use `JOIN FETCH` in custom `@Query` methods (e.g. `PostRepository.findRecentWithAuthors`).
- **Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`.** Avoided `@Data` because its auto-generated `equals`/`hashCode`/`toString` cause infinite loops on bidirectional relations and surprises with JPA proxies.
- **`AppUserDetails` wraps `User`.** Could have made `User` implement `UserDetails`, but that ties the entity to Spring Security. Wrapping keeps the entity framework-free.

## Chapter 4 — Implementation

### Auth

- **BCrypt with cost factor 10** (Spring default). Raw passwords never persisted — only the hash. `Evidence: screenshot of users table showing password_hash starting $2a$10$...`
- **`SignupForm` DTO separate from `User` entity.** Validation annotations live on the DTO; the entity never carries a raw-password field. Clean break between form layer and persistence layer.
- **CSRF on.** Thymeleaf's `th:action` injects the token automatically. No need for manual hidden fields.
- **Uniqueness validation.** Username and email duplicates rejected as `BindingResult` field errors, so the form re-renders with a message next to the offending input. Good UX; also demonstrates Jakarta Bean Validation + Spring MVC integration in the report.

### Security boundary

- **Spring Boot is the security boundary, not Postgres RLS.** We connect as the `postgres` superuser, so Supabase Row Level Security policies are bypassed. This keeps authorisation logic in one place (Java) for the MVP.
- **Future work:** create a limited-privilege Postgres role, enable RLS on all tables, move authorisation into the DB. Discuss in Conclusions.

### Configuration / secrets

- **Environment variables for all secrets.** DB password and OMDb API key read via `${SUPABASE_DB_PASSWORD}` and `${OMDB_API_KEY}` in `application.properties`. Nothing sensitive is committed to git.
- **`.gitignore` defensively blocks `*password*`, `*secret*`, `*.env`, `application-local.properties`.** Belt-and-braces against accidental commits.

### Watch parties / events (Day 5)

The PPD originally listed both "watch parties" and "events" as separate features. Discussion in development settled on a single reframing: **a watch party is an event scheduled around a film, not a real-time chat with synchronised playback.** Attendees watch on their own devices; the platform provides the "where to discuss" thread.

**Why this is the right reframe:**
- The original "≤4 person real-time chatroom" interpretation would have needed Spring WebSocket + STOMP + connection state + reconnection handling — at least 2–3 days for a feature that supports tiny groups.
- The reframed version reuses the post + comment patterns already in the codebase. ~half a day of work for something that exercises the same architecture.
- Watching together asynchronously (or genuinely simultaneously on personal devices) is closer to how dispersed friend groups actually consume films.

**Data model — three new tables:**
- `events` (host, movie, title, description, scheduled_for) — the watch party itself.
- `event_attendances` (event, user, rsvped_at) — junction table for RSVPs. Surrogate `id` + unique constraint on `(event_id, user_id)` rather than a composite primary key. Same effect, less JPA ceremony.
- `event_comments` — comments scoped to events. Kept separate from `comments` (which is scoped to posts) to avoid polymorphic associations. Slight duplication, massive simplification.

**UX patterns reused from posts:**
- Two-step creation: search OMDb for a film → fill in title/description/datetime → save. Same `/events/new` → `/events/new/details?imdbId=…` flow as `/posts/new` → `/posts/new/write`.
- Eager `JOIN FETCH` queries for the show page so Thymeleaf doesn't trigger lazy-loads with `open-in-view=false`. Same lesson, reapplied — proves the pattern is generalisable.
- Comment-creation re-renders the page on validation error so drafts survive.

**RSVP design:**
- Single `POST /events/{id}/rsvp` endpoint that **toggles**. If the user already has an attendance row, delete it; else insert one. Idempotent semantics — the user can mash the button and end up in a defined state.
- Attendees list rendered comma-separated on the show page (no pagination — events realistically don't get hundreds of attendees in this kind of platform).

**Date handling worth a paragraph in Chapter 4:**
- Form input uses HTML5 `<input type="datetime-local">` — no zone in the value.
- Controller reads as `LocalDateTime`, attaches `Europe/London` zone, stores as `OffsetDateTime` in Postgres `timestamptz` column (which then converts to UTC internally).
- Display side benefits from the existing `hibernate.jdbc.time_zone=Europe/London` config — values render in UK local time.
- Future work: per-user timezone preference instead of assuming Europe/London. Mention in Chapter 6.

**Migration discipline:**
- Updated `sql/schema.sql` so fresh setups get the new tables.
- Added `sql/migration_add_events.sql` for the existing DB — pasteable into Supabase SQL Editor, idempotent (`IF NOT EXISTS`).
- Pattern worth noting in Chapter 4: schema + migrations as separate concerns, both checked into the repo.

**Past events visibility iteration:**
- First pass of `/events` only listed *upcoming* events (`scheduled_for >= now`). This bit during testing — past events disappeared from view, even though their data and comment threads were intact in the DB.
- Fix: split `/events` into two sections. **Upcoming** at the top (forward-looking, the headline use case), **Past** below (de-emphasised with reduced opacity and slight greyscale on posters), capped at the 20 most recent. Past events still take you straight to the live comment thread for that event.
- Worth a sentence in Chapter 4: "small UX iterations during development surfaced visibility issues invisible in design — a reminder that querying `where date >= now()` is the correct DB query but the wrong UX default."

### Conversation depth (was "Comment system" — design pivoted mid-build)

**Original design:** strictly flat comments, no threading. Reasoning: threading encourages pile-ons; flat keeps focus on the post itself.

**Revised design (during informal testing in development):** *one* level of threading is allowed. A user can reply to a top-level comment, but replies-to-replies are rejected at the controller. Two motivations for the pivot:

1. **No engagement signal otherwise.** Without sub-comments, all top-level comments are equal. The "what's the most engaged take on this post?" question — which the user wanted as a feed-card preview — has no answer. One level of threading provides exactly that signal: most-replied-to top-level comment = the conversation worth surfacing.
2. **Pile-on dynamics are a *deep* nesting problem, not a one-level problem.** Reddit and similar platforms produce echo chambers around viral comments because nesting goes 5+ levels deep, allowing a single comment to spawn its own ecosystem. Capping at one level keeps each thread anchored to the original post.

**Thesis reconciliation for Chapter 3:** "Comments are flat at the top level to keep focus on the post itself. *One* level of replies is permitted because direct response to a specific take is a normal conversational mechanic that doesn't drive the pile-on dynamics deeper nesting causes." This is a *defensible iteration* — exactly the kind of "refinement during development" the marking grid rewards.

**Implementation details:**
- **Schema:** `parent_comment_id` nullable FK on the `comments` table, self-referencing. Replies still carry `post_id` so a single "all comments for this post" query covers both levels and counts work without filtering.
- **One-level enforcement:** application layer (`CommentController.reply`) rejects replies whose parent already has a parent. Could be a Postgres trigger, but app-side keeps the schema simple.
- **Feed-card preview:** "top comment per post" = the top-level comment with the most replies, ties broken by recency. Two batched queries inside `CommentService` regardless of feed length — N+1 avoided.
- **Total count includes replies.** The existing `countByPostIdIn` query counts every row in `comments` for a post, so the "23 comments" badge naturally includes both levels.
- **Reply UX.** Each top-level comment has its own collapsible reply form (`<details><summary>`), JS-free. Validation errors on replies survive a redirect via `RedirectAttributes` flash — draft preserved, no lost typing.
- **Existing patterns reused:** unidirectional relationships (Post doesn't hold `List<Comment>`), oldest-first ordering (blog-style, not Reddit-style), the lazy-loading discipline (`JOIN FETCH` everywhere we render).

**Migration:** `sql/migration_add_comment_threading.sql` — pasteable into Supabase. Idempotent (`ADD COLUMN IF NOT EXISTS`, `DROP CONSTRAINT IF EXISTS` before `ADD CONSTRAINT`).

**Worth a paragraph in Chapter 3 and another in Chapter 4.** The design pivot is honest, the implementation is clean, and "iterative refinement during development" is exactly the marking-grid criterion this hits.

### Card layout consistency

A small but worth-mentioning UX iteration during development.

**Problem:** First pass of feed cards used Bootstrap's `row + col` layout, which defaults to `align-items: stretch` — meaning the poster column was forced to fill the height of the right column. With `object-fit: cover` on the poster image, that produced aggressive cropping on tall cards (cards with a top-comment preview were taller than cards without). Two visible issues: (1) posters looked stretched/over-cropped, (2) cards with comments were visibly bigger than cards without.

**Fix:**
- Swapped `row + col` to `d-flex align-items-start` with a **fixed-size poster** (100×150 px — standard 2:3 movie poster ratio). Posters now keep their natural aspect ratio regardless of card height.
- **Always render the top-comment block** — when there are no comments, show a "No comments yet — be the first to react" placeholder in the same visual slot as a real preview. Eliminates the height delta between commented and uncommented cards.

**Why this is worth mentioning in Chapter 4:** small consistency fixes accumulate into a feed that feels considered rather than uneven. The placeholder block also doubles as a soft CTA for engagement on cold posts, which is mildly thesis-aligned (encouraging contribution rather than passive scrolling).

### Lazy loading discipline (important Chapter 4 material)

- **We have `spring.jpa.open-in-view=false`.** Default Spring Boot leaves this on, which keeps the Hibernate session open until after view rendering — convenient, but it means Thymeleaf can silently trigger extra DB queries mid-render. Turning it off is the recommended practice but forces every code path to declare what it needs up front.
- **The lesson (paid for in a real bug).** The first `/posts/{id}` implementation used `postRepository.findById(id)`, which returns lazy proxies for `user` and `movie`. Thymeleaf then tried `post.movie.title`, Hibernate tried to lazy-load, the session was already closed — `org.attoparser.ParseException`, white-label error page.
- **Fix: explicit JOIN FETCH queries per view.** Added `PostRepository.findByIdWithAuthor(id)` with `JOIN FETCH p.user JOIN FETCH p.movie`. Did the same for `CommentRepository.findByPostOrderByCreatedAtAsc` so the comments list can render author usernames without lazy-loading.
- **Why this is the right trade.** Open-in-view hides the problem; disabling it makes the cost visible. With `findById` you get one query + N surprise queries during rendering. With `JOIN FETCH` you get one query total, declared where the data is fetched. Louder when wrong, cheaper when right.
- **Pattern name for Chapter 4:** *"Fail fast on unintended lazy loads."* Contrasts nicely against the Spring Boot default convenience behaviour.
- `Evidence: commit where findByIdWithAuthor was added (post-comments fix).`

### Post creation flow

- **Two-page flow, not one-page AJAX.** `/posts/new` → search + results, then `/posts/new/write?imdbId=...` → compose. Server-side only; no JS build pipeline. Survives page refresh (good UX) and cheaper to build than a single-page form.
- **`getReferenceById` for the author FK.** When saving a post we need the user's id for the foreign key but not the full entity. `userRepository.getReferenceById(me.getId())` returns a Hibernate proxy with just the id, skipping a `SELECT users WHERE id = ?`. One less DB round-trip per post. Small win, but illustrative of the "make the common path cheap" principle.
- **Validation errors re-render the write page** with the user's text intact and inline error messages next to fields. Uses Spring's `BindingResult` pattern. No lost drafts.
- **Thesis-aligned UX copy.** The textarea placeholder is "No likes, no scores, no five-star rating. Just what actually stuck with you." The UI copy *is* a design argument against visible popularity metrics; flag this in Chapter 3 when discussing design choices.
- **Feed shows posts newest-first using `findRecentWithAuthors`** (JOIN FETCH — no N+1 queries). Content respects whitespace (`white-space: pre-wrap`) so line breaks in reviews survive.

### OMDb integration

- **Layered design.** `OmdbClient` is a thin HTTP wrapper (knows nothing about our domain). `MovieService` sits above it and handles caching via `MovieRepository`. Controllers talk to `MovieService`, not `OmdbClient`. Keeps each layer single-purpose.
- **Cache-first lookup.** `MovieService.findOrFetch(imdbId)` checks the `movies` table first; only hits OMDb on a miss, then writes the result back. So the platform hits OMDb at most once per film.
- **Why cache, given we're on the paid OMDb Standard tier (250k/day, more than enough):**
  - **Latency.** OMDb round-trip is ~100–500 ms over the internet; a local Postgres hit is under 10 ms. A feed page that shows 20 posts = 20 movie lookups. That's the difference between a page that loads instantly and one that takes several seconds.
  - **Resilience.** If OMDb has a brief outage or rate-limits the API key, the feed still renders from our cached copies.
  - **Queryability.** Once movies are in Postgres we can write SQL like "top 10 most-discussed films this week" via `posts JOIN movies`. If movie data only lived at OMDb, that query is impossible.
  - **Data ownership.** Movie metadata used in community posts lives with the community's data.
- **Live search not cached.** Users type free text ("interstell"), so search hits OMDb every time. Cheap per call.
- **Error handling is quiet.** Network failures / rate limits log a warning and return empty list / empty Optional. The UI treats that as "no results" — degrades gracefully instead of throwing 500s at users. Future work: distinguish "OMDb is down" from "no matches" with a proper error banner.
- **DTOs in `omdb/`, entity in `model/`.** Jackson maps OMDb's PascalCase JSON to our Java camelCase via `@JsonProperty`. DTOs never leak out of the service layer.
- **Year parsing quirk.** OMDb sends TV-series years as `"2014–"` or `"2014–2017"`. `MovieService.parseYear` strips non-digits and takes the first 4 chars. Flagged as a known limitation — tweak if we need series support.
- `Evidence: OmdbClient.java, MovieService.java, OmdbMovie.java.`

### For You algorithm (thesis centrepiece)

**The problem being solved.** Mainstream recommender systems (Letterboxd's popular page, IMDb's most-rated, social-media For You pages) rank content by engagement metrics, surfacing what already has eyes on it. This reinforces the echo-chamber effect documented in Lee, Park & Han (2008) — users see consensus, internalise consensus, produce consensus.

**The design goal.** Per the PPD: every 5 feed slots, 1 should come from the "long tail" — low-engagement posts a popularity-only feed would bury. This is the 1:4 diversity ratio.

**The scoring formula** (lives in `ForYouService.score(...)`):
```
score = commentCount × w_comment
      + otherPostsOnSameMovie × w_movie
      − daysOld × w_age_penalty
```
With default weights `{comment: 3.0, movie: 1.0, age_penalty: 0.2}`. All three live in `application.properties` — tunable without recompiling. **Future work:** learn these weights from actual engagement (gradient descent on click-through rate, say).

**How popular and unconventional buckets are picked:**
1. Pool = most recent N posts (default 100)
2. Rank by score DESC
3. Popular = top (N - N/(ratio+1)) slots
4. Unconventional = bottom-scoring posts NOT already in Popular (deduplicated)
5. Interleave: positions 4, 9, 14, 19 (1-indexed: every 5th) = unconventional; rest = popular

**Defensibility choices:**
- **"Least popular" as the unconventional signal.** Simplest honest signal available without sentiment analysis. Alternatives considered and rejected for MVP: "posts about less-discussed movies" (covered indirectly via the movie-popularity term), "posts by newer users" (overlaps with low comment counts). `Citation: Cheung & Thadani 2012` — eWOM effect implies low-visibility posts are disproportionately *silenced*, so surfacing them is the corrective move.
- **Linear weighted sum, not multiplicative.** Easier to explain in the report, easier to tune by hand, no surprising interactions. Trade-off acknowledged: multiplicative would be more expressive. Fine for MVP.
- **Transparent labels on the feed, but honest ones.** Cards show either `popular`, `quiet pick`, or nothing. Unlike mainstream platforms which hide their weighting, we tell the user what the algorithm is doing. Critically, **labels are decoupled from slot placement** — a post doesn't get the `popular` badge just because the algorithm ranked it high; it has to actually have engagement. Threshold: comment count ≥ 20% of the peak comment count on any post in the last 7 days (configurable). Stops the feed from lying ("popular" with 0 comments makes no sense). `quiet pick` is only applied when the algorithm deliberately surfaced a low-engagement post via the diversity slot. Everything else gets no badge. Worth 1–2 paragraphs in Chapter 3.
- **Naming: "quiet pick", not "long tail".** "Long tail" is statistics jargon (the low-frequency portion of a power-law distribution); unfamiliar users find it opaque. "Quiet pick" is plain English and carries the same meaning: an underheard voice the algorithm has foregrounded.
- **Tunable via config, not hardcoded.** Weights and ratio in `application.properties`, not constants. Shows engineering discipline to the grader.
- **Comment counts shown, upvote counts not.** Feed cards show "N comments" because a comment count represents *qualitative depth of discussion* — someone took time to write a reply. This is fundamentally different from a like count, which is a one-click numeric judgement that invites conformity (Cheung & Thadani 2012). Comment counts give users a discussion-density cue without supplying a popularity-metric to conform to.

**Quantitative evaluation hooks** (for Chapter 5):
- Same pool of posts, run through both algorithms. Compare:
  - Shannon entropy over movie genres in the top-20
  - Count of distinct authors in the top-20
  - Percentage of posts drawn from below-median comment count
- Expect our feed to score measurably higher on all three. Report as a table.

### Demo data seeding

Realistic seed data lives across three files:

1. **`DemoDataSeeder.java`** (Java, runs on app startup). Two idempotent phases.
   - Phase 1 (checked against `users.alice`): 5 users, 10 movies, 16 posts, 20 comments.
   - Phase 2 (checked against the Matrix movie being cached): +5 movies, +15 posts, +30 comments concentrated on a handful of posts to lift peak engagement into double digits.
2. **`sql/extra_seed_data.sql`** (pasted into Supabase SQL Editor): +50 posts, +100 comments, all from the original 5 seeded users. Engagement distribution designed on purpose:
   - 8 hot posts with 6 comments each
   - 8 medium with 3 each
   - 14 light with 2 each
   - 20 quiet posts with zero — explicit long-tail fodder for the For You algorithm
3. **`sql/extra_users_data.sql`**: +30 users, +20 posts from them, +30 comments from them across existing hot threads. Fixes the "5 people in a Discord server" feel that the earlier seed had. All 30 new users inherit alice's BCrypt hash via `INSERT ... SELECT`, so they share password `demo1234` without any hardcoded hash literal.

**Total seeded corpus:** ~35 users, ~101 posts, ~180 comments. Peak comment count ~8. Popular threshold (20% of peak) lands at 2 — a meaningful cut-off rather than the degenerate "anything with a single comment" we started with.

**Design choices worth writing up:**
- **Two SQL files rather than one big seeder.** The Java seeder runs automatically; the two `.sql` files are opt-in so the user can add volume without waiting for another app restart cycle. Each SQL file is self-contained, idempotent, and atomic (wrapped in `DO $$ ... $$` blocks — if anything fails mid-way, nothing is written).
- **Idempotency checks per file** — not per table. Each SQL file looks for a canary row (specific post content for `extra_seed_data`, specific username for `extra_users_data`) and bails out if present. Re-running is safe.
- **Atomic all-or-nothing.** Postgres `DO` blocks implicitly transaction-wrap the whole body. A failure in comment 90 of 100 rolls back the whole seed, not a partial state. Important for a demo dataset — you either have it fully or not at all.
- **Genre variety** — combined corpus covers drama, sci-fi, horror, thriller, Korean drama, animation, comedy, fantasy, war, crime. Shannon-entropy metric in Chapter 5 will have real variance to measure across ~15 genres.
- **Realistic engagement skew** — a few hot posts, many quiet ones. Matches natural distribution on real platforms, which is what the For You algorithm was designed to correct against.
- **All seeded passwords = `demo1234`.** Fine for dev; flagged for Chapter 6 future work.
- **Evidence:** `DemoDataSeeder.java`, `sql/extra_seed_data.sql`, `sql/extra_users_data.sql` — all three belong in the appendix code listing. `sql/schema.sql` + these three together = "everything needed to reproduce the evaluation environment."

### Pagination

- `/feed` uses Spring Data's `Page<Post>` with a page size of 20. URL param `?page=N` (0-indexed). Template shows "Page X / Y" with prev/next buttons.
- Custom `@Query` with explicit `countQuery` — Hibernate can't auto-generate a count query from a `JOIN FETCH`, so we supply one.

### Timezone handling (short but worth 1 paragraph in Chapter 4)

- **Storage: UTC.** Postgres `timestamptz` columns store every `created_at` value in UTC internally, regardless of client session timezone. Industry best practice — decouples the stored value from any particular locale.
- **Display: Europe/London.** `application.properties` sets `spring.jpa.properties.hibernate.jdbc.time_zone=Europe/London`, so Hibernate converts values into the UK timezone when reading into Java `OffsetDateTime`. Templates then render `HH:mm` in local wall-clock time without any per-template timezone conversion logic.
- **DST handled automatically** — the IANA `Europe/London` zone switches between BST and GMT on the correct dates, so timestamps never drift twice a year.
- **Trade-off acknowledged:** this assumes all users are in the UK (fair for an FYP demo; the user-testing cohort is UK-based). A global platform would need per-user timezone preferences stored in the `users` table and applied at render time. Flag as future work in Chapter 6.
- **Why this matters for the report:** a small, concrete example of storage-vs-display separation — the kind of engineering-discipline detail that moves a paragraph from descriptive to analytical. "Stored in UTC, displayed in Europe/London via a single Hibernate property" is exactly the level of detail Chapter 4 wants.

### For You is single-page on purpose (Chapter 3 material)

**Decision:** `/for-you` is a fixed 20-item feed. No pagination. Definitely no infinite scroll.

**Rationale:** the PPD cites Madhav, Sherchand & Sherchan (2017) on the association between extended daily screen time and depression, and explicitly names "endless scrolling mechanisms that hook their brain" as a problem the project sets out to resist. Infinite scroll on For You would be exactly the pattern the thesis argues against. Mainstream platform For You pages (TikTok, Instagram, Twitter) are infinite for a reason — engagement capture — and that reason is the thing we're pushing back on.

**Alternatives considered:**
- Infinite scroll: rejected (thesis violation).
- Paginated For You (multiple pages of 20 each): possible, but each page re-runs the diversity algorithm on an expanding "exclude" set, which bloats the DB calls and makes the "highlights reel" framing weaker.
- Hard-capped pagination (max 3 pages, say): tenable compromise. Not pursued for MVP; flagged as a design-space alternative worth mentioning.

**End-of-feed checkout** — a stronger UX than "silent end." After the 20 picks the user is presented with two deliberate choices:

- **Take a break** (primary button) — logs them out. The MOST thesis-aligned action on a platform built to resist compulsive engagement is to actually *leave* when you're done.
- **Keep reading →** (secondary button) — takes them to the paginated chronological feed. Allowed, but framed as the conscious choice rather than the default.

Surrounding both buttons: a "notice whether you're doing it because you want to, or because scrolling feels automatic" prompt. Reflection-first copy, not command-and-control copy.

**Pattern name worth coining in the report:** "end-of-feed checkout." It is a deliberate *friction interstitial* — the opposite of the engagement-maximising designs documented across mainstream social platforms. Most platforms invest heavily in *removing* end-of-feed moments (infinite scroll, autoplay, pull-to-refresh). We invest in *creating* them. Worth citing this in Chapter 3 alongside Madhav et al. (2017) and in Chapter 6 as a concrete example of the project's professional/ethical stance (acting in the user's best interests per BCS Code of Conduct, rather than only in the platform's engagement interests).

**Worth a paragraph in Chapter 3:** this is a design choice that looks like a missing feature (no pagination) but is actually the opposite — a deliberate constraint driven by the thesis. Anti-feature. Same vein as "no reactions, no like counts." Good narrative for the viva.

## Chapter 5 — Results / Discussion

**Important: the marking grid rewards quantitative evaluation explicitly.** 1st/2.1 criteria call for "numerical performance data, statistical comparisons, measurable system metrics". Purely qualitative evaluation caps at low 2.2. So Chapter 5 needs numbers, not just survey quotes.

### Quantitative measurements to bake in (plan these into build)

**Feed-diversity metrics** — run offline on a seeded DB, compare "popular-only feed" vs "our 1:4 diverse feed":
- **Shannon entropy over genres** across the top-20 feed. Higher = more diverse. Expect our feed to score substantially higher.
- **Unique-author ratio** — fraction of distinct users in top-20. Popularity bias clusters around power-users; diversity should broaden this.
- **Long-tail coverage** — % of feed items drawn from below-median-engagement posts. Popular-only ≈ 0%, ours ≈ 20% by design.
- Report as a table: metric / popular-only feed / our feed / delta.

**Performance metrics** — cheap to capture with Spring Boot logging:
- Feed page load time (with cache warm / cold)
- OMDb cache hit rate over a session
- DB queries per feed render (we already dodge N+1 via JOIN FETCH — we can show the measurable benefit)

**User testing (A/B design)** — each participant sees both feeds, side by side or in sequence:
- Likert 1–5 for each feed on: "this felt authentic", "I saw opinions I wouldn't find on Letterboxd", "I'd want to comment on these"
- Forced choice: which would you rather use daily?
- Report means, std devs, and if n≥5 a paired comparison (e.g. Wilcoxon signed-rank — small-n appropriate).

### Survey design (Google Form)

- 5–8 participants (downscoped from PPD's 10 — justify honestly: 14-day timeline, quality over quantity).
- Participants must be 18+; consent page first; right to withdraw; data anonymised.
- Aim for ≥75% per PPD.
- Capture free-text reactions to each feed — pull quotes for the discussion section.
- TODO — design the Google Form when we get to Day 7.

## Chapter 6 — Conclusions / Future Work

- Honest acknowledgement of the scope cut (no chatrooms, no watch parties, no slur filter) and why — 14-day timeline.
- What was achieved: working end-to-end MVP demonstrating the core thesis (diversity-weighted feed vs. popularity-weighted feed).
- Future work candidates (each justifies a paragraph):
  - Chatrooms and watch parties (original PPD scope)
  - Slur filtering / moderation pipeline
  - AI-assisted For You page (embeddings for "opinion diversity")
  - Mobile client
  - Supabase RLS + reduced-privilege DB role (security hardening)
  - Horizontal scaling stability tests (original PPD W30 milestone)

### Admin moderation (Day 6)

The PPD originally listed full report/flag systems, queue-based moderation, and ban workflows. That's a feature in its own right — easily a week of work. Took a smaller approach that still hits the LSEPI commitment.

**Design:**
- Single boolean `is_admin` flag on the `users` table. Granted manually via SQL update — no in-app promotion flow yet (deliberate; adding admin-grants-admin would need its own audit trail and confirmation UX).
- `AppUserDetails.getAuthorities()` returns both `ROLE_USER` and `ROLE_ADMIN` for admin users.
- Spring Security gates `/admin/**` to `hasRole('ADMIN')`.
- `AdminController` exposes four POST endpoints — delete post, delete comment (top-level or reply), delete event, delete event comment. POST not DELETE because HTML forms don't natively support DELETE method.
- Cascades happen at the DB level (`ON DELETE CASCADE` on every FK in `sql/schema.sql`), so deleting a post automatically removes its comments and replies; deleting an event removes RSVPs and the comment thread.
- Delete buttons render via Thymeleaf's `sec:authorize="hasRole('ADMIN')"` — invisible to regular users, visible to admins on `/posts/{id}` and `/events/{id}` show pages. Native browser `confirm()` for the safety prompt — no custom-modal JS needed.

**What this hits:**
- **Social LSEPI:** community moderation. The PPD called for "moderation, report mechanisms and clear guidelines that each users must follow." The guidelines page and admin moderation together cover the first and third; report mechanisms remain future work.
- **BCS Code of Conduct (Professional LSEPI):** acting in users' best interests means content that violates the published guidelines can actually be removed, not just disapproved-of in the abstract.

**Iterated on the limitations (Day 6 second pass):**

After the first pass, three of the originally-listed limitations were closed:

1. **Author can delete their own content.** Posts, comments (top-level or reply), watch parties (host only), and event messages — each has a separate non-admin endpoint (e.g. `POST /posts/{id}/delete`). Owner-or-admin distinction enforced in the controller, not via Spring Security expressions, so the rules live with the entity logic.
2. **Reason-required prompt on admin delete.** Admin delete buttons trigger a JS-native `prompt()` for a reason (3–500 characters validated client-side and server-side). No custom modal infrastructure — uses the browser's built-in dialog.
3. **Audit log.** New `moderation_actions` table records every admin deletion with `(admin_user_id, action_type, target_type, target_id, target_summary, reason, created_at)`. Survives admin account deletion via `ON DELETE SET NULL` on the admin FK. Viewable at `/admin/log`, latest 100 actions newest-first, in a small table view linked from a yellow "Admin" button in the navbar.

**Still acknowledged as future work in Chapter 6 (intentionally cut on time grounds):**
- **Soft delete with undo.** Significant refactor — every query that returns posts/comments/events would need to filter `WHERE deleted_at IS NULL`. Trade-off: implementation simplicity now vs. recoverability later. For an MVP, hard-delete with audit log is defensible.
- **User flag/report mechanism.** Substantial new feature (table + user UI + admin queue). Admins currently notice content directly or are emailed. Listed as the highest-priority Future Work item.
- **Promote-to-admin in the UI.** Manual SQL grant remains. Adding a promote-other-users flow requires its own audit/confirm path. Low frequency, low priority.

**Migrations:** `sql/migration_add_admin_flag.sql` (is_admin flag), then `sql/migration_add_moderation_log.sql` (audit table). Both idempotent. Initial admin granted with one manual SQL update.

**Why this iteration is worth a paragraph in Chapter 4:** the Day 6 first-pass deliberately scoped admin moderation small (one boolean, four endpoints, no audit). The second pass extended it based on review of the limitations themselves — author-delete to close a basic-UX gap, reason+audit to give admin actions accountability. **A clean example of "first pass small, second pass extended where it matters" — exactly the iterative-development narrative the marking grid rewards.**

### Static policy pages (Day 6)

Three plain-HTML pages added to honour LSEPI commitments from the PPD:

- **`/privacy`** — what data is stored, where, how it's protected (BCrypt + TLS), explicit no-trackers / no-ads / no-data-sharing statement, and DPA 2018 / GDPR rights (request copy, request deletion, withdraw consent). Acknowledges that account deletion is currently a manual email-the-author process — flagged as future work. Hits Legal LSEPI (DPA / GDPR transparency).
- **`/terms`** — eligibility (18+), content rules, project context (this is a dissertation project, no SLA, no warranty, "don't post anything you'd be upset to lose"). Plain English, intentionally short. Hits Ethical LSEPI (informed consent — users know what they're signing up for).
- **`/guidelines`** — community norms tied directly to the thesis. Honest takes over performance, disagree-with-takes-not-people, no hate or harassment, no spam, spoiler-tag convention, and an explicit explanation of why threading is one-level only. Hits Social LSEPI (clear community guidelines, scaffolding for moderation).

**Implementation:**
- `StaticPagesController` with three `GET` endpoints, all `permitAll()` in Spring Security.
- Footer fragment (`templates/fragments/footer.html`) included on `/login`, `/signup`, `/feed`, `/for-you`, `/events`, and the three static pages — links to the policies are accessible from every "front-of-house" surface.
- Signup page links to all three with explicit consent copy: "By creating an account you agree to our terms, community guidelines, and confirm you've read the privacy notice."

**Why these are worth their own subsection in Chapter 4 / Chapter 6:**
Static-content pages are not technically interesting, but the LSEPI argument they enable is. The PPD made specific commitments — *user consent*, *transparency about data*, *clear community guidelines*. Without these pages those commitments are aspirational. With them, every LSEPI tick has a URL behind it the marker can navigate to.

## LSEPIs — evidence log

### Legal
- **UK DPA 2018 / GDPR:** passwords hashed with BCrypt, never stored in plain text. Email used only for account identification.
- **Computer Misuse Act 1990:** database access controlled by Spring Security authentication; no unauthenticated endpoints apart from `/login`, `/signup`, and static assets.
- TODO — add data-deletion flow for the report (right to be forgotten).

### Social
- Moderation / reporting: scoped out of the MVP. Flag this honestly as Future Work.
- Diversity-weighted feed directly addresses the echo-chamber concern flagged in the PPD.

### Ethical
- User-testing participants must be 18+ and give informed consent — include the Google Form consent page screenshot.
- Survey data collected should be anonymised — no linking responses to user accounts.

### Professional
- BCS Code of Conduct — honesty about scope cut in the report is itself an expression of integrity.
- Version control (`github.com/Poaty/FYP_Test1`) demonstrates professional development practice.

## Appendix pointers

- **Appendix A — Gantt Chart:** from the original PPD.
- **Appendix B — Rest of Year 3 Gantt:** from the original PPD.
- **Appendix C — Risk Management:** from the original PPD.
- **Appendix D — Code Listing:** pull from the final state of `github.com/Poaty/FYP_Test1` at submission.
- **Appendix E — Database Schema:** paste contents of `sql/schema.sql`.
- **Appendix F — Demo Dataset SQL:** paste contents of `sql/extra_seed_data.sql` and `sql/extra_users_data.sql` — together with `sql/schema.sql`, this is "everything a marker would need to stand up the exact corpus used in evaluation."
- **Appendix G — User Testing Survey and Responses:** screenshots of the Google Form + anonymised responses.
