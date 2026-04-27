package com.rubymusic.catalog.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emitted when an album becomes publicly visible — either created with a past
 * release date, updated to pull the date back to the past, or flipped by the
 * scheduler when its scheduled moment arrives.
 *
 * <p>Carries the full minimal card payload so the realtime consumer doesn't
 * need to refetch from the catalog API.
 */
public record AlbumReleasedEvent(
        UUID albumId,
        UUID artistId,
        String artistName,
        String title,
        String coverUrl,
        LocalDateTime releaseDateTime
) {}
