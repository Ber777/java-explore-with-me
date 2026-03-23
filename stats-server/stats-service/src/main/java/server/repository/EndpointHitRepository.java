package server.repository;

import server.model.EndpointHit;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT eh.uri FROM EndpointHit eh WHERE eh.timestamp BETWEEN :start AND :end")
    List<String> findAllEndpointHitBetweenDates(LocalDateTime start, LocalDateTime end);

    // Для уникальных IP: GROUP BY uri, COUNT(DISTINCT ip)
    @Query("SELECT eh.uri, COUNT(DISTINCT eh.ip) " +
            "FROM EndpointHit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri IN :uris " +
            "GROUP BY eh.uri")
    List<Object[]> findEndpointsHitByUrisAndUniqueIp(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    // Для всех хитов: GROUP BY uri, COUNT(*)
    @Query("SELECT eh.uri, COUNT(*) " +
            "FROM EndpointHit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri IN :uris " +
            "GROUP BY eh.uri")
    List<Object[]> findEndpointHitsByUrisNotUnique(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);
}
