package com.rubymusic.catalog.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicFeedKafkaBridgeTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private MusicFeedKafkaBridge bridge;

    @Test
    void onAlbumReleased_serializesAndSendsToKafka() throws Exception {
        UUID albumId = UUID.randomUUID();
        AlbumReleasedEvent event = new AlbumReleasedEvent(
                albumId, UUID.randomUUID(), "Drake", "Album", "cover.jpg",
                LocalDateTime.now());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"json\":true}");

        bridge.onAlbumReleased(event);

        verify(kafkaTemplate).send(MusicFeedTopics.ALBUM_RELEASED, albumId.toString(), "{\"json\":true}");
    }

    @Test
    void onAlbumReleased_jsonFailure_isLoggedAndSwallowed() throws Exception {
        AlbumReleasedEvent event = new AlbumReleasedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "X", "Y", "c.jpg", LocalDateTime.now());
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("boom") {});

        bridge.onAlbumReleased(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void onAlbumReleased_kafkaFailure_isLoggedAndSwallowed() throws Exception {
        AlbumReleasedEvent event = new AlbumReleasedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "X", "Y", "c.jpg", LocalDateTime.now());
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("broker down"));

        // Must NOT propagate — bridge is a "best effort" notifier
        bridge.onAlbumReleased(event);
    }

    @Test
    void onArtistTopChanged_serializesAndSendsToKafka() throws Exception {
        UUID artistId = UUID.randomUUID();
        ArtistTopChangedEvent event = new ArtistTopChangedEvent(
                artistId, "Bad Bunny", "p.jpg", true, LocalDateTime.now());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"top\":true}");

        bridge.onArtistTopChanged(event);

        verify(kafkaTemplate).send(eq(MusicFeedTopics.ARTIST_TOP_CHANGED),
                eq(artistId.toString()), eq("{\"top\":true}"));
    }

    @Test
    void onArtistTopChanged_jsonFailure_isLoggedAndSwallowed() throws Exception {
        ArtistTopChangedEvent event = new ArtistTopChangedEvent(
                UUID.randomUUID(), "X", "p.jpg", false, LocalDateTime.now());
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("boom") {});

        bridge.onArtistTopChanged(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
