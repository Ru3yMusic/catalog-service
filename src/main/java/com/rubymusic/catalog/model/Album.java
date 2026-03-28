package com.rubymusic.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "albums", indexes = {
        @Index(name = "idx_albums_artist_id", columnList = "artist_id"),
        @Index(name = "idx_albums_release_date", columnList = "release_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    /** Cover image URL in cloud storage */
    @Column(name = "cover_url", nullable = false, columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    /**
     * Optional station this album belongs to. Drives the "Escucha en [station]"
     * and "Navegar a la estación" actions in the album detail view.
     * Plain cross-service reference within catalog-service DB.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    /** Incremented via Kafka events — eventual consistency is acceptable */
    @Column(name = "total_streams", nullable = false)
    @Builder.Default
    private Long totalStreams = 0L;

    /** Computed: number of songs in this album (not stored — SQL subquery) */
    @Formula("(SELECT COUNT(*) FROM songs s WHERE s.album_id = id)")
    private Integer songCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Song> songs = new ArrayList<>();
}
