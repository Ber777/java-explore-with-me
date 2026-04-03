package ewm.category;

import ewm.category.dto.*;
import ewm.category.service.CategoryService;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public Collection<CategoryDtoResponse> getCategories(
            @RequestParam(name = "from", defaultValue = "0") @Min(0) Integer offset,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) Integer limit
    ) {
        return categoryService.getAllCategories(offset, limit);
    }

    @GetMapping("/categories/{id}")
    public CategoryDtoResponse getCategory(@PathVariable @Min(1) Long id) {
        return categoryService.getCategory(id);
    }

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtoResponse createCategory(@Validated @RequestBody CategoryDto categoryDto) {
        log.info("Создание категории '{}' админом", categoryDto.getName());
        return categoryService.createCategory(categoryDto);
    }

    @PatchMapping("/admin/categories/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryDtoResponse updateCategory(@Validated @RequestBody CategoryDto categoryDto, @PathVariable Long id) {
        log.info("Обновление категории с id:{} админом", id);
        return categoryService.updateCategory(id, categoryDto);
    }

    @DeleteMapping("/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        log.info("Удаление категории с id:{} админом", id);
        categoryService.deleteCategory(id);
    }
}
