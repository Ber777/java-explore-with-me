package ewm.event;

import ewm.event.dto.*;
import ewm.exception.*;
import ewm.event.model.EventState;
import ewm.event.service.EventService;
import ewm.event.controller.EventPrivateController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class EventPrivateControllerTests {

    @InjectMocks
    private EventPrivateController eventPrivateController;

    @Mock
    private EventService eventService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventPrivateController)
                .setControllerAdvice(new ErrorHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void shouldGetEventsByUser() throws Exception {
        Long userId = 1L;
        Integer offset = 0;
        Integer limit = 10;

        EventShortDtoResponse eventDto = EventShortDtoResponse.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test Annotation")
                .confirmedRequests(5)
                .views(100)
                .build();

        when(eventService.getByUserId(userId, offset, limit))
                .thenReturn(List.of(eventDto));

        mockMvc.perform(get("/users/{userId}/events", userId)
                        .param("from", offset.toString())
                        .param("size", limit.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"))
                .andExpect(jsonPath("$[0].confirmedRequests").value(5));

        verify(eventService, times(1)).getByUserId(userId, offset, limit);
    }

    @Test
    void shouldThrowValidationErrorForInvalidUserIdInCreateEvent() throws Exception {
        Long invalidUserId = 0L;

        EventDto eventDto = EventDto.builder().title("Test").build();

        mockMvc.perform(post("/users/{userId}/events", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldThrowValidationErrorForInvalidEventData() throws Exception {
        Long userId = 1L;

        EventDto invalidEventDto = EventDto.builder()
                .title("") // пустое название — нарушение валидации
                .build();

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateEventSuccessfully() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;

        EventUpdateDto updateDto = EventUpdateDto.builder()
                .title("Updated Title")
                .build();

        EventDtoResponse responseDto = EventDtoResponse.builder()
                .id(eventId)
                .title("Updated Title")
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(eventService.updateEventByUser(
                eq(userId),
                eq(eventId),
                any(EventUpdateDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Updated Title"));

        // В verify также используем матчеры для корректной проверки вызовов
        verify(eventService, times(1)).updateEventByUser(
                eq(userId),
                eq(eventId),
                any(EventUpdateDto.class)
        );
    }

    @Test
    void shouldGetEventByIdSuccessfully() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;

        EventDtoResponse responseDto = EventDtoResponse.builder()
                .id(eventId)
                .title("Test Event")
                .description("Description")
                .state(EventState.PENDING)
                .views(100)
                .confirmedRequests(5)
                .build();

        when(eventService.getEvent(userId, eventId)).thenReturn(responseDto);

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.views").value(100))
                .andExpect(jsonPath("$.confirmedRequests").value(5));

        verify(eventService, times(1)).getEvent(userId, eventId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenEventDoesNotExist() throws Exception {
        Long userId = 1L;
        Long nonExistentEventId = 999L;

        when(eventService.getEvent(userId, nonExistentEventId))
                .thenThrow(new NotFoundException("Событие", nonExistentEventId));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, nonExistentEventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHandleValidationExceptionsProperly() throws Exception {
        Long userId = 1L;

        EventDto invalidEventDto = EventDto.builder()
                .title("") // пустое название — нарушение валидации
                .eventDate(LocalDateTime.now().minusDays(1)) // прошедшая дата
                .build();

        mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"));
    }

    @Test
    void shouldReturnHandleNotFoundExceptionProperly() throws Exception {
        Long userId = 1L;
        Long nonExistentEventId = 999L;

        when(eventService.getEvent(userId, nonExistentEventId))
                .thenThrow(new NotFoundException("Событие", nonExistentEventId));

        mockMvc.perform(get("/users/{userId}/events/{eventId}", userId, nonExistentEventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Событие с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnHandleNoAccessExceptionProperly() throws Exception {
        Long userId = 1L;
        Long eventId = 1L;

        EventUpdateDto updateDto = EventUpdateDto.builder().title("Update").build();

        doThrow(new NoAccessException("Нет доступа к событию"))
                .when(eventService).updateEventByUser(
                        eq(userId),
                        eq(eventId),
                        any(EventUpdateDto.class)
                );

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Нет доступа к событию"))
                .andExpect(jsonPath("$.reason").value("Нет доступа"));
    }
}
