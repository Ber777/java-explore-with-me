package ewm.request.repository;

import ewm.request.model.*;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {
    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<UserRequest> findAllByRequesterId(Long userId);

    List<UserRequest> findAllByEventId(Long eventId);

    Integer countByEventIdAndStatus(Long eventId, UserRequestStatus status);

    @Query("""
            SELECT ur.event.id as id, COUNT(ur) as count
            FROM UserRequest ur
            WHERE ur.event.id IN :ids AND ur.status = 'CONFIRMED'
            GROUP BY ur.event.id""")
    List<UserRequestCount> countConfirmedRequestsForEvents(@Param("ids") List<Long> ids);
}
