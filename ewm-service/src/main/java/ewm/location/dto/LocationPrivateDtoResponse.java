package ewm.location.dto;

import ewm.location.model.LocationState;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPrivateDtoResponse {
    private Long id;
    private String name;
    private String address;

    @JsonProperty(value = "lat")
    private Double latitude;
    @JsonProperty(value = "lon")
    private Double longitude;

    private LocationState state;
}
