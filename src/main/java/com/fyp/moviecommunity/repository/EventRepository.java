package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Event;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Upcoming events (scheduledFor in the future), soonest first.
     * JOIN FETCH on host + movie so the list page can render without
     * lazy-loading exploding (open-in-view is off, same drill as posts).
     */
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.scheduledFor >= :now
        order by e.scheduledFor asc
        """)
    List<Event> findUpcoming(OffsetDateTime now);

    /**
     * Single event with host + movie eagerly loaded -- for the show page.
     */
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.id = :id
        """)
    Optional<Event> findByIdWithHost(Long id);

    /**
     * Past events (already happened), most recent first.
     * Pageable so we can cap the list -- old comment threads still accessible
     * via /events/{id} directly even if we don't list every past event forever.
     */
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.scheduledFor < :now
        order by e.scheduledFor desc
        """)
    List<Event> findPast(OffsetDateTime now, Pageable pageable);
}
