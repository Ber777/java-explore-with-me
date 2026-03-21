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
import java.util.*;
import java.util.stream.Collectors;

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

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/events", 5L},
                new Object[]{"/api/users", 3L}
        );

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — по одному для каждого URI");

        // Проверяем первый элемент
        ViewStatsDto firstStats = stats.getFirst();
        assertEquals("ewm-service", firstStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/events", firstStats.getUri(), "URI должен быть '/api/events'");
        assertEquals(5L, firstStats.getHits(), "Количество хитов должно быть 5");

        // Проверяем второй элемент
        ViewStatsDto secondStats = stats.get(1);
        assertEquals("ewm-service", secondStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/users", secondStats.getUri(), "URI должен быть '/api/users'");
        assertEquals(3L, secondStats.getHits(), "Количество хитов должно быть 3");

        // Дополнительная проверка: сортировка по убыванию хитов
        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Результаты должны быть отсортированы по убыванию количества хитов");
    }

    @Test
    void shouldGetStatsUniqueHitsShouldReturnUniqueStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/unique");

        // Возвращаем List<Object[]> для уникальных хитов
        List<Object[]> hitsResult = new ArrayList<>();
        hitsResult.add(new Object[]{"/api/unique", 2L});

        when(repository.findEndpointsHitByUrisAndUniqueIp(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, true);

        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате — для переданного URI");

        ViewStatsDto uniqueStats = stats.getFirst();
        assertEquals("ewm-service", uniqueStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique", uniqueStats.getUri(), "URI должен быть '/api/unique'");
        assertEquals(2L, uniqueStats.getHits(), "Количество уникальных хитов должно быть 2");

        assertEquals(stats.stream()
                .sorted(Comparator.comparingLong(x -> -x.getHits()))
                .toList(), stats, "Результаты должны быть отсортированы по убыванию количества хитов");
    }

    @Test
    void shouldGetStatsWithNoUrisShouldReturnAllUrisStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<String> allUris = Arrays.asList("/api/uri1", "/api/uri2", "/api/uri3");

        when(repository.findAllEndpointHitBetweenDates(eq(start), eq(end)))
                .thenReturn(allUris);

        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/uri1", 4L},
                new Object[]{"/api/uri2", 7L},
                new Object[]{"/api/uri3", 1L}
        );

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(allUris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = service.getStats(start, end, null, false);

        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате — по одному для каждого URI из БД");

        // Проверяем первый элемент (должен быть с наибольшим количеством хитов)
        ViewStatsDto firstStats = stats.getFirst();
        assertEquals("ewm-service", firstStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri2", firstStats.getUri(), "Первый URI должен быть '/api/uri2' (7 хитов)");
        assertEquals(7L, firstStats.getHits(), "Количество хитов для первого URI должно быть 7");

        // Проверяем второй элемент
        ViewStatsDto secondStats = stats.get(1);
        assertEquals("ewm-service", secondStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri1", secondStats.getUri(), "Второй URI должен быть '/api/uri1' (4 хита)");
        assertEquals(4L, secondStats.getHits(), "Количество хитов для второго URI должно быть 4");

        // Проверяем третий элемент (должен быть с наименьшим количеством хитов)
        ViewStatsDto thirdStats = stats.get(2);
        assertEquals("ewm-service", thirdStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri3", thirdStats.getUri(), "Третий URI должен быть '/api/uri3' (1 хит)");
        assertEquals(1L, thirdStats.getHits(), "Количество хитов для третьего URI должно быть 1");

        // Проверка сортировки
        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество хитов, чем второй");
        assertTrue(stats.get(1).getHits() >= stats.get(2).getHits(),
                "Второй элемент должен иметь больше или равное количество хитов, чем третий");
    }

    @Test
    void shouldGetStatsEmptyUrisListShouldReturnEmptyStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> emptyUris = new ArrayList<>();

        // Когда список URI пуст, метод findAllEndpointHitBetweenDates возвращает пустой список
        when(repository.findAllEndpointHitBetweenDates(any(), any()))
                .thenReturn(emptyUris);

        List<ViewStatsDto> stats = service.getStats(start, end, emptyUris, false);

        assertNotNull(stats);
        assertTrue(stats.isEmpty(), "Для пустого списка URI должен возвращаться пустой результат");
    }

    @Test
    void shouldGetStatsNoDataForUrisShouldReturnZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = List.of("/api/empty");

        // Для URI без данных метод возвращает пустой список Object[]
        List<Object[]> emptyResult = new ArrayList<>();

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(emptyResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(1, stats.size(), "Должно быть 1 элемент в результате — для переданного URI");

        ViewStatsDto emptyStats = stats.getFirst();
        assertEquals("ewm-service", emptyStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/empty", emptyStats.getUri(), "URI должен быть '/api/empty'");
        assertEquals(0L, emptyStats.getHits(), "Количество хитов должно быть 0, так как данных нет");
    }

    @Test
    void shouldGetStatsMultipleUrisWithMixedResultsShouldReturnCorrectStats() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/high", "/api/medium", "/api/low");

        // Используем List<Object[]> для смешанного результата
        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/high", 10L},   // 10 хитов для первого URI
                new Object[]{"/api/medium", 5L}, // 5 хитов для второго URI
                new Object[]{"/api/low", 1L}     // 1 хит для третьего URI
        );

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате — по одному для каждого URI");

        // Проверяем первый элемент (должен быть с наибольшим количеством хитов)
        ViewStatsDto highStats = stats.getFirst();
        assertEquals("ewm-service", highStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/high", highStats.getUri(), "Первый URI должен быть '/api/high' (10 хитов)");
        assertEquals(10L, highStats.getHits(), "Количество хитов для первого URI должно быть 10");

        // Проверяем второй элемент
        ViewStatsDto mediumStats = stats.get(1);
        assertEquals("ewm-service", mediumStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/medium", mediumStats.getUri(), "Второй URI должен быть '/api/medium' (5 хитов)");
        assertEquals(5L, mediumStats.getHits(), "Количество хитов для второго URI должно быть 5");

        // Проверяем третий элемент (должен быть с наименьшим количеством хитов)
        ViewStatsDto lowStats = stats.get(2);
        assertEquals("ewm-service", lowStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/low", lowStats.getUri(), "Третий URI должен быть '/api/low' (1 хит)");
        assertEquals(1L, lowStats.getHits(), "Количество хитов для третьего URI должно быть 1");

        // Проверка сортировки по убыванию хитов
        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество хитов, чем второй");
        assertTrue(stats.get(1).getHits() >= stats.get(2).getHits(),
                "Второй элемент должен иметь больше или равное количество хитов, чем третий");

        // Проверка полного соответствия ожидаемому порядку
        List<Long> hitsOrder = stats.stream()
                .map(ViewStatsDto::getHits)
                .collect(Collectors.toList());
        List<Long> expectedOrder = List.of(10L, 5L, 1L);
        assertEquals(expectedOrder, hitsOrder, "Результаты должны быть отсортированы по убыванию количества хитов");
    }

    @Test
    void shouldGetStatsUniqueHitsWithMultipleUrisShouldReturnUniqueCounts() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/unique1", "/api/unique2");

        // Возвращаем List<Object[]> для уникальных хитов с несколькими URI
        List<Object[]> hitsResult = Arrays.asList(
                new Object[]{"/api/unique1", 3L}, // 3 уникальных IP для первого URI
                new Object[]{"/api/unique2", 1L}  // 1 уникальный IP для второго URI
        );

        when(repository.findEndpointsHitByUrisAndUniqueIp(any(), any(), eq(uris)))
                .thenReturn(hitsResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, true);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — по одному для каждого URI");

        // Проверяем первый элемент (должен быть с наибольшим количеством уникальных хитов)
        ViewStatsDto unique1Stats = stats.getFirst();
        assertEquals("ewm-service", unique1Stats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique1", unique1Stats.getUri(), "Первый URI должен быть '/api/unique1' (3 уникальных хитов)");
        assertEquals(3L, unique1Stats.getHits(), "Количество уникальных хитов для первого URI должно быть 3");

        // Проверяем второй элемент
        ViewStatsDto unique2Stats = stats.get(1);
        assertEquals("ewm-service", unique2Stats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/unique2", unique2Stats.getUri(), "Второй URI должен быть '/api/unique2' (1 уникальный хит)");
        assertEquals(1L, unique2Stats.getHits(), "Количество уникальных хитов для второго URI должно быть 1");

        // Проверка сортировки по убыванию уникальных хитов
        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество уникальных хитов, чем второй");

        // Проверка полного соответствия ожидаемому порядку
        List<Long> hitsOrder = stats.stream()
                .map(ViewStatsDto::getHits)
                .collect(Collectors.toList());
        List<Long> expectedOrder = List.of(3L, 1L);
        assertEquals(expectedOrder, hitsOrder, "Результаты должны быть отсортированы по убыванию количества уникальных хитов");
    }

    @Test
    void shouldGetStatsWithEmptyResultFromRepositoryShouldReturnZeroHitsForAllUris() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/uri1", "/api/uri2");

        // Репозиторий возвращает пустой список — значит, нет данных для этих URI
        List<Object[]> emptyResult = new ArrayList<>();

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(emptyResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — для каждого переданного URI");

        // Проверяем первый URI
        ViewStatsDto firstStats = stats.getFirst();
        assertEquals("ewm-service", firstStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri1", firstStats.getUri(), "Первый URI должен быть '/api/uri1'");
        assertEquals(0L, firstStats.getHits(), "Количество хитов должно быть 0 для URI без данных");

        // Проверяем второй URI
        ViewStatsDto secondStats = stats.get(1);
        assertEquals("ewm-service", secondStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals("/api/uri2", secondStats.getUri(), "Второй URI должен быть '/api/uri2'");
        assertEquals(0L, secondStats.getHits(), "Количество хитов должно быть 0 для URI без данных");


        // Проверка сортировки (оба имеют 0 хитов — порядок может быть любым)
        assertTrue(stats.getFirst().getHits() >= stats.get(1).getHits(),
                "Первый элемент должен иметь больше или равное количество хитов, чем второй (оба 0)");
    }

    @Test
    void shouldGetStatsWithUrisNotInRepositoryResultShouldIncludeAllUrisWithZeroHits() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/present", "/api/missing");

        // Репозиторий возвращает данные только для одного URI
        List<Object[]> partialResult = new ArrayList<>();
        partialResult.add(new Object[]{"/api/present", 3L});

        when(repository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(partialResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, false);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — для каждого переданного URI");

        // Проверяем URI, который есть в данных репозитория
        ViewStatsDto presentStats = stats.stream()
                .filter(s -> "/api/present".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(presentStats, "Должен быть результат для URI '/api/present'");
        assertEquals("ewm-service", presentStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(3L, presentStats.getHits(), "Количество хитов для '/api/present' должно быть 3");

        // Проверяем URI, которого нет в данных репозитория
        ViewStatsDto missingStats = stats.stream()
                .filter(s -> "/api/missing".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(missingStats, "Должен быть результат для URI '/api/missing'");
        assertEquals("ewm-service", missingStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(0L, missingStats.getHits(), "Количество хитов для отсутствующего URI должно быть 0");

        // Проверка сортировки: URI с хитами должен идти перед URI без хитов
        assertEquals("/api/present", stats.getFirst().getUri(),
                "URI с хитами ('/api/present') должен быть первым в отсортированном списке");
        assertEquals("/api/missing", stats.get(1).getUri(),
                "URI без хитов ('/api/missing') должен быть вторым в отсортированном списке");
    }

    @Test
    void shouldGetStatsUniqueHitsWithUrisNotInRepositoryShouldIncludeAllUris() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        List<String> uris = Arrays.asList("/api/unique-present", "/api/unique-missing");

        // Репозиторий возвращает данные только для одного URI (уникальные IP)
        List<Object[]> uniqueResult = new ArrayList<>();
        uniqueResult.add(new Object[]{"/api/unique-present", 2L});

        when(repository.findEndpointsHitByUrisAndUniqueIp(any(), any(), eq(uris)))
                .thenReturn(uniqueResult);

        List<ViewStatsDto> stats = service.getStats(start, end, uris, true);

        assertEquals(2, stats.size(), "Должно быть 2 элемента в результате — для каждого переданного URI");

        // Проверяем URI с уникальными хитами
        ViewStatsDto uniquePresentStats = stats.stream()
                .filter(s -> "/api/unique-present".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(uniquePresentStats, "Должен быть результат для URI '/api/unique-present'");
        assertEquals("ewm-service", uniquePresentStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(2L, uniquePresentStats.getHits(), "Количество уникальных хитов для '/api/unique-present' должно быть 2");

        // Проверяем отсутствующий URI
        ViewStatsDto uniqueMissingStats = stats.stream()
                .filter(s -> "/api/unique-missing".equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(uniqueMissingStats, "Должен быть результат для URI '/api/unique-missing'");
        assertEquals("ewm-service", uniqueMissingStats.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(0L, uniqueMissingStats.getHits(), "Количество уникальных хитов для отсутствующего URI должно быть 0");

        // Проверка сортировки
        assertEquals("/api/unique-present", stats.getFirst().getUri(),
                "URI с уникальными хитами должен быть первым в отсортированном списке");
        assertEquals("/api/unique-missing", stats.get(1).getUri(),
                "URI без уникальных хитов должен быть вторым в отсортированном списке");
    }
}
