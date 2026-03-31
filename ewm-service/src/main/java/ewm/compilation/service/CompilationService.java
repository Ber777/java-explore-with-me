package ewm.compilation.service;

import ewm.compilation.dto.*;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compId, CompilationUpdateDto dto);

    CompilationDto getCompilationById(Long compId);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    void deleteCompilation(Long compId);
}
