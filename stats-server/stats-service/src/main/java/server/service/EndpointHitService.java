package server.service;

import dto.ViewStatsDto;
import dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitService {
    EndpointHitDto createEndpointHit(EndpointHitDto endpointHitDto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
