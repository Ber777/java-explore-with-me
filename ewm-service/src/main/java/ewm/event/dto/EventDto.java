package ewm.event.dto;

import jakarta.validation.constraints.*;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

import static ewm.Constants.DATE_TIME_FORMAT;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    @NotBlank
    @Size(min = 3, max = 120, message = "Заголовок должен содержать минимум 3 и максимум 120 символов")
    private String title;

    @NotBlank
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать минимум 20 и максимум 2000 символов")
    private String annotation;

    @NotNull
    @JsonProperty("category")
    private Long categoryId;

    @NotBlank
    @Size(min = 20, max = 7000, message = "Описание должно содержать минимум 20 и максимум 7000 символов")
    private String description;

    @NotNull(message = "Дата события не можеть быть пустой")
    @Future(message = "Дата события должна быть в будущем")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @NotNull
    private LocationDto location;

    @Builder.Default
    private Boolean paid = false;

    @Min(0)
    @Builder.Default
    private Integer participantLimit = 0;

    @Builder.Default
    private Boolean requestModeration = true;
}
