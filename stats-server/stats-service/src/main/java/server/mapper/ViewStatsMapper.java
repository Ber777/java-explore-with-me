package server.mapper;

import dto.ViewStatsDto;

public class ViewStatsMapper {
    public static ViewStatsDto toStatsDto(String app, String uri, Long hits) {
        return ViewStatsDto.builder()
                .app(app)
                .uri(uri)
                .hits(hits)
                .build();
    }
}
