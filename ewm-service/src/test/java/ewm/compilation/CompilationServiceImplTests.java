package ewm.compilation;

import ewm.exception.*;
import ewm.event.model.*;
import ewm.compilation.dto.*;
import ewm.user.model.User;
import ewm.location.model.Location;
import ewm.category.model.Category;
import ewm.compilation.model.Compilation;
import ewm.event.repository.EventRepository;
import ewm.compilation.service.CompilationServiceImpl;
import ewm.compilation.repository.CompilationRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTests {

    @InjectMocks
    private CompilationServiceImpl compilationService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CompilationRepository compilationRepository;

    // Вспомогательный метод для создания тестовой подборки
    private Compilation getCompilation() {
        return Compilation.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(false)
                .events(new HashSet<>())
                .build();
    }

    // Вспомогательный метод для создания NewCompilationDto
    private NewCompilationDto getNewCompilationDto() {
        return NewCompilationDto.builder()
                .title("New Compilation")
                .pinned(false)
                .events(Set.of(1L, 2L))
                .build();
    }

    // Вспомогательный метод для создания CompilationUpdateDto
    private CompilationUpdateDto getCompilationUpdateDto() {
        return CompilationUpdateDto.builder()
                .title("Updated Title")
                .pinned(true)
                .events(Set.of(3L, 4L))
                .build();
    }

    @Test
    void shouldCreateCompilationSuccessfully() {
        NewCompilationDto newCompilationDto = getNewCompilationDto();
        Set<Event> events = new HashSet<>();

        // Создаём сохранённый объект с ID через builder()
        Compilation savedCompilation = Compilation.builder()
                .id(1L)
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .events(events)
                .build();

        when(compilationRepository.existsByTitle(newCompilationDto.getTitle())).thenReturn(false);
        when(eventRepository.findAllById(newCompilationDto.getEvents())).thenReturn(List.of());
        when(compilationRepository.save(any(Compilation.class))).thenReturn(savedCompilation);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertNotNull(result);
        assertEquals(savedCompilation.getId(), result.getId());
        assertEquals(newCompilationDto.getTitle(), result.getTitle());
        verify(compilationRepository, times(1)).existsByTitle(newCompilationDto.getTitle());
        verify(eventRepository, times(1)).findAllById(newCompilationDto.getEvents());
        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }

    @Test
    void shouldThrowConditionNotMetExceptionWhenTitleExists() {
        NewCompilationDto newCompilationDto = getNewCompilationDto();

        when(compilationRepository.existsByTitle(newCompilationDto.getTitle())).thenReturn(true);

        ConditionNotMetException exception = assertThrows(ConditionNotMetException.class,
                () -> compilationService.createCompilation(newCompilationDto));
        assertEquals("Подборка с таким названием уже существует", exception.getMessage());
        verify(compilationRepository, times(1)).existsByTitle(newCompilationDto.getTitle());
        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCompilationSuccessfully() {
        Long compId = 1L;
        CompilationUpdateDto updateDto = getCompilationUpdateDto();
        Compilation existingCompilation = getCompilation();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(eventRepository.findAllById(updateDto.getEvents())).thenReturn(List.of());

        CompilationDto result = compilationService.updateCompilation(compId, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getTitle(), result.getTitle());
        assertEquals(updateDto.getPinned(), result.getPinned());
        verify(compilationRepository, times(1)).findById(compId);
        verify(eventRepository, times(1)).findAllById(updateDto.getEvents());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCompilationNotExists() {
        Long compId = 999L;
        CompilationUpdateDto updateDto = getCompilationUpdateDto();

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.updateCompilation(compId, updateDto));

        verify(compilationRepository, times(1)).findById(compId);
        verify(eventRepository, never()).findAllById(any());
    }

    @Test
    void shouldReturnPinnedCompilations() {
        Boolean pinned = true;
        int from = 0;
        int size = 10;

        Compilation compilation = getCompilation();
        List<Compilation> compilations = List.of(compilation);
        Pageable pageable = PageRequest.of(0, size);

        when(compilationRepository.findByPinned(pinned, pageable)).thenReturn(compilations);

        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);

        assertEquals(1, result.size());
        assertEquals(compilation.getId(), result.getFirst().getId());
        verify(compilationRepository, times(1)).findByPinned(pinned, pageable);
    }

    @Test
    void shouldReturnAllCompilationsWhenPinnedIsNull() {
        Boolean pinned = null;
        int from = 0;
        int size = 10;

        Compilation compilation = getCompilation();
        Page<Compilation> page = new PageImpl<>(List.of(compilation));
        Pageable pageable = PageRequest.of(0, size);

        when(compilationRepository.findAll(pageable)).thenReturn(page);

        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);

        assertEquals(1, result.size());
        assertEquals(compilation.getId(), result.getFirst().getId());
        verify(compilationRepository, times(1)).findAll(pageable);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForInvalidPagination() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> compilationService.getCompilations(true, -1, 10));
        assertEquals("Параметры пагинации некорректны", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> compilationService.getCompilations(true, 0, 0));
        assertEquals("Параметры пагинации некорректны", exception.getMessage());
    }

    @Test
    void shouldReturnCompilation() {
        Long compId = 1L;
        Compilation compilation = getCompilation();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));

        CompilationDto result = compilationService.getCompilationById(compId);

        assertNotNull(result);
        assertEquals(compilation.getId(), result.getId());
        assertEquals(compilation.getTitle(), result.getTitle());
        verify(compilationRepository, times(1)).findById(compId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCompilationNotExistsById() {
        Long compId = 999L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.getCompilationById(compId));

        verify(compilationRepository, times(1)).findById(compId);
    }

    @Test
    void shouldDeleteCompilationSuccessfully() {
        Long compId = 1L;
        Compilation compilation = getCompilation();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));

        compilationService.deleteCompilation(compId);

        verify(compilationRepository, times(1)).findById(compId);
        verify(compilationRepository, times(1)).delete(compilation);
    }

    @Test
    void shouldThrowNotFoundExceptionOnDeleteWhenCompilationNotExists() {
        Long compId = 999L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.deleteCompilation(compId));

        verify(compilationRepository, times(1)).findById(compId);
        verify(compilationRepository, never()).delete(any());
    }

    @Test
    void shouldCreateCompilationWithEvents() {
        NewCompilationDto newCompilationDto = getNewCompilationDto();
        Set<Long> eventIds = newCompilationDto.getEvents();

        // Создаём категорию для событий
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        Location location1 = Location.builder()
                .id(1L)
                .latitude(55.7558)
                .longitude(37.6173)
                .build();

        Location location2 = Location.builder()
                .id(2L)
                .latitude(59.9343)
                .longitude(30.3351)
                .build();

        // Инициализируем события с заполненной категорией и другими обязательными полями
        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .category(category)
                .initiator(User.builder().id(1L).build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(location1)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .category(category)
                .initiator(User.builder().id(2L).build())
                .eventDate(LocalDateTime.now().plusDays(2))
                .location(location2)
                .build();

        List<Event> events = List.of(event1, event2);

        // Создаём подборку с корректно инициализированными полями через CompilationMapper
        Compilation compilation = CompilationMapper.fromCompilationDto(newCompilationDto, new LinkedHashSet<>(events));

        // Создаём сохранённый объект с ID и всеми полями через builder()
        Compilation savedCompilation = Compilation.builder()
                .id(1L)
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .events(new LinkedHashSet<>(events))
                .build();

        when(compilationRepository.existsByTitle(newCompilationDto.getTitle())).thenReturn(false);
        when(eventRepository.findAllById(eventIds)).thenReturn(events);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(savedCompilation);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertNotNull(result);
        assertEquals(savedCompilation.getId(), result.getId());
        assertEquals(newCompilationDto.getTitle(), result.getTitle());
        assertEquals(2, result.getEvents().size());

        // Проверяем корректность маппинга событий и их категорий
        assertEquals("Event 1", result.getEvents().getFirst().getTitle());
        assertEquals("Event 2", result.getEvents().get(1).getTitle());
        assertEquals(1L, result.getEvents().getFirst().getCategory().getId());
        assertEquals(1L, result.getEvents().get(1).getCategory().getId());

        verify(compilationRepository, times(1)).existsByTitle(newCompilationDto.getTitle());
        verify(eventRepository, times(1)).findAllById(eventIds);
        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Long compId = 1L;
        CompilationUpdateDto updateDto = CompilationUpdateDto.builder()
                .title("Updated Title")
                .build(); // Только заголовок, остальные поля null

        Compilation existingCompilation = getCompilation();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));

        CompilationDto result = compilationService.updateCompilation(compId, updateDto);

        assertEquals("Updated Title", result.getTitle());
        assertFalse(result.getPinned());
        verify(eventRepository, never()).findAllById(any()); // События не должны обновляться
    }

    @Test
    void shouldNotUpdateEventsIfEventsFieldIsNull() {
        Long compId = 1L;
        CompilationUpdateDto updateDto = CompilationUpdateDto.builder()
                .title("Updated Title")
                .events(null)
                .build();

        // Создаём категорию для события
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        Location location = Location.builder()
                .id(1L)
                .latitude(55.7558)
                .longitude(37.6173)
                .build();

        // Инициализируем событие с заполненной категорией и другими обязательными полями
        Event existingEvent = Event.builder()
                .id(1L)
                .title("Existing Event")
                .category(category)
                .initiator(User.builder().id(1L).build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(location)
                .build();

        Compilation existingCompilation = getCompilation();
        existingCompilation.setEvents(Set.of(existingEvent));

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));

        CompilationDto result = compilationService.updateCompilation(compId, updateDto);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertNotNull(result.getEvents());
        assertEquals(1, result.getEvents().size()); // События должны остаться прежними

        // Проверяем, что существующее событие сохранилось и его категория маппится корректно
        assertEquals("Existing Event", result.getEvents().getFirst().getTitle());
        assertEquals(1L, result.getEvents().getFirst().getCategory().getId());

        verify(eventRepository, never()).findAllById(any());
        verify(compilationRepository, times(1)).findById(compId);
        verify(compilationRepository, never()).save(any(Compilation.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoCompilationsFound() {
        Boolean pinned = true;
        int from = 0;
        int size = 10;

        Pageable pageable = PageRequest.of(0, size);

        when(compilationRepository.findByPinned(pinned, pageable)).thenReturn(List.of());

        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);

        assertTrue(result.isEmpty());
        verify(compilationRepository, times(1)).findByPinned(pinned, pageable);
    }

    @Test
    void shouldReturnHandleEmptyEventsList() {
        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Compilation Without Events")
                .pinned(false)
                .events(Set.of())
                .build();

        Set<Event> events = new HashSet<>();

        // Создаём подборку с корректно инициализированными полями
        Compilation compilation = CompilationMapper.fromCompilationDto(newCompilationDto, events);

        // Создаём сохранённый объект с ID и всеми полями через builder()
        Compilation savedCompilation = Compilation.builder()
                .id(1L)
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned())
                .events(events) // Явно передаем пустую коллекцию событий
                .build();

        when(compilationRepository.existsByTitle(newCompilationDto.getTitle())).thenReturn(false);
        when(compilationRepository.save(any(Compilation.class))).thenReturn(savedCompilation);

        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        assertNotNull(result);
        assertEquals(savedCompilation.getId(), result.getId());
        assertEquals(newCompilationDto.getTitle(), result.getTitle());
        assertTrue(result.getEvents().isEmpty(), "Список событий в DTO должен быть пустым");

        // Проверяем, что метод findAllById не вызывался
        verify(eventRepository, never()).findAllById(any());

        // Верифицируем вызовы ключевых методов
        verify(compilationRepository, times(1)).existsByTitle(newCompilationDto.getTitle());
        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }

    @Test
    void shouldUpdateEventsSuccessfully() {
        Long compId = 1L;
        CompilationUpdateDto updateDto = getCompilationUpdateDto();

        // Создаём категорию для событий
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        Location location3 = Location.builder()
                .id(3L)
                .latitude(55.7558)
                .longitude(37.6173)
                .build();

        Location location4 = Location.builder()
                .id(4L)
                .latitude(59.9343)
                .longitude(30.3351)
                .build();

        // Инициализируем события с заполненной категорией
        Event event3 = Event.builder()
                .id(3L)
                .title("Updated Event 3")
                .category(category)
                .initiator(User.builder().id(1L).build())
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(location3)
                .build();

        Event event4 = Event.builder()
                .id(4L)
                .title("Updated Event 4")
                .category(category)
                .initiator(User.builder().id(2L).build())
                .eventDate(LocalDateTime.now().plusDays(2))
                .location(location4)
                .build();

        Compilation existingCompilation = getCompilation();

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(existingCompilation));
        when(eventRepository.findAllById(updateDto.getEvents())).thenReturn(List.of(event3, event4));

        CompilationDto result = compilationService.updateCompilation(compId, updateDto);

        assertNotNull(result);
        assertEquals(2, result.getEvents().size());

        // Проверяем наличие событий без привязки к порядку
        assertTrue(result.getEvents().stream()
                        .anyMatch(e -> "Updated Event 3".equals(e.getTitle())),
                "Должен присутствовать event с заголовком 'Updated Event 3'");
        assertTrue(result.getEvents().stream()
                        .anyMatch(e -> "Updated Event 4".equals(e.getTitle())),
                "Должен присутствовать event с заголовком 'Updated Event 4'");

        // Проверяем категории для обоих событий
        assertTrue(result.getEvents().stream()
                        .allMatch(e -> e.getCategory() != null && e.getCategory().getId() == 1L),
                "Все события должны иметь категорию с ID = 1");

        verify(eventRepository, times(1)).findAllById(updateDto.getEvents());
        verify(compilationRepository, times(1)).findById(compId);
    }
}
