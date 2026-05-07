package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Event;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {

    // upcoming events for the list page
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.scheduledFor >= :now
        order by e.scheduledFor asc
        """)
    List<Event> findUpcoming(OffsetDateTime now);

    // event with host and movie loaded
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.id = :id
        """)
    Optional<Event> findByIdWithHost(Long id);

    // older events shown under the upcoming list
    @Query("""
        select e from Event e
        join fetch e.host
        join fetch e.movie
        where e.scheduledFor < :now
        order by e.scheduledFor desc
        """)
    List<Event> findPast(OffsetDateTime now, Pageable pageable);
}