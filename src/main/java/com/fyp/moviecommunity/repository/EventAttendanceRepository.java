package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventAttendance;
import com.fyp.moviecommunity.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {

    // existing rsvp row
    Optional<EventAttendance> findByEventAndUser(Event event, User user);

    // attendee count for event cards
    long countByEvent(Event event);

    // attendees shown on the event page
    @Query("""
        select a from EventAttendance a
        join fetch a.user
        where a.event = :event
        order by a.rsvpedAt asc
        """)
    List<EventAttendance> findByEventOrderByRsvpedAtAsc(Event event);
}