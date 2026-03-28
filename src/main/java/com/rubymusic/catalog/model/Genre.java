package com.rubymusic.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "genres")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Hex color for gradient start — e.g. #FF5733 */
    @Column(name = "gradient_start", nullable = false, length = 7)
    private String gradientStart;

    /** Hex color for gradient end */
    @Column(name = "gradient_end", nullable = false, length = 7)
    private String gradientEnd;

    /** Computed at query time — number of songs tagged with this genre */
    @Formula("(SELECT COUNT(*) FROM song_genres sg WHERE sg.genre_id = id)")
    private Integer songCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Song> songs = new HashSet<>();
}
