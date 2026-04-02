package server.mapper;

import dto.EndpointHitDto;
import server.model.EndpointHit;

public class EndpointHitMapper {
    public static EndpointHit toEndpointHit(EndpointHitDto endpointHitDto) {
        return EndpointHit.builder()
                .app(endpointHitDto.getApp())
                .ip(endpointHitDto.getIp())
                .timestamp(endpointHitDto.getTimestamp())
                .uri(endpointHitDto.getUri())
                .build();
    }
}
