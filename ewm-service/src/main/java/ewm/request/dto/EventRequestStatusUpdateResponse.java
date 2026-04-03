package ewm.request.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdateResponse {
    private List<UserRequestDto> confirmedRequests;
    private List<UserRequestDto> rejectedRequests;
}