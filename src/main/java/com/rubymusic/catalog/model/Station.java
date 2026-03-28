package com.rubymusic.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Display name shown in the UI (e.g. "Rock", "Salsa"). Must be unique. */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Hex color for the gradient start — e.g. #8B0000 */
    @Column(name = "gradient_start", nullable = false, length = 7)
    private String gradientStart;

    /** Hex color for the gradient end */
    @Column(name = "gradient_end", nullable = false, length = 7)
    private String gradientEnd;

    /**
     * The single genre that categorizes this station (shown in the station detail view).
     * Songs are NOT derived from this genre — they are directly assigned via {@code songs}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    /**
     * Songs explicitly assigned to this station by the admin (min 3 enforced on creation).
     * Join table: station_songs (station_id, song_id).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "station_songs",
            joinColumns = @JoinColumn(name = "station_id"),
            inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    @Builder.Default
    private Set<Song> songs = new HashSet<>();

    /** Computed: number of songs directly assigned to this station. */
    @Formula("(SELECT COUNT(*) FROM station_songs ss WHERE ss.station_id = id)")
    private Integer songCount;

    /**
     * When false the station is hidden from all listings and search.
     * Used for admin-disabled stations without deletion.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
