-- ============================================================
-- extra_users_data.sql
-- +30 users, +20 posts from them, +30 comments from them.
-- ============================================================
-- HOW TO USE:
--   1. Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
--
-- Fixes the "same 5 people talking to each other" feel.
-- All 30 new users get alice's BCrypt hash copied over -- they can
-- log in with password "demo1234", same as the original 5.
--
-- Idempotency: checks for 'filmfan_23' before doing anything.
-- Atomic: wrapped in a DO block, so partial failures roll back.
--
-- Prerequisites:
--   - Java DemoDataSeeder has run at least once (so alice, bob, carla,
--     dave, eve exist and the hot posts from Phase 2 are in the DB).
--
-- Run order (recommended):
--   1. Start Spring Boot once (creates the 5 seed users + their posts).
--   2. extra_seed_data.sql   (50 posts, 100 comments from the 5 seeds).
--   3. extra_users_data.sql  (this file -- 30 users, 20 posts, 30 comments).
-- ============================================================

DO $extra_users$
DECLARE
    alice_hash text;

    -- 30 new user IDs
    u01 bigint; u02 bigint; u03 bigint; u04 bigint; u05 bigint;
    u06 bigint; u07 bigint; u08 bigint; u09 bigint; u10 bigint;
    u11 bigint; u12 bigint; u13 bigint; u14 bigint; u15 bigint;
    u16 bigint; u17 bigint; u18 bigint; u19 bigint; u20 bigint;
    u21 bigint; u22 bigint; u23 bigint; u24 bigint; u25 bigint;
    u26 bigint; u27 bigint; u28 bigint; u29 bigint; u30 bigint;

    -- 20 new post IDs
    np01 bigint; np02 bigint; np03 bigint; np04 bigint; np05 bigint;
    np06 bigint; np07 bigint; np08 bigint; np09 bigint; np10 bigint;
    np11 bigint; np12 bigint; np13 bigint; np14 bigint; np15 bigint;
    np16 bigint; np17 bigint; np18 bigint; np19 bigint; np20 bigint;

    -- Existing Java-seeder hot posts (looked up by content prefix)
    p_interstellar_alice bigint;
    p_darkknight_alice   bigint;
    p_inception_alice    bigint;
    p_matrix_alice       bigint;
    p_parasite_carla     bigint;
    p_seven_eve          bigint;
BEGIN
    -- Idempotency
    IF EXISTS (SELECT 1 FROM users WHERE username = 'filmfan_23') THEN
        RAISE NOTICE 'Extra user data already present. Nothing to do.';
        RETURN;
    END IF;

    -- Grab alice's password hash so new users inherit "demo1234" without
    -- us having to hardcode a BCrypt value.
    SELECT password_hash INTO alice_hash FROM users WHERE username = 'alice';
    IF alice_hash IS NULL THEN
        RAISE EXCEPTION 'alice user not found. Run the Spring Boot app at least once first.';
    END IF;

    -- ================================================================
    -- 30 new users
    -- ================================================================
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('filmfan_23',         'filmfan23@demo.local',    alice_hash, 'New to reviewing, not new to watching.')
      RETURNING id INTO u01;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('mica.r',             'mica.r@demo.local',       alice_hash, 'Short takes, rare opinions.')
      RETURNING id INTO u02;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('kay_reads',          'kay@demo.local',          alice_hash, 'Books first, films a close second.')
      RETURNING id INTO u03;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('arthousebrb',        'arthouse@demo.local',     alice_hash, 'Slow cinema defender.')
      RETURNING id INTO u04;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('midnightcine',       'midnight@demo.local',     alice_hash, 'If it''s after 11pm and on your TV, I''m probably watching it too.')
      RETURNING id INTO u05;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('popcornandpasta',    'popcorn@demo.local',      alice_hash, 'Italian dinners and Italian directors.')
      RETURNING id INTO u06;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('quentinfan',         'qfan@demo.local',         alice_hash, 'Apologist. Unapologetic.')
      RETURNING id INTO u07;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('screenwatcher',      'screen@demo.local',       alice_hash, '')
      RETURNING id INTO u08;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('criterionkid',       'crit@demo.local',         alice_hash, 'Collecting since 2018, watching since before that.')
      RETURNING id INTO u09;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('nightscreen',        'nightscreen@demo.local',  alice_hash, 'Films that feel like weather.')
      RETURNING id INTO u10;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('ellef',              'ellef@demo.local',        alice_hash, '')
      RETURNING id INTO u11;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('cinecrit',           'cinecrit@demo.local',     alice_hash, 'Academic by day, viewer by night.')
      RETURNING id INTO u12;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('mr_j',               'mrj@demo.local',          alice_hash, '')
      RETURNING id INTO u13;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('greta_wants_oscar',  'greta@demo.local',        alice_hash, 'Gerwig stan. No further questions.')
      RETURNING id INTO u14;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('pta_fan',            'pta@demo.local',          alice_hash, 'Magnolia apologist.')
      RETURNING id INTO u15;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('lenorae',            'lenorae@demo.local',      alice_hash, 'Watching everything before 1970 this year.')
      RETURNING id INTO u16;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('theatreghost',       'theatre@demo.local',      alice_hash, 'Preferred format: 35mm.')
      RETURNING id INTO u17;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('kurozawa_fan',       'kuro@demo.local',         alice_hash, 'Seven Samurai is always the answer.')
      RETURNING id INTO u18;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('moviedad',           'moviedad@demo.local',     alice_hash, 'Showing my kids everything I grew up on.')
      RETURNING id INTO u19;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('cutscene',           'cutscene@demo.local',     alice_hash, '')
      RETURNING id INTO u20;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('vhsfuzz',            'vhs@demo.local',          alice_hash, '80s horror. That''s it. That''s the bio.')
      RETURNING id INTO u21;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('reelhonest',         'reel@demo.local',         alice_hash, 'Say what you mean even when nobody agrees.')
      RETURNING id INTO u22;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('latenight_murch',    'murch@demo.local',        alice_hash, 'Editors get the credit here.')
      RETURNING id INTO u23;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('blackbarsonly',      'blackbars@demo.local',    alice_hash, 'Widescreen or I''m not watching.')
      RETURNING id INTO u24;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('mothlight',          'mothlight@demo.local',    alice_hash, 'Avant-garde curious.')
      RETURNING id INTO u25;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('flickflick',         'flick@demo.local',        alice_hash, 'Too many films, not enough time.')
      RETURNING id INTO u26;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('sarahb',             'sarahb@demo.local',       alice_hash, '')
      RETURNING id INTO u27;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('cine_poet',          'cinepoet@demo.local',     alice_hash, 'Looking for beauty in every frame.')
      RETURNING id INTO u28;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('tarkovskydid',       'tark@demo.local',         alice_hash, 'Stalker rewatch every autumn.')
      RETURNING id INTO u29;
    INSERT INTO users (username, email, password_hash, bio) VALUES
      ('normalmovienjoyer',  'normal@demo.local',       alice_hash, 'I just like films. Let me be.')
      RETURNING id INTO u30;

    -- ================================================================
    -- 20 new posts from the new users
    -- ================================================================
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u01, 'tt0816692', 'First time watching this. I see why everyone cries. I''m crying. Why am I crying.')
      RETURNING id INTO np01;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u04, 'tt0457430', 'Del Toro''s fairy tale structure here borrows from classical tragedy more than fantasy. Underrated formal rigour.')
      RETURNING id INTO np02;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u05, 'tt0081505', 'Put this on at 2am expecting background noise. Got hypnotised. Still thinking about the tracking shots.')
      RETURNING id INTO np03;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u09, 'tt0338013', 'Criterion release next month. Already planning the rewatch.')
      RETURNING id INTO np04;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u21, 'tt0087332', 'The 80s haze on this film is part of the film. Any remaster loses something.')
      RETURNING id INTO np05;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u18, 'tt0114369', 'Fincher quoted Kurosawa. I see it. The framing in the finale is pure Throne of Blood.')
      RETURNING id INTO np06;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u07, 'tt0110912', 'I will defend this film on my deathbed.')
      RETURNING id INTO np07;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u02, 'tt0133093', 'Still on the list of films I can point to and say "that one."')
      RETURNING id INTO np08;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u14, 'tt6751668', 'Would Gerwig have made this if she were Korean? The character work is Gerwig-adjacent. High compliment.')
      RETURNING id INTO np09;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u15, 'tt1375666', 'Nolan and PTA are opposites. Nolan explains everything; PTA trusts you. Both work for different reasons.')
      RETURNING id INTO np10;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u17, 'tt0107048', 'Seeing this in a theatre would undo me. Repetition at 24fps for 2 hours. Perfect.')
      RETURNING id INTO np11;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u19, 'tt0468569', 'Showed this to my teenager. She got it. That''s the test passed.')
      RETURNING id INTO np12;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u30, 'tt0816692', 'I just liked it. Thought it was good. That''s it. That''s the post.')
      RETURNING id INTO np13;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u24, 'tt0111161', 'The 1.85:1 aspect ratio here is perfectly judged. Any wider and the prison wouldn''t feel enclosed.')
      RETURNING id INTO np14;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u27, 'tt0119698', 'My first Miyazaki. Thought I''d start with something gentle. I did not.')
      RETURNING id INTO np15;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u11, 'tt0081505', 'Kubrick didn''t trust audiences enough to be scared of metaphor alone. So he gave us Jack with an axe. Fine by me.')
      RETURNING id INTO np16;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u28, 'tt0457430', 'Every frame a painting. Every line a poem. Some films deserve the cliches.')
      RETURNING id INTO np17;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u12, 'tt0133093', 'Baudrillard is in this film if you want to find him, and not if you don''t. Excellent either way.')
      RETURNING id INTO np18;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u23, 'tt0110912', 'The editing hides cuts where you expect them. Every transition is a choice.')
      RETURNING id INTO np19;
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (u26, 'tt0107048', 'Comforting without being syrupy. Rare balance for a studio comedy.')
      RETURNING id INTO np20;

    -- ================================================================
    -- 30 new comments (new users entering existing threads + new threads)
    -- ================================================================
    -- Look up hot existing posts by content prefix.
    SELECT id INTO p_interstellar_alice FROM posts
      WHERE content LIKE 'Finally rewatched this. The docking scene%' LIMIT 1;
    SELECT id INTO p_darkknight_alice FROM posts
      WHERE content LIKE 'Ledger carries a whole franchise%' LIMIT 1;
    SELECT id INTO p_inception_alice FROM posts
      WHERE content LIKE 'Best rewatch value of any Nolan%' LIMIT 1;
    SELECT id INTO p_matrix_alice FROM posts
      WHERE content LIKE 'Holds up in a way I really wasn''t expecting on a 2025 rewatch%' LIMIT 1;
    SELECT id INTO p_parasite_carla FROM posts
      WHERE content LIKE 'Every shot is composed like a still from a better film%' LIMIT 1;
    SELECT id INTO p_seven_eve FROM posts
      WHERE content LIKE 'watched this alone at night and could not sleep%' LIMIT 1;

    -- Comments on existing hot Java-seeder posts (18 comments).
    -- Wrapped so missing posts don't crash the whole script.
    IF p_interstellar_alice IS NOT NULL
       AND p_darkknight_alice IS NOT NULL
       AND p_inception_alice IS NOT NULL
       AND p_matrix_alice IS NOT NULL
       AND p_parasite_carla IS NOT NULL
       AND p_seven_eve IS NOT NULL
    THEN
        INSERT INTO comments (post_id, user_id, content) VALUES
          -- On alice's Interstellar post: 3
          (p_interstellar_alice, u01, 'Watching Interstellar for the first time tonight after reading this thread. No pressure.'),
          (p_interstellar_alice, u05, 'IMAX was a religious experience. Home viewing is a lesser sacrament but still counts.'),
          (p_interstellar_alice, u30, 'Best Nolan for me. That''s my whole comment.'),

          -- On alice's Dark Knight post: 3
          (p_darkknight_alice, u19, 'My kids know every line now. Terrifying.'),
          (p_darkknight_alice, u21, 'Batman Begins is still my favourite, but I understand why this one gets the love.'),
          (p_darkknight_alice, u22, 'Overrated? No. Over-quoted? Absolutely.'),

          -- On alice's Inception post: 3
          (p_inception_alice, u15, 'Nolan at his most Nolan. Take it or leave it, but he knows exactly what he is.'),
          (p_inception_alice, u12, 'Dream logic as structural device. The film IS the dream.'),
          (p_inception_alice, u04, 'Respect to Nolan''s ambition here. I don''t fully love it but I respect it.'),

          -- On alice's Matrix post: 3
          (p_matrix_alice, u07, '1999 was peak cinema year. Fight Club, Matrix, Magnolia, Eyes Wide Shut. Name me a better year.'),
          (p_matrix_alice, u17, 'Watched on 35mm recently. The digital master loses some grit. Worth seeking out film prints.'),
          (p_matrix_alice, u09, 'Criterion collection inclusion overdue. Someone make it happen.'),

          -- On carla's Parasite post: 3
          (p_parasite_carla, u18, 'Bong is doing what Kurosawa did with Seven Samurai: a genre film carrying a sociology paper inside it.'),
          (p_parasite_carla, u14, 'This film deserved everything it got. I don''t want to hear the backlash.'),
          (p_parasite_carla, u11, 'The stairs.'),

          -- On eve's Seven post: 3
          (p_seven_eve, u24, 'Seven in 1.85:1 is perfect. The darkness feels contained. The box scene would be less in a wider frame.'),
          (p_seven_eve, u28, '"What''s in the box" is the most economical plot-reveal in thriller history. Two lines. One word. Cinema.'),
          (p_seven_eve, u23, 'Howard Shore''s score during the final scene. Minimal. Devastating.');
    ELSE
        RAISE NOTICE 'Some Java-seeder hot posts missing -- skipped their 18 extra comments.';
    END IF;

    -- Comments on the new posts we just added (12 comments).
    INSERT INTO comments (post_id, user_id, content) VALUES
      -- np01 (filmfan_23 / Interstellar first watch): 2
      (np01, u05, 'Welcome to the club. Stay for the rewatch, it''s better the second time.'),
      (np01, u16, 'Crying at Interstellar is a rite of passage. You passed.'),

      -- np03 (midnightcine / Shining 2am): 1
      (np03, u02, 'Tracking shots. Yes.'),

      -- np04 (criterionkid / Eternal Sunshine): 2
      (np04, u28, 'The Criterion for this is going to be the version I watch from now on.'),
      (np04, u25, 'Gondry commentary track will be the reason to buy it.'),

      -- np07 (quentinfan / Pulp Fiction deathbed): 2
      (np07, u24, 'Deathbed-worthy film. Confirmed.'),
      (np07, u22, 'I''ll be there with you.'),

      -- np09 (greta_wants_oscar / Parasite): 2
      (np09, u12, 'Bong does theme first and the characters crystallise around it. Gerwig works the other way. Both work.'),
      (np09, u27, 'I haven''t seen Parasite yet. Adding it.'),

      -- np13 (normalmovienjoyer / Interstellar): 1
      (np13, u03, 'Sometimes the best take is just "it was good."'),

      -- np18 (cinecrit / Matrix Baudrillard): 2
      (np18, u25, 'Baudrillard is reportedly annoyed by the film. Which doesn''t stop it being a valid reading.'),
      (np18, u29, 'Simulacra and Simulation being on Neo''s desk was Lana Wachowski telling us exactly what to read.');

    RAISE NOTICE 'Done. Added 30 users, 20 posts, 30 comments.';
    RAISE NOTICE 'Total users now: %',    (SELECT COUNT(*) FROM users);
    RAISE NOTICE 'Total posts now: %',    (SELECT COUNT(*) FROM posts);
    RAISE NOTICE 'Total comments now: %', (SELECT COUNT(*) FROM comments);

END $extra_users$;
