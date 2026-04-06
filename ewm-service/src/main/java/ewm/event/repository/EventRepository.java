package ewm.event.repository;

import ewm.event.model.Event;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.*;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    boolean existsByLocationId(Long locationId);
    boolean existsByCategoryId(Long categoryId);

    @Query(value = """
        SELECT * FROM events
        WHERE user_id = :userId
        ORDER BY id
        LIMIT :limit
        OFFSET :offset
        """, nativeQuery = true)
    Collection<Event> findByUserId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
        SELECT e FROM Event e
        WHERE e.id = :id AND e.state = 'PUBLISHED'
        """)
    Optional<Event> findPublishedById(@Param("id") Long id);
}
