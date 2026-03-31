package ewm.request.controller;

import ewm.request.dto.*;
import ewm.request.service.UserRequestService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/requests")
public class UserRequestPrivateController {
    private final UserRequestService requestService;

    @PatchMapping
    public EventRequestStatusUpdateResponse updateRequestStatuses(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateDto request) {
        log.info("PATCH /users/{}/events/{}/requests with body {}", userId, eventId, request);
        return requestService.updateRequestStatus(userId, eventId, request);
    }

    @GetMapping
    public List<UserRequestDto> getRequests(@PathVariable @Min(1) Long userId,
                                            @PathVariable @Min(1) Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return requestService.getRequestsForEvent(eventId, userId);
    }
}
