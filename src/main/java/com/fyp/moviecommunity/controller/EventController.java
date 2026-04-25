package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.dto.CreateEventCommentForm;
import com.fyp.moviecommunity.dto.CreateEventForm;
import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventAttendance;
import com.fyp.moviecommunity.model.Movie;
import com.fyp.moviecommunity.model.User;
import com.fyp.moviecommunity.omdb.OmdbSearchItem;
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

/**
 * Watch parties.
 *
 *   GET  /events                       -- upcoming events list
 *   GET  /events/new                   -- search a movie to schedule
 *   GET  /events/new/details?imdbId=X  -- title + description + datetime form
 *   POST /events                       -- save the event, redirect to its page
 *   GET  /events/{id}                  -- show event + attendees + comment thread
 *   POST /events/{id}/rsvp             -- toggle RSVP (idempotent)
 *
 * POST /events/{id}/comments lives on EventCommentController.
 */
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

    /** Upcoming + past events list. Past section gives users a way back to
     *  comment threads on events that have already happened. */
    @GetMapping("/events")
    public String list(Model model) {
        OffsetDateTime now = OffsetDateTime.now();
        List<Event> upcoming = events.findUpcoming(now);
        List<Event> past = events.findPast(now, PageRequest.of(0, 20));

        // Combined attendance count map for both lists. Per-event count call
        // is fine while N is small (~20 each); batch with a GROUP BY if it grows.
        Map<Long, Long> counts = new HashMap<>();
        for (Event e : upcoming) counts.put(e.getId(), attendances.countByEvent(e));
        for (Event e : past)     counts.put(e.getId(), attendances.countByEvent(e));

        model.addAttribute("upcoming", upcoming);
        model.addAttribute("past", past);
        model.addAttribute("attendanceCounts", counts);
        return "events/list";
    }

    /** Step 1 of new-event creation: search OMDb for the film. */
    @GetMapping("/events/new")
    public String search(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        if (q != null && !q.isBlank()) {
            List<OmdbSearchItem> results = movies.search(q);
            model.addAttribute("results", results);
        }
        return "events/new";
    }

    /** Step 2: they picked a movie, now fill in the details. */
    @GetMapping("/events/new/details")
    public String details(@RequestParam String imdbId, Model model) {
        Optional<Movie> movie = movies.findOrFetch(imdbId);
        if (movie.isEmpty()) return "redirect:/events/new?error=notfound";
        model.addAttribute("movie", movie.get());
        if (!model.containsAttribute("eventForm")) {
            CreateEventForm form = new CreateEventForm();
            form.setImdbId(imdbId);
            model.addAttribute("eventForm", form);
        }
        return "events/details";
    }

    /** Step 3: save the event. */
    @PostMapping("/events")
    public String create(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("eventForm") CreateEventForm form,
                         BindingResult result,
                         Model model) {

        if (result.hasErrors()) {
            movies.findOrFetch(form.getImdbId()).ifPresent(m -> model.addAttribute("movie", m));
            return "events/details";
        }

        Movie movie = movies.findOrFetch(form.getImdbId())
                .orElseThrow(() -> new IllegalStateException("Movie vanished: " + form.getImdbId()));
        User host = users.getReferenceById(me.getId());

        Event event = new Event();
        event.setHost(host);
        event.setMovie(movie);
        event.setTitle(form.getTitle());
        event.setDescription(form.getDescription());
        // The form gives us a LocalDateTime (no zone); attach UK time so it round-trips
        // through Postgres timestamptz storage cleanly.
        event.setScheduledFor(form.getScheduledFor().atZone(UK).toOffsetDateTime());
        events.save(event);

        return "redirect:/events/" + event.getId();
    }

    /** Single event detail page. */
    @GetMapping("/events/{id}")
    public String show(@PathVariable Long id,
                       @AuthenticationPrincipal AppUserDetails me,
                       Model model) {
        Optional<Event> found = events.findByIdWithHost(id);
        if (found.isEmpty()) return "redirect:/events?notfound";
        Event event = found.get();

        List<EventAttendance> attending = attendances.findByEventOrderByRsvpedAtAsc(event);
        boolean iAmAttending = attending.stream()
                .anyMatch(a -> a.getUser().getId().equals(me.getId()));

        model.addAttribute("event", event);
        model.addAttribute("attendees", attending);
        model.addAttribute("attendingCount", attending.size());
        model.addAttribute("iAmAttending", iAmAttending);
        model.addAttribute("eventComments", eventComments.findByEventOrderByCreatedAtAsc(event));
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CreateEventCommentForm());
        }
        return "events/show";
    }

    /** Toggle RSVP. If you're already in, this removes you; otherwise adds you. */
    @PostMapping("/events/{id}/rsvp")
    public String toggleRsvp(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        Optional<Event> found = events.findById(id);
        if (found.isEmpty()) return "redirect:/events?notfound";
        Event event = found.get();
        User user = users.getReferenceById(me.getId());

        Optional<EventAttendance> existing = attendances.findByEventAndUser(event, user);
        if (existing.isPresent()) {
            attendances.delete(existing.get());
        } else {
            EventAttendance a = new EventAttendance();
            a.setEvent(event);
            a.setUser(user);
            attendances.save(a);
        }

        return "redirect:/events/" + id;
    }
}
