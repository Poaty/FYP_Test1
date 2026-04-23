# Report Notes

Running log for the FYP dissertation. Every bullet here is a thing worth writing about in the report. When drafting the final doc, open this file and lift bullets into prose.

**How to use:**
- Claude updates this after each meaningful decision or piece of work
- Bullets are grouped by report chapter so you can write chapters in order
- `Citation:` tags mark where a Harvard reference needs to slot in
- `Evidence:` tags mark where a screenshot, code listing, or log should be pasted

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
- `Evidence: schema.sql in the repo root.`

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

### For You algorithm

- TODO — core feature. Design the 1:4 diversity logic, justify the choice of "unconventional" signal, discuss tuning.

## Chapter 5 — Results / Discussion

- User testing target: 5–8 participants via Google Form (downscoped from the PPD's 10 due to the compressed timeline — justify honestly in the report).
- Metrics to capture in the survey:
  - Overall satisfaction (1–5)
  - "The feed showed me movie opinions I wouldn't have found on Letterboxd/IMDb" (agree/disagree)
  - "I felt comfortable sharing my honest opinion" (agree/disagree)
  - Free-text: what was confusing / what did you like
- Aiming for ≥75% satisfaction per PPD.
- TODO — add data and screenshots post-testing.

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
- **Appendix E — Database Schema:** paste contents of `schema.sql`.
- **Appendix F — User Testing Survey and Responses:** screenshots of the Google Form + anonymised responses.
