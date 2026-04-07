package ewm.category.service;

import ewm.exception.*;
import ewm.category.dto.*;
import ewm.category.model.Category;
import ewm.category.CategoryMapper;
import ewm.event.repository.EventRepository;
import ewm.category.repository.CategoryRepository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    public Collection<CategoryDtoResponse> getAllCategories(Integer offset, Integer limit) {
        Collection<Category> categories = categoryRepository.findWithOffsetAndLimit(offset, limit);
        return categories.stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDtoResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория", id));

        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDtoResponse createCategory(CategoryDto categoryDto) {
        Category category = CategoryMapper.fromCategoryDto(categoryDto);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public CategoryDtoResponse updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория", id));

        category.setName(categoryDto.getName());
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Категория", id);
        }

        if (eventRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Невозможно удалить категорию. В ней есть события.");
        }

        categoryRepository.deleteById(id);
    }
}
