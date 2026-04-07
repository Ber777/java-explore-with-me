package server.model;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointHitModelTests {

    @Test
    void shouldTestNoArgsConstructor() {
        EndpointHit endpointHit = new EndpointHit();

        assertNull(endpointHit.getId());
        assertNull(endpointHit.getApp());
        assertNull(endpointHit.getUri());
        assertNull(endpointHit.getIp());
        assertNull(endpointHit.getTimestamp());
    }

    @Test
    void shouldTestAllArgsConstructor() {
        Long id = 1L;
        String app = "test-app";
        String uri = "/test";
        String ip = "127.0.0.1";
        LocalDateTime timestamp = LocalDateTime.now();

        EndpointHit endpointHit = new EndpointHit(id, app, uri, ip, timestamp);

        assertEquals(id, endpointHit.getId());
        assertEquals(app, endpointHit.getApp());
        assertEquals(uri, endpointHit.getUri());
        assertEquals(ip, endpointHit.getIp());
        assertEquals(timestamp, endpointHit.getTimestamp());
    }

    @Test
    void shouldTestBuilder() {
        Long id = 2L;
        String app = "builder-app";
        String uri = "/builder";
        String ip = "192.168.1.1";
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 15, 10, 30);

        EndpointHit endpointHit = EndpointHit.builder()
                .id(id)
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();

        assertEquals(id, endpointHit.getId());
        assertEquals(app, endpointHit.getApp());
        assertEquals(uri, endpointHit.getUri());
        assertEquals(ip, endpointHit.getIp());
        assertEquals(timestamp, endpointHit.getTimestamp());
    }

    @Test
    void shouldTestGettersAndSetters() {
        EndpointHit endpointHit = new EndpointHit();
        Long id = 3L;
        String app = "setter-app";
        String uri = "/setter";
        String ip = "10.0.0.1";
        LocalDateTime timestamp = LocalDateTime.of(2026, 3, 16, 14, 45);

        endpointHit.setId(id);
        endpointHit.setApp(app);
        endpointHit.setUri(uri);
        endpointHit.setIp(ip);
        endpointHit.setTimestamp(timestamp);

        assertEquals(id, endpointHit.getId());
        assertEquals(app, endpointHit.getApp());
        assertEquals(uri, endpointHit.getUri());
        assertEquals(ip, endpointHit.getIp());
        assertEquals(timestamp, endpointHit.getTimestamp());
    }

    @Test
    void shouldTestEntityAnnotation() {
        Class<EndpointHit> clazz = EndpointHit.class;
        boolean hasEntityAnnotation = clazz.isAnnotationPresent(Entity.class);
        boolean hasTableAnnotation = clazz.isAnnotationPresent(Table.class);

        assertTrue(hasEntityAnnotation, "Класс должен быть аннотирован @Entity");
        assertTrue(hasTableAnnotation, "Класс должен быть аннотирован @Table");

        Table tableAnnotation = clazz.getAnnotation(Table.class);
        assertEquals("endpoint_hits", tableAnnotation.name(),
                "Имя таблицы в @Table должно быть 'endpoint_hits'");
    }

    @Test
    void shouldTestIdFieldAnnotations() {
        try {
            java.lang.reflect.Field idField = EndpointHit.class.getDeclaredField("id");
            boolean hasIdAnnotation = idField.isAnnotationPresent(Id.class);
            boolean hasGeneratedValueAnnotation = idField.isAnnotationPresent(GeneratedValue.class);

            assertTrue(hasIdAnnotation, "Поле id должно быть аннотировано @Id");
            assertTrue(hasGeneratedValueAnnotation, "Поле id должно быть аннотировано @GeneratedValue");

            GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
            assertEquals(GenerationType.IDENTITY, generatedValue.strategy(),
                    "Стратегия генерации ID должна быть GenerationType.IDENTITY");
        } catch (NoSuchFieldException e) {
            fail("Поле id не найдено в классе EndpointHit");
        }
    }

    @Test
    void shouldTestDateTimeFormatAnnotation() {
        try {
            java.lang.reflect.Field timestampField = EndpointHit.class.getDeclaredField("timestamp");
            boolean hasDateTimeFormat = timestampField.isAnnotationPresent(DateTimeFormat.class);

            assertTrue(hasDateTimeFormat, "Поле timestamp должно быть аннотировано @DateTimeFormat");

            DateTimeFormat dateTimeFormat = timestampField.getAnnotation(DateTimeFormat.class);
            assertEquals("yyyy-MM-dd HH:mm:ss", dateTimeFormat.pattern(),
                    "Паттерн @DateTimeFormat должен быть 'yyyy-MM-dd HH:mm:ss'");
        } catch (NoSuchFieldException e) {
            fail("Поле timestamp не найдено в классе EndpointHit");
        }
    }
}