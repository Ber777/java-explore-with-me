package ewm.compilation;

import ewm.exception.*;
import ewm.compilation.dto.CompilationDto;
import ewm.compilation.service.CompilationService;
import ewm.compilation.controller.CompilationPublicController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
class CompilationPublicControllerTests {

    @Mock
    private CompilationService compilationService;

    @InjectMocks
    private CompilationPublicController compilationPublicController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(compilationPublicController)
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void shouldReturnSuccess() throws Exception {
        Boolean pinned = true;
        int from = 0;
        int size = 10;

        CompilationDto compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .build();

        List<CompilationDto> expectedCompilations = List.of(compilationDto);

        when(compilationService.getCompilations(pinned, from, size))
                .thenReturn(expectedCompilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Compilation"))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void shouldReturnSuccessWithMissingPinnedParam() throws Exception {
        int from = 0;
        int size = 10;

        CompilationDto compilationDto = CompilationDto.builder().id(1L).build();
        List<CompilationDto> expectedCompilations = List.of(compilationDto);

        when(compilationService.getCompilations(null, from, size))
                .thenReturn(expectedCompilations);

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldReturnInternalServerError() throws Exception {
        MvcResult result = mockMvc.perform(get("/compilations")
                        .param("from", "abc")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Actual response body: " + responseBody);

        mockMvc.perform(get("/compilations")
                        .param("from", "abc")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"))
                .andExpect(jsonPath("$.reason").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnCompilation() throws Exception {
        Long compId = 1L;

        CompilationDto expectedDto = CompilationDto.builder()
                .id(compId)
                .title("Test Compilation")
                .pinned(false)
                .build();

        when(compilationService.getCompilationById(compId)).thenReturn(expectedDto);

        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compId))
                .andExpect(jsonPath("$.title").value("Test Compilation"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

    @Test
    void shouldThrowInternalServerError() throws Exception {
        MvcResult result = mockMvc.perform(get("/compilations/abc"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Actual response body: " + responseBody);

        mockMvc.perform(get("/compilations/abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Произошла непредвиденная ошибка"))
                .andExpect(jsonPath("$.reason").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        Long nonExistentId = 999L;

        doThrow(new NotFoundException("Подборка", nonExistentId))
                .when(compilationService).getCompilationById(nonExistentId);

        mockMvc.perform(get("/compilations/{compId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"));
    }

    @Test
    void shouldReturnSuccessWithMaxValues() throws Exception {
        int maxFrom = Integer.MAX_VALUE - 100;
        int maxSize = Integer.MAX_VALUE;

        CompilationDto compilationDto = CompilationDto.builder().id(1L).build();
        List<CompilationDto> expectedCompilations = List.of(compilationDto);

        when(compilationService.getCompilations(null, maxFrom, maxSize))
                .thenReturn(expectedCompilations);

        mockMvc.perform(get("/compilations")
                        .param("from", String.valueOf(maxFrom))
                        .param("size", String.valueOf(maxSize)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUseDefaults() throws Exception {
        CompilationDto compilationDto = CompilationDto.builder().id(1L).build();
        List<CompilationDto> expectedCompilations = List.of(compilationDto);

        when(compilationService.getCompilations(null, 0, 10))
                .thenReturn(expectedCompilations);

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}