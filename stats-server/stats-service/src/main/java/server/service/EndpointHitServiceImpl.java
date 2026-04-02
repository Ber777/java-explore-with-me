package server.service;

import dto.*;
import server.exception.*;
import server.repository.EndpointHitRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;
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
        if (start.isAfter(end))
            throw new InvalidException("Дата начала должна быть меньше даты окончания");

        // Гарантируем, что uris не null
        if (uris == null) {
            uris = new ArrayList<>();
        }

        // Выполняем один запрос для всех URI (или всех доступных, если uris пуст)
        Map<String, Long> hitsMap;
        List<Object[]> result;
        if (unique) {
            result = endpointHitRepository.findEndpointsHitByUrisAndUniqueIpBetweenDates(start, end, uris.isEmpty() ? null : uris);
        } else {
            result = endpointHitRepository.findEndpointHitsByUrisNotUniqueBetweenDates(start, end, uris.isEmpty() ? null : uris);
        }
        hitsMap = convertToMap(result);

        // Если uris пуст, берём ключи из hitsMap (все URI за период)
        List<String> targetUris = uris.isEmpty() ? new ArrayList<>(hitsMap.keySet()) : uris;

        List<ViewStatsDto> stats = targetUris.stream()
                .map(uri -> toStatsDto("ewm-service", uri, hitsMap.getOrDefault(uri, 0L)))
                .toList();

        // Сортируем по убыванию количества хитов
        return stats.stream()
                .sorted(Comparator.comparingLong(x -> -x.getHits()))
                .collect(Collectors.toList());
    }

    // Вспомогательный метод для преобразования List<Object[]> в Map<String, Long>
    private Map<String, Long> convertToMap(List<Object[]> results) {
        if (results == null) {
            return new HashMap<>();
        }

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }
}
