package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.Event;
import com.fyp.moviecommunity.model.EventComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventCommentRepository extends JpaRepository<EventComment, Long> {

    /**
     * Comments on an event, oldest first. Author joined in so the show
     * page renders without lazy-loading.
     */
    @Query("""
        select c from EventComment c
        join fetch c.user
        where c.event = :event
        order by c.createdAt asc
        """)
    List<EventComment> findByEventOrderByCreatedAtAsc(Event event);
}
