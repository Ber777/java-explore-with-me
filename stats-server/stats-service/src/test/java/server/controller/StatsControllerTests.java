package server.controller;

import dto.EndpointHitDto;
import dto.ViewStatsDto;
import server.service.EndpointHitService;
import server.exception.InvalidException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc
public class StatsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EndpointHitService endpointHitService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldAddEndpointHitAndReturnStatus201() throws Exception {
        EndpointHitDto hit =  EndpointHitDto.builder()
                .app("test-service")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());

        Mockito.verify(endpointHitService, Mockito.times(1)).createEndpointHit(any(EndpointHitDto.class));
    }

    @Test
    void shouldReturnStatus400IfEndpointHitDtoIsInvalid() throws Exception {
        EndpointHitDto invalidHit = EndpointHitDto.builder()
                .app("test-service")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())  // Отсутствует URI (обязательное поле)
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnGetStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ViewStatsDto dto = new ViewStatsDto("test-service", "/test", 5L);
        Mockito.when(endpointHitService.getStats(any(), any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/stats")
                        .param("start", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("end", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .param("uris", "/test")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-service"))
                .andExpect(jsonPath("$[0].uri").value("/test"))
                .andExpect(jsonPath("$[0].hits").value(5));
    }

    @Test
    void shouldReturnStatus400IfStartParamIsMissing() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("end", "2025-01-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnStatus400IfEndParamIsMissing() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2025-01-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnStatus400IfStartAfterEnd() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Mockito.when(endpointHitService.getStats(any(), any(), any(), any()))
                .thenThrow(new InvalidException("start > end"));

        mockMvc.perform(get("/stats")
                        .param("start", now.plusHours(1).format(FORMATTER))
                        .param("end", now.format(FORMATTER))
                        .param("unique", "false"))
                .andExpect(status().isBadRequest());
    }
}