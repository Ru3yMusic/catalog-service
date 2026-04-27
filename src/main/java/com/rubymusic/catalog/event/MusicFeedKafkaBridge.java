package com.rubymusic.catalog.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges Spring application events → Kafka. Listens with
 * {@link TransactionPhase#AFTER_COMMIT} so a rolled-back DB transaction
 * never produces a downstream realtime broadcast.
 *
 * <p>Failures here are logged and swallowed: the source of truth is the DB
 * (which is already committed by the time we get here). A missed feed event
 * just means the user sees the change on next page load instead of in
 * realtime — acceptable for a UI-feed contract.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicFeedKafkaBridge {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onAlbumReleased(AlbumReleasedEvent event) {
        send(MusicFeedTopics.ALBUM_RELEASED, event.albumId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onArtistTopChanged(ArtistTopChangedEvent event) {
        send(MusicFeedTopics.ARTIST_TOP_CHANGED, event.artistId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Kafka publish [{}] key={} payload={}", topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize music-feed event for topic {}: {}", topic, payload, e);
        } catch (Exception e) {
            log.error("Failed to publish music-feed event to {} (key={}): {}", topic, key, e.getMessage(), e);
        }
    }
}
