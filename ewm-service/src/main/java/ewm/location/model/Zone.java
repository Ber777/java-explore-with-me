package ewm.location.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class Zone {
    private Double latitude;
    private Double longitude;
    private Double radius;
}
