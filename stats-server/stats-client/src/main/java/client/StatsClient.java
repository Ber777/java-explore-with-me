package client;

import dto.ViewStatsDto;
import dto.EndpointHitDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public class StatsClient {
    private final RestClient restClient;
    private final String serverUrl;

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(5);

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        factory.setConnectionRequestTimeout(DEFAULT_CONNECTION_REQUEST_TIMEOUT);

        restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(serverUrl)
                .build();
    }

    public void endpointHit(String app, String url, String ip) {
        EndpointHitDto endpointHitDto = new EndpointHitDto();
        endpointHitDto.setApp(app);
        endpointHitDto.setUri(url);
        endpointHitDto.setIp(ip);
        endpointHitDto.setTimestamp(LocalDateTime.now());
        this.endpointHit(endpointHitDto);
    }

    public void endpointHit(EndpointHitDto endpointHitDto) throws StatsClientException {
        try {
            restClient.post()
                    .uri(HIT_ENDPOINT)
                    .contentType(APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve();
        } catch (ResourceAccessException e) {
            throw new StatsClientException("Не удалось соединиться с сервисом статистики: " + e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка в EndpointHit(): {}", e.getMessage());
            throw new StatsClientException("Ошибка сохранения запроса: " + e.getMessage());
        }
    }

    public Collection<ViewStatsDto> getStats(LocalDateTime start,
                                             LocalDateTime end,
                                             Collection<String> uris,
                                             boolean unique) throws StatsClientException {
        validateDates(start, end);

        String url = UriComponentsBuilder.fromUriString(serverUrl + STATS_ENDPOINT)
                .queryParam("start", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("end", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        log.debug("Получение статистики: {}", url);

        try {
            ViewStatsDto[] stats = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("Ошибка сервиса статистики: " + res.getStatusCode());
                    })
                    .body(ViewStatsDto[].class);
            return stats != null ? Arrays.asList(stats) : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Сервис статистики недоступен. URL: {}, ошиюка: {}", url, e.getMessage());
            throw new StatsClientException("Ошибка соединения с сервисом статистики: " + e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка в getStats(): {}", e.getMessage());
            throw new StatsClientException("Ошибка получения статистики: " + e.getMessage());
        }
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Даты не должны быть null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала должна быть меньше даты окончания");
        }
    }
}
