-- ============================================================
-- migration_add_admin_flag.sql
-- Adds the is_admin flag to users table.
-- ============================================================
-- HOW TO USE:
--   1. Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
-- Idempotent: safe to run multiple times.
--
-- After running, grant yourself admin with:
--   UPDATE users SET is_admin = true WHERE username = 'YOUR_USERNAME';
-- ============================================================

alter table users
    add column if not exists is_admin boolean not null default false;
