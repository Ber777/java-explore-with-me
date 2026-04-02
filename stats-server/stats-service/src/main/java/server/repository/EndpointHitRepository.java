package server.repository;

import server.model.EndpointHit;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    // Для уникальных IP: GROUP BY uri, COUNT(DISTINCT ip)
    @Query("SELECT eh.uri, COUNT(DISTINCT eh.ip) " +
            "FROM EndpointHit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR eh.uri IN :uris) " +
            "GROUP BY eh.uri")
    List<Object[]> findEndpointsHitByUrisAndUniqueIpBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);

    // Для всех хитов: GROUP BY uri, COUNT(*)
    @Query("SELECT eh.uri, COUNT(*) " +
            "FROM EndpointHit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR eh.uri IN :uris) " +
            "GROUP BY eh.uri")
    List<Object[]> findEndpointHitsByUrisNotUniqueBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris);
}
