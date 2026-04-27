package com.rubymusic.catalog.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emitted whenever the {@code isTop} flag of an artist transitions in either
 * direction (false→true via admin, or true→false via admin). The {@code isTop}
 * field reflects the new state; consumers decide whether to add or remove the
 * card from the top-artists carousel.
 *
 * <p>{@code createdAt} is included so the frontend can place the artist in the
 * correct slot of the createdAt-DESC ordered list without an extra query.
 */
public record ArtistTopChangedEvent(
        UUID artistId,
        String name,
        String photoUrl,
        boolean isTop,
        LocalDateTime createdAt
) {}
