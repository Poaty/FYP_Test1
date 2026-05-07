package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateCommentForm;
import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        // reload the post page with the validation error
        if (result.hasErrors()) {
            return rebuildShowModel(postId, model);
        }

        Post post = posts.findById(postId).orElse(null);
        if (post == null) return "redirect:/feed?notfound";

        Comment c = new Comment();
        c.setPost(post);
        c.setUser(users.getReferenceById(me.getId()));
        c.setContent(form.getContent());
        comments.save(c);

        // jump back down to the comments section
        return "redirect:/posts/" + postId + "#comments";
    }

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

        // stop replies being added to the wrong post
        if (!parent.getPost().getId().equals(postId)) {
            return "redirect:/posts/" + postId + "?error=mismatch";
        }

        // only one level of replies is allowed
        if (!parent.isTopLevel()) {
            return "redirect:/posts/" + postId + "?error=nested";
        }

        if (result.hasErrors()) {
            // send the failed reply back to the same reply box
            String msg = "Write something";
            if (result.getFieldError("content") != null) {
                msg = result.getFieldError("content").getDefaultMessage();
            }
            flash.addFlashAttribute("replyError", msg);
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

    @PostMapping("/comments/{id}/delete")
    public String deleteOwn(@PathVariable Long id,
                            @AuthenticationPrincipal AppUserDetails me) {
        Optional<Comment> opt = comments.findById(id);
        if (opt.isEmpty()) return "redirect:/feed";
        Comment c = opt.get();

        Long postId = c.getPost().getId();

        // users can only delete their own comments
        if (!c.getUser().getId().equals(me.getId())) {
            return "redirect:/posts/" + postId + "?error=notyours";
        }

        comments.deleteById(id);
        return "redirect:/posts/" + postId;
    }

    private String rebuildShowModel(Long postId, Model model) {
        Optional<Post> found = posts.findByIdWithAuthor(postId);
        if (found.isEmpty()) return "redirect:/feed?notfound";
        Post post = found.get();

        List<Comment> topLevel = comments.findTopLevelByPost(post);
        List<Long> topLevelIds = topLevel.stream().map(Comment::getId).toList();

        Map<Long, List<Comment>> repliesByParent;
        if (topLevelIds.isEmpty()) {
            repliesByParent = Map.of();
        } else {
            repliesByParent = comments.findRepliesByParentIds(topLevelIds).stream()
                    .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        }

        long total = topLevel.size()
                + repliesByParent.values().stream().mapToLong(List::size).sum();

        model.addAttribute("post", post);
        model.addAttribute("topLevelComments", topLevel);
        model.addAttribute("repliesByParent", repliesByParent);
        model.addAttribute("totalCommentCount", total);
        return "posts/show";
    }
}