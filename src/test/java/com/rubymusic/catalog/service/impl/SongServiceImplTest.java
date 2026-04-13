package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.repository.AlbumRepository;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.repository.SongRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SongServiceImpl} — focuses on counter methods.
 *
 * <p>Task 4.3 — verifies that when {@code song.played} is processed,
 * the artist's {@code monthlyListeners} counter is incremented alongside
 * the song's {@code playCount}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SongServiceImpl — counter methods")
class SongServiceImplTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private SongServiceImpl songService;

    // ── incrementPlayCount (Task 4.3) ─────────────────────────────────────────

    @Test
    @DisplayName("incrementPlayCount() — song exists with artist → playCount AND artistMonthlyListeners incremented")
    void incrementPlayCount_songWithArtist_incrementsBothCounters() {
        UUID songId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();

        when(songRepository.findArtistIdBySongId(songId)).thenReturn(Optional.of(artistId));

        songService.incrementPlayCount(songId);

        verify(songRepository).incrementPlayCount(songId);
        verify(artistRepository).incrementMonthlyListeners(artistId);
    }

    @Test
    @DisplayName("incrementPlayCount() — song not found (orphaned event) → playCount incremented, monthlyListeners skipped")
    void incrementPlayCount_songNotFound_onlyIncrementsSongCounter() {
        UUID songId = UUID.randomUUID();

        when(songRepository.findArtistIdBySongId(songId)).thenReturn(Optional.empty());

        songService.incrementPlayCount(songId);

        verify(songRepository).incrementPlayCount(songId);
        verify(artistRepository, never()).incrementMonthlyListeners(any());
    }

    // ── incrementLikesCount ───────────────────────────────────────────────────

    @Test
    @DisplayName("incrementLikesCount() — delegates to repository")
    void incrementLikesCount_delegatesToRepository() {
        UUID songId = UUID.randomUUID();

        songService.incrementLikesCount(songId);

        verify(songRepository).incrementLikesCount(songId);
    }

    // ── decrementLikesCount ───────────────────────────────────────────────────

    @Test
    @DisplayName("decrementLikesCount() — delegates to repository")
    void decrementLikesCount_delegatesToRepository() {
        UUID songId = UUID.randomUUID();

        songService.decrementLikesCount(songId);

        verify(songRepository).decrementLikesCount(songId);
    }
}
