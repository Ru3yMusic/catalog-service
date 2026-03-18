package com.rubymusic.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "songs", indexes = {
        @Index(name = "idx_songs_artist_id", columnList = "artist_id"),
        @Index(name = "idx_songs_album_id", columnList = "album_id"),
        @Index(name = "idx_songs_play_count", columnList = "play_count")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    /** Null for standalone singles */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    /** Cover image URL in cloud storage */
    @Column(name = "cover_url", nullable = false, columnDefinition = "TEXT")
    private String coverUrl;

    /** Audio file URL in cloud storage */
    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;

    /** Duration in seconds */
    @Column(nullable = false)
    private Integer duration;

    @Column(columnDefinition = "TEXT")
    private String lyrics;

    /** Incremented via song.played Kafka consumer — not inline */
    @Column(name = "play_count", nullable = false)
    @Builder.Default
    private Long playCount = 0L;

    /** Incremented/decremented via Kafka events from interaction-service */
    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Long likesCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_genres",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @Builder.Default
    private Set<Genre> genres = new HashSet<>();
}
