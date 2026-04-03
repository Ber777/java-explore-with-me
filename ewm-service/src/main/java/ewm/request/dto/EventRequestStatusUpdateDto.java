package ewm.request.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdateDto {
    @NotEmpty(message = "Список id заявок не должен быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "Поле 'status' обязательно для заполнения")
    @Pattern(
            regexp = "CONFIRMED|REJECTED",
            message = "Допустимые значения для поля 'status': CONFIRMED или REJECTED"
    )
    private String status;
}
