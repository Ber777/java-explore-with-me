package ewm.category;

import ewm.exception.*;
import ewm.category.dto.*;
import ewm.category.service.CategoryService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static junit.framework.Assert.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnBadRequestWithNegativeOffset() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Нарушение валидации"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithZeroSize() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Нарушение валидации"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnInternalServerErrorWithInvalidOffsetType() throws Exception {
        mockMvc.perform(get("/categories")
                        .param("from", "invalid")
                        .param("size", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void shouldNotReturnValidationErrorWithMissingFromParameter() throws Exception {
        when(categoryService.getAllCategories(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/categories")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.contains("\"reason\":\"Нарушение валидации\""));
                    assertFalse(content.contains("\"status\":\"BAD_REQUEST\""));
                    assertFalse(content.contains("\"reason\":\"Внутренняя ошибка сервера\""));
                    assertFalse(content.contains("\"status\":\"INTERNAL_SERVER_ERROR\""));
                });
    }

    @Test
    void shouldReturnOk() throws Exception {
        when(categoryService.getAllCategories(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnBadRequestWithNegativeId() throws Exception {
        mockMvc.perform(get("/categories/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Нарушение валидации"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithZeroId() throws Exception {
        mockMvc.perform(get("/categories/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Нарушение валидации"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnNotFoundWithNonExistentId() throws Exception {
        when(categoryService.getCategory(anyLong()))
                .thenThrow(new NotFoundException("Категория", 999L));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Категория с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnOkCategory() throws Exception {
        CategoryDtoResponse category = new CategoryDtoResponse(1L, "Test Category");
        when(categoryService.getCategory(eq(1L))).thenReturn(category);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Category"));
    }

    @Test
    void shouldReturnBadRequestWithEmptyName() throws Exception {
        CategoryDto categoryDto = new CategoryDto("");

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithNameTooLong() throws Exception {
        String longName = "A".repeat(51); // 51 символ
        CategoryDto categoryDto = new CategoryDto(longName);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnCreated() throws Exception {
        CategoryDto categoryDto = new CategoryDto("New Category");
        CategoryDtoResponse response = new CategoryDtoResponse(1L, "New Category");

        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("New Category"));
    }

    @Test
    void shouldProcessWithoutValidationWithNegativeId() throws Exception {
        CategoryDto categoryDto = new CategoryDto("Updated Category");
        CategoryDtoResponse mockResponse = new CategoryDtoResponse(-1L, "Updated Category");

        when(categoryService.updateCategory(eq(-1L), any(CategoryDto.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(patch("/admin/categories/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(-1L))
                .andExpect(jsonPath("$.name").value("Updated Category"))
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.contains("\"reason\":\"Нарушение валидации\""));
                    assertFalse(content.contains("\"status\":\"BAD_REQUEST\""));
                });
    }

    @Test
    void shouldNotReturnValidationError() throws Exception {
        CategoryDto categoryDto = new CategoryDto("Updated Category");
        CategoryDtoResponse mockResponse = new CategoryDtoResponse(0L, "Updated Category");

        when(categoryService.updateCategory(eq(0L), any(CategoryDto.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(patch("/admin/categories/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.contains("\"message\""));
                    assertFalse(content.contains("\"reason\":\"Нарушение валидации\""));
                    assertFalse(content.contains("\"status\":\"BAD_REQUEST\""));
                });
    }

    @Test
    void shouldReturnNotFoundWithNonExistentIdOnUpdate() throws Exception {
        CategoryDto categoryDto = new CategoryDto("Updated Category");
        when(categoryService.updateCategory(eq(999L), any(CategoryDto.class)))
                .thenThrow(new NotFoundException("Категория", 999L));

        mockMvc.perform(patch("/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Категория с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnBadRequestWithEmptyNameOnUpdate() throws Exception {
        CategoryDto categoryDto = new CategoryDto("");

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnBadRequestWithTooLongName() throws Exception {
        String longName = "A".repeat(51); // 51 символ
        CategoryDto categoryDto = new CategoryDto(longName);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.reason").value("Ошибка валидации аргумента метода"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnOkOnUpdate() throws Exception {
        CategoryDto categoryDto = new CategoryDto("Updated Category");
        CategoryDtoResponse response = new CategoryDtoResponse(1L, "Updated Category");

        when(categoryService.updateCategory(eq(1L), any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Updated Category"));
    }

    @Test
    void shouldNotReturnValidationErrorOnDelete() throws Exception {
        doNothing().when(categoryService).deleteCategory(eq(-1L));

        mockMvc.perform(delete("/admin/categories/-1"))
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.contains("\"message\""));
                    assertFalse(content.contains("\"reason\":\"Нарушение валидации\""));
                    assertFalse(content.contains("\"status\":\"BAD_REQUEST\""));
                });
    }

    @Test
    void shouldNotReturnValidationErrorOnDeleteWithZeroId() throws Exception {
        doNothing().when(categoryService).deleteCategory(eq(0L));

        mockMvc.perform(delete("/admin/categories/0"))
                .andExpect(status().isNoContent())
                // Проверяем, что ответ не содержит полей ошибки валидации
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertFalse(content.contains("\"message\""));
                    assertFalse(content.contains("\"reason\":\"Нарушение валидации\""));
                    assertFalse(content.contains("\"status\":\"BAD_REQUEST\""));
                });
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Категория", 999L))
                .when(categoryService).deleteCategory(eq(999L));

        mockMvc.perform(delete("/admin/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Категория с id: 999 не найден(-а)"))
                .andExpect(jsonPath("$.reason").value("Запрашиваемый объект не найден"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(eq(1L));

        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());
    }
}