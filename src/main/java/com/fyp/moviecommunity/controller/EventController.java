package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateEventCommentForm;
import com.fyp.moviecommunity.dto.CreateEventForm;
import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventAttendance;
import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.repository.EventAttendanceRepository;
import com.fyp.moviecommunity.repository.EventCommentRepository;
import com.fyp.moviecommunity.repository.EventRepository;
import com.fyp.moviecommunity.repository.UserRepository;
import com.fyp.moviecommunity.security.AppUserDetails;
import com.fyp.moviecommunity.service.MovieService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EventController {

    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final EventRepository events;
    private final EventAttendanceRepository attendances;
    private final EventCommentRepository eventComments;
    private final MovieService movies;
    private final UserRepository users;

    public EventController(EventRepository events,
                           EventAttendanceRepository attendances,
                           EventCommentRepository eventComments,
                           MovieService movies,
                           UserRepository users) {
        this.events = events;
        this.attendances = attendances;
        this.eventComments = eventComments;
        this.movies = movies;
        this.users = users;
    }

    @GetMapping("/events")
    public String list(Model model) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Event> upcoming = events.findUpcoming(now);

        // only show a small set of older events
        List<Event> past = events.findPast(now, PageRequest.of(0, 20));

        Map<Long, Long> counts = new HashMap<>();
        for (Event e : upcoming) counts.put(e.getId(), attendances.countByEvent(e));
        for (Event e : past)     counts.put(e.getId(), attendances.countByEvent(e));

        model.addAttribute("upcoming", upcoming);
        model.addAttribute("past", past);
        model.addAttribute("attendanceCounts", counts);
        return "events/list";
    }

    @GetMapping("/events/new")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        model.addAttribute("q", q);

        // omdb search before making the event
        if (q != null && !q.isBlank()) {
            model.addAttribute("searchPage", movies.searchPaged(q, page));
        }
        return "events/new";
    }

    @GetMapping("/events/new/details")
    public String details(@RequestParam String imdbId, Model model) {
        Movie movie = movies.findOrFetch(imdbId).orElse(null);
        if (movie == null) return "redirect:/events/new?error=notfound";

        model.addAttribute("movie", movie);

        // keep the selected film in the form
        if (!model.containsAttribute("eventForm")) {
            CreateEventForm form = new CreateEventForm();
            form.setImdbId(imdbId);
            model.addAttribute("eventForm", form);
        }
        return "events/details";
    }

    @PostMapping("/events")
    public String create(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("eventForm") CreateEventForm form,
                         BindingResult result,
                         Model model) {

        if (result.hasErrors()) {
            // put the movie back on the page after validation fails
            movies.findOrFetch(form.getImdbId()).ifPresent(m -> model.addAttribute("movie", m));
            return "events/details";
        }

        Movie movie = movies.findOrFetch(form.getImdbId())
                .orElseThrow(() -> new IllegalStateException(
                        "Movie " + form.getImdbId() + " disappeared between pages"));

        Event event = new Event();
        event.setHost(users.getReferenceById(me.getId()));
        event.setMovie(movie);
        event.setTitle(form.getTitle());
        event.setDescription(form.getDescription());

        // save the posted datetime as uk time
        event.setScheduledFor(form.getScheduledFor().atZone(UK).toOffsetDateTime());
        events.save(event);

        return "redirect:/events/" + event.getId();
    }

    @GetMapping("/events/{id}")
    public String show(@PathVariable Long id,
                       @AuthenticationPrincipal AppUserDetails me,
                       Model model) {
        Optional<Event> found = events.findByIdWithHost(id);
        if (found.isEmpty()) return "redirect:/events?notfound";
        Event event = found.get();

        List<EventAttendance> attending = attendances.findByEventOrderByRsvpedAtAsc(event);

        // this page can be viewed while logged out
        Long meId = (me != null) ? me.getId() : null;

        boolean iAmAttending = false;
        if (meId != null) {
            for (EventAttendance a : attending) {
                if (a.getUser().getId().equals(meId)) {
                    iAmAttending = true;
                    break;
                }
            }
        }

        boolean iAmHost = (meId != null) && meId.equals(event.getHost().getId());

        model.addAttribute("event", event);
        model.addAttribute("attendees", attending);
        model.addAttribute("attendingCount", attending.size());
        model.addAttribute("iAmAttending", iAmAttending);
        model.addAttribute("iAmHost", iAmHost);
        model.addAttribute("currentUserId", meId);
        model.addAttribute("eventComments", eventComments.findByEventOrderByCreatedAtAsc(event));

        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CreateEventCommentForm());
        }
        return "events/show";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteOwn(@PathVariable Long id,
                            @AuthenticationPrincipal AppUserDetails me) {
        Optional<Event> ev = events.findById(id);
        if (ev.isEmpty()) return "redirect:/events";

        // only the host can delete their event
        if (!ev.get().getHost().getId().equals(me.getId())) {
            return "redirect:/events/" + id + "?error=notyours";
        }

        events.deleteById(id);
        return "redirect:/events";
    }

    @PostMapping("/events/{id}/rsvp")
    public String toggleRsvp(@PathVariable Long id,
                             @AuthenticationPrincipal AppUserDetails me) {
        Event event = events.findById(id).orElse(null);
        if (event == null) return "redirect:/events?notfound";

        User user = users.getReferenceById(me.getId());
        Optional<EventAttendance> already = attendances.findByEventAndUser(event, user);

        // rsvp button works as a toggle
        if (already.isPresent()) {
            attendances.delete(already.get());
        } else {
            EventAttendance att = new EventAttendance();
            att.setEvent(event);
            att.setUser(user);
            attendances.save(att);
        }

        return "redirect:/events/" + id;
    }
}