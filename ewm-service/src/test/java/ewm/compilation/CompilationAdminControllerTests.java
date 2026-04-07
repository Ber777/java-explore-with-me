package ewm.compilation;

import ewm.exception.*;
import ewm.compilation.dto.CompilationDto;
import ewm.event.dto.EventShortDtoResponse;
import ewm.compilation.dto.CompilationUpdateDto;
import ewm.compilation.service.CompilationService;
import ewm.compilation.controller.CompilationAdminController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
public class CompilationAdminControllerTests {

    @Mock
    private CompilationService compilationService;

    @InjectMocks
    private CompilationAdminController compilationAdminController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(compilationAdminController)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void shouldReturnOkOnUpdate() throws Exception {
        Long compId = 1L;
        Set<Long> events = new HashSet<>(Set.of(1L, 2L, 3L));
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title("Updated Title")
                .pinned(false)
                .events(events)
                .build();

        // Создаём корректные объекты EventShortDtoResponse
        List<EventShortDtoResponse> eventResponses = List.of(
                EventShortDtoResponse.builder().id(1L).title("Event 1").build(),
                EventShortDtoResponse.builder().id(2L).title("Event 2").build(),
                EventShortDtoResponse.builder().id(3L).title("Event 3").build()
        );

        CompilationDto updatedCompilation = CompilationDto.builder()
                .id(compId)
                .title("Updated Title")
                .pinned(false)
                .events(eventResponses)
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(compilationService.updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        )).thenReturn(updatedCompilation);

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.pinned").value(false))
                .andExpect(jsonPath("$.events", hasSize(3)))
                .andExpect(jsonPath("$.events[0].id").value(1L))
                .andExpect(jsonPath("$.events[1].id").value(2L))
                .andExpect(jsonPath("$.events[2].id").value(3L));

        // В verify также используем матчеры для корректной проверки вызова
        verify(compilationService, times(1)).updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        );
    }

    @Test
    void shouldReturnBadRequestOnUpdate() throws Exception {
        long compId = 1L;
        CompilationUpdateDto invalidRequest = CompilationUpdateDto.builder()
                .title("")
                .pinned(true)
                .events(new HashSet<>(Set.of(1L)))
                .build();
        MvcResult result = mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Actual response body: " + responseBody);

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Длина заголовка должна быть от 1 до 50 символов"))
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithTooLongTittle() throws Exception {
        long compId = 1L;
        String longTitle = "A".repeat(51); // 51 символ — нарушает @Size(max=50)
        CompilationUpdateDto invalidRequest = CompilationUpdateDto.builder()
                .title(longTitle)
                .pinned(true)
                .events(new HashSet<>(Set.of(1L)))
                .build();

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Длина заголовка должна быть от 1 до 50 символов"));
    }

    @Test
    void shouldReturnSuccessOnUpdate() throws Exception {
        Long compId = 1L;
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title("Updated Title")
                .pinned(true)
                .events(null)
                .build();

        CompilationDto updatedCompilation = CompilationDto.builder()
                .id(compId)
                .title("Updated Title")
                .pinned(true)
                .events(List.of())
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(compilationService.updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        )).thenReturn(updatedCompilation);

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        // Проверяем, что метод был вызван с правильными аргументами
        verify(compilationService, times(1)).updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        );
    }

    @Test
    void shouldReturnSuccess() throws Exception {
        Long compId = 1L;
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title("Updated Title")
                .pinned(true)
                .events(new HashSet<>())
                .build();

        CompilationDto updatedCompilation = CompilationDto.builder()
                .id(compId)
                .title("Updated Title")
                .pinned(true)
                .events(Collections.emptyList()) // события очищены
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(compilationService.updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        )).thenReturn(updatedCompilation);

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.events").isEmpty());

        // Проверяем, что метод был вызван с правильными аргументами
        verify(compilationService, times(1)).updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        );
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title("Updated Title")
                .build();

        // Используем матчеры для корректного выброса исключения
        doThrow(new NotFoundException("Подборка", nonExistentId))
                .when(compilationService).updateCompilation(
                        eq(nonExistentId),
                        any(CompilationUpdateDto.class)
                );

        mockMvc.perform(patch("/admin/compilations/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        // Проверяем, что метод был вызван с правильными аргументами
        verify(compilationService, times(1)).updateCompilation(
                eq(nonExistentId),
                any(CompilationUpdateDto.class)
        );
    }

    @Test
    void shouldReturnInternalServerError() throws Exception {
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title("Updated Title")
                .build();

        MvcResult result = mockMvc.perform(patch("/admin/compilations/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Actual response body: " + responseBody);

        mockMvc.perform(patch("/admin/compilations/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"))
                .andExpect(jsonPath("$.reason").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void shouldReturnSuccessWithNullTitle() throws Exception {
        Long compId = 1L;
        CompilationUpdateDto updateRequest = CompilationUpdateDto.builder()
                .title(null) // null — допустимо, значит не обновлять заголовок
                .pinned(true)
                .events(new HashSet<>(Set.of(1L, 2L)))
                .build();

        CompilationDto updatedCompilation = CompilationDto.builder()
                .id(compId)
                .title("Existing Title")
                .pinned(true)
                .events(List.of())
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(compilationService.updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        )).thenReturn(updatedCompilation);

        mockMvc.perform(patch("/admin/compilations/" + compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compId))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.title").value("Existing Title"));

        // Проверяем, что метод был вызван с правильными аргументами
        verify(compilationService, times(1)).updateCompilation(
                eq(compId),
                any(CompilationUpdateDto.class)
        );
    }
}
