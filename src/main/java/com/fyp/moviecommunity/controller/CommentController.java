package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateCommentForm;
import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Just one endpoint: POST /posts/{postId}/comments.
 *
 * GET for the single-post page lives on PostController.show (keeps the post
 * page's read logic in one place).
 */
@Controller
public class CommentController {

    private final CommentRepository comments;
    private final PostRepository posts;
    private final UserRepository users;

    public CommentController(CommentRepository comments, PostRepository posts, UserRepository users) {
        this.comments = comments;
        this.posts = posts;
        this.users = users;
    }

    @PostMapping("/posts/{postId}/comments")
    public String create(@PathVariable Long postId,
                         @AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("commentForm") CreateCommentForm form,
                         BindingResult result,
                         Model model) {

        // Validation failed (empty or too long) -- re-render the show page with the errors.
        // Need the post with author + movie joined so Thymeleaf can render without
        // lazy-loading exploding (open-in-view is off).
        if (result.hasErrors()) {
            Optional<Post> found = posts.findByIdWithAuthor(postId);
            if (found.isEmpty()) return "redirect:/feed?notfound";
            Post post = found.get();
            model.addAttribute("post", post);
            model.addAttribute("postComments", comments.findByPostOrderByCreatedAtAsc(post));
            return "posts/show";
        }

        // Happy path -- just need a reference to the Post for the FK.
        Optional<Post> postRef = posts.findById(postId);
        if (postRef.isEmpty()) return "redirect:/feed?notfound";

        Comment c = new Comment();
        c.setPost(postRef.get());
        c.setUser(users.getReferenceById(me.getId()));
        c.setContent(form.getContent());
        comments.save(c);

        // Send them back to the same post, anchored to the new comment area.
        return "redirect:/posts/" + postId + "#comments";
    }
}
