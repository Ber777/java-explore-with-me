package ewm.request.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResponse {
    private List<UserRequestDto> confirmedRequests;
    private List<UserRequestDto> rejectedRequests;
}