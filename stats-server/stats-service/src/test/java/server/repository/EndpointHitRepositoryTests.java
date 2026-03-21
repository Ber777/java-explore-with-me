package server.repository;

import dto.ViewStatsDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import server.model.EndpointHit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import server.service.EndpointHitServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@DataJpaTest
public class EndpointHitRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EndpointHitRepository repository;

    @Mock
    private EndpointHitRepository endpointHitRepository;

    @InjectMocks
    private EndpointHitServiceImpl endpointHitService;

    @Test
    void shouldFindEndpointHitsByUrisNotUniqueShouldReturnCorrectCount() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        String uri1 = "/api/test1";
        String uri2 = "/api/test2";
        String uri3 = "/api/test3"; // URI без хитов

        List<String> uris = List.of(uri1, uri2, uri3);

        // Данные, которые должен вернуть мок репозитория:
        // - uri1: 2 хита
        // - uri2: 1 хит
        // - uri3: нет данных → hits = 0
        List<Object[]> mockResult = Arrays.asList(
                new Object[]{uri1, 2L},
                new Object[]{uri2, 1L}
        );

        // Настраиваем мок репозитория
        when(endpointHitRepository.findEndpointHitsByUrisNotUnique(any(), any(), eq(uris)))
                .thenReturn(mockResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, false);

        assertNotNull(stats, "Результат не должен быть null");
        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате — по одному для каждого URI из списка");

        // Проверяем URI с хитами
        ViewStatsDto statsForUri1 = stats.stream()
                .filter(s -> uri1.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri1, "Должен быть результат для uri1");
        assertEquals("ewm-service", statsForUri1.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(2L, statsForUri1.getHits(), "Для uri1 должно быть 2 хита");

        ViewStatsDto statsForUri2 = stats.stream()
                .filter(s -> uri2.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri2, "Должен быть результат для uri2");
        assertEquals("ewm-service", statsForUri2.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(1L, statsForUri2.getHits(), "Для uri2 должен быть 1 хит");

        // Проверяем URI без хитов — hits должен быть 0
        ViewStatsDto statsForUri3 = stats.stream()
                .filter(s -> uri3.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri3, "Должен быть результат для uri3 (даже если хитов нет)");
        assertEquals("ewm-service", statsForUri3.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(0L, statsForUri3.getHits(), "Для uri3 должно быть 0 хитов (нет записей в БД)");

        // Проверяем сортировку — uri1 (2 хита) должен идти перед uri2 (1 хит), uri3 (0 хитов) — последним
        assertEquals(uri1, stats.get(0).getUri(), "Первый элемент должен быть uri1 с 2 хитами");
        assertEquals(uri2, stats.get(1).getUri(), "Второй элемент должен быть uri2 с 1 хитом");
        assertEquals(uri3, stats.get(2).getUri(), "Третий элемент должен быть uri3 с 0 хитами");

        // Дополнительная проверка: убедимся, что все URI из входного списка присутствуют в результате
        Set<String> resultUris = stats.stream()
                .map(ViewStatsDto::getUri)
                .collect(Collectors.toSet());
        Set<String> expectedUris = new HashSet<>(uris);
        assertEquals(expectedUris, resultUris, "Результат должен содержать все URI из входного списка");
    }

    @Test
    void shouldFindEndpointHitsByUrisAndUniqueIpShouldReturnUniqueCount() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        String uri1 = "/api/unique1";
        String uri2 = "/api/unique2";
        String uri3 = "/api/unique3"; // URI без хитов

        List<String> uris = List.of(uri1, uri2, uri3);

        // Данные, которые должен вернуть мок репозитория:
        // - uri1: 2 уникальных IP (192.168.1.1 и 192.168.1.2)
        // - uri2: 1 уникальный IP (10.0.0.1)
        // - uri3: нет данных → hits = 0
        List<Object[]> mockResult = Arrays.asList(
                new Object[]{uri1, 2L},
                new Object[]{uri2, 1L}
        );

        // Настраиваем мок репозитория
        when(endpointHitRepository.findEndpointsHitByUrisAndUniqueIp(any(), any(), eq(uris)))
                .thenReturn(mockResult);

        List<ViewStatsDto> stats = endpointHitService.getStats(start, end, uris, true);

        assertNotNull(stats, "Результат не должен быть null");
        assertEquals(3, stats.size(), "Должно быть 3 элемента в результате — по одному для каждого URI из списка");

        // Проверяем URI с уникальными хитами
        ViewStatsDto statsForUri1 = stats.stream()
                .filter(s -> uri1.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri1, "Должен быть результат для uri1");
        assertEquals("ewm-service", statsForUri1.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(2L, statsForUri1.getHits(), "Для uri1 должно быть 2 уникальных IP (192.168.1.1 и 192.168.1.2)");

        ViewStatsDto statsForUri2 = stats.stream()
                .filter(s -> uri2.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri2, "Должен быть результат для uri2");
        assertEquals("ewm-service", statsForUri2.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(1L, statsForUri2.getHits(), "Для uri2 должен быть 1 уникальный IP (10.0.0.1)");

        // Проверяем URI без хитов — hits должен быть 0
        ViewStatsDto statsForUri3 = stats.stream()
                .filter(s -> uri3.equals(s.getUri()))
                .findFirst()
                .orElse(null);
        assertNotNull(statsForUri3, "Должен быть результат для uri3 (даже если хитов нет)");
        assertEquals("ewm-service", statsForUri3.getApp(), "Приложение должно быть 'ewm-service'");
        assertEquals(0L, statsForUri3.getHits(), "Для uri3 должно быть 0 уникальных IP (нет записей в БД)");

        // Проверяем сортировку — uri1 (2 уникальных IP) должен идти перед uri2 (1 уникальный IP), uri3 (0) — последним
        assertEquals(uri1, stats.get(0).getUri(), "Первый элемент должен быть uri1 с 2 уникальными IP");
        assertEquals(uri2, stats.get(1).getUri(), "Второй элемент должен быть uri2 с 1 уникальным IP");
        assertEquals(uri3, stats.get(2).getUri(), "Третий элемент должен быть uri3 с 0 уникальными IP");

        // Дополнительная проверка: убедимся, что все URI из входного списка присутствуют в результате
        Set<String> resultUris = stats.stream()
                .map(ViewStatsDto::getUri)
                .collect(Collectors.toSet());
        Set<String> expectedUris = new HashSet<>(uris);
        assertEquals(expectedUris, resultUris, "Результат должен содержать все URI из входного списка");
    }

    @Test
    void shouldFindAllEndpointHitBetweenDatesShouldReturnUrisInRange() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        EndpointHit hit1 = new EndpointHit();
        hit1.setApp("app1");
        hit1.setUri("/api/uri1");
        hit1.setIp("127.0.0.1");
        hit1.setTimestamp(start.plusHours(1));

        EndpointHit hit2 = new EndpointHit();
        hit2.setApp("app2");
        hit2.setUri("/api/uri2");
        hit2.setIp("127.0.0.2");
        hit2.setTimestamp(start.plusHours(2));

        // Вне диапазона — не должен попасть в результат
        EndpointHit hit3 = new EndpointHit();
        hit3.setApp("app3");
        hit3.setUri("/api/old");
        hit3.setIp("127.0.0.3");
        hit3.setTimestamp(start.minusDays(1));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.persist(hit3);
        entityManager.flush();

        List<String> uris = repository.findAllEndpointHitBetweenDates(start, end);

        assertEquals(2, uris.size());
        assertTrue(uris.contains("/api/uri1"));
        assertTrue(uris.contains("/api/uri2"));
        assertFalse(uris.contains("/api/old"));
    }

    @Test
    void shouldFindAllEndpointHitBetweenDatesWithNoDataShouldReturnEmptyList() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);

        List<String> uris = repository.findAllEndpointHitBetweenDates(start, end);

        assertNotNull(uris);
        assertTrue(uris.isEmpty());
    }
}
