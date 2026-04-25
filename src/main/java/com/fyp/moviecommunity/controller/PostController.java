package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateCommentForm;
import com.fyp.moviecommunity.dto.CreatePostForm;
import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import com.fyp.moviecommunity.service.MovieService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The post-writing flow is two pages:
 *
 *   GET /posts/new                -> search box; with ?q=... also shows OMDb results
 *   GET /posts/new/write?imdbId=X -> shows the chosen movie + textarea
 *   POST /posts                    -> saves the post, bounces to /feed
 *
 * Why two pages: keeps things simple. Could be one page with AJAX later,
 * but for the MVP the round-trip is totally fine.
 */
@Controller
@RequestMapping("/posts")
public class PostController {

    private final MovieService movies;
    private final PostRepository posts;
    private final UserRepository users;
    private final CommentRepository comments;

    public PostController(MovieService movies, PostRepository posts,
                          UserRepository users, CommentRepository comments) {
        this.movies = movies;
        this.posts = posts;
        this.users = users;
        this.comments = comments;
    }

    /** Search page. Empty box on first load; with ?q= we render OMDb hits. */
    @GetMapping("/new")
    public String newPost(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        if (q != null && !q.isBlank()) {
            model.addAttribute("results", movies.search(q));
        }
        return "posts/new";
    }

    /** The write page. They've already picked a movie on the previous screen. */
    @GetMapping("/new/write")
    public String writeForm(@RequestParam String imdbId, Model model) {
        Optional<Movie> movie = movies.findOrFetch(imdbId);
        if (movie.isEmpty()) {
            // Shouldn't happen unless they hand-edited the URL. Kick them back.
            return "redirect:/posts/new?error=notfound";
        }
        model.addAttribute("movie", movie.get());
        if (!model.containsAttribute("postForm")) {
            CreatePostForm form = new CreatePostForm();
            form.setImdbId(imdbId);
            model.addAttribute("postForm", form);
        }
        return "posts/write";
    }

    /** Actually save the post. */
    @PostMapping
    public String create(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("postForm") CreatePostForm form,
                         BindingResult result,
                         Model model) {

        // Validation failed -- re-render the write page with the errors.
        // We need to re-fetch the movie so the template can render it.
        if (result.hasErrors()) {
            movies.findOrFetch(form.getImdbId()).ifPresent(m -> model.addAttribute("movie", m));
            return "posts/write";
        }

        // Grab (or fetch + cache) the movie. Bail out if OMDb somehow forgot about it.
        Movie movie = movies.findOrFetch(form.getImdbId())
                .orElseThrow(() -> new IllegalStateException(
                        "Movie " + form.getImdbId() + " vanished between pages"));

        Post post = new Post();
        // getReferenceById returns a proxy with just the id -- no extra SELECT, Hibernate
        // just uses the id for the FK.
        post.setUser(users.getReferenceById(me.getId()));
        post.setMovie(movie);
        post.setContent(form.getContent());
        posts.save(post);

        return "redirect:/feed";
    }

    /**
     * Single-post page. Shows the post + movie header, top-level comments
     * grouped with their replies, and forms for both new comments and replies.
     *
     * Two batched queries for the comment tree: top-level + replies. Java
     * groups replies under their parent so the template doesn't need to do
     * lookups per comment.
     */
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Optional<Post> found = posts.findByIdWithAuthor(id);
        if (found.isEmpty()) {
            return "redirect:/feed?notfound";
        }
        Post post = found.get();
        model.addAttribute("post", post);

        // Build the threaded comment view: top-level + replies grouped by parent.
        java.util.List<com.fyp.moviecommunity.model.Comment> topLevel =
                comments.findTopLevelByPost(post);
        java.util.List<Long> topLevelIds = topLevel.stream()
                .map(com.fyp.moviecommunity.model.Comment::getId)
                .toList();
        java.util.Map<Long, java.util.List<com.fyp.moviecommunity.model.Comment>> repliesByParent =
                topLevelIds.isEmpty()
                        ? java.util.Map.of()
                        : comments.findRepliesByParentIds(topLevelIds).stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    c -> c.getParent().getId()));

        model.addAttribute("topLevelComments", topLevel);
        model.addAttribute("repliesByParent", repliesByParent);
        long totalCommentCount = topLevel.size()
                + repliesByParent.values().stream().mapToLong(java.util.List::size).sum();
        model.addAttribute("totalCommentCount", totalCommentCount);

        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CreateCommentForm());
        }
        return "posts/show";
    }
}
