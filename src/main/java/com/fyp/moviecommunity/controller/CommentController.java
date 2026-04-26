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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *   POST /posts/{postId}/comments                   -- new top-level comment
 *   POST /posts/{postId}/comments/{parentId}/reply  -- one-level reply
 *
 * GET for the show page lives on PostController.show. We don't allow
 * replies-to-replies; that's enforced here, not in the DB.
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

    /** New top-level comment on a post. */
    @PostMapping("/posts/{postId}/comments")
    public String create(@PathVariable Long postId,
                         @AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("commentForm") CreateCommentForm form,
                         BindingResult result,
                         Model model) {

        // Validation failed -- re-render the show page with errors and the
        // user's typed text intact. Need to repopulate the model.
        if (result.hasErrors()) {
            return repopulateShowPage(postId, model);
        }

        Optional<Post> postRef = posts.findById(postId);
        if (postRef.isEmpty()) return "redirect:/feed?notfound";

        Comment c = new Comment();
        c.setPost(postRef.get());
        c.setUser(users.getReferenceById(me.getId()));
        c.setContent(form.getContent());
        comments.save(c);

        return "redirect:/posts/" + postId + "#comments";
    }

    /**
     * Reply to a top-level comment. Rejects replies-to-replies (one level only)
     * and replies on a different post than the URL.
     */
    @PostMapping("/posts/{postId}/comments/{parentId}/reply")
    public String reply(@PathVariable Long postId,
                        @PathVariable Long parentId,
                        @AuthenticationPrincipal AppUserDetails me,
                        @Valid @ModelAttribute("replyForm") CreateCommentForm form,
                        BindingResult result,
                        RedirectAttributes flash) {

        Optional<Comment> parentOpt = comments.findById(parentId);
        if (parentOpt.isEmpty()) return "redirect:/posts/" + postId + "?error=notfound";
        Comment parent = parentOpt.get();

        // Sanity: parent must belong to this post.
        if (!parent.getPost().getId().equals(postId)) {
            return "redirect:/posts/" + postId + "?error=mismatch";
        }
        // One-level enforcement: cannot reply to a reply.
        if (!parent.isTopLevel()) {
            return "redirect:/posts/" + postId + "?error=nested";
        }

        // Validation failed -- redirect with flash attributes so the show page
        // can re-render the form with the user's draft text intact.
        if (result.hasErrors()) {
            flash.addFlashAttribute("replyError",
                    result.getFieldError("content") != null
                            ? result.getFieldError("content").getDefaultMessage()
                            : "Write something");
            flash.addFlashAttribute("replyDraft", form.getContent());
            flash.addFlashAttribute("replyParentId", parentId);
            return "redirect:/posts/" + postId + "#comment-" + parentId;
        }

        Comment c = new Comment();
        c.setPost(parent.getPost());
        c.setUser(users.getReferenceById(me.getId()));
        c.setParent(parent);
        c.setContent(form.getContent());
        comments.save(c);

        return "redirect:/posts/" + postId + "#comment-" + parentId;
    }

    /** Author can delete their own comment (top-level or reply). Cascades
     *  through any replies via the DB FK. */
    @PostMapping("/comments/{id}/delete")
    public String deleteOwn(@PathVariable Long id,
                            @AuthenticationPrincipal AppUserDetails me) {
        Optional<Comment> c = comments.findById(id);
        if (c.isEmpty()) return "redirect:/feed";
        if (!c.get().getUser().getId().equals(me.getId())) {
            return "redirect:/posts/" + c.get().getPost().getId() + "?error=notyours";
        }
        Long postId = c.get().getPost().getId();
        comments.deleteById(id);
        return "redirect:/posts/" + postId;
    }

    /** Re-render the show page after a top-level-comment validation error.
     *  Mirrors PostController.show's model setup. */
    private String repopulateShowPage(Long postId, Model model) {
        Optional<Post> found = posts.findByIdWithAuthor(postId);
        if (found.isEmpty()) return "redirect:/feed?notfound";
        Post post = found.get();

        var topLevel = comments.findTopLevelByPost(post);
        var topLevelIds = topLevel.stream().map(Comment::getId).toList();
        var repliesByParent = topLevelIds.isEmpty()
                ? java.util.Map.<Long, java.util.List<Comment>>of()
                : comments.findRepliesByParentIds(topLevelIds).stream()
                    .collect(java.util.stream.Collectors.groupingBy(c -> c.getParent().getId()));

        model.addAttribute("post", post);
        model.addAttribute("topLevelComments", topLevel);
        model.addAttribute("repliesByParent", repliesByParent);
        long total = topLevel.size()
                + repliesByParent.values().stream().mapToLong(java.util.List::size).sum();
        model.addAttribute("totalCommentCount", total);
        return "posts/show";
    }
}
