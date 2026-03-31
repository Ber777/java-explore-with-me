package ewm.request.controller;

import ewm.request.dto.UserRequestDto;
import ewm.request.service.UserRequestService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserRequestPublicController {
    private final UserRequestService requestService;

    @PostMapping("/{userId}/requests")
    public ResponseEntity<UserRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        UserRequestDto createdRequest = requestService.createUserRequest(userId, eventId);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}/requests")
    public ResponseEntity<List<UserRequestDto>> getUserRequests(@PathVariable Long userId) {
        List<UserRequestDto> requests = requestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<UserRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        UserRequestDto canceledRequest = requestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(canceledRequest);
    }
}