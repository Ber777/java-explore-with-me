package ewm.location.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LocationDtoResponse {
    private Long id;
    private String name;
    private String address;

    @JsonProperty(value = "lat")
    private Double latitude;
    @JsonProperty(value = "lon")
    private Double longitude;
}
