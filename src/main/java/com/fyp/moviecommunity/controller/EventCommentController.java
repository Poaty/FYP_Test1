package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateEventCommentForm;
import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventAttendance;
import com.fyp.moviecommunity.model.EventComment;
import com.fyp.moviecommunity.repository.EventAttendanceRepository;
import com.fyp.moviecommunity.repository.EventCommentRepository;
import com.fyp.moviecommunity.repository.EventRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EventCommentController {

    private final EventRepository events;
    private final EventCommentRepository comments;
    private final EventAttendanceRepository attendances;
    private final UserRepository users;

    public EventCommentController(EventRepository events,
                                  EventCommentRepository comments,
                                  EventAttendanceRepository attendances,
                                  UserRepository users) {
        this.events = events;
        this.comments = comments;
        this.attendances = attendances;
        this.users = users;
    }

    @PostMapping("/events/{eventId}/comments/{commentId}/delete")
    public String deleteOwn(@PathVariable Long eventId,
                            @PathVariable Long commentId,
                            @AuthenticationPrincipal AppUserDetails me) {
        Optional<EventComment> ec = comments.findById(commentId);
        if (ec.isEmpty()) return "redirect:/events/" + eventId;

        // only the author can remove their event comment
        if (!ec.get().getUser().getId().equals(me.getId())) {
            return "redirect:/events/" + eventId + "?error=notyours";
        }

        comments.deleteById(commentId);
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/events/{eventId}/comments")
    public String create(@PathVariable Long eventId,
                         @AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("commentForm") CreateEventCommentForm form,
                         BindingResult result,
                         Model model) {

        if (result.hasErrors()) {
            Optional<Event> found = events.findByIdWithHost(eventId);
            if (found.isEmpty()) return "redirect:/events?notfound";
            Event event = found.get();

            List<EventAttendance> attending = attendances.findByEventOrderByRsvpedAtAsc(event);

            // rebuild the event page so the form error can show
            boolean iAmAttending = false;
            for (EventAttendance a : attending) {
                if (a.getUser().getId().equals(me.getId())) {
                    iAmAttending = true;
                    break;
                }
            }

            model.addAttribute("event", event);
            model.addAttribute("attendees", attending);
            model.addAttribute("attendingCount", attending.size());
            model.addAttribute("iAmAttending", iAmAttending);
            model.addAttribute("eventComments", comments.findByEventOrderByCreatedAtAsc(event));
            return "events/show";
        }

        Event event = events.findById(eventId).orElse(null);
        if (event == null) return "redirect:/events?notfound";

        // save the new event comment
        EventComment c = new EventComment();
        c.setEvent(event);
        c.setUser(users.getReferenceById(me.getId()));
        c.setContent(form.getContent());
        comments.save(c);

        return "redirect:/events/" + eventId + "#comments";
    }
}