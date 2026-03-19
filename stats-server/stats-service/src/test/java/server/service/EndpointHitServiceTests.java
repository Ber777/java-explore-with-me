package server.service;

import dto.EndpointHitDto;
import dto.ViewStatsDto;
import server.model.EndpointHit;
import server.repository.EndpointHitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EndpointHitServiceTests {
    @Mock
    private EndpointHitRepository repository;

    @InjectMocks
    private EndpointHitServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateEndpointHitWithValidDataShouldCallRepositorySave() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHitDto inputDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        // Мокаем сохранение: возвращаем тот же объект с ID
        when(repository.save(any(EndpointHit.class))).thenAnswer(invocation -> {
            EndpointHit hit = invocation.getArgument(0);
            hit.setId(1L);
            return hit;
        });

        EndpointHitDto result = service.createEndpointHit(inputDto);

        verify(repository, times(1)).save(argThat(hit ->
                "test-app".equals(hit.getApp()) &&
                        "/test".equals(hit.getUri()) &&
                        "127.0.0.1".equals(hit.getIp()) &&
                        timestamp.equals(hit.getTimestamp())
        ));
        assertNotNull(result);
        assertEquals("test-app", result.getApp());
        assertEquals("/test", result.getUri());
        assertEquals("127.0.0.1", result.getIp());
        assertEquals(timestamp, result.getTimestamp());
    }

    @Test
    void shouldGetStatsNonUniqueHitsShouldReturnStatsList() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/events", "/api/users");

        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/events")))
                .thenReturn(5L);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/users")))
                .thenReturn(3L);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(2, stats.size());
        assertEquals("ewm-main-service", stats.getFirst().getApp());
        assertEquals("/api/events", stats.get(0).getUri());
        assertEquals(5L, stats.get(0).getHits());
        assertEquals("/api/users", stats.get(1).getUri());
        assertEquals(3L, stats.get(1).getHits());
    }

    @Test
    void shouldGetStatsUniqueHitsShouldReturnUniqueStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/unique");

        when(repository.findEndpointHitByUriAndUniqueIp(any(), any(), eq("/api/unique")))
                .thenReturn(2L);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, true);

        assertEquals(1, stats.size());
        assertEquals("/api/unique", stats.getFirst().getUri());
        assertEquals(2L, stats.getFirst().getHits());
    }

    @Test
    void shouldGetStatsWithNoUrisShouldReturnAllUrisStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<String> allUris = Arrays.asList("/api/uri1", "/api/uri2", "/api/uri3");
        when(repository.findAllEndpointHitBetweenDates(start, end))
                .thenReturn(allUris);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri1")))
                .thenReturn(4L);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri2")))
                .thenReturn(7L);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri3")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = service.getStats(start, end, null, false);

        assertEquals(3, stats.size());
        assertEquals("/api/uri2", stats.get(0).getUri()); // 7 хитов
        assertEquals("/api/uri1", stats.get(1).getUri()); // 4 хита
        assertEquals("/api/uri3", stats.get(2).getUri()); // 1 хит
    }

    @Test
    void shouldGetStatsEmptyUrisListShouldReturnEmptyStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> emptyUris = new ArrayList<>();

        List<ViewStatsDto> stats = service.getStats(start, end, emptyUris, false);

        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }

    @Test
    void shouldGetStatsNoDataForUrisShouldReturnZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/empty");

        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/empty")))
                .thenReturn(0L);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(1, stats.size());
        assertEquals("/api/empty", stats.getFirst().getUri());
        assertEquals(0L, stats.getFirst().getHits());
    }

    @Test
    void shouldGetStatsMultipleUrisWithMixedResultsShouldReturnCorrectStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/high", "/api/medium", "/api/low");

        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/high")))
                .thenReturn(10L);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/medium")))
                .thenReturn(5L);
        when(repository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/low")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(3, stats.size());
        assertEquals("/api/high", stats.get(0).getUri());   // 10 хитов
        assertEquals(10L, stats.get(0).getHits());
        assertEquals("/api/medium", stats.get(1).getUri());  // 5 хитов
        assertEquals(5L, stats.get(1).getHits());
        assertEquals("/api/low", stats.get(2).getUri());     // 1 хит
        assertEquals(1L, stats.get(2).getHits());
    }

    @Test
    void shouldGetStatsUniqueHitsWithMultipleUrisShouldReturnUniqueCounts() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/unique1", "/api/unique2");

        when(repository.findEndpointHitByUriAndUniqueIp(any(), any(), eq("/api/unique1")))
                .thenReturn(3L);
        when(repository.findEndpointHitByUriAndUniqueIp(any(), any(), eq("/api/unique2")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, true);

        assertEquals(2, stats.size());
        assertEquals("/api/unique1", stats.get(0).getUri());
        assertEquals(3L, stats.get(0).getHits());
        assertEquals("/api/unique2", stats.get(1).getUri());
        assertEquals(1L, stats.get(1).getHits());
    }
}
