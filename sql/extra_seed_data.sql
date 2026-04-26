-- ============================================================
-- extra_seed_data.sql
-- 50 extra posts + 100 extra comments for the movie-community DB.
-- ============================================================
-- HOW TO USE:
--   1. Open Supabase -> SQL Editor -> New query
--   2. Paste this entire file
--   3. Click Run
--
-- Uses existing seeded users (alice, bob, carla, dave, eve) and
-- existing seeded movies (the 15 cached by the Java DemoDataSeeder).
-- Idempotent: re-running detects a known post content and bails out.
--
-- Engagement distribution chosen on purpose:
--   - 8 "hot" posts        -> 6 comments each (= 48)
--   - 8 "medium" posts     -> 3 comments each (= 24)
--   - 14 "light" posts     -> 2 comments each (= 28)
--   - 20 "quiet" posts     -> 0 comments       (= long-tail candidates)
--   Total: 50 posts, 100 comments.
--
-- After running this, peak comment count will be ~8, so the For You
-- "popular" threshold (20% of peak) lands at ~2 -- a properly meaningful
-- cut-off rather than the degenerate "any comment at all" we had before.
-- ============================================================

DO $seeder$
DECLARE
    alice_id bigint;
    bob_id   bigint;
    carla_id bigint;
    dave_id  bigint;
    eve_id   bigint;

    p01 bigint; p02 bigint; p03 bigint; p04 bigint; p05 bigint;
    p06 bigint; p07 bigint; p08 bigint; p09 bigint; p10 bigint;
    p11 bigint; p12 bigint; p13 bigint; p14 bigint; p15 bigint;
    p16 bigint; p17 bigint; p18 bigint; p19 bigint; p20 bigint;
    p21 bigint; p22 bigint; p23 bigint; p24 bigint; p25 bigint;
    p26 bigint; p27 bigint; p28 bigint; p29 bigint; p30 bigint;
    p31 bigint; p32 bigint; p33 bigint; p34 bigint; p35 bigint;
    p36 bigint; p37 bigint; p38 bigint; p39 bigint; p40 bigint;
    p41 bigint; p42 bigint; p43 bigint; p44 bigint; p45 bigint;
    p46 bigint; p47 bigint; p48 bigint; p49 bigint; p50 bigint;
BEGIN

    -- Idempotency guard: bail if this data is already present.
    IF EXISTS (SELECT 1 FROM posts WHERE content LIKE 'Third watch and still the ending breaks me%') THEN
        RAISE NOTICE 'Extra seed data already present. Nothing to do.';
        RETURN;
    END IF;

    -- User IDs
    SELECT id INTO alice_id FROM users WHERE username = 'alice';
    SELECT id INTO bob_id   FROM users WHERE username = 'bob';
    SELECT id INTO carla_id FROM users WHERE username = 'carla';
    SELECT id INTO dave_id  FROM users WHERE username = 'dave';
    SELECT id INTO eve_id   FROM users WHERE username = 'eve';

    IF alice_id IS NULL OR bob_id IS NULL OR carla_id IS NULL OR dave_id IS NULL OR eve_id IS NULL THEN
        RAISE EXCEPTION 'Seeded users missing. Run the Spring Boot app at least once first so DemoDataSeeder can create alice/bob/carla/dave/eve.';
    END IF;

    -- ================================================================
    -- Section A: 8 HOT posts (destined for 6 comments each)
    -- ================================================================
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0816692',
       'Third watch and still the ending breaks me. Not the science -- the scene where Cooper watches Murph''s messages after the time dilation.')
      RETURNING id INTO p01;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0133093',
       'Every frame of Neo meeting Morpheus is a masterclass in escalating tension without a single punch thrown.')
      RETURNING id INTO p02;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt6751668',
       'The scene where the Kim family is hiding under the table is the most stressful 10 minutes I have sat through. Silence, a dog bowl, nothing else.')
      RETURNING id INTO p03;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0468569',
       'just watched this with my flatmate who didn''t know anything about batman. she didn''t breathe for the last 30 minutes')
      RETURNING id INTO p04;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0133093',
       'The lobby scene. The reload. The coat hitting the ground in slow motion. Still peak 25 years later.')
      RETURNING id INTO p05;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0133093',
       'The Architect scene is deliberately incomprehensible because Neo isn''t supposed to fully get it either. Most films would explain; this one trusts you.')
      RETURNING id INTO p06;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0133093',
       'The red pill / blue pill conversation is older than the memes made of it. Watch it knowing nothing and it still lands cold.')
      RETURNING id INTO p07;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0133093',
       'my dad watched this with me. said it held up. my dad is notoriously hard to impress. just saying.')
      RETURNING id INTO p08;

    -- ================================================================
    -- Section B: 8 MEDIUM posts (destined for 3 comments each)
    -- ================================================================
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt1375666',
       'Caught a detail on rewatch -- the spinning top wobbles but the camera cuts before it falls. Still doesn''t confirm anything.')
      RETURNING id INTO p09;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0114369',
       'Fincher is the only director who makes me think cities themselves are characters. Seven''s unnamed metropolis is doing as much work as Freeman.')
      RETURNING id INTO p10;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0107048',
       'The fact that Bill Murray''s character goes through genuine despair before kindness is the whole point. Stealth-philosophical comedy.')
      RETURNING id INTO p11;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt6751668',
       'spent a week thinking about the stone. what does the stone MEAN')
      RETURNING id INTO p12;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt1375666',
       'the hallway fight in zero gravity was done PRACTICALLY??? like they built a spinning set??? I''m unwell')
      RETURNING id INTO p13;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0119698',
       'Miyazaki''s women. San, Eboshi -- each one complex enough to carry her own film. Villains that aren''t villains.')
      RETURNING id INTO p14;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt6751668',
       'The rain sequence from the upper house to the lower house. Geography as metaphor, weather as plot device, class as staircase.')
      RETURNING id INTO p15;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt5294550',
       'A film that swings so wildly between genres you never quite know where you are, and that disorientation is the point.')
      RETURNING id INTO p16;

    -- ================================================================
    -- Section C: 14 LIGHT posts (destined for 2 comments each)
    -- ================================================================
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0111161',
       'Hot take: the ending goes on too long. Cut it at the poster and it''s a perfect film.')
      RETURNING id INTO p17;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0081505',
       'The corridor scenes where the geometry of the hotel doesn''t quite add up. Kubrick did that on purpose.')
      RETURNING id INTO p18;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0816692',
       'Anne Hathaway''s love-transcends-dimensions speech is the most divisive scene in the film and I''m the one defending it.')
      RETURNING id INTO p19;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0338013',
       'The house collapsing around them as memories are erased. I''ll watch anything Michel Gondry does for the rest of his career.')
      RETURNING id INTO p20;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0114369',
       'Freeman''s opening scene where he surveys the crime is the best "establishing character through action" in any film I can name.')
      RETURNING id INTO p21;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0133093',
       'The sequels. Let''s not talk about the sequels.')
      RETURNING id INTO p22;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0468569',
       'The ferry scene is the moral heart of the film and it''s the only Batman film with one.')
      RETURNING id INTO p23;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0110912',
       'Jackson''s delivery of "what ain''t no country I''ve ever heard of" is the line reading of the 90s.')
      RETURNING id INTO p24;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0107048',
       'A film about a man learning to love himself before he can love anyone else. Sneaks this in under comedy.')
      RETURNING id INTO p25;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0111161',
       'The narration shouldn''t work. In any other film it would be a crutch. Here it''s the soul.')
      RETURNING id INTO p26;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0816692',
       'Respect to Nolan but this is a film where the science pretends to be the point and the emotion IS the point.')
      RETURNING id INTO p27;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0114369',
       'The quiet library scene where Somerset is researching. A procedural that breathes before it explodes.')
      RETURNING id INTO p28;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0110912',
       'The non-linear structure isn''t a gimmick, it''s the whole film. Every chapter re-contextualises the one before.')
      RETURNING id INTO p29;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0087332',
       'Murray''s ad-libs in the hotel scene. There''s a reason this is THE comedy everyone remembers.')
      RETURNING id INTO p30;

    -- ================================================================
    -- Section D: 20 QUIET posts (0 comments -- long-tail fodder)
    -- ================================================================
    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0119698',
       'The forest spirit. The way it walks. The way it dies. Nothing in animated cinema prepares you for that final sequence.')
      RETURNING id INTO p31;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt1375666',
       'The ending isn''t ambiguous. The ring on Cobb''s finger is the tell. People who say it''s ambiguous missed the point.')
      RETURNING id INTO p32;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0133093',
       'Trinity. Carrie-Anne Moss deserved more films this good. The opening chase is her film; Neo just catches up.')
      RETURNING id INTO p33;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0081505',
       'The camera movement through the hotel is the real ghost. Kubrick''s Steadicam work is the horror.')
      RETURNING id INTO p34;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0338013',
       'watched with my ex. we are not back together. the film was right about us')
      RETURNING id INTO p35;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0468569',
       'The "why so serious" line is good once and overquoted ever since. Can we retire it.')
      RETURNING id INTO p36;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0457430',
       'Del Toro''s films are fables. Here the fable is happening during an actual war and it matters that it is.')
      RETURNING id INTO p37;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt1375666',
       'The snow fortress level is overshadowed by the hotel. It shouldn''t be. Structurally it''s the most coherent part of the film.')
      RETURNING id INTO p38;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0133093',
       'first watch tonight, i finally get why everyone won''t shut up about this film')
      RETURNING id INTO p39;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0107048',
       'The date-with-Rita sequence is the emotional peak. Most comedies would put the jokes here. Ramis put the yearning.')
      RETURNING id INTO p40;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0081505',
       'Kubrick''s Shining vs the novel is a legitimate debate. King has a point, actually.')
      RETURNING id INTO p41;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0457430',
       'Ofelia''s ending is the most contested scene in modern fantasy. I keep thinking about what the film wants you to believe.')
      RETURNING id INTO p42;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt0087332',
       'Weaver as Dana. Aykroyd as Ray. Murray as Venkman. A perfect ensemble in every scene.')
      RETURNING id INTO p43;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0114369',
       'the box. the box. the BOX.')
      RETURNING id INTO p44;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (carla_id, 'tt0338013',
       'Kate Winslet''s performance is so unlike anything else in her career. Watch it back-to-back with Titanic and it''s uncanny.')
      RETURNING id INTO p45;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (dave_id, 'tt0119698',
       'Overrated in the context of Miyazaki. Spirited Away is better. I said what I said.')
      RETURNING id INTO p46;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt6751668',
       'The scholar''s stone. The basement door. Every object in this film is a weapon pointed at capitalism.')
      RETURNING id INTO p47;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (alice_id, 'tt5294550',
       'The equisapien reveal is the most audacious swing I''ve seen a film take this decade. Respect, even where the film misses.')
      RETURNING id INTO p48;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (bob_id, 'tt0816692',
       'The TARS humour beats are the best character moments in the film. A robot is the emotional anchor. That''s a choice.')
      RETURNING id INTO p49;

    INSERT INTO posts (user_id, imdb_id, content) VALUES
      (eve_id, 'tt0111161',
       'watched this at midnight. cried when brooks died. cried when andy made it out. went to bed emotional')
      RETURNING id INTO p50;

    -- ================================================================
    -- COMMENTS (100 total)
    -- ================================================================
    INSERT INTO comments (post_id, user_id, content) VALUES
      -- p01 (alice / Interstellar): 6 comments
      (p01, bob_id,   'Cooper watching Murph''s video messages. It hits different every single watch.'),
      (p01, carla_id, 'The time dilation makes the separation physical as well as emotional. That''s the filmmaking trick.'),
      (p01, dave_id,  'Eh. I find the resolution too tidy. The emotion is earned, the plot mechanics aren''t.'),
      (p01, eve_id,   'i cried three times the first watch. i''m prepared for it now but i still cry twice'),
      (p01, alice_id, 'eve that''s the right ratio'),
      (p01, bob_id,   'Agreed with Eve. Knowing it''s coming doesn''t help. Maybe makes it worse.'),

      -- p02 (bob / Matrix Morpheus): 6 comments
      (p02, alice_id, 'The sunglasses. Morpheus''s whole silhouette. The slow reveal of his face.'),
      (p02, carla_id, 'Laurence Fishburne''s voice work deserves its own award. Half the scene is the voice.'),
      (p02, dave_id,  'Fine, this scene is perfect. The rest of the film is still overrated.'),
      (p02, eve_id,   'i love that you notice these things. i just vibe when i watch films'),
      (p02, bob_id,   'Every frame is composed like someone who knew this would be studied in film classes.'),
      (p02, alice_id, '^ Yes. They knew.'),

      -- p03 (carla / Parasite table): 6 comments
      (p03, bob_id,   'Ten minutes of no music, no dialogue, just the dog bowl scraping the floor. That''s filmmaking confidence.'),
      (p03, alice_id, 'The way Bong Joon-ho uses silence is a genre of its own at this point.'),
      (p03, dave_id,  'Most stressful scene is the stairs down to the basement, not the table. Fight me.'),
      (p03, eve_id,   'watched it through my fingers. i''m not built for thrillers and this film knew'),
      (p03, carla_id, 'Dave, the stairs scene is excellent but the table is longer and more sustained. Different kind of dread.'),
      (p03, bob_id,   'Both are right. Parasite is structured like a series of held breaths.'),

      -- p04 (eve / Dark Knight flatmate): 6 comments
      (p04, alice_id, 'Welcome to the "Dark Knight changed my teenage years" club. Membership never expires.'),
      (p04, bob_id,   'The interrogation scene is where you realise Ledger isn''t just good, he''s historic.'),
      (p04, carla_id, 'eve, you picked the right first watch. It only loses a little on subsequent rewatches.'),
      (p04, dave_id,  'Still overrated. Fine, I''ll stop.'),
      (p04, eve_id,   'dave i can HEAR you typing that with an eye roll'),
      (p04, alice_id, 'Dave has one (1) opinion and it''s "overrated".'),

      -- p05 (alice / Matrix lobby): 6 comments
      (p05, bob_id,   'That scene launched a thousand imitators and none of them matched it.'),
      (p05, carla_id, 'The choreography. Yuen Wo-Ping''s work here is still the benchmark.'),
      (p05, dave_id,  'I concede. The lobby scene is legitimately great.'),
      (p05, eve_id,   'the sound. the PIT PIT PIT of the brass shells hitting the floor'),
      (p05, alice_id, 'eve the sound design deserves its own conversation'),
      (p05, bob_id,   'Sound design is the underrated hero of this entire film.'),

      -- p06 (bob / Matrix Architect): 6 comments
      (p06, alice_id, 'Took me three watches to realise the Architect''s speech isn''t contradictory, he''s just using math.'),
      (p06, carla_id, 'The Architect scene is the Wachowskis testing how patient the audience is. It''s fine, they should have.'),
      (p06, dave_id,  'The scene where the franchise disappears up its own premise. Reloaded, ugh.'),
      (p06, eve_id,   'i''m still lost here. hold me'),
      (p06, bob_id,   'Eve, you''re not supposed to fully follow it. That''s the point.'),
      (p06, carla_id, 'Confusion is the point. Agreed with Bob.'),

      -- p07 (carla / Matrix red pill): 6 comments
      (p07, alice_id, 'The stillness before the choice. Morpheus doesn''t rush him.'),
      (p07, bob_id,   'Which is the only reason it has the weight it does. A modern film would cut away twice.'),
      (p07, dave_id,  'Fine, this scene is iconic for a reason. I''ve conceded, we can move on.'),
      (p07, eve_id,   'the hands. the pills. i think about this scene so often'),
      (p07, carla_id, 'Eve, you''re describing what hundreds of film students write essays about.'),
      (p07, bob_id,   'It really is the platonic ideal of a "threshold" scene in cinema.'),

      -- p08 (eve / Matrix dad): 6 comments
      (p08, alice_id, 'Dad-approved cinema is the highest honour.'),
      (p08, bob_id,   'This is how you know something has ascended into canon.'),
      (p08, carla_id, 'Eve, your dad and I would agree on more than you think.'),
      (p08, dave_id,  'Tell your dad I respect him but I still disagree on the sequels.'),
      (p08, eve_id,   'dave he doesn''t watch sequels. he says "the first was enough." iconic behaviour'),
      (p08, alice_id, 'Your dad is right.'),

      -- ---- MEDIUM POSTS (3 comments each) ----
      -- p09 (alice / Inception top): 3 comments
      (p09, bob_id,   'The wobble is the key detail everyone misses on first watch.'),
      (p09, carla_id, 'Nolan himself has said he knows and won''t say. Which is the right answer.'),
      (p09, dave_id,  'The whole question is a red herring. The film is about Cobb, not the top.'),

      -- p10 (bob / Seven cities): 3 comments
      (p10, alice_id, 'Zodiac''s San Francisco. Seven''s unnamed metropolis. Fincher knows what cities do to people.'),
      (p10, carla_id, 'The rain in Seven IS a character. It''s relentless and it doesn''t let anyone dry off.'),
      (p10, dave_id,  'Fincher is good. This scene is good. Yes, we all agree.'),

      -- p11 (bob / Groundhog despair): 3 comments
      (p11, alice_id, 'The suicide montage is genuinely dark for a "comedy". Kids watching for the first time must be shocked.'),
      (p11, carla_id, 'Ramis said in interviews it was meant to show the character''s full spiritual journey. Full meaning deep.'),
      (p11, eve_id,   'i remember being a kid watching this thinking "this is a funny movie??"'),

      -- p12 (eve / Parasite stone): 3 comments
      (p12, alice_id, 'Object as metaphor. The stone represents aspirational weight -- heavy, unwanted, passed down.'),
      (p12, carla_id, 'It''s a suseok. A Korean scholar''s rock. The class markers of old wealth aspired to by new money.'),
      (p12, bob_id,   'Also in the end the stone is returned to water. Meaning: only nature accepts it back.'),

      -- p13 (eve / Inception hallway): 3 comments
      (p13, alice_id, 'Practical rotating sets. It looks impossible because it WAS impossible until they built the set.'),
      (p13, bob_id,   'Nolan''s commitment to practical effects is why every action scene feels weightier than CGI action.'),
      (p13, carla_id, 'Gordon-Levitt did most of the stunts himself. Watch it again knowing that.'),

      -- p14 (carla / Mononoke women): 3 comments
      (p14, alice_id, 'San and Eboshi never meet without the tension of the whole film in their scene.'),
      (p14, bob_id,   'Miyazaki''s villains aren''t villains. Eboshi is doing what she thinks is right for her people.'),
      (p14, dave_id,  'Okay this is fair. Mononoke is actually his best.'),

      -- p15 (alice / Parasite rain): 3 comments
      (p15, bob_id,   'Going down to the basement is going down the economic ladder. The staircase IS the film.'),
      (p15, carla_id, 'Water as carrier of class -- water runs downhill, and so does everything that''s hidden underneath.'),
      (p15, eve_id,   'this kind of comment is why i keep reading this platform'),

      -- p16 (bob / Sorry to Bother): 3 comments
      (p16, alice_id, 'The equisapien reveal might be the most audacious thing in any modern film. I respect the swing.'),
      (p16, carla_id, 'Swings you can see coming don''t swing. Boots Riley takes swings you don''t see coming.'),
      (p16, dave_id,  'I respect it more than I enjoy it. Which might be the point of it.'),

      -- ---- LIGHT POSTS (2 comments each) ----
      -- p17 (dave / Shawshank ending): 2 comments
      (p17, alice_id, 'The ocean reunion is the whole film''s emotional payoff. Cutting it would be a crime.'),
      (p17, bob_id,   'Respectfully, Dave is wrong. The film needs the catharsis.'),

      -- p18 (carla / Shining corridor): 2 comments
      (p18, alice_id, 'The impossible geometry is the haunting. The building is wrong and your eye can''t say why.'),
      (p18, bob_id,   'The Overlook is the real antagonist. Jack is just its instrument.'),

      -- p19 (carla / Interstellar Hathaway): 2 comments
      (p19, alice_id, 'Hathaway sells that scene. She''s doing heroic work with a line that shouldn''t work.'),
      (p19, dave_id,  'Hathaway IS good. The line reading is great. The line itself is terrible.'),

      -- p20 (bob / Eternal Sunshine): 2 comments
      (p20, alice_id, 'The beach house sequence is the most romantic scene of the 2000s and I won''t be told otherwise.'),
      (p20, carla_id, 'Gondry''s practical effects here are in a category of their own.'),

      -- p21 (alice / Seven opening): 2 comments
      (p21, bob_id,   'Introduction of character through action, no dialogue. Screenwriting class material.'),
      (p21, carla_id, 'Freeman''s whole performance is about restraint and precision. The opening sets it.'),

      -- p22 (dave / Matrix sequels): 2 comments
      (p22, alice_id, 'The Architect scene is the turning point. Before it they were one film, after they were confused.'),
      (p22, eve_id,   'i pretend the sequels don''t exist'),

      -- p23 (bob / Dark Knight ferry): 2 comments
      (p23, alice_id, 'The ferry scene is Nolan''s response to post-9/11 sentiment. Rare for a superhero film to go there.'),
      (p23, carla_id, 'The moment where both ships make the same choice. That''s the thesis of the film.'),

      -- p24 (alice / Pulp Fiction line): 2 comments
      (p24, bob_id,   'Tarantino was lucky to cast Jackson before anyone else did. That line gave him a career.'),
      (p24, carla_id, 'A line reading that became its own subgenre of film moment.'),

      -- p25 (carla / Groundhog love): 2 comments
      (p25, alice_id, 'The final morning sequence is so quiet. Most comedies would play a swell. Ramis didn''t.'),
      (p25, bob_id,   'Self-love before romantic love is the entire moral, snuck in under jokes.'),

      -- p26 (bob / Shawshank narration): 2 comments
      (p26, alice_id, 'Morgan Freeman''s voice is the film''s secret weapon. Try imagining it without him.'),
      (p26, carla_id, 'The narration does the exposition the film needs without feeling like exposition.'),

      -- p27 (dave / Interstellar science): 2 comments
      (p27, alice_id, 'This is a rare Dave take I fully agree with. The emotional is the substance.'),
      (p27, bob_id,   'The physics scenes are there to make the emotional beats feel earned. Not the other way around.'),

      -- p28 (carla / Seven library): 2 comments
      (p28, alice_id, 'Libraries in Fincher films are where characters think. Quiet and intentional.'),
      (p28, bob_id,   'Procedural sequences pace the film. The library scene is a breath before the finale.'),

      -- p29 (bob / Pulp structure): 2 comments
      (p29, alice_id, 'Each timeline jump re-flavours what you thought you saw. Genius filmmaking.'),
      (p29, carla_id, 'Structure as storytelling. Not a gimmick, a thesis.'),

      -- p30 (bob / Ghostbusters Murray): 2 comments
      (p30, alice_id, '"Back off, man, I''m a scientist." Peak Murray.'),
      (p30, eve_id,   'i watched this last week and didn''t realise how many lines i already knew by osmosis');

    RAISE NOTICE 'Done. Added 50 posts and 100 comments.';
    RAISE NOTICE 'Total posts now: %',    (SELECT COUNT(*) FROM posts);
    RAISE NOTICE 'Total comments now: %', (SELECT COUNT(*) FROM comments);

END $seeder$;
