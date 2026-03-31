package ewm.user.repository;

import ewm.user.model.User;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u ORDER BY u.id")
    List<User> findAllWithPageable(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.id")
    List<User> findByIdInWithPageable(List<Long> ids, Pageable pageable);
}
