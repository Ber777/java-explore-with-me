package server.service;

import dto.*;
import server.exception.*;
import server.model.EndpointHit;
import server.repository.EndpointHitRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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

        when(endpointHitRepository.save(any())).thenAnswer(invocation -> {
            EndpointHit hit = invocation.getArgument(0);
            hit.setId(1L);
            return hit;
        });

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

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/high", 10L},
                new Object[]{"/api/medium", 5L},
                new Object[]{"/api/low", 1L}
        );

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате — по одному для каждого URI");

        ViewStatsDto highStats = stats.getFirst();
        assertEquals("ewm-service", highStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/high", highStats.getUri(), "Первый URI должен быть '/api/high' (10 хитов)");
        assertEquals(10L, highStats.getHits(), "Количество хитов для первого URI должно быть 10");

        ViewStatsDto mediumStats = stats.get(1);
        assertEquals("ewm-service", mediumStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/medium", mediumStats.getUri(), "Второй URI должен быть '/api/medium' (5 хитов)");
        assertEquals(5L, mediumStats.getHits(), "Количество хитов для второго URI должно быть 5");

        ViewStatsDto lowStats = stats.get(2);
        assertEquals("ewm-service", lowStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/low", lowStats.getUri(), "Третий URI должен быть '/api/low' (1 хит)");
        assertEquals(1L, lowStats.getHits(), "Количество хитов для третьего URI должно быть 1");

        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество хитов, чем второй");
        assertTrue(stats.get(1).getHits() >= stats.get(2).getHits(),
                "Второй элемент должен иметь больше или равное количество хитов, чем третий");
    }

    @Test
    void getStatsWithUrisAndUniqueShouldReturnUniqueStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/unique1", "/api/unique2");

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/unique1", 3L},
                new Object[]{"/api/unique2", 1L}
        );

        when(endpointHitRepository.findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, true);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — по одному для каждого URI");

        ViewStatsDto unique1Stats = stats.getFirst();
        assertEquals("ewm-service", unique1Stats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique1", unique1Stats.getUri(), "Первый URI должен быть '/api/unique1' (3 уникальных хитов)");
        assertEquals(3L, unique1Stats.getHits(), "Количество уникальных хитов для первого URI должно быть 3");

        ViewStatsDto unique2Stats = stats.get(1);
        assertEquals("ewm-service", unique2Stats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique2", unique2Stats.getUri(), "Второй URI должен быть '/api/unique2' (1 уникальный хит)");
        assertEquals(1L, unique2Stats.getHits(), "Количество уникальных хитов для второго URI должно быть 1");

        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество уникальных хитов, чем второй");
    }

    @Test
    void getStatsWithNullUrisShouldFetchAllUrisFromRepository() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/uri1", 4L},
                new Object[]{"/api/uri2", 7L},
                new Object[]{"/api/uri3", 1L}
        );

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(
                eq(start), eq(end), isNull()))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, null, false);

        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате");

        // Проверяем сортировку по убыванию хитов: /api/uri2 (7), /api/uri1 (4), /api/uri3 (1)
        assertEquals("ewm-service", stats.getFirst().getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri2", stats.getFirst().getUri(), "Первый URI должен быть '/api/uri2' (7 хитов)");
        assertEquals(7L, stats.getFirst().getHits(), "Количество хитов для первого URI должно быть 7");

        assertEquals("ewm-service", stats.get(1).getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri1", stats.get(1).getUri(), "Второй URI должен быть '/api/uri1' (4 хита)");
        assertEquals(4L, stats.get(1).getHits(), "Количество хитов для второго URI должно быть 4");

        assertEquals("ewm-service", stats.get(2).getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri3", stats.get(2).getUri(), "Третий URI должен быть '/api/uri3' (1 хит)");
        assertEquals(1L, stats.get(2).getHits(), "Количество хитов для третьего URI должно быть 1");

        // Дополнительно проверяем, что метод для уникальных IP не вызывался
        verify(endpointHitRepository, never())
                .findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), any());
    }

    @Test
    void getStatsWithEmptyUrisShouldReturnEmptyList() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<String> emptyUris = new ArrayList<>();
        List<Object[]> hitsResult = new ArrayList<>(); // пустой результат из БД

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(
                any(), any(), isNull()))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, emptyUris, false);

        assertNotNull(stats, "Результат не должен быть null");
        assertTrue(stats.isEmpty(), "Результат должен быть пустым списком, если в БД нет URI за указанный период");

        // Проверяем, что был вызван метод подсчёта хитов с null для uris
        verify(endpointHitRepository, times(1))
                .findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), isNull());

        // Убеждаемся, что метод для уникальных хитов не вызывался
        verify(endpointHitRepository, never())
                .findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), anyList());
    }

    @Test
    void getStatsNoDataForUrisShouldReturnZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/empty");

        List<Object[]> hitsResult = new ArrayList<>();

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате — для переданного URI");

        ViewStatsDto emptyStats = stats.getFirst();
        assertEquals("ewm-service", emptyStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/empty", emptyStats.getUri(), "URI должен быть '/api/empty'");
        assertEquals(0L, emptyStats.getHits(), "Количество хитов должно быть 0, так как данных нет");
    }

    @Test
    void getStatsSingleUriShouldReturnSingleStat() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/single");

        List<Object[]> hitsResult = new ArrayList<>();
        hitsResult.add(new Object[]{"/api/single", 42L});

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате");

        ViewStatsDto singleStats = stats.getFirst();
        assertEquals("ewm-service", singleStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/single", singleStats.getUri(), "URI должен быть '/api/single'");
        assertEquals(42L, singleStats.getHits(), "Количество хитов для URI должно быть 42");
    }

    @Test
    void shouldGetStatsMultipleUrisWithSameHitsShouldMaintainOrder() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/first", "/api/second");

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/first", 5L},
                new Object[]{"/api/second", 5L}
        );

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате");

        ViewStatsDto firstStats = stats.getFirst();
        assertEquals("ewm-service", firstStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/first", firstStats.getUri(), "Первый URI должен быть '/api/first'");
        assertEquals(5L, firstStats.getHits(), "Количество хитов для первого URI должно быть 5");

        ViewStatsDto secondStats = stats.get(1);
        assertEquals("ewm-service", secondStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/second", secondStats.getUri(), "Второй URI должен быть '/api/second'");
        assertEquals(5L, secondStats.getHits(), "Количество хитов для второго URI должно быть 5");

        // Проверка: при одинаковых значениях hits порядок должен сохраняться как в исходном списке
        assertEquals("/api/first", stats.getFirst().getUri(),
                "Первый элемент должен соответствовать первому URI в исходном списке");
        assertEquals("/api/second", stats.get(1).getUri(),
                "Второй элемент должен соответствовать второму URI в исходном списке");
    }

    @Test
    void shouldGetStatsStartAfterEndShouldThrowInvalidException() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 17, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/test");

        InvalidException exception = assertThrows(
                InvalidException.class,
                () -> endpointHitService.getStats(start, end, uris, false),
                "Метод должен выбрасывать InvalidException при start.isAfter(end)"
        );

        // Проверяем сообщение исключения
        assertEquals(
                "Дата начала должна быть меньше даты окончания",
                exception.getMessage(),
                "Сообщение исключения должно быть корректным"
        );

        // Проверяем, что репозиторий не был вызван (операция прервана на ранней стадии валидации)
        verify(endpointHitRepository, never()).findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), any());
        verify(endpointHitRepository, never()).findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), any());
    }

    @Test
    void shouldCreateEndpointHitWithNullFieldsInDtoShouldSaveWithNulls() {
        EndpointHitDto inputDto = EndpointHitDto.builder().app(null)
                .uri("/test")
                .ip(null)
                .timestamp(LocalDateTime.now())
                .build();

        when(endpointHitRepository.save(any())).thenAnswer(invocation -> {
            EndpointHit hit = invocation.getArgument(0);
            hit.setId(1L);
            return hit;
        });

        EndpointHitDto result = endpointHitService.createEndpointHit(inputDto);

        verify(endpointHitRepository, times(1)).save(argThat(hit ->
                hit.getApp() == null &&
                        "/test".equals(hit.getUri()) &&
                        hit.getIp() == null
        ));
        assertEquals(inputDto, result);
    }

    @Test
    void shouldGetStatsWithUrisNotInRepositoryResultShouldIncludeAllUrisWithZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/present", "/api/missing");

        // Репозиторий возвращает данные только для одного URI
        List<Object[]> partialResult = new ArrayList<>();
        partialResult.add(new Object[]{"/api/present", 3L});

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(partialResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — для каждого переданного URI");

        // Проверяем URI, который есть в данных репозитория
        ViewStatsDto presentStats = stats.stream()
                .filter(s -> "/api/present".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(presentStats, "Должен быть результат для '/api/present'");
        assertEquals("ewm-service", presentStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/present", presentStats.getUri(), "URI должен быть '/api/present'");
        assertEquals(3L, presentStats.getHits(), "Количество хитов для '/api/present' должно быть 3");

        // Проверяем URI, которого нет в данных репозитория — должен быть с 0 хитов
        ViewStatsDto missingStats = stats.stream()
                .filter(s -> "/api/missing".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(missingStats, "Должен быть результат для '/api/missing'");
        assertEquals("ewm-service", missingStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/missing", missingStats.getUri(), "URI должен быть '/api/missing'");
        assertEquals(0L, missingStats.getHits(), "Количество хитов для отсутствующего URI должно быть 0");
    }

    @Test
    void shouldGetStatsWhenRepositoryReturnsNullShouldHandleGracefully() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/test");

        // Симулируем случай, когда репозиторий возвращает null
        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(null);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertNotNull(stats, "Результат не должен быть null");
        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате — для переданного URI");

        ViewStatsDto testStats = stats.getFirst();
        assertEquals("ewm-service", testStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/test", testStats.getUri(), "URI должен быть '/api/test'");
        assertEquals(0L, testStats.getHits(), "Количество хитов должно быть 0 при null от репозитория");

        // Дополнительная проверка: убедимся, что сервис корректно обработал null и не выбросил исключение
        verify(endpointHitRepository, times(1)).findEndpointHitsByUrisNotUniqueBetweenDates(
                eq(start), eq(end), eq(uris)
        );
    }

    @Test
    void shouldGetStatsWithUniqueHitsWhenRepositoryReturnsNullShouldHandleGracefully() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/unique-test");

        // Симулируем null для уникальных хитов
        when(endpointHitRepository.findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), eq(uris)))
                .thenReturn(null);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, true);

        assertNotNull(stats, "Результат не должен быть null");
        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате — для переданного URI");

        ViewStatsDto testStats = stats.getFirst();
        assertEquals("ewm-service", testStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique-test", testStats.getUri(), "URI должен быть '/api/unique-test'");
        assertEquals(0L, testStats.getHits(), "Количество уникальных хитов должно быть 0 при null от репозитория");

        // Проверка вызова правильного метода репозитория
        verify(endpointHitRepository, times(1)).findEndpointsHitByUrisAndUniqueIpBetweenDates(
                eq(start), eq(end), eq(uris)
        );
    }

    @Test
    void shouldGetStatsWithMultipleCallsToRepositoryShouldAggregateCorrectly() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/group1", "/api/group2");

        // Первый вызов для не уникальных хитов
        List<Object[]> nonUniqueHits = Arrays.asList(
                new Object[]{"/api/group1", 10L},
                new Object[]{"/api/group2", 5L}
        );

        // Второй вызов для уникальных хитов (если unique = true)
        List<Object[]> uniqueHits = Arrays.asList(
                new Object[]{"/api/group1", 7L},
                new Object[]{"/api/group2", 3L}
        );

        when(endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(any(), any(), eq(uris)))
                .thenReturn(nonUniqueHits);
        when(endpointHitRepository.findEndpointsHitByUrisAndUniqueIpBetweenDates(any(), any(), eq(uris)))
                .thenReturn(uniqueHits);

        // Тест для не уникальных хитов
        List<ViewStatsDto> nonUniqueStats = endpointHitService.getStats(start, end, uris, false);
        assertEquals(2, nonUniqueStats.size());
        assertEquals(10L, nonUniqueStats.getFirst().getHits());

        // Тест для уникальных хитов
        List<ViewStatsDto> uniqueStats = endpointHitService.getStats(start, end, uris, true);
        assertEquals(2, uniqueStats.size());
        assertEquals(7L, uniqueStats.getFirst().getHits());
    }
}
