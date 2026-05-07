package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.model.Comment;
import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventComment;
import com.fyp.moviecommunity.model.ModerationAction;
import com.fyp.moviecommunity.model.Post;
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

// Admin moderation endpoints. Access is handled in SecurityConfig.
// Deletes are posted from normal HTML forms, and an audit row is kept
// before the target content is removed.
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final int PREVIEW_CHARS = 200;

    private final PostRepository posts;
    private final CommentRepository comments;
    private final EventRepository events;
    private final EventCommentRepository eventComments;
    private final ModerationActionRepository modLog;
    private final UserRepository users;

    public AdminController(PostRepository posts,
                           CommentRepository comments,
                           EventRepository events,
                           EventCommentRepository eventComments,
                           ModerationActionRepository modLog,
                           UserRepository users) {
        this.posts = posts;
        this.comments = comments;
        this.events = events;
        this.eventComments = eventComments;
        this.modLog = modLog;
        this.users = users;
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam String reason,
                             @AuthenticationPrincipal AppUserDetails me) {
        String r = reason == null ? "" : reason.trim();
        if (r.length() < 3 || r.length() > 500) {
            return "redirect:/feed";
        }

        Post p = posts.findById(id).orElse(null);
        if (p == null) return "redirect:/feed";

        writeAudit(me, "DELETE_POST", "POST", id, preview(p.getContent()), r);
        posts.deleteById(id);
        log.info("[admin:{}] removed post #{} -- {}", me.getUsername(), id, r);
        return "redirect:/feed";
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam String reason,
                                @AuthenticationPrincipal AppUserDetails me) {
        Optional<Comment> maybe = comments.findById(id);
        if (maybe.isEmpty()) return "redirect:/feed";
        Comment c = maybe.get();

        Long postId = c.getPost().getId();

        if (!reasonOk(reason)) return "redirect:/posts/" + postId;
        String r = reason.trim();

        writeAudit(me, "DELETE_COMMENT", "COMMENT", id, preview(c.getContent()), r);
        comments.deleteById(id);
        log.info("[admin:{}] removed comment #{} on post {} -- {}",
                me.getUsername(), id, postId, r);
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id,
                              @RequestParam String reason,
                              @AuthenticationPrincipal AppUserDetails me) {
        if (!reasonOk(reason)) return "redirect:/events";

        Optional<Event> evtOpt = events.findById(id);
        if (evtOpt.isEmpty()) return "redirect:/events";
        Event evt = evtOpt.get();

        String r = reason.trim();
        writeAudit(me, "DELETE_EVENT", "EVENT", id, preview(evt.getTitle()), r);
        events.deleteById(id);
        log.info("[admin:{}] removed event #{} \"{}\" -- {}",
                me.getUsername(), id, evt.getTitle(), r);
        return "redirect:/events";
    }

    @PostMapping("/event-comments/{id}/delete")
    public String deleteEventComment(@PathVariable Long id,
                                     @RequestParam String reason,
                                     @AuthenticationPrincipal AppUserDetails me) {
        Optional<EventComment> opt = eventComments.findById(id);
        if (opt.isEmpty() || !reasonOk(reason)) return "redirect:/events";

        EventComment ec = opt.get();
        Long eventId = ec.getEvent().getId();
        String r = reason.trim();

        writeAudit(me, "DELETE_EVENT_COMMENT", "EVENT_COMMENT", id, preview(ec.getContent()), r);
        eventComments.deleteById(id);
        log.info("[admin:{}] removed event-comment #{} (event {}) -- {}",
                me.getUsername(), id, eventId, r);
        return "redirect:/events/" + eventId;
    }

    @GetMapping("/log")
    public String log(Model model) {
        model.addAttribute("actions", modLog.findRecent(PageRequest.of(0, 100)));
        return "admin/log";
    }

    private boolean reasonOk(String reason) {
        if (reason == null) return false;
        int n = reason.trim().length();
        return n >= 3 && n <= 500;
    }

    private String preview(String s) {
        if (s == null) return null;
        if (s.length() <= PREVIEW_CHARS) return s;
        return s.substring(0, PREVIEW_CHARS - 3) + "...";
    }

    private void writeAudit(AppUserDetails me, String action, String targetType,
                            Long targetId, String preview, String reason) {
        ModerationAction row = new ModerationAction();
        row.setAdmin(users.getReferenceById(me.getId()));
        row.setActionType(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setTargetSummary(preview);
        row.setReason(reason);
        modLog.save(row);
    }
}