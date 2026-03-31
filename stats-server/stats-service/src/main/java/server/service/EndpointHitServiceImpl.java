package server.service;

import dto.ViewStatsDto;
import dto.EndpointHitDto;
import server.exception.InvalidException;
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

        List<String> targetUris;

        // Если список пуст, получаем все уникальные URI из БД за период
        if (uris.isEmpty()) {
            targetUris = endpointHitRepository.findAllEndpointHitBetweenDates(start, end);
        } else {
            targetUris = uris;
        }

        // Выполняем один запрос для всех URI сразу
        Map<String, Long> hitsMap;
        if (unique) {
            List<Object[]> result = endpointHitRepository.findEndpointsHitByUrisAndUniqueIp(start, end, targetUris);
            hitsMap = convertToMap(result);
        } else {
            List<Object[]> result = endpointHitRepository.findEndpointHitsByUrisNotUnique(start, end, targetUris);
            hitsMap = convertToMap(result);
        }

        // Формируем результат из данных карты, гарантируя наличие 0 для отсутствующих URI
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
