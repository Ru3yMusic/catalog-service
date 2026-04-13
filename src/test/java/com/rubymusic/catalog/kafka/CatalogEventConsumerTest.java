package com.rubymusic.catalog.kafka;

import com.rubymusic.catalog.service.ArtistService;
import com.rubymusic.catalog.service.SongService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CatalogEventConsumer}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Valid UUID → correct service method called</li>
 *   <li>Invalid (non-UUID) message → error logged, exception NOT propagated (consumer keeps running)</li>
 *   <li>Null message → error logged, exception NOT propagated</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogEventConsumer — error handling")
class CatalogEventConsumerTest {

    @Mock
    private SongService songService;

    @Mock
    private ArtistService artistService;

    @InjectMocks
    private CatalogEventConsumer consumer;

    // ── song.played ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("onSongPlayed() — valid UUID → incrementPlayCount called")
    void onSongPlayed_validUuid_callsIncrementPlayCount() {
        UUID songId = UUID.randomUUID();

        assertDoesNotThrow(() -> consumer.onSongPlayed(songId.toString()));

        verify(songService).incrementPlayCount(songId);
    }

    @Test
    @DisplayName("onSongPlayed() — invalid UUID string → logs error, does not throw")
    void onSongPlayed_invalidUuid_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onSongPlayed("not-a-uuid"));

        verify(songService, never()).incrementPlayCount(any());
    }

    @Test
    @DisplayName("onSongPlayed() — null message → logs error, does not throw")
    void onSongPlayed_null_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onSongPlayed(null));

        verify(songService, never()).incrementPlayCount(any());
    }

    // ── song.liked ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onSongLiked() — valid UUID → incrementLikesCount called")
    void onSongLiked_validUuid_callsIncrementLikesCount() {
        UUID songId = UUID.randomUUID();

        assertDoesNotThrow(() -> consumer.onSongLiked(songId.toString()));

        verify(songService).incrementLikesCount(songId);
    }

    @Test
    @DisplayName("onSongLiked() — invalid UUID string → logs error, does not throw")
    void onSongLiked_invalidUuid_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onSongLiked("garbage-input"));

        verify(songService, never()).incrementLikesCount(any());
    }

    // ── song.unliked ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("onSongUnliked() — valid UUID → decrementLikesCount called")
    void onSongUnliked_validUuid_callsDecrementLikesCount() {
        UUID songId = UUID.randomUUID();

        assertDoesNotThrow(() -> consumer.onSongUnliked(songId.toString()));

        verify(songService).decrementLikesCount(songId);
    }

    @Test
    @DisplayName("onSongUnliked() — invalid UUID string → logs error, does not throw")
    void onSongUnliked_invalidUuid_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onSongUnliked("garbage-input"));

        verify(songService, never()).decrementLikesCount(any());
    }

    // ── artist.followed ───────────────────────────────────────────────────────

    @Test
    @DisplayName("onArtistFollowed() — valid UUID → incrementFollowersCount called")
    void onArtistFollowed_validUuid_callsIncrementFollowersCount() {
        UUID artistId = UUID.randomUUID();

        assertDoesNotThrow(() -> consumer.onArtistFollowed(artistId.toString()));

        verify(artistService).incrementFollowersCount(artistId);
    }

    @Test
    @DisplayName("onArtistFollowed() — invalid UUID string → logs error, does not throw")
    void onArtistFollowed_invalidUuid_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onArtistFollowed("garbage-input"));

        verify(artistService, never()).incrementFollowersCount(any());
    }

    // ── artist.unfollowed ─────────────────────────────────────────────────────

    @Test
    @DisplayName("onArtistUnfollowed() — valid UUID → decrementFollowersCount called")
    void onArtistUnfollowed_validUuid_callsDecrementFollowersCount() {
        UUID artistId = UUID.randomUUID();

        assertDoesNotThrow(() -> consumer.onArtistUnfollowed(artistId.toString()));

        verify(artistService).decrementFollowersCount(artistId);
    }

    @Test
    @DisplayName("onArtistUnfollowed() — invalid UUID string → logs error, does not throw")
    void onArtistUnfollowed_invalidUuid_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onArtistUnfollowed("garbage-input"));

        verify(artistService, never()).decrementFollowersCount(any());
    }
}
