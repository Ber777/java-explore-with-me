package ewm.event;

import ewm.exception.*;
import ewm.event.dto.*;
import client.StatsClient;
import ewm.event.model.EventState;
import ewm.event.model.EventFilter;
import client.StatsClientException;
import ewm.event.service.EventService;
import ewm.event.controller.EventPublicController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class EventPublicControllerTests {

    @InjectMocks
    private EventPublicController eventPublicController;

    @Mock
    private EventService eventService;

    @Mock
    private StatsClient statsClient;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        eventPublicController.setAppName("ewm");
    }

    // Вспомогательный метод для создания тестового короткого DTO события
    private EventShortDtoResponse getEventShortDtoResponse(String title) {
        return EventShortDtoResponse.builder()
                .id(1L)
                .title(title)
                .annotation("Test Annotation")
                .paid(false)
                .confirmedRequests(0)
                .views(0)
                .build();
    }

    // Вспомогательный метод для создания полного DTO события
    private EventDtoResponse getEventDtoResponse(Long id) {
        return EventDtoResponse.builder()
                .id(id)
                .title("Test Event")
                .annotation("Test Annotation")
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .views(0)
                .build();
    }

    @Test
    void shouldGetEventsSuccessFullWithFullParameters() {
        String text = "test event";
        List<Long> categories = List.of(1L, 2L);
        Boolean paid = true;
        LocalDateTime rangeStart = LocalDateTime.now();
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(7);
        Boolean onlyAvailable = true;
        String sort = "VIEWS";
        Integer from = 5;
        Integer size = 20;

        EventShortDtoResponse event = getEventShortDtoResponse("Test Event");
        List<EventShortDtoResponse> events = List.of(event);

        when(eventService.getShortEventsBy(any(EventFilter.class))).thenReturn(events);

        Collection<EventShortDtoResponse> result = eventPublicController.getEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(event.getId(), result.iterator().next().getId());

        verify(eventService, times(1)).getShortEventsBy(any(EventFilter.class));
        verify(statsClient, times(1)).endpointHit(anyString(), eq("/events/1"), anyString());
        verify(statsClient, times(1)).endpointHit(anyString(), eq("/events"), anyString());
    }

    @Test
    void shouldThrowInvalidRequestExceptionWhenRangeStartAfterRangeEnd() {
        LocalDateTime rangeStart = LocalDateTime.now().plusDays(2);
        LocalDateTime rangeEnd = LocalDateTime.now();

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> eventPublicController.getEvents(
                        "test", null, null, rangeStart, rangeEnd, false, "EVENT_DATE", 0, 10, request));

        assertEquals("Дата начала должна быть раньше, чем дата окончания.", exception.getMessage());
        verify(eventService, never()).getShortEventsBy(any());
    }

    @Test
    void shouldGetEventByIdSuccessfully() {
        Long eventId = 1L;
        EventDtoResponse eventDtoResponse = getEventDtoResponse(eventId);
        when(eventService.getPublished(eventId)).thenReturn(eventDtoResponse);

        EventDtoResponse result = eventPublicController.get(eventId, request);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());

        verify(eventService, times(1)).getPublished(eventId);
        verify(statsClient, times(1)).endpointHit(notNull(), eq("/events/1"), anyString());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventNotExists() {
        Long eventId = 999L;

        when(eventService.getPublished(eventId))
                .thenThrow(new NotFoundException("Событие", eventId));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventPublicController.get(eventId, request));

        assertEquals("Событие с id: 999 не найден(-а)", exception.getMessage());
        verify(eventService, times(1)).getPublished(eventId);
    }

    @Test
    void shouldHandleEmptyEventsList() {
        when(eventService.getShortEventsBy(any(EventFilter.class)))
                .thenReturn(Collections.emptyList());

        Collection<EventShortDtoResponse> result = eventPublicController.getEvents(
                null, null, null, null, null, false, "EVENT_DATE", 0, 10, request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventService, times(1)).getShortEventsBy(any(EventFilter.class));
    }

    @Test
    void shouldUseDefaultValuesWhenParametersNotProvided() {
        EventShortDtoResponse event = getEventShortDtoResponse("Default Event");
        when(eventService.getShortEventsBy(any(EventFilter.class)))
                .thenReturn(List.of(event));

        Collection<EventShortDtoResponse> result = eventPublicController.getEvents(
                null, null, null, null, null, null, null, null, null, request);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<EventFilter> filterCaptor = ArgumentCaptor.forClass(EventFilter.class);
        verify(eventService, times(1)).getShortEventsBy(filterCaptor.capture());

        EventFilter capturedFilter = filterCaptor.getValue();

        // Проверяем с учётом возможного null
        assertFalse(capturedFilter.getOnlyAvailable() != null ? capturedFilter.getOnlyAvailable() : false);
        assertEquals("EVENT_DATE", capturedFilter.getSort() != null ? capturedFilter.getSort() : "EVENT_DATE");
        assertEquals(0, capturedFilter.getFrom() != null ? capturedFilter.getFrom() : 0);
        assertEquals(10, capturedFilter.getSize() != null ? capturedFilter.getSize() : 10);
        assertEquals(EventState.PUBLISHED, capturedFilter.getState() != null ? capturedFilter.getState() : EventState.PUBLISHED);
    }

    @Test
    void shouldLogErrorWhenStatsClientThrowsException() {
        EventShortDtoResponse event = getEventShortDtoResponse("Event with Stats Error");
        when(eventService.getShortEventsBy(any(EventFilter.class)))
                .thenReturn(List.of(event));
        doThrow(new StatsClientException("Connection error"))
                .when(statsClient).endpointHit(anyString(), anyString(), anyString());

        Collection<EventShortDtoResponse> result = eventPublicController.getEvents(
                "test", null, null, null, null, false, "EVENT_DATE", 0, 10, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventService, times(1)).getShortEventsBy(any(EventFilter.class));
        verify(statsClient, times(2)).endpointHit(anyString(), anyString(), anyString());
    }
}
