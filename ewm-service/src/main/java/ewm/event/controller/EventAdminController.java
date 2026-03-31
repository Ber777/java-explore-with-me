package ewm.event.controller;

import ewm.event.dto.*;
import ewm.event.model.*;
import ewm.event.service.EventService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ewm.Constants.DATE_TIME_FORMAT;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class EventAdminController {
    private final EventService eventService;

    @GetMapping
    public Collection<EventDtoResponse> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Запрос от админа: получить все события");
        EventAdminFilter filter = EventAdminFilter.builder()
                .users(users)
                .categories(categories)
                .states(states)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(offset)
                .size(limit)
                .build();
        return eventService.getFullEventsBy(filter);
    }

    @PatchMapping("/{eventId}")
    public EventDtoResponse updateEvent(
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventUpdateAdminDto eventDto) {
        log.info("Запрос от админа: обновить событие:{}", eventId);
        return eventService.updateEventByAdmin(eventId, eventDto);
    }
}

