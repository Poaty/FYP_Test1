package com.fyp.moviecommunity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A registered user of the platform.
 *
 * <p>Maps to the {@code users} table (sql/schema.sql). We store the BCrypt
 * hash only, never the raw password. {@code createdAt} is populated
 * by Postgres via {@code default now()}, so it's marked non-insertable
 * here -- Hibernate won't try to send a value.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(columnDefinition = "text")
    private String bio;

    /** Admins can delete other people's posts/comments/events.
     *  Granted manually with a SQL update; no in-app way to promote yet. */
    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
