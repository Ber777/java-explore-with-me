package ewm.location.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Zone {
    private Double latitude;
    private Double longitude;
    private Double radius;
}
