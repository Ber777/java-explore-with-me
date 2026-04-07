package ewm.compilation.dto;

import lombok.*;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationUpdateDto {

    @Size(min = 1, max = 50, message = "Длина заголовка должна быть от 1 до 50 символов")
    private String title;

    private Boolean pinned;
    private Set<Long> events;
}
