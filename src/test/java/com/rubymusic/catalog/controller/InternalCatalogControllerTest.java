package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.config.JwtAuthenticationFilter;
import com.rubymusic.catalog.config.JwtTokenProvider;
import com.rubymusic.catalog.config.SecurityConfig;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.service.ArtistService;
import com.rubymusic.catalog.service.SongService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link InternalCatalogController} with full security chain.
 *
 * <p>Verifies the security contract:
 * <ul>
 *   <li>Services JWT (ROLE_SERVICE) → 200 on all internal endpoints</li>
 *   <li>User JWT (ROLE_USER)       → 403 Forbidden on internal endpoints</li>
 *   <li>No JWT                     → 401 Unauthorized on internal endpoints</li>
 * </ul>
 *
 * TDD RED: tests written before InternalCatalogController existed
 * TDD GREEN: controller implemented, security chain wired → all pass.
 */
@WebMvcTest(InternalCatalogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
@DisplayName("InternalCatalogController security + behavior")
class InternalCatalogControllerTest {

    // ── Static RSA key pair shared across all test instances ──────────────────

    static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KEY_PAIR = gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA key pair", e);
        }
    }

    /** Provides the test PublicKey — overrides JwtConfig via @ConditionalOnMissingBean */
    @TestConfiguration
    static class SecurityTestConfig {
        @Bean
        public PublicKey testPublicKey() {
            return KEY_PAIR.getPublic();
        }
    }

    // ── MockMvc + mock services ────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SongService songService;

    @MockBean
    private ArtistService artistService;

    // ── Test tokens ────────────────────────────────────────────────────────────

    private String serviceToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 60_000);

        serviceToken = Jwts.builder()
                .subject("realtime-service")
                .claim("role", "SERVICE")
                .issuedAt(now)
                .expiration(exp)
                .signWith(KEY_PAIR.getPrivate())
                .compact();

        userToken = Jwts.builder()
                .subject("user-abc-123")
                .claim("role", "USER")
                .issuedAt(now)
                .expiration(exp)
                .signWith(KEY_PAIR.getPrivate())
                .compact();
    }

    // ── GET /api/internal/v1/songs/{id} ───────────────────────────────────────

    @Test
    @DisplayName("GET /api/internal/v1/songs/{id} — service JWT → 200 with SongInternalDto")
    void getInternalSongById_withServiceJwt_returns200() throws Exception {
        UUID songId = UUID.randomUUID();
        Song song = buildTestSong(songId);
        when(songService.findById(songId)).thenReturn(song);

        mockMvc.perform(get("/api/internal/v1/songs/{id}", songId)
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(songId.toString()))
                .andExpect(jsonPath("$.title").value("Test Song"))
                .andExpect(jsonPath("$.artistName").value("Test Artist"))
                .andExpect(jsonPath("$.coverUrl").value("https://cdn.example.com/cover.jpg"))
                .andExpect(jsonPath("$.audioUrl").value("https://cdn.example.com/audio.mp3"))
                .andExpect(jsonPath("$.durationMs").value(180_000L));
    }

    @Test
    @DisplayName("GET /api/internal/v1/songs/{id} — user JWT → 403 Forbidden")
    void getInternalSongById_withUserJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/internal/v1/songs/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/v1/songs/{id} — no JWT → 401 Unauthorized")
    void getInternalSongById_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/v1/songs/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/internal/v1/songs/batch ─────────────────────────────────────

    @Test
    @DisplayName("GET /api/internal/v1/songs/batch — service JWT → 200 with list")
    void getInternalSongsBatch_withServiceJwt_returns200() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(songService.findByIds(any())).thenReturn(List.of(buildTestSong(id1), buildTestSong(id2)));

        mockMvc.perform(get("/api/internal/v1/songs/batch")
                        .param("ids", id1 + "," + id2)
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/internal/v1/songs/batch — empty ids string → 200 empty list")
    void getInternalSongsBatch_emptyIds_returns200EmptyList() throws Exception {
        when(songService.findByIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/internal/v1/songs/batch")
                        .param("ids", "")
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/internal/v1/songs/batch — user JWT → 403 Forbidden")
    void getInternalSongsBatch_withUserJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/internal/v1/songs/batch")
                        .param("ids", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/v1/songs/batch — no JWT → 401 Unauthorized")
    void getInternalSongsBatch_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/v1/songs/batch")
                        .param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/internal/v1/artists/{id} ────────────────────────────────────

    @Test
    @DisplayName("GET /api/internal/v1/artists/{id} — service JWT → 200 with ArtistInternalDto")
    void getInternalArtistById_withServiceJwt_returns200() throws Exception {
        UUID artistId = UUID.randomUUID();
        Artist artist = buildTestArtist(artistId);
        when(artistService.findById(eq(artistId))).thenReturn(artist);

        mockMvc.perform(get("/api/internal/v1/artists/{id}", artistId)
                        .header("Authorization", "Bearer " + serviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artistId.toString()))
                .andExpect(jsonPath("$.name").value("Test Artist"))
                .andExpect(jsonPath("$.photoUrl").value("https://cdn.example.com/photo.jpg"))
                .andExpect(jsonPath("$.monthlyListeners").value(5000));
    }

    @Test
    @DisplayName("GET /api/internal/v1/artists/{id} — user JWT → 403 Forbidden")
    void getInternalArtistById_withUserJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/internal/v1/artists/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/v1/artists/{id} — no JWT → 401 Unauthorized")
    void getInternalArtistById_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/v1/artists/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Song buildTestSong(UUID id) {
        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name("Test Artist")
                .monthlyListeners(1000L)
                .followersCount(500L)
                .isTop(false)
                .build();
        return Song.builder()
                .id(id)
                .title("Test Song")
                .artist(artist)
                .coverUrl("https://cdn.example.com/cover.jpg")
                .audioUrl("https://cdn.example.com/audio.mp3")
                .duration(180)
                .playCount(0L)
                .likesCount(0L)
                .build();
    }

    private Artist buildTestArtist(UUID id) {
        return Artist.builder()
                .id(id)
                .name("Test Artist")
                .photoUrl("https://cdn.example.com/photo.jpg")
                .bio("Test bio")
                .monthlyListeners(5000L)
                .followersCount(2000L)
                .isTop(true)
                .build();
    }
}
