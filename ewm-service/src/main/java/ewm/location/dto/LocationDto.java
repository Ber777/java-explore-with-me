package ewm.location.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Long id;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    private Double latitude;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    private Double longitude;
}
