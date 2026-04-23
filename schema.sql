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
-- Comments (on posts)
-- ============================================================
create table if not exists comments (
  id         bigserial primary key,
  post_id    bigint not null references posts(id) on delete cascade,
  user_id    bigint not null references users(id) on delete cascade,
  content    text   not null check (char_length(content) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists comments_post_idx on comments(post_id);
create index if not exists comments_user_idx on comments(user_id);

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
