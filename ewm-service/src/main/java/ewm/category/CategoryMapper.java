package ewm.category;

import ewm.category.dto.*;
import ewm.category.model.Category;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CategoryMapper {
    public static CategoryDtoResponse toCategoryDto(Category category) {
        return new CategoryDtoResponse(category.getId(), category.getName());
    }

    public static Category fromCategoryDto(CategoryDto dto) {
        return new Category(null, dto.getName());
    }
}
