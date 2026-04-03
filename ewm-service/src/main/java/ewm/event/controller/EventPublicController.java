package ewm.event.controller;

import client.*;
import ewm.event.dto.*;
import ewm.event.model.EventFilter;
import ewm.event.model.EventState;
import ewm.event.service.EventService;
import ewm.exception.InvalidRequestException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static ewm.Constants.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventPublicController {
    private final EventService eventService;
    private final StatsClient statsClient;

    @Setter
    @Value("${spring.application.name:ewm}")
    private String appName;

    // Получение событий с возможностью фильтрации
    @GetMapping
    public Collection<EventShortDtoResponse> getEvents(
            @Size(min = 3, max = 1000, message = "Текст должен быть длиной от 3 до 1000 символов")
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        EventFilter filter = EventFilter.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .state(EventState.PUBLISHED)
                .build();

        if (filter.getRangeStart() != null && filter.getRangeEnd() != null
                && filter.getRangeStart().isAfter(filter.getRangeEnd())) {
            throw new InvalidRequestException("Дата начала должна быть раньше, чем дата окончания.");
        }

        Collection<EventShortDtoResponse> events = eventService.getShortEventsBy(filter);

        Collection<Long> ids = events.stream()
                .map(EventShortDtoResponse::getId)
                .toList();

        writeStatisticsByIds(ids, request.getRemoteAddr());
        writeStatisticsByUris(List.of("/events"), request.getRemoteAddr());

        return events;
    }

    @GetMapping("/{eventId}")
    public EventDtoResponse get(@PathVariable @Min(1) Long eventId,
                           HttpServletRequest request) {
        log.debug("Запрос для публикации события с id:{}", eventId);
        EventDtoResponse dtoOut = eventService.getPublished(eventId);

        writeStatisticsByIds(List.of(eventId), request.getRemoteAddr());

        return dtoOut;
    }

    private void writeStatisticsByUris(Collection<String> uris, String ip) {
        try {
            for (String uri : uris)
                statsClient.endpointHit(appName, uri, ip);

        } catch (StatsClientException ex) {
            log.error(ex.getMessage());
        }
    }

    private void writeStatisticsByIds(Collection<Long> ids, String ip) {
        writeStatisticsByUris(ids.stream().map(id -> STATS_EVENTS_URL + id).toList(), ip);
    }
}

