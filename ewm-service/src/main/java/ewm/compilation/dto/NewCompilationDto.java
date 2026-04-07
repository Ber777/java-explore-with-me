package ewm.compilation.dto;

import lombok.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    @NotBlank(message = "Заголовок не должен быть пустым")
    @Size(min = 1, max = 50, message = "Длина заголовка должна быть от 1 до 50 символов")
    private String title;

    @Builder.Default
    private Boolean pinned = false;

    @Builder.Default
    @NotNull(message = "Список событий не может быть null")
    private Set<@NotNull(message = "Идентификатор события не может быть пустым") Long> events = Set.of();
}
