-- ============================================================
-- migration_add_moderation_log.sql
-- Adds moderation_actions table for the admin audit log.
-- ============================================================
-- HOW TO USE:
--   1. Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
-- Idempotent: safe to run multiple times.
-- ============================================================

create table if not exists moderation_actions (
  id              bigserial primary key,
  admin_user_id   bigint references users(id) on delete set null,
  action_type     text   not null,
  target_type     text   not null,
  target_id       bigint not null,
  target_summary  text,
  reason          text   not null check (char_length(reason) between 3 and 500),
  created_at      timestamptz not null default now()
);

create index if not exists moderation_actions_created_idx on moderation_actions(created_at desc);
create index if not exists moderation_actions_admin_idx   on moderation_actions(admin_user_id);
