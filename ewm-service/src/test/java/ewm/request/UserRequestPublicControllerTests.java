package ewm.request;

import ewm.exception.*;
import ewm.request.dto.UserRequestDto;
import ewm.request.service.UserRequestService;
import ewm.request.controller.UserRequestPublicController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
class UserRequestPublicControllerTests {

    @Mock
    private UserRequestService requestService;

    @InjectMocks
    private UserRequestPublicController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void shouldReturnCreated() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;
        UserRequestDto requestDto = UserRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .event(eventId)
                .requester(userId)
                .status("PENDING")
                .build();

        when(requestService.createUserRequest(userId, eventId)).thenReturn(requestDto);

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.event").value(eventId))
                .andExpect(jsonPath("$.requester").value(userId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundOnCreate() throws Exception {
        Long userId = 999L;
        Long eventId = 100L;

        doThrow(new NotFoundException("Пользователь", userId))
                .when(requestService).createUserRequest(userId, eventId);

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnBadRequest() throws Exception {
        Long userId = 1L;

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Параметр 'eventId' отсутствует"))
                .andExpect(jsonPath("$.reason").value("Обязательный параметр запроса отсутствует"));
    }

    @Test
    void shouldReturnCreatedOk() throws Exception {
        Long userId = 1L;
        List<UserRequestDto> requests = List.of(
                UserRequestDto.builder().id(1L).event(100L).requester(userId).status("CONFIRMED").build(),
                UserRequestDto.builder().id(2L).event(200L).requester(userId).status("PENDING").build()
        );

        when(requestService.getUserRequests(userId)).thenReturn(requests);

        mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        Long userId = 999L;

        doThrow(new NotFoundException("Пользователь", userId))
                .when(requestService).getUserRequests(userId);

        mockMvc.perform(get("/users/{userId}/requests", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnOk() throws Exception {
        Long userId = 1L;
        Long requestId = 10L;
        UserRequestDto canceledRequest = UserRequestDto.builder()
                .id(requestId)
                .event(100L)
                .requester(userId)
                .status("CANCELED")
                .build();

        when(requestService.cancelRequest(userId, requestId)).thenReturn(canceledRequest);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void shouldReturnNotFoundWithNonExistentRequest() throws Exception {
        Long userId = 1L;
        Long requestId = 999L;

        doThrow(new NotFoundException("Запрос", requestId))
                .when(requestService).cancelRequest(userId, requestId);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Запрос с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnForbidden() throws Exception {
        Long userId = 2L; // другой пользователь
        Long requestId = 10L;

        doThrow(new ForbiddenException("Только создатель события может его отменить."))
                .when(requestService).cancelRequest(userId, requestId);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", userId, requestId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Только создатель события может его отменить."))
                .andExpect(jsonPath("$.reason").value("В доступе отказано"));
    }

    @Test
    void shouldReturnConflict() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        doThrow(new ConditionNotMetException("Заявка на участие уже существует"))
                .when(requestService).createUserRequest(userId, eventId);

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Заявка на участие уже существует"))
                .andExpect(jsonPath("$.reason").value("Условие не выполнено"));
    }

    @Test
    void shouldReturnConflictWhenEventNotPublished() throws Exception {
        Long userId = 1L;
        Long eventId = 100L;

        doThrow(new ConditionNotMetException("Нельзя принять участие в неопубликованном событии"))
                .when(requestService).createUserRequest(userId, eventId);

        mockMvc.perform(post("/users/{userId}/requests", userId)
                        .param("eventId", eventId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Нельзя принять участие в неопубликованном событии"))
                .andExpect(jsonPath("$.reason").value("Условие не выполнено"));
    }
}
