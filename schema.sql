-- Movie Community Platform — database schema
-- Paste this into Supabase → SQL Editor → New query → Run
-- Safe to re-run (uses IF NOT EXISTS)

-- ============================================================
-- Users
-- ============================================================
create table if not exists users (
  id            bigserial primary key,
  username      text unique not null check (char_length(username) between 3 and 30),
  email         text unique not null,
  password_hash text not null,
  bio           text,
  created_at    timestamptz not null default now()
);

-- ============================================================
-- Movies (cached from OMDb to avoid re-hitting the API)
-- ============================================================
create table if not exists movies (
  imdb_id    text primary key,            -- e.g. "tt3896198"
  title      text not null,
  year       int,
  poster_url text,
  plot       text,
  genre      text,
  director   text,
  cached_at  timestamptz not null default now()
);

-- ============================================================
-- Posts (a user's opinion/review on a specific movie)
-- ============================================================
create table if not exists posts (
  id         bigserial primary key,
  user_id    bigint not null references users(id) on delete cascade,
  imdb_id    text   not null references movies(imdb_id) on delete cascade,
  content    text   not null check (char_length(content) between 1 and 5000),
  created_at timestamptz not null default now()
);

create index if not exists posts_user_idx    on posts(user_id);
create index if not exists posts_movie_idx   on posts(imdb_id);
create index if not exists posts_created_idx on posts(created_at desc);

-- ============================================================
-- Comments (on posts) -- one-level threading via parent_comment_id
-- ============================================================
create table if not exists comments (
  id                bigserial primary key,
  post_id           bigint not null references posts(id) on delete cascade,
  user_id           bigint not null references users(id) on delete cascade,
  parent_comment_id bigint          references comments(id) on delete cascade,
  content           text   not null check (char_length(content) between 1 and 2000),
  created_at        timestamptz not null default now()
);

create index if not exists comments_post_idx   on comments(post_id);
create index if not exists comments_user_idx   on comments(user_id);
create index if not exists comments_parent_idx on comments(parent_comment_id);

-- One-level threading is enforced in the application layer (see CommentController).
-- A trigger could enforce it in the DB too, but the app-side check is enough
-- for an MVP and keeps the schema simple.

-- ============================================================
-- Events (watch parties: a scheduled time + a movie + a chat thread)
-- ============================================================
create table if not exists events (
  id            bigserial primary key,
  host_user_id  bigint not null references users(id) on delete cascade,
  imdb_id       text   not null references movies(imdb_id) on delete cascade,
  title         text   not null check (char_length(title) between 3 and 120),
  description   text   check (description is null or char_length(description) <= 2000),
  scheduled_for timestamptz not null,
  created_at    timestamptz not null default now()
);

create index if not exists events_scheduled_idx on events(scheduled_for);
create index if not exists events_host_idx      on events(host_user_id);
create index if not exists events_movie_idx     on events(imdb_id);

-- ============================================================
-- Event RSVPs (who's attending which event)
-- ============================================================
create table if not exists event_attendances (
  id        bigserial primary key,
  event_id  bigint not null references events(id) on delete cascade,
  user_id   bigint not null references users(id) on delete cascade,
  rsvped_at timestamptz not null default now(),
  unique (event_id, user_id)
);

create index if not exists event_attendances_event_idx on event_attendances(event_id);
create index if not exists event_attendances_user_idx  on event_attendances(user_id);

-- ============================================================
-- Comments on events (kept separate from post comments to avoid
-- polymorphic-association complexity in the JPA layer)
-- ============================================================
create table if not exists event_comments (
  id         bigserial primary key,
  event_id   bigint not null references events(id) on delete cascade,
  user_id    bigint not null references users(id) on delete cascade,
  content    text   not null check (char_length(content) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists event_comments_event_idx on event_comments(event_id);

-- ============================================================
-- Notes for the report (Implementation chapter)
-- ============================================================
-- * We connect from Spring Boot as the `postgres` superuser via
--   the session pooler, so Supabase RLS policies are bypassed.
--   The Spring Boot layer is the security boundary, not Postgres.
--   Future work: create a limited-privilege role + enable RLS.
-- * `movies` is effectively a cache of OMDb responses keyed by
--   imdb_id. On post creation we upsert the movie first, then
--   insert the post referencing it.
-- * Cascade deletes are enabled so removing a user/movie/post
--   cleanly removes their children.
