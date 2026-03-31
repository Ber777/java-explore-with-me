package ewm.category;

import ewm.category.dto.*;
import ewm.category.model.Category;
import ewm.exception.NotFoundException;
import ewm.category.service.CategoryServiceImpl;
import ewm.event.repository.EventRepository;
import ewm.category.repository.CategoryRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private static final Long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "Test Category";

    @Test
    void shouldReturnListOfCategoryDtoResponseWhenCategoriesExist() {
        int offset = 0;
        int limit = 10;

        Category category = new Category(CATEGORY_ID, CATEGORY_NAME);
        List<Category> categories = List.of(category);

        when(categoryRepository.findWithOffsetAndLimit(offset, limit)).thenReturn(categories);

        Collection<CategoryDtoResponse> result = categoryService.getAllCategories(offset, limit);

        assertNotNull(result);
        assertEquals(1, result.size());

        CategoryDtoResponse dto = result.iterator().next();
        assertEquals(CATEGORY_ID, dto.getId());
        assertEquals(CATEGORY_NAME, dto.getName());

        verify(categoryRepository, times(1)).findWithOffsetAndLimit(offset, limit);
    }

    @Test
    void shouldReturnEmptyListWhenNoCategoriesFound() {
        int offset = 0;
        int limit = 10;

        when(categoryRepository.findWithOffsetAndLimit(offset, limit)).thenReturn(List.of());

        Collection<CategoryDtoResponse> result = categoryService.getAllCategories(offset, limit);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(categoryRepository, times(1)).findWithOffsetAndLimit(offset, limit);
    }

    @Test
    void shouldReturnCategoryDtoResponseWhenCategoryExists() {
        Category category = new Category(CATEGORY_ID, CATEGORY_NAME);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        CategoryDtoResponse result = categoryService.getCategory(CATEGORY_ID);

        assertNotNull(result);
        assertEquals(CATEGORY_ID, result.getId());
        assertEquals(CATEGORY_NAME, result.getName());

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
    }

    @Test
    void shouldCreateAndReturnCategoryDtoResponse() {
        CategoryDto categoryDto = new CategoryDto(CATEGORY_NAME);
        Category savedCategory = new Category(CATEGORY_ID, CATEGORY_NAME);

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryDtoResponse result = categoryService.createCategory(categoryDto);

        assertNotNull(result);
        assertEquals(CATEGORY_ID, result.getId());
        assertEquals(CATEGORY_NAME, result.getName());

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void shouldUpdateAndReturnCategoryDtoResponse() {
        String updatedName = "Updated Category";
        CategoryDto categoryDto = new CategoryDto(updatedName);
        Category existingCategory = new Category(CATEGORY_ID, CATEGORY_NAME);
        Category updatedCategory = new Category(CATEGORY_ID, updatedName);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        CategoryDtoResponse result = categoryService.updateCategory(CATEGORY_ID, categoryDto);

        assertNotNull(result);
        assertEquals(CATEGORY_ID, result.getId());
        assertEquals(updatedName, result.getName());
        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    void shouldDeleteCategoryWhenNoEventsInCategory() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(eventRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(false);

        categoryService.deleteCategory(CATEGORY_ID);

        verify(categoryRepository, times(1)).existsById(CATEGORY_ID);
        verify(eventRepository, times(1)).existsByCategoryId(CATEGORY_ID);
        verify(categoryRepository, times(1)).deleteById(CATEGORY_ID);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenCategoryDoesNotExist() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.deleteCategory(CATEGORY_ID));

        assertEquals("Категория с id: 1 не найден(-а)", exception.getMessage());
        verify(categoryRepository, times(1)).existsById(CATEGORY_ID);
        verify(eventRepository, never()).existsByCategoryId(anyLong());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenEventsExistInCategory() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(eventRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> categoryService.deleteCategory(CATEGORY_ID));

        assertEquals("Невозможно удалить категорию. В ней есть события.", exception.getMessage());
        verify(categoryRepository, times(1)).existsById(CATEGORY_ID);
        verify(eventRepository, times(1)).existsByCategoryId(CATEGORY_ID);
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}