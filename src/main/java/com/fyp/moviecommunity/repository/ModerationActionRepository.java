package com.fyp.moviecommunity.repository;

import com.fyp.moviecommunity.model.ModerationAction;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {

    /** Recent admin actions, newest first, with admin user joined in. */
    @Query("""
        select a from ModerationAction a
        left join fetch a.admin
        order by a.createdAt desc
        """)
    List<ModerationAction> findRecent(Pageable pageable);
}
