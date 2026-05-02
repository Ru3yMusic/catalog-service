package com.rubymusic.catalog.event;

import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MusicFeedEventPublisherTest {

    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MusicFeedEventPublisher publisher;

    @Test
    void publishAlbumReleased_emitsAlbumReleasedEvent_withAllFields() {
        UUID albumId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        LocalDateTime release = LocalDateTime.of(2026, 5, 1, 21, 0);

        Artist artist = Artist.builder().id(artistId).name("Drake").build();
        Album album = Album.builder()
                .id(albumId)
                .artist(artist)
                .title("Scary Hours 4")
                .coverUrl("https://cdn.example.com/c.jpg")
                .releaseDateTime(release)
                .build();

        publisher.publishAlbumReleased(album);

        ArgumentCaptor<AlbumReleasedEvent> captor = ArgumentCaptor.forClass(AlbumReleasedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        AlbumReleasedEvent event = captor.getValue();

        assertThat(event.albumId()).isEqualTo(albumId);
        assertThat(event.artistId()).isEqualTo(artistId);
        assertThat(event.artistName()).isEqualTo("Drake");
        assertThat(event.title()).isEqualTo("Scary Hours 4");
        assertThat(event.coverUrl()).isEqualTo("https://cdn.example.com/c.jpg");
        assertThat(event.releaseDateTime()).isEqualTo(release);
    }

    @Test
    void publishArtistTopChanged_isTopTrue_emitsEvent() {
        UUID artistId = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);
        Artist artist = Artist.builder()
                .id(artistId)
                .name("Bad Bunny")
                .photoUrl("https://cdn.example.com/p.jpg")
                .isTop(true)
                .createdAt(created)
                .build();

        publisher.publishArtistTopChanged(artist);

        ArgumentCaptor<ArtistTopChangedEvent> captor =
                ArgumentCaptor.forClass(ArtistTopChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        ArtistTopChangedEvent event = captor.getValue();

        assertThat(event.artistId()).isEqualTo(artistId);
        assertThat(event.name()).isEqualTo("Bad Bunny");
        assertThat(event.photoUrl()).isEqualTo("https://cdn.example.com/p.jpg");
        assertThat(event.isTop()).isTrue();
        assertThat(event.createdAt()).isEqualTo(created);
    }

    @Test
    void publishArtistTopChanged_isTopNull_treatedAsFalse() {
        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name("X")
                .isTop(null)
                .createdAt(LocalDateTime.now())
                .build();

        publisher.publishArtistTopChanged(artist);

        ArgumentCaptor<ArtistTopChangedEvent> captor =
                ArgumentCaptor.forClass(ArtistTopChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isTop()).isFalse();
    }

    @Test
    void publishArtistTopChanged_isTopFalse_emitsFalse() {
        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name("X")
                .isTop(false)
                .createdAt(LocalDateTime.now())
                .build();

        publisher.publishArtistTopChanged(artist);

        ArgumentCaptor<ArtistTopChangedEvent> captor =
                ArgumentCaptor.forClass(ArtistTopChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isTop()).isFalse();
    }
}
