package ewm.event;

import ewm.exception.*;
import ewm.event.dto.*;
import ewm.event.model.*;
import ewm.event.service.EventService;
import ewm.event.model.EventAdminFilter;
import ewm.event.controller.EventAdminController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class EventAdminControllerTests {

    @InjectMocks
    private EventAdminController eventAdminController;

    @Mock
    private EventService eventService;

    // Вспомогательный метод для создания тестового DTO события
    private EventDtoResponse getEventDtoResponse() {
        return EventDtoResponse.builder()
                .id(1L)
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

    // Вспомогательный метод для создания DTO обновления события
    private EventUpdateAdminDto getEventUpdateAdminDto() {
        return EventUpdateAdminDto.builder()
                .title("Updated Title")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .categoryId(1L)
                .eventDate(LocalDateTime.now().plusDays(2))
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .stateAction(StateAction.PUBLISH_EVENT)
                .build();
    }

    @Test
    void shouldGetEventsWithFullParameters() {
        List<Long> users = List.of(1L, 2L);
        List<Long> categories = List.of(3L, 4L);
        List<EventState> states = List.of(EventState.PUBLISHED, EventState.PENDING);
        LocalDateTime rangeStart = LocalDateTime.now();
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(7);
        Long locationId = 5L;
        Double lat = 55.751244;
        Double lon = 37.618423;
        Double radius = 10.0;
        Integer offset = 5;
        Integer limit = 20;

        EventDtoResponse event = getEventDtoResponse();
        List<EventDtoResponse> events = List.of(event);

        when(eventService.getFullEventsBy(any())).thenReturn(events);

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                users, categories, states, rangeStart, rangeEnd, locationId, lat, lon, radius, offset, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(event.getId(), result.iterator().next().getId());

        ArgumentCaptor<EventAdminFilter> filterCaptor = ArgumentCaptor.forClass(EventAdminFilter.class);
        verify(eventService, times(1)).getFullEventsBy(filterCaptor.capture());

        EventAdminFilter capturedFilter = filterCaptor.getValue();
        assertEquals(users, capturedFilter.getUsers());
        assertEquals(categories, capturedFilter.getCategories());
        assertEquals(states, capturedFilter.getStates());
        assertEquals(rangeStart, capturedFilter.getRangeStart());
        assertEquals(rangeEnd, capturedFilter.getRangeEnd());
        assertEquals(locationId, capturedFilter.getLocationId());
        assertEquals(offset, capturedFilter.getFrom());
        assertEquals(limit, capturedFilter.getSize());
        assertNotNull(capturedFilter.getZone());
        assertEquals(lat, capturedFilter.getZone().getLatitude());
        assertEquals(lon, capturedFilter.getZone().getLongitude());
        assertEquals(radius, capturedFilter.getZone().getRadius());
    }

    @Test
    void shouldUseDefaultValuesWhenParametersNotProvided() {
        EventDtoResponse event = getEventDtoResponse();
        when(eventService.getFullEventsBy(any(EventAdminFilter.class)))
                .thenReturn(List.of(event));

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                null, null, null, null, null, null, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<EventAdminFilter> filterCaptor = ArgumentCaptor.forClass(EventAdminFilter.class);
        verify(eventService, times(1)).getFullEventsBy(filterCaptor.capture());

        EventAdminFilter capturedFilter = filterCaptor.getValue();

        assertNull(capturedFilter.getUsers());
        assertNull(capturedFilter.getCategories());
        assertNull(capturedFilter.getStates());
        assertNull(capturedFilter.getRangeStart());
        assertNull(capturedFilter.getRangeEnd());
        assertNull(capturedFilter.getLocationId());
        assertNull(capturedFilter.getZone());
        assertEquals(0, capturedFilter.getFrom());
        assertEquals(10, capturedFilter.getSize());
    }


    @Test
    void shouldHandleEmptyEventsList() {
        when(eventService.getFullEventsBy(any(EventAdminFilter.class)))
                .thenReturn(Collections.emptyList());

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                null, null, null, null, null, null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventService, times(1)).getFullEventsBy(any(EventAdminFilter.class));
    }

    @Test
    void shouldReturnEventsWithLocationFilter() {
        Double lat = 55.751244;
        Double lon = 37.618423;
        Double radius = 5.0;

        EventDtoResponse event = getEventDtoResponse();
        when(eventService.getFullEventsBy(any(EventAdminFilter.class)))
                .thenReturn(List.of(event));

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                null, null, null, null, null, null, lat, lon, radius, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<EventAdminFilter> filterCaptor = ArgumentCaptor.forClass(EventAdminFilter.class);
        verify(eventService, times(1)).getFullEventsBy(filterCaptor.capture());

        EventAdminFilter capturedFilter = filterCaptor.getValue();
        assertNotNull(capturedFilter.getZone());
        assertEquals(lat, capturedFilter.getZone().getLatitude());
        assertEquals(lon, capturedFilter.getZone().getLongitude());
        assertEquals(radius, capturedFilter.getZone().getRadius());
    }

    @Test
    void shouldUpdateEventSuccessfully() {
        Long eventId = 1L;
        EventUpdateAdminDto updateDto = getEventUpdateAdminDto();

        EventDtoResponse updatedEvent = EventDtoResponse.builder()
                .id(eventId)
                .title(updateDto.getTitle())
                .annotation(updateDto.getAnnotation())
                .description(updateDto.getDescription())
                .eventDate(updateDto.getEventDate())
                .paid(updateDto.getPaid())
                .participantLimit(updateDto.getParticipantLimit())
                .requestModeration(updateDto.getRequestModeration())
                .state(EventState.PUBLISHED) // предполагаемое состояние после обновления
                .views(0)
                .build();

        when(eventService.updateEventByAdmin(eventId, updateDto)).thenReturn(updatedEvent);

        EventDtoResponse result = eventAdminController.updateEvent(eventId, updateDto);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Updated Title", result.getTitle());
        assertEquals(updateDto.getAnnotation(), result.getAnnotation());
        assertEquals(updateDto.getDescription(), result.getDescription());
        assertEquals(updateDto.getEventDate(), result.getEventDate());
        assertEquals(updateDto.getPaid(), result.getPaid());
        assertEquals(updateDto.getParticipantLimit(), result.getParticipantLimit());
        assertEquals(updateDto.getRequestModeration(), result.getRequestModeration());
        assertEquals(EventState.PUBLISHED, result.getState());

        verify(eventService, times(1)).updateEventByAdmin(eventId, updateDto);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventNotExistsForUpdate() {
        Long eventId = 999L;
        EventUpdateAdminDto updateDto = getEventUpdateAdminDto();

        when(eventService.updateEventByAdmin(eventId, updateDto))
                .thenThrow(new NotFoundException("Событие", eventId));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventAdminController.updateEvent(eventId, updateDto));

        assertEquals("Событие с id: 999 не найден(-а)", exception.getMessage());
        verify(eventService, times(1)).updateEventByAdmin(eventId, updateDto);
    }

    @Test
    void shouldGetEventsWithMinimalParameters() {
        EventDtoResponse event = getEventDtoResponse();
        when(eventService.getFullEventsBy(any(EventAdminFilter.class)))
                .thenReturn(List.of(event));

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                null, null, null, null, null, null, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<EventAdminFilter> filterCaptor = ArgumentCaptor.forClass(EventAdminFilter.class);
        verify(eventService, times(1)).getFullEventsBy(filterCaptor.capture());

        EventAdminFilter capturedFilter = filterCaptor.getValue();
        assertNull(capturedFilter.getUsers());
        assertNull(capturedFilter.getCategories());
        assertNull(capturedFilter.getStates());
        assertNull(capturedFilter.getRangeStart());
        assertNull(capturedFilter.getRangeEnd());
        assertEquals(0, capturedFilter.getFrom());
        assertEquals(10, capturedFilter.getSize());
    }

    @Test
    void shouldGetEventsWithLocationIdFilter() {
        Long locationId = 5L;

        EventDtoResponse event = getEventDtoResponse();
        when(eventService.getFullEventsBy(any(EventAdminFilter.class)))
                .thenReturn(List.of(event));

        Collection<EventDtoResponse> result = eventAdminController.getEvents(
                null, null, null, null, null, locationId, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<EventAdminFilter> filterCaptor = ArgumentCaptor.forClass(EventAdminFilter.class);
        verify(eventService, times(1)).getFullEventsBy(filterCaptor.capture());

        EventAdminFilter capturedFilter = filterCaptor.getValue();
        assertEquals(locationId, capturedFilter.getLocationId());
        assertNull(capturedFilter.getZone());
    }
}
