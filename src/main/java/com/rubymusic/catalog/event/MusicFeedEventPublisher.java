package com.rubymusic.catalog.event;

import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Domain-side publisher: services call these methods inside their
 * {@code @Transactional} flows. The event is captured into Spring's
 * application context immediately, but the Kafka send is deferred to
 * {@link MusicFeedKafkaBridge} which only fires AFTER the transaction
 * commits — so a rollback never produces a phantom realtime broadcast.
 *
 * <p>All entity-to-DTO mapping happens here while the persistence context
 * is still open, so the bridge can safely serialize the immutable record
 * without lazy-loading risk.
 */
@Component
@RequiredArgsConstructor
public class MusicFeedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishAlbumReleased(Album album) {
        applicationEventPublisher.publishEvent(new AlbumReleasedEvent(
                album.getId(),
                album.getArtist().getId(),
                album.getArtist().getName(),
                album.getTitle(),
                album.getCoverUrl(),
                album.getReleaseDateTime()
        ));
    }

    public void publishArtistTopChanged(Artist artist) {
        applicationEventPublisher.publishEvent(new ArtistTopChangedEvent(
                artist.getId(),
                artist.getName(),
                artist.getPhotoUrl(),
                Boolean.TRUE.equals(artist.getIsTop()),
                artist.getCreatedAt()
        ));
    }
}
