package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateCommentForm;
import com.fyp.moviecommunity.dto.CreatePostForm;
import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import com.fyp.moviecommunity.service.MovieService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

    @GetMapping("/new")
    public String newPost(@RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "1") int page,
                          Model model) {
        model.addAttribute("q", q);

        // search omdb before writing the post
        if (q != null && !q.isBlank()) {
            model.addAttribute("searchPage", movies.searchPaged(q, page));
        }
        return "posts/new";
    }

    @GetMapping("/new/write")
    public String writeForm(@RequestParam String imdbId, Model model) {
        Movie movie = movies.findOrFetch(imdbId).orElse(null);
        if (movie == null) return "redirect:/posts/new?error=notfound";

        model.addAttribute("movie", movie);

        // keep the selected movie on the form
        if (!model.containsAttribute("postForm")) {
            CreatePostForm form = new CreatePostForm();
            form.setImdbId(imdbId);
            model.addAttribute("postForm", form);
        }
        return "posts/write";
    }

    @PostMapping("/{id}/delete")
    public String deleteOwn(@PathVariable Long id,
                            @AuthenticationPrincipal AppUserDetails me) {
        Post p = posts.findById(id).orElse(null);
        if (p == null) return "redirect:/feed";

        // only the post author can delete it here
        if (!p.getUser().getId().equals(me.getId())) {
            return "redirect:/posts/" + id + "?error=notyours";
        }

        posts.deleteById(id);
        return "redirect:/feed";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("postForm") CreatePostForm form,
                         BindingResult result,
                         Model model) {

        if (result.hasErrors()) {
            // put the movie back after validation fails
            movies.findOrFetch(form.getImdbId()).ifPresent(m -> model.addAttribute("movie", m));
            return "posts/write";
        }

        Movie movie = movies.findOrFetch(form.getImdbId())
                .orElseThrow(() -> new IllegalStateException(
                        "Movie " + form.getImdbId() + " vanished between pages"));

        Post post = new Post();
        post.setUser(users.getReferenceById(me.getId()));
        post.setMovie(movie);
        post.setContent(form.getContent());
        posts.save(post);

        return "redirect:/feed";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id,
                       @AuthenticationPrincipal AppUserDetails me,
                       Model model) {
        Optional<Post> found = posts.findByIdWithAuthor(id);
        if (found.isEmpty()) return "redirect:/feed?notfound";
        Post post = found.get();

        // post pages can be viewed while logged out
        Long meId = (me != null) ? me.getId() : null;
        boolean iAmPostAuthor = (meId != null) && meId.equals(post.getUser().getId());

        List<Comment> topLevel = comments.findTopLevelByPost(post);
        List<Long> topLevelIds = topLevel.stream().map(Comment::getId).toList();

        // replies grouped by their top level comment
        Map<Long, List<Comment>> repliesByParent;
        if (topLevelIds.isEmpty()) {
            repliesByParent = Map.of();
        } else {
            repliesByParent = comments.findRepliesByParentIds(topLevelIds).stream()
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        }

        long totalCommentCount = topLevel.size()
                + repliesByParent.values().stream().mapToLong(List::size).sum();

        model.addAttribute("post", post);
        model.addAttribute("currentUserId", meId);
        model.addAttribute("iAmPostAuthor", iAmPostAuthor);
        model.addAttribute("topLevelComments", topLevel);
        model.addAttribute("repliesByParent", repliesByParent);
        model.addAttribute("totalCommentCount", totalCommentCount);

        // blank comment form unless validation already added one
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CreateCommentForm());
        }
        return "posts/show";
    }
}