package server.service;

import dto.EndpointHitDto;
import dto.ViewStatsDto;
import server.repository.EndpointHitRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static server.mapper.EndpointHitMapper.*;
import static server.mapper.ViewStatsMapper.toStatsDto;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EndpointHitServiceImpl implements EndpointHitService {
    private final EndpointHitRepository endpointHitRepository;

    @Override
    @Transactional
    public EndpointHitDto createEndpointHit(EndpointHitDto endpointHitDto) {
        endpointHitRepository.save(toEndpointHit(endpointHitDto));
        return endpointHitDto;
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        List<ViewStatsDto> stats = new ArrayList<>();

        // Гарантируем, что uris не null
        if (uris == null) {
            uris = new ArrayList<>();
        }

        // Если список пуст, получаем все уникальные URI из БД за период
        if (uris.isEmpty()) {
            uris = endpointHitRepository.findAllEndpointHitBetweenDates(start, end);
        }

        for (String uri : uris) {
            long hits;
            if (unique) {
                hits = endpointHitRepository.findEndpointHitByUriAndUniqueIp(start, end, uri);
            } else {
                hits = endpointHitRepository.findEndpointHitByUriNotUnique(start, end, uri);
            }
            stats.add(toStatsDto("ewm-service", uri, hits));
        }
        return stats.stream()
                .sorted(Comparator.comparingLong(x -> -x.getHits()))
                .collect(Collectors.toList());
    }
}
