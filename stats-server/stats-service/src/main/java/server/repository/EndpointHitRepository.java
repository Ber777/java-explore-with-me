package server.repository;

import server.model.EndpointHit;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {
    @Query("SELECT COUNT(eh) FROM EndpointHit eh WHERE eh.uri = :uri AND eh.timestamp BETWEEN :start AND :end")
    long findEndpointHitByUriNotUnique(LocalDateTime start, LocalDateTime end, String uri);

    @Query("SELECT COUNT(DISTINCT eh.ip) FROM EndpointHit eh WHERE eh.uri = :uri AND eh.timestamp BETWEEN :start AND :end")
    long findEndpointHitByUriAndUniqueIp(LocalDateTime start, LocalDateTime end, String uri);

    @Query("SELECT eh.uri FROM EndpointHit eh WHERE eh.timestamp BETWEEN :start AND :end")
    List<String> findAllEndpointHitBetweenDates(LocalDateTime start, LocalDateTime end);
}
