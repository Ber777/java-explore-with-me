package ewm.location.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewLocationDto {
    @NotBlank(message = "Локация не может быть пустой")
    @Size(min = 4, max = 64, message = "Длина названия локации должна быть минимум 4 и максимум 64 символа")
    private String name;

    private String address;

    @NotNull(message = "Широта не можеть быть пустой")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    private Double latitude;

    @NotNull(message = "Долгота не может быть пустой")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    private Double longitude;
}