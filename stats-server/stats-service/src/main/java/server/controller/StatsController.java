package server.controller;

import dto.ViewStatsDto;
import dto.EndpointHitDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import server.service.EndpointHitService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Validated
@RestController
public class StatsController {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final EndpointHitService endpointHitService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitDto createEndpointHit(@RequestBody @Valid EndpointHitDto endpointHitDto) {
        return endpointHitService.createEndpointHit(endpointHitDto);
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public List<ViewStatsDto> getStats(@RequestParam("start") @NotNull @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime start,
                                       @RequestParam("end") @NotNull @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime end,
                                       @RequestParam(value = "uris", required = false, defaultValue = "") List<String> uris,
                                       @RequestParam(value = "unique", required = false, defaultValue = "false") Boolean unique) {
        return endpointHitService.getStats(start, end, uris, unique);
    }
}
