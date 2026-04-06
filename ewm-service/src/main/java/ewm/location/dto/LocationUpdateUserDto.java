package ewm.location.dto;

import lombok.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@ToString
public class LocationUpdateUserDto {
    private String name;
    private String address;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty(value = "lat")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty(value = "lon")
    private Double longitude;
}
