package ewm.request;

import ewm.exception.*;
import ewm.request.dto.*;
import ewm.request.dto.UserRequestDto;
import ewm.request.service.UserRequestService;
import ewm.request.controller.UserRequestPrivateController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
class UserRequestPrivateControllerTests {

    @Mock
    private UserRequestService requestService;

    @InjectMocks
    private UserRequestPrivateController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnOkOnUpdate() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        EventRequestStatusUpdateDto requestDto = EventRequestStatusUpdateDto.builder()
                .requestIds(List.of(1L, 2L))
                .status("CONFIRMED")
                .build();

        EventRequestStatusUpdateResponse response = EventRequestStatusUpdateResponse.builder()
                .confirmedRequests(List.of(
                        UserRequestDto.builder().id(1L).status("CONFIRMED").build()
                ))
                .rejectedRequests(List.of())
                .build();

        when(requestService.updateRequestStatus(
                eq(userId),
                eq(eventId),
                any(EventRequestStatusUpdateDto.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.length()").value(1))
                .andExpect(jsonPath("$.rejectedRequests.length()").value(0))
                .andExpect(jsonPath("$.confirmedRequests[0].status").value("CONFIRMED"));
    }

    @Test
    void shouldReturnBadRequestWithInvalidStatus() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        EventRequestStatusUpdateDto requestDto = EventRequestStatusUpdateDto.builder()
                .requestIds(List.of(1L))
                .status("INVALID_STATUS") // Некорректный статус
                .build();

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", userId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Допустимые значения для поля 'status': CONFIRMED или REJECTED"));
    }

    @Test
    void shouldReturnList() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;
        List<UserRequestDto> requests = List.of(
                UserRequestDto.builder().id(1L).event(eventId).requester(userId).status("PENDING").build(),
                UserRequestDto.builder().id(2L).event(eventId).requester(userId).status("CONFIRMED").build()
        );

        when(requestService.getRequestsForEvent(eventId, userId)).thenReturn(requests);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        Long userId = 1L;
        Long eventId = 999L;

        doThrow(new NotFoundException("Событие", eventId))
                .when(requestService).getRequestsForEvent(eventId, userId);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Событие с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnForbidden() throws Exception {
        Long userId = 2L; // другой пользователь
        Long eventId = 100L;

        doThrow(new NoAccessException("Только создатель может смотреть запросы события"))
                .when(requestService).getRequestsForEvent(eventId, userId);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Только создатель может смотреть запросы события"))
                .andExpect(jsonPath("$.reason").value("Нет доступа"));
    }

    @Test
    void shouldReturnConflict() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        doThrow(new ConditionNotMetException("Событие должно быть опубликовано"))
                .when(requestService).getRequestsForEvent(eventId, userId);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Событие должно быть опубликовано"))
                .andExpect(jsonPath("$.reason").value("Условие не выполнено"));
    }
}
