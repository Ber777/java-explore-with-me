package server.repository;

import server.model.EndpointHit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class EndpointHitRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EndpointHitRepository repository;

    @Test
    void shouldFindEndpointHitByUriNotUniqueShouldReturnCorrectCount() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        String uri = "/api/test";

        EndpointHit hit1 = new EndpointHit();
        hit1.setApp("app1");
        hit1.setUri(uri);
        hit1.setIp("127.0.0.1");
        hit1.setTimestamp(start.plusHours(1));

        EndpointHit hit2 = new EndpointHit();
        hit2.setApp("app2");
        hit2.setUri(uri);
        hit2.setIp("127.0.0.2");
        hit2.setTimestamp(start.plusHours(2));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.flush();

        long count = repository.findEndpointHitByUriNotUnique(start, end, uri);

        assertEquals(2, count);
    }

    @Test
    void shouldFindEndpointHitByUriAndUniqueIpShouldReturnUniqueCount() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 16, 0, 0);
        String uri = "/api/unique";

        EndpointHit hit1 = new EndpointHit();
        hit1.setApp("app1");
        hit1.setUri(uri);
        hit1.setIp("192.168.1.1");
        hit1.setTimestamp(start.plusHours(1));

        EndpointHit hit2 = new EndpointHit();
        hit2.setApp("app2");
        hit2.setUri(uri);
        hit2.setIp("192.168.1.2");
        hit2.setTimestamp(start.plusHours(2));

        // Тот же IP — должен учитываться как один уникальный
        EndpointHit hit3 = new EndpointHit();
        hit3.setApp("app3");
        hit3.setUri(uri);
        hit3.setIp("192.168.1.1");
        hit3.setTimestamp(start.plusHours(3));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.persist(hit3);
        entityManager.flush();

        long uniqueCount = repository.findEndpointHitByUriAndUniqueIp(start, end, uri);

        assertEquals(2, uniqueCount); // 2 уникальных IP: 192.168.1.1 и 192.168.1.2
    }

    @Test
    void shouldFindAllEndpointHitBetweenDatesShouldReturnUrisInRange() {
        // Given
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
