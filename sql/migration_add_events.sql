-- ============================================================
-- migration_add_events.sql
-- Adds the events / event_attendances / event_comments tables to
-- an existing DB that was created from the original schema.sql.
-- ============================================================
-- HOW TO USE:
--   1. Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
-- Idempotent (uses IF NOT EXISTS); safe to run multiple times.
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

create table if not exists event_attendances (
  id        bigserial primary key,
  event_id  bigint not null references events(id) on delete cascade,
  user_id   bigint not null references users(id) on delete cascade,
  rsvped_at timestamptz not null default now(),
  unique (event_id, user_id)
);

create index if not exists event_attendances_event_idx on event_attendances(event_id);
create index if not exists event_attendances_user_idx  on event_attendances(user_id);

create table if not exists event_comments (
  id         bigserial primary key,
  event_id   bigint not null references events(id) on delete cascade,
  user_id    bigint not null references users(id) on delete cascade,
  content    text   not null check (char_length(content) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists event_comments_event_idx on event_comments(event_id);
