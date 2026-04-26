package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventComment;
import com.fyp.moviecommunity.model.ModerationAction;
import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.EventCommentRepository;
import com.fyp.moviecommunity.repository.EventRepository;
import com.fyp.moviecommunity.repository.ModerationActionRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin moderation endpoints. Hard-deletes content (cascades happen at the DB
 * level via the schema's ON DELETE CASCADE on every FK).
 *
 *   POST /admin/posts/{id}/delete            -- delete a post + its comments
 *   POST /admin/comments/{id}/delete         -- delete a comment + its replies
 *   POST /admin/events/{id}/delete           -- delete an event + RSVPs + thread
 *   POST /admin/event-comments/{id}/delete   -- delete one event comment
 *
 * All endpoints require ROLE_ADMIN (enforced in SecurityConfig). No in-app
 * way to grant admin yet -- run a SQL update on the users.is_admin column.
 *
 * POST not DELETE because HTML forms don't natively support DELETE.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final int SUMMARY_LEN = 200;

    private final PostRepository posts;
    private final CommentRepository comments;
    private final EventRepository events;
    private final EventCommentRepository eventComments;
    private final ModerationActionRepository moderationLog;
    private final UserRepository users;

    public AdminController(PostRepository posts,
                           CommentRepository comments,
                           EventRepository events,
                           EventCommentRepository eventComments,
                           ModerationActionRepository moderationLog,
                           UserRepository users) {
        this.posts = posts;
        this.comments = comments;
        this.events = events;
        this.eventComments = eventComments;
        this.moderationLog = moderationLog;
        this.users = users;
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam String reason,
                             @AuthenticationPrincipal AppUserDetails me) {
        Optional<Post> p = posts.findById(id);
        if (p.isEmpty() || !validReason(reason)) return "redirect:/feed";
        record(me, "DELETE_POST", "POST", id, abbreviate(p.get().getContent()), reason);
        posts.deleteById(id);
        log.info("Admin {} deleted post id={} -- reason: {}", me.getUsername(), id, reason);
        return "redirect:/feed";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam String reason,
                                @AuthenticationPrincipal AppUserDetails me) {
        Optional<Comment> c = comments.findById(id);
        if (c.isEmpty() || !validReason(reason)) return "redirect:/feed";
        Long postId = c.get().getPost().getId();
        record(me, "DELETE_COMMENT", "COMMENT", id, abbreviate(c.get().getContent()), reason);
        comments.deleteById(id);
        log.info("Admin {} deleted comment id={} (post {}) -- reason: {}",
                me.getUsername(), id, postId, reason);
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @RequestParam String reason,
                              @AuthenticationPrincipal AppUserDetails me) {
        Optional<Event> e = events.findById(id);
        if (e.isEmpty() || !validReason(reason)) return "redirect:/events";
        record(me, "DELETE_EVENT", "EVENT", id, abbreviate(e.get().getTitle()), reason);
        events.deleteById(id);
        log.info("Admin {} deleted event id={} -- reason: {}", me.getUsername(), id, reason);
        return "redirect:/events";
    }

    @PostMapping("/event-comments/{id}/delete")
    public String deleteEventComment(@PathVariable Long id,
                                     @RequestParam String reason,
                                     @AuthenticationPrincipal AppUserDetails me) {
        Optional<EventComment> ec = eventComments.findById(id);
        if (ec.isEmpty() || !validReason(reason)) return "redirect:/events";
        Long eventId = ec.get().getEvent().getId();
        record(me, "DELETE_EVENT_COMMENT", "EVENT_COMMENT", id, abbreviate(ec.get().getContent()), reason);
        eventComments.deleteById(id);
        log.info("Admin {} deleted event comment id={} (event {}) -- reason: {}",
                me.getUsername(), id, eventId, reason);
        return "redirect:/events/" + eventId;
    }

    /** Admin audit log -- recent actions, newest first. */
    @GetMapping("/log")
    public String log(Model model) {
        model.addAttribute("actions", moderationLog.findRecent(PageRequest.of(0, 100)));
        return "admin/log";
    }

    // ------------------------------------------------------------------------

    private boolean validReason(String reason) {
        return reason != null && reason.trim().length() >= 3 && reason.trim().length() <= 500;
    }

    private String abbreviate(String s) {
        if (s == null) return null;
        return s.length() <= SUMMARY_LEN ? s : s.substring(0, SUMMARY_LEN - 3) + "...";
    }

    private void record(AppUserDetails me, String actionType, String targetType,
                        Long targetId, String targetSummary, String reason) {
        ModerationAction a = new ModerationAction();
        // getReferenceById gives us a proxy with just the id -- no extra SELECT
        // and the audit row survives even if the admin's account is later deleted.
        User admin = users.getReferenceById(me.getId());
        a.setAdmin(admin);
        a.setActionType(actionType);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setTargetSummary(targetSummary);
        a.setReason(reason.trim());
        moderationLog.save(a);
    }
}
