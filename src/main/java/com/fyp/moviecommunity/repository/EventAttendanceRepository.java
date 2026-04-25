package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventAttendance;
import com.fyp.moviecommunity.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {

    /** Has this user RSVPed to this event already? */
    Optional<EventAttendance> findByEventAndUser(Event event, User user);

    /** How many people are going to this event? */
    long countByEvent(Event event);

    /**
     * Attendees of a single event, with the user joined in so the show
     * page can list usernames without lazy-loading.
     */
    @Query("""
        select a from EventAttendance a
        join fetch a.user
        where a.event = :event
        order by a.rsvpedAt asc
        """)
    List<EventAttendance> findByEventOrderByRsvpedAtAsc(Event event);
}
