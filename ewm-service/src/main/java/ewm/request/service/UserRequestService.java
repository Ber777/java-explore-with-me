package ewm.request.service;

import ewm.request.dto.*;

import java.util.List;

public interface UserRequestService {
    UserRequestDto createUserRequest(Long userId, Long eventId);

    List<UserRequestDto> getUserRequests(Long userId);

    List<UserRequestDto> getRequestsForEvent(Long eventId, Long userId);

    UserRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResponse updateRequestStatus(Long userId,
                                                         Long eventId,
                                                         EventRequestStatusUpdateDto request);
}