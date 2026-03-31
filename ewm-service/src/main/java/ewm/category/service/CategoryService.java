package ewm.category.service;

import ewm.category.dto.*;

import java.util.Collection;

public interface CategoryService {
    Collection<CategoryDtoResponse> getAllCategories(Integer offset, Integer limit);

    CategoryDtoResponse getCategory(Long id);

    CategoryDtoResponse createCategory(CategoryDto categoryDto);

    CategoryDtoResponse updateCategory(Long id, CategoryDto categoryDto);

    void deleteCategory(Long id);
}