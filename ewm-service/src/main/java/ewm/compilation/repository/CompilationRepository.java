package ewm.compilation.repository;

import ewm.compilation.model.Compilation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    boolean existsByTitle(String title);

    List<Compilation> findByPinned(Boolean pinned, Pageable pageable);
}