package server.service;

import dto.EndpointHitDto;
import dto.ViewStatsDto;
import server.repository.EndpointHitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EndpointHitServiceImplTests {
    @Mock
    private EndpointHitRepository endpointHitRepository;

    @InjectMocks
    private EndpointHitServiceImpl endpointHitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createEndpointHitShouldSaveAndReturnDto() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHitDto inputDto = EndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHitDto result = endpointHitService.createEndpointHit(inputDto);

        verify(endpointHitRepository, times(1)).save(argThat(hit ->
                "test-app".equals(hit.getApp()) &&
                        "/test".equals(hit.getUri()) &&
                        "127.0.0.1".equals(hit.getIp()) &&
                        timestamp.equals(hit.getTimestamp())
        ));
        assertEquals(inputDto, result);
    }

    @Test
    void getStatsWithUrisAndNonUniqueShouldReturnSortedStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/high", "/api/medium", "/api/low");

        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/high")))
                .thenReturn(10L);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/medium")))
                .thenReturn(5L);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/low")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(3, stats.size());
        assertEquals("/api/high", stats.get(0).getUri());   // 10 хитов (первый по сортировке)
        assertEquals(10L, stats.get(0).getHits());
        assertEquals("/api/medium", stats.get(1).getUri());  // 5 хитов
        assertEquals(5L, stats.get(1).getHits());
        assertEquals("/api/low", stats.get(2).getUri());     // 1 хит
        assertEquals(1L, stats.get(2).getHits());
    }

    @Test
    void getStats_WithUrisAndUniqueShouldReturnUniqueStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/unique1", "/api/unique2");

        when(endpointHitRepository.findEndpointHitByUriAndUniqueIp(any(), any(), eq("/api/unique1")))
                .thenReturn(3L);
        when(endpointHitRepository.findEndpointHitByUriAndUniqueIp(any(), any(), eq("/api/unique2")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, true);

        assertEquals(2, stats.size());
        assertEquals("/api/unique1", stats.get(0).getUri());
        assertEquals(3L, stats.get(0).getHits());
        assertEquals("/api/unique2", stats.get(1).getUri());
        assertEquals(1L, stats.get(1).getHits());
    }

    @Test
    void getStats_WithNullUrisShouldFetchAllUrisFromRepository() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<String> allUris = Arrays.asList("/api/uri1", "/api/uri2", "/api/uri3");
        when(endpointHitRepository.findAllEndpointHitBetweenDates(start, end))
                .thenReturn(allUris);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri1")))
                .thenReturn(4L);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri2")))
                .thenReturn(7L);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/uri3")))
                .thenReturn(1L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, null, false);

        assertEquals(3, stats.size());
        assertEquals("/api/uri2", stats.get(0).getUri()); // 7 хитов — первый по сортировке
        assertEquals("/api/uri1", stats.get(1).getUri()); // 4 хита
        assertEquals("/api/uri3", stats.get(2).getUri()); // 1 хит
    }

    @Test
    void getStats_WithEmptyUrisShouldReturnEmptyList() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> emptyUris = new ArrayList<>();

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, emptyUris, false);

        assertNotNull(stats);
        assertTrue(stats.isEmpty());
        verify(endpointHitRepository, never()).findEndpointHitByUriNotUnique(any(), any(), anyString());
    }

    @Test
    void getStats_NoDataForUrisShouldReturnZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/empty");

        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/empty")))
                .thenReturn(0L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(1, stats.size());
        assertEquals("/api/empty", stats.getFirst().getUri());
        assertEquals(0L, stats.getFirst().getHits());
    }

    @Test
    void getStats_SingleUriShouldReturnSingleStat() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/single");

        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/single")))
                .thenReturn(42L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(1, stats.size());
        assertEquals("/api/single", stats.getFirst().getUri());
        assertEquals(42L, stats.getFirst().getHits());
        assertEquals("ewm-main-service", stats.getFirst().getApp());
    }

    @Test
    void shouldGetStatsMultipleUrisWithSameHitsShouldMaintainOrder() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/first", "/api/second");

        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/first")))
                .thenReturn(5L);
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), eq("/api/second")))
                .thenReturn(5L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        // при одинаковых значениях hits порядок должен сохраняться как в исходном списке
        assertEquals(2, stats.size());
        assertEquals("/api/first", stats.get(0).getUri());   // первый в списке
        assertEquals(5L, stats.get(0).getHits());
        assertEquals("/api/second", stats.get(1).getUri());  // второй в списке
        assertEquals(5L, stats.get(1).getHits());
    }

    @Test
    void shouldGetStatsStartAfterEndShouldHandleGracefully() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 17, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/test");

        // Repository должен вернуть 0 хитов для такого интервала
        when(endpointHitRepository.findEndpointHitByUriNotUnique(any(), any(), anyString()))
                .thenReturn(0L);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(1, stats.size());
        assertEquals("/api/test", stats.getFirst().getUri());
        assertEquals(0L, stats.getFirst().getHits());
    }

    @Test
    void shouldCreateEndpointHitWithNullFieldsInDtoShouldSaveWithNulls() {
        EndpointHitDto inputDto = EndpointHitDto.builder()
                .app(null)
                .uri("/test")
                .ip(null)
                .timestamp(LocalDateTime.now())
                .build();

        EndpointHitDto result = endpointHitService.createEndpointHit(inputDto);

        verify(endpointHitRepository, times(1)).save(argThat(hit ->
                hit.getApp() == null &&
                        "/test".equals(hit.getUri()) &&
                        hit.getIp() == null
        ));
        assertEquals(inputDto, result);
    }
}
