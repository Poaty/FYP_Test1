-- ============================================================
-- migration_add_comment_threading.sql
-- Adds parent_comment_id to comments table for one-level threading.
-- ============================================================
-- HOW TO USE:
--   1. Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
-- Idempotent: safe to run multiple times.
-- ============================================================

alter table comments add column if not exists parent_comment_id bigint;

-- Drop and recreate the FK so re-runs don't fail.
alter table comments drop constraint if exists comments_parent_fk;
alter table comments
    add constraint comments_parent_fk
    foreign key (parent_comment_id) references comments(id) on delete cascade;

create index if not exists comments_parent_idx on comments(parent_comment_id);
