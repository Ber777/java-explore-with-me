package ewm.category.dto;

import lombok.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    @NotBlank
    @Size(min = 1, max = 50, message = "Длина от 1 до 50 символов")
    public String name;
}
