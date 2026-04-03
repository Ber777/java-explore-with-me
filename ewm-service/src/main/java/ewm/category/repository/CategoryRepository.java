package ewm.category.repository;

import ewm.category.model.Category;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(value = "SELECT * FROM categories ORDER BY id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Category> findWithOffsetAndLimit(@Param("offset") long offset, @Param("limit") int limit);
}
