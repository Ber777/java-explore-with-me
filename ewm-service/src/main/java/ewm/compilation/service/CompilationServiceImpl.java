package ewm.compilation.service;

import ewm.exception.*;
import ewm.compilation.dto.*;
import ewm.event.model.Event;
import ewm.compilation.CompilationMapper;
import ewm.compilation.model.Compilation;
import ewm.event.repository.EventRepository;
import ewm.compilation.repository.CompilationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConditionNotMetException("Подборка с таким названием уже существует");
        }

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
        }

        Compilation compilation = CompilationMapper.fromCompilationDto(newCompilationDto, events);
        Compilation saved = compilationRepository.save(compilation);

        return CompilationMapper.toCompilationDto(saved);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, CompilationUpdateDto dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка", compId));

        validateCompilation(compilation, dto);

        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new IllegalArgumentException("Параметры пагинации некорректны");
        }

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Compilation> compilations = (pinned != null)
                ? compilationRepository.findByPinned(pinned, pageable)
                : compilationRepository.findAll(pageable).getContent();

        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка", compId));
        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка", compId));
        compilationRepository.delete(compilation);
    }

    private void validateCompilation(Compilation compilation, CompilationUpdateDto dto) {
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            compilation.setEvents(events);
        }
    }
}
