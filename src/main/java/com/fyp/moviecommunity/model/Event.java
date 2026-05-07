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

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user who made the watch party
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    // film picked for the event
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imdb_id", nullable = false)
    private Movie movie;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    // saved after the controller applies uk time
    @Column(name = "scheduled_for", nullable = false)
    private OffsetDateTime scheduledFor;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}