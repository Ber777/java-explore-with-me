package ewm.event.controller;

import ewm.event.dto.*;
import ewm.event.service.EventService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class EventPrivateController {
    private final EventService eventService;

    // Получение событий, добавленных текущим пользователем
    @GetMapping("/{userId}/events")
    public Collection<EventShortDtoResponse> getEventsCreatedByUser(
            @PathVariable @Min(1) Long userId,
            @RequestParam(name = "from", defaultValue = "0") @Min(0) Integer offset,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) Integer limit) {

        log.info("Запрос от пользователя: получить все события созданные пользователем с id:{}", userId);

        return eventService.getByUserId(userId, offset, limit);
    }

    // Добавление нового события
    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtoResponse createEvent(@PathVariable @Min(1) Long userId,
                                   @RequestBody @Valid EventDto eventDto) {
        log.info("Запрос от пользователя: создание нового события: {}", eventDto);
        return eventService.createEvent(userId, eventDto);
    }

    // Обновление события
    @PatchMapping("/{userId}/events/{eventId}")
    public EventDtoResponse updateEvent(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventUpdateDto eventDto) {
        log.info("Запрос от пользователя: обновить событие: {}", eventDto);
        return eventService.updateEventByUser(userId, eventId, eventDto);
    }

    // Получение события по ID
    @GetMapping("/{userId}/events/{eventId}")
    public EventDtoResponse getEventById(@PathVariable @Min(1) Long userId,
                                    @PathVariable @Min(1) Long eventId) {
        log.info("Запрос от пользователя: получить событие: {}", eventId);
        return eventService.getEvent(userId, eventId);
    }
}

