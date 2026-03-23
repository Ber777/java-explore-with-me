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

    public EndpointHit toEndpointHitDto(EndpointHitDto endpointHitDto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(endpointHitDto.getApp());
        hit.setUri(endpointHitDto.getUri());
        hit.setIp(endpointHitDto.getIp());
        hit.setTimestamp(endpointHitDto.getTimestamp());
        return hit;
    }
}
