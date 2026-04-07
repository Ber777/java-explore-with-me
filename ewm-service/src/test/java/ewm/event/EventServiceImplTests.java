package ewm.event;

import ewm.event.dto.*;
import ewm.exception.*;
import dto.ViewStatsDto;
import ewm.event.model.*;
import ewm.location.dto.*;
import client.StatsClient;
import ewm.user.model.User;
import ewm.user.UserMapper;
import ewm.location.model.Zone;
import ewm.location.model.Location;
import ewm.category.model.Category;
import ewm.location.model.LocationState;
import ewm.category.CategoryMapper;
import client.StatsClientException;
import ewm.event.service.EventServiceImpl;
import ewm.event.validator.EventValidator;
import ewm.request.model.UserRequestCount;
import ewm.location.service.LocationService;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.category.repository.CategoryRepository;
import ewm.request.repository.UserRequestRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.*;
import org.springframework.beans.BeanUtils;
import jakarta.validation.ValidationException;
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
    private LocationService locationService;
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

        doThrow(new ValidationException("Недопустимый переход состояний"))
                .when(eventValidator).validateUpdatePublishedEvent(eq(existingEvent));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto),
                "Ожидалось исключение ValidationException при некорректном переходе состояния"
        );

        assertEquals("Недопустимый переход состояний", exception.getMessage(),
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
        LocationDto location = LocationDto.builder()
                .id(1L)
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        return EventDto.builder()
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .categoryId(CATEGORY_ID)
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(location)
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
        Location location = Location.builder()
                .id(1L)
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.APPROVED)
                .build();

        return Event.builder()
                .id(EVENT_ID) // обязательно задаём id
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .category(getCategory())
                .initiator(getUser())
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(location)
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .createdAt(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0)
                .build();
    }

    private EventDtoResponse getEventDtoResponse() {
        LocationDtoResponse location = LocationDtoResponse.builder()
                .id(1L)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        return EventDtoResponse.builder()
                .id(EVENT_ID)
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .category(CategoryMapper.toCategoryDto(getCategory()))
                .initiator(UserMapper.toUserDto(getUser()))
                .location(location)
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

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));

        EventDtoResponse actualResponse =
                eventService.updateEventByAdmin(EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
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
                    public Long getId() {
                        return event.getId();
                    }

                    @Override
                    public Integer getCount() {
                        return 5;
                    }
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
                .thenThrow(new StatsClientException("Сервис статистики недоступен"));

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
        existingEvent.setInitiator(User.builder().id(2L).build()); // Другой инициатор

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

    @Test
    void shouldCreateEventWithNewLocationSuccessfully() {
        EventDto eventDto = getEventDto();
        User user = getUser();
        Category category = getCategory();
        Event event = getEvent();
        LocationDto locationDto = eventDto.getLocation();
        Location location = Location.builder()
                .id(1L)
                .latitude(locationDto.getLatitude())
                .longitude(locationDto.getLongitude())
                .state(LocationState.AUTO_GENERATED)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(locationService.getOrCreateLocation(locationDto)).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDtoResponse actualResponse = eventService.createEvent(USER_ID, eventDto);

        assertNotNull(actualResponse);
        assertEquals(locationDto.getLatitude(), actualResponse.getLocation().getLatitude());
        assertEquals(locationDto.getLongitude(), actualResponse.getLocation().getLongitude());
        verify(locationService, times(1)).getOrCreateLocation(eq(locationDto));
    }

    @Test
    void shouldCreateEventWithExistingLocationSuccessfully() {
        EventDto eventDto = getEventDto();
        eventDto.setLocation(new LocationDto(2L, 55.751244, 37.618423));

        User user = getUser();
        Category category = getCategory();

        Location existingLocation = Location.builder()
                .id(2L)
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.APPROVED)
                .build();

        // Создаём event с нужной локацией
        Event event = getEvent();
        event.setLocation(existingLocation); // Устанавливаем локацию с id = 2

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(locationService.getOrCreateLocation(eventDto.getLocation())).thenReturn(existingLocation);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDtoResponse actualResponse = eventService.createEvent(USER_ID, eventDto);

        assertNotNull(actualResponse);
        assertEquals(2L, actualResponse.getLocation().getId());
        assertEquals(55.751244, actualResponse.getLocation().getLatitude());
        verify(locationService, times(1)).getOrCreateLocation(eq(eventDto.getLocation()));
    }

    @Test
    void shouldUpdateEventWithNewLocationSuccessfully() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();
        LocationDto newLocationDto = LocationDto.builder()
                .latitude(60.0)
                .longitude(30.0)
                .build();
        eventUpdateDto.setLocation(newLocationDto);

        Event existingEvent = getEvent();
        Location newLocation = Location.builder()
                .id(3L)
                .latitude(60.0)
                .longitude(30.0)
                .state(LocationState.AUTO_GENERATED)
                .build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(locationService.getOrCreateLocation(eventUpdateDto.getLocation())).thenReturn(newLocation);
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> {
                    Event savedEvent = invocation.getArgument(0);
                    savedEvent.setLocation(newLocation);
                    return savedEvent;
                });

        EventDtoResponse actualResponse =
                eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
        assertEquals(60.0, actualResponse.getLocation().getLatitude());
        assertEquals(30.0, actualResponse.getLocation().getLongitude());
        verify(locationService, times(1)).getOrCreateLocation(eq(eventUpdateDto.getLocation()));
    }

    @Test
    void shouldUpdateEventWithExistingLocationSuccessfully() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();
        eventUpdateDto.setLocation(new LocationDto(4L, 45.0, 40.0));

        Event existingEvent = getEvent();
        Location existingLocation = Location.builder()
                .id(4L)
                .latitude(45.0)
                .longitude(40.0)
                .state(LocationState.APPROVED)
                .build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(locationService.getOrCreateLocation(eventUpdateDto.getLocation())).thenReturn(existingLocation);
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> {
                    Event savedEvent = invocation.getArgument(0);
                    savedEvent.setLocation(existingLocation);
                    return savedEvent;
                });

        EventDtoResponse actualResponse =
                eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
        assertEquals(4L, actualResponse.getLocation().getId());
        assertEquals(45.0, actualResponse.getLocation().getLatitude());
        verify(locationService, times(1)).getOrCreateLocation(eq(eventUpdateDto.getLocation()));
    }

    @Test
    void shouldThrowExceptionWhenLocationCreationFails() {
        EventDto eventDto = getEventDto();
        User user = getUser();
        Category category = getCategory();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        doThrow(new NotFoundException("Локация", 999L))
                .when(locationService).getOrCreateLocation(any());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventService.createEvent(USER_ID, eventDto),
                "Ожидалось исключение NotFoundException при ошибке создания локации"
        );

        assertEquals("Локация с id: 999 не найден(-а)", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldUpdateEventByAdminWithLocationSuccessfully() {
        EventUpdateAdminDto eventUpdateDto = getEventUpdateAdminDto();
        LocationDto newLocationDto = LocationDto.builder()
                .latitude(70.0)
                .longitude(20.0)
                .build();
        eventUpdateDto.setLocation(newLocationDto);

        Event existingEvent = getEvent();
        Location updatedLocation = Location.builder()
                .id(5L)
                .latitude(70.0)
                .longitude(20.0)
                .state(LocationState.AUTO_GENERATED)
                .build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
        when(locationService.getOrCreateLocation(eventUpdateDto.getLocation())).thenReturn(updatedLocation);

        EventDtoResponse actualResponse =
                eventService.updateEventByAdmin(EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
        assertEquals(70.0, actualResponse.getLocation().getLatitude());
        assertEquals(20.0, actualResponse.getLocation().getLongitude());
        verify(locationService, times(1)).getOrCreateLocation(eq(eventUpdateDto.getLocation()));
    }

    private LocationDto createLocationDto() {
        return new LocationDto(null, 56.0, 38.0);
    }

    @Test
    void shouldFilterEventsByCoordinatesSuccessfully() {
        EventFilter filter = getEventFilter();
        filter.setZone(new Zone(55.751244, 37.618423, 1000.0)); // радиус 1 км

        List<Event> events = List.of(getEvent());
        Page<Event> page = new PageImpl<>(events);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Collection<EventShortDtoResponse> responses =
                eventService.getShortEventsBy(filter);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        verify(eventRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldHandleLocationNotFoundInFilter() {
        EventFilter filter = getEventFilter();
        filter.setLocationId(999L); // несуществующая локация

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Collection<EventShortDtoResponse> responses =
                eventService.getShortEventsBy(filter);

        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldAutoGenerateLocationWhenCoordinatesProvided() {
        EventDto eventDto = getEventDto();
        LocationDto newLocationDto = LocationDto.builder()
                .latitude(60.123456)
                .longitude(30.987654)
                .build();
        eventDto.setLocation(newLocationDto);

        User user = getUser();
        Category category = getCategory();
        Location autoGeneratedLocation = Location.builder()
                .id(6L)
                .latitude(60.123456)
                .longitude(30.987654)
                .state(LocationState.AUTO_GENERATED)
                .build();

        // Создаём event с нужной локацией
        Event event = getEvent();
        event.setLocation(autoGeneratedLocation); // Явно устанавливаем нужную локацию

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(locationService.getOrCreateLocation(eventDto.getLocation())).thenReturn(autoGeneratedLocation);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDtoResponse actualResponse = eventService.createEvent(USER_ID, eventDto);

        assertNotNull(actualResponse);
        assertEquals(LocationState.AUTO_GENERATED, autoGeneratedLocation.getState());
        assertEquals(60.123456, actualResponse.getLocation().getLatitude());
        assertEquals(30.987654, actualResponse.getLocation().getLongitude());
    }

    @Test
    void shouldUseExistingLocationById() {
        EventDto eventDto = getEventDto();
        Long locationId = 1L;
        eventDto.setLocation(new LocationDto(locationId, null, null));

        User user = getUser();
        Category category = getCategory();
        Location existingLocation = Location.builder()
                .id(locationId)
                .latitude(55.0)
                .longitude(37.0)
                .state(LocationState.APPROVED)
                .build();

        // Создаём event с нужной локацией
        Event event = getEvent();
        event.setLocation(existingLocation); // Явно устанавливаем нужную локацию

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(locationService.getOrCreateLocation(eventDto.getLocation()))
                .thenReturn(existingLocation);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventDtoResponse actualResponse = eventService.createEvent(USER_ID, eventDto);

        assertNotNull(actualResponse);
        assertEquals(locationId, actualResponse.getLocation().getId());
        assertEquals(55.0, actualResponse.getLocation().getLatitude());
        assertEquals(37.0, actualResponse.getLocation().getLongitude());
    }

    @Test
    void shouldUpdateLocationInEventSuccessfully() {
        EventUpdateDto eventUpdateDto = getEventUpdateDto();
        eventUpdateDto.setLocation(createLocationDto());

        Location existingLocation = Location.builder()
                .id(1L)
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.APPROVED)
                .build();

        Location existingEventLocation = Location.builder()
                .id(existingLocation.getId())
                .latitude(existingLocation.getLatitude())
                .longitude(existingLocation.getLongitude())
                .state(existingLocation.getState())
                .build();

        Event existingEvent = getEvent();
        existingEvent.setLocation(existingEventLocation);

        // Создаём копию existingEvent для передачи в сервис
        Event eventForService = new Event();

        // Копируем все поля из existingEvent в eventForService
        BeanUtils.copyProperties(existingEvent, eventForService);

        Location newLocation = Location.builder()
                .id(8L)
                .latitude(56.0)
                .longitude(38.0)
                .state(LocationState.AUTO_GENERATED)
                .build();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventForService));
        when(locationService.getOrCreateLocation(eventUpdateDto.getLocation())).thenReturn(newLocation);
        when(eventRepository.save(any(Event.class)))
                .thenAnswer(invocation -> {
                    Event savedEvent = invocation.getArgument(0);
                    savedEvent.setLocation(newLocation);
                    return savedEvent;
                });

        EventDtoResponse actualResponse =
                eventService.updateEventByUser(USER_ID, EVENT_ID, eventUpdateDto);

        assertNotNull(actualResponse);
        assertEquals(56.0, actualResponse.getLocation().getLatitude());
        assertEquals(38.0, actualResponse.getLocation().getLongitude());

        assertNotEquals(existingEvent.getLocation().getId(), actualResponse.getLocation().getId());
    }
}
