package com.fyp.moviecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per admin moderation action. Survives deletion of the target
 * (target_id is just a number, not an FK -- the row that was deleted
 * is gone, and that's fine for an audit log).
 *
 * Survives admin account deletion too: the admin FK is ON DELETE SET NULL,
 * so the action is preserved with admin_user_id = null and a username we
 * can't recover. Acceptable for an MVP audit log.
 */
@Entity
@Table(name = "moderation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable -- if the admin account is later deleted, this becomes null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User admin;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_summary", columnDefinition = "text")
    private String targetSummary;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
