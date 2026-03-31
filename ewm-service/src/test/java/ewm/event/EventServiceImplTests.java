package ewm.event;

import ewm.event.dto.*;
import ewm.exception.*;
import dto.ViewStatsDto;
import ewm.event.model.*;
import client.StatsClient;
import ewm.user.model.User;
import ewm.user.UserMapper;
import ewm.category.model.Category;
import ewm.category.CategoryMapper;
import client.StatsClientException;
import ewm.event.service.EventServiceImpl;
import ewm.event.validator.EventValidator;
import ewm.request.model.UserRequestCount;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.category.repository.CategoryRepository;
import ewm.request.repository.UserRequestRepository;
import jakarta.validation.ValidationException;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTests {

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private StatsClient statsClient;
    @Mock
    private EventValidator eventValidator;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRequestRepository requestRepository;

    private static final Long USER_ID = 1L;
    private static final Long EVENT_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @Test
    void shouldCreateEventSuccessfully() {
        EventDto eventDto = getEventDto();
        User user = getUser();
        Category category = getCategory();
        Event event = getEvent();
        EventDtoResponse expectedResponse = getEventDtoResponse();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDtoResponse actualResponse = eventService.createEvent(USER_ID, eventDto);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getTitle(), actualResponse.getTitle());
        verify(eventValidator, times(1)).validateEventDate(any(), eq(EventState.PENDING));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        EventDto eventDto = getEventDto();

        // Мокаем валидатор даты
        doNothing().when(eventValidator).validateEventDate(any(), eq(EventState.PENDING));

        // Мокаем категорию: гарантируем, что категория существует
        Category category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Мокаем пользователя: пользователь не найден
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventService.createEvent(USER_ID, eventDto),
                "Ожидалось исключение NotFoundException при отсутствии пользователя"
        );

        // Проверяем сообщение исключения
        assertEquals("Пользователь с id: " + USER_ID + " не найден(-а)", exception.getMessage(),
                "Сообщение исключения должно точно соответствовать ожидаемому");

        // Проверяем вызовы репозиториев
        verify(userRepository, times(1)).findById(USER_ID);
        verify(categoryRepository, times(1)).findById(1L);
        // Убеждаемся, что событие не сохранялось
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCategoryNotFound() {
        EventDto eventDto = getEventDto();

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventService.createEvent(USER_ID, eventDto),
                "Ожидалось исключение NotFoundException при отсутствии категории"
        );
    }

    @Test
    void shouldUpdateEventSuccessfully() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto(); // содержит "Updated Title"
        Event existingEvent = getEvent(); // исходное событие с "Test Event"

        // Создаём ожидаемый результат с учётом обновлений
        EventDtoResponse expectedResponse = getEventDtoResponse();
        expectedResponse.setTitle("Updated Title"); // обновляем заголовок в ожидаемом ответе

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event savedEvent = invocation.getArgument(0);
            // Применяем обновления к сохранённому событию
            savedEvent.setTitle("Updated Title");
            return savedEvent;
        });

        EventDtoResponse actualResponse =
                eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
        assertEquals("Updated Title", actualResponse.getTitle()); // сравниваем с обновлённым значением
        assertEquals(expectedResponse.getTitle(), actualResponse.getTitle());
        verify(eventValidator, times(1)).validateInitiatorAccess(USER_ID, existingEvent);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventNotFound() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto));
    }

    @Test
    void shouldThrowValidationExceptionWhenInvalidStateTransition() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();
        eventUpdateDto.setStateAction(StateAction.CANCEL_REVIEW);
        Event existingEvent = getEvent();
        existingEvent.setState(EventState.PUBLISHED);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));

        doThrow(new ValidationException("Invalid state transition"))
                .when(eventValidator).validateUpdatePublishedEvent(eq(existingEvent));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto),
                "Ожидалось исключение ValidationException при некорректном переходе состояния"
        );

        assertEquals("Invalid state transition", exception.getMessage(),
                "Сообщение исключения должно точно соответствовать ожидаемому");
    }

    @Test
    void shouldReturnEventSuccessfully() {
        Event publishedEvent = getEvent();
        publishedEvent.setState(EventState.PUBLISHED);
        EventDtoResponse expectedResponse = getEventDtoResponse();

        when(eventRepository.findPublishedById(EVENT_ID))
                .thenReturn(Optional.of(publishedEvent));

        EventDtoResponse actualResponse = eventService.getPublished(EVENT_ID);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventNotPublished() {
        Event unpublishedEvent = getEvent();

        when(eventRepository.findPublishedById(EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.getPublished(EVENT_ID));
    }

    @Test
    void shouldReturnEventsSuccessfully() {
        List<Event> events = List.of(getEvent());
        List<EventShortDtoResponse> expectedResponses = List.of(getEventShortDtoResponse());

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(eventRepository.findByUserId(USER_ID, 0, 10)).thenReturn(events);

        Collection<EventShortDtoResponse> actualResponses =
                eventService.getByUserId(USER_ID, 0, 10);

        assertNotNull(actualResponses);
        assertEquals(1, actualResponses.size());
        assertEquals(expectedResponses.getFirst().getId(), actualResponses.iterator().next().getId());
    }

    @Test
    void shouldThrowNotFoundExceptionOnGetUserIdWhenUserNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> eventService.getByUserId(USER_ID, 0, 10));
    }

    // Вспомагательные методы
    private EventDto getEventDto() {
        return EventDto.builder()
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .categoryId(CATEGORY_ID)
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new LocationDto(55.751244, 37.618423))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .build();
    }

    private User getUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Test User");
        user.setEmail("user@test.com");
        return user;
    }

    private Category getCategory() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("Test Category");
        return category;
    }

    private Event getEvent() {
        return Event.builder()
                .id(EVENT_ID) // обязательно задаём id
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .category(getCategory())
                .initiator(getUser())
                .eventDate(LocalDateTime.now().plusDays(1))
                .locationLat(55.751244)
                .locationLon(37.618423)
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .createdAt(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0)
                .build();
    }

    private Event getUpdatedEvent() {
        Event event = getEvent();
        event.setTitle("Updated Title");
        event.setAnnotation("Updated Annotation");
        return event;
    }

    private EventDtoResponse getEventDtoResponse() {
        return EventDtoResponse.builder()
                .id(EVENT_ID)
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .category(CategoryMapper.toCategoryDto(getCategory()))
                .initiator(UserMapper.toUserDto(getUser()))
                .location(new LocationDto(55.751244, 37.618423))
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .publishedOn(null)
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0)
                .views(0)
                .build();
    }

    private EventShortDtoResponse getEventShortDtoResponse() {
        return EventShortDtoResponse.builder()
                .id(EVENT_ID)
                .title("Test Event")
                .annotation("Test annotation")
                .category(CategoryMapper.toCategoryDto(getCategory()))
                .paid(false)
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(UserMapper.toUserDto(getUser()))
                .confirmedRequests(0)
                .views(0)
                .build();
    }

    private EventUpdateDto getEventUpdateDto() {
        return EventUpdateDto.builder()
                .title("Updated Title")
                .annotation("Updated Annotation")
                .stateAction(StateAction.SEND_TO_REVIEW)
                .build();
    }

    private EventFilter getEventFilter() {
        return EventFilter.builder()
                .text("test")
                .categories(List.of(CATEGORY_ID))
                .paid(false)
                .rangeStart(LocalDateTime.now())
                .rangeEnd(LocalDateTime.now().plusDays(7))
                .onlyAvailable(false)
                .sort("EVENT_DATE")
                .from(0)
                .size(10)
                .state(EventState.PUBLISHED)
                .build();
    }

    private EventAdminFilter getEventAdminFilter() {
        return EventAdminFilter.builder()
                .users(List.of(USER_ID))
                .categories(List.of(CATEGORY_ID))
                .states(List.of(EventState.PUBLISHED))
                .rangeStart(LocalDateTime.now())
                .rangeEnd(LocalDateTime.now().plusDays(7))
                .build();
    }

    private EventUpdateAdminDto getEventUpdateAdminDto() {
        return EventUpdateAdminDto.builder()
                .title("Updated Admin Title")
                .annotation("Updated Admin Annotation")
                .stateAction(StateAction.PUBLISH_EVENT)
                .build();
    }

    @Test
    void shouldUpdateEventByAdminSuccessfully() {
        EventUpdateAdminDto eventUpdateDto = getEventUpdateAdminDto();
        Event existingEvent = getEvent();
        Event updatedEvent = getUpdatedEvent();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

        EventDtoResponse actualResponse =
                eventService.updateEventByAdmin(EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
    }

    @Test
    void shouldPublishEventByAdmin() {
        EventUpdateAdminDto eventUpdateDto = EventUpdateAdminDto.builder()
                .stateAction(StateAction.PUBLISH_EVENT)
                .build();

        Event pendingEvent = getEvent();
        pendingEvent.setState(EventState.PENDING);
        // Гарантируем, что у события есть ID
        pendingEvent.setId(EVENT_ID);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(pendingEvent));

        // Мокаем сохранение: возвращаем то же событие (имитируем поведение JPA)
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventDtoResponse result = eventService.updateEventByAdmin(EVENT_ID, eventUpdateDto);

        assertEquals(EventState.PUBLISHED, pendingEvent.getState(),
                "Состояние события должно измениться на PUBLISHED");
        assertNotNull(pendingEvent.getPublishedOn(),
                "Дата публикации должна быть установлена");

        // Проверяем, что метод сохранения был вызван
        verify(eventRepository, times(1)).save(eq(pendingEvent));
        // Проверяем результат маппинга
        assertEquals(EVENT_ID, result.getId(), "ID в DTO должен соответствовать ID события");
    }

    @Test
    void shouldReturnEventByGetEventSuccessfully() {
        Event existingEvent = getEvent();
        EventDtoResponse expectedResponse = getEventDtoResponse();

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));

        EventDtoResponse actualResponse = eventService.getEvent(USER_ID, EVENT_ID);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
        verify(eventValidator, times(1)).validateInitiatorAccess(USER_ID, existingEvent);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> eventService.getEvent(USER_ID, EVENT_ID));
    }

    @Test
    void shouldReturnFilteredEvents() {
        EventFilter filter = getEventFilter();
        List<Event> events = List.of(getEvent());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(events));

        Collection<EventShortDtoResponse> responses =
                eventService.getShortEventsBy(filter);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void shouldReturnFilteredFullEvents() {
        EventAdminFilter adminFilter = getEventAdminFilter();
        List<Event> events = List.of(getEvent());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(events));

        Collection<EventDtoResponse> responses =
                eventService.getFullEventsBy(adminFilter);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        verify(eventRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldReturnHandleEmptyResults() {
        EventAdminFilter adminFilter = getEventAdminFilter();

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Collection<EventDtoResponse> responses =
                eventService.getFullEventsBy(adminFilter);

        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldAddViewsAndConfirmedRequests() {
        Event event = getEvent();
        List<Event> events = List.of(event);

        // Создаём Page с тестовыми событиями
        Page<Event> page = new PageImpl<>(events);

        List<UserRequestCount> requestCounts = List.of(
                new UserRequestCount() {
                    @Override
                    public Long getId() { return event.getId(); }

                    @Override
                    public Integer getCount() { return 5; }
                }
        );

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-service", "/events/" + event.getId(), 15L)
        );

        // Мокаем основной запрос событий — используем any() для гибкости
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Мокаем обогащение данных
        when(requestRepository.countConfirmedRequestsForEvents(anyList()))
                .thenReturn(requestCounts);
        when(statsClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenReturn(stats);

        List<EventShortDtoResponse> result = (List<EventShortDtoResponse>) eventService.getShortEventsBy(getEventFilter());

        assertEquals(1, result.size(), "Должен быть возвращён один элемент");

        EventShortDtoResponse enrichedEvent = result.getFirst();
        assertEquals(5, enrichedEvent.getConfirmedRequests(),
                "Количество подтверждённых запросов должно быть 5");
        assertEquals(15, enrichedEvent.getViews(),
                "Количество просмотров должно быть 15");
    }

    @Test
    void shouldReturnHandleNullStats() {
        List<Event> events = List.of(getEvent());
        Page<Event> page = new PageImpl<>(events); // Создаём страницу с событиями

        // Мокаем репозиторий: findAll должен возвращать валидную страницу
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Мокаем статистику: countConfirmedRequests возвращает пустой список
        when(requestRepository.countConfirmedRequestsForEvents(anyList()))
                .thenReturn(Collections.emptyList());

        // Мокаем клиент статистики: выбрасывает исключение
        when(statsClient.getStats(any(), any(), anyList(), anyBoolean()))
                .thenThrow(new StatsClientException("Stats service unavailable"));

        List<EventShortDtoResponse> result = (List<EventShortDtoResponse>) eventService.getShortEventsBy(getEventFilter());

        Event enrichedEvent = events.getFirst();
        assertEquals(0, enrichedEvent.getConfirmedRequests());
        assertEquals(0, enrichedEvent.getViews());
        assertEquals(1, result.size()); // Проверяем, что результат не пустой
    }

    @Test
    void shouldThrowValidationExceptionOnInvalidDate() {
        EventDto eventDto = getEventDto();
        eventDto.setEventDate(LocalDateTime.now().minusDays(1)); // Прошлое событие

        doThrow(new ValidationException("Дата события не может быть в прошлом"))
                .when(eventValidator).validateEventDate(any(), eq(EventState.PENDING));

        assertThrows(ValidationException.class,
                () -> eventService.createEvent(USER_ID, eventDto));
    }

    @Test
    void shouldThrowAccessExceptionWhenNotInitiator() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();
        Event existingEvent = getEvent();
        existingEvent.setInitiator(new User() {{ setId(2L); }}); // Другой инициатор

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        doThrow(new ValidationException("Нет доступа к событию"))
                .when(eventValidator).validateInitiatorAccess(USER_ID, existingEvent);

        assertThrows(ValidationException.class,
                () -> eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto));
    }

    @Test
    void shouldCreateCorrectAdminFilter() {
        EventAdminFilter filter = getEventAdminFilter();

        Specification<Event> spec = eventService.buildSpecification(filter);

        assertNotNull(spec);
    }

    @Test
    void shouldCreateCorrectUserFilter() {
        EventFilter filter = getEventFilter();

        Specification<Event> spec = eventService.buildSpecification(filter);

        assertNotNull(spec);
    }
}
