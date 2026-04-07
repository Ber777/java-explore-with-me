package ewm.location;

import ewm.location.dto.*;
import ewm.location.model.*;
import ewm.location.service.LocationService;
import ewm.location.controller.LocationPrivateController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class LocationPrivateControllerTests {

    @InjectMocks
    private LocationPrivateController locationPrivateController;

    @Mock
    private LocationService locationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationPrivateController).build();
    }

    @Test
    void shouldCreateLocationSuccessfully() throws Exception {
        Long userId = 1L;
        NewLocationDto newLocationDto = NewLocationDto.builder()
                .name("User Location")
                .address("User Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        LocationPrivateDtoResponse expectedResponse = LocationPrivateDtoResponse.builder()
                .id(1L)
                .name("User Location")
                .address("User Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.PENDING)
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(locationService.createLocation(
                eq(userId),
                any(NewLocationDto.class)
        )).thenReturn(expectedResponse);

        mockMvc.perform(post("/users/{userId}/locations", userId)
                        .contentType("application/json")
                        .content(asJsonString(newLocationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("User Location"))
                .andExpect(jsonPath("$.state").value("PENDING"));

        // В verify также используем матчеры для корректной проверки вызова
        verify(locationService, times(1)).createLocation(
                eq(userId),
                any(NewLocationDto.class)
        );
    }

    @Test
    void shouldUpdateLocationSuccessfully() throws Exception {
        Long userId = 1L;
        Long locationId = 1L;
        LocationUpdateUserDto updateDto = LocationUpdateUserDto.builder()
                .name("Updated User Location")
                .build();

        LocationPrivateDtoResponse updatedResponse = LocationPrivateDtoResponse.builder()
                .id(locationId)
                .name("Updated User Location")
                .state(LocationState.PENDING)
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(locationService.updateLocation(
                eq(locationId),
                eq(userId),
                any(LocationUpdateUserDto.class)
        )).thenReturn(updatedResponse);

        mockMvc.perform(patch("/users/{userId}/locations/{id}", userId, locationId)
                        .contentType("application/json")
                        .content(asJsonString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId))
                .andExpect(jsonPath("$.name").value("Updated User Location"));

        // В verify также используем матчеры для корректной проверки вызова
        verify(locationService, times(1)).updateLocation(
                eq(locationId),
                eq(userId),
                any(LocationUpdateUserDto.class)
        );
    }

    @Test
    void shouldGetAllLocationsWithFilters() throws Exception {
        Long userId = 123L;
        String text = "test";
        LocationState state = LocationState.APPROVED;
        Double lat = 55.751244;
        Double lon = 37.618423;
        Double radius = 10.0;
        int offset = 0;
        int limit = 10;

        LocationPrivateDtoResponse location = LocationPrivateDtoResponse.builder()
                .id(1L)
                .name("Test User Location")
                .state(state)
                .build();
        List<LocationPrivateDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByFilter(anyLong(), any(LocationPrivateFilter.class)))
                .thenReturn(locations);

        mockMvc.perform(get("/users/{userId}/locations", userId)
                        .param("text", text)
                        .param("state", state.name())
                        .param("lat", lat.toString())
                        .param("lon", lon.toString())
                        .param("radius", radius.toString())
                        .param("offset", Integer.toString(offset))
                        .param("limit", Integer.toString(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        ArgumentCaptor<LocationPrivateFilter> filterCaptor = ArgumentCaptor.forClass(LocationPrivateFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(eq(userId), filterCaptor.capture());

        LocationPrivateFilter capturedFilter = filterCaptor.getValue();
        assertEquals(text, capturedFilter.getText());
        assertEquals(state, capturedFilter.getState());
        assertNotNull(capturedFilter.getZone());
        assertEquals(lat, capturedFilter.getZone().getLatitude());
        assertEquals(lon, capturedFilter.getZone().getLongitude());
        assertEquals(radius, capturedFilter.getZone().getRadius());
        assertEquals(offset, capturedFilter.getOffset());
        assertEquals(limit, capturedFilter.getLimit());
    }

    @Test
    void shouldFilterByAllLocationStates() throws Exception {
        Long userId = 456L;

        for (LocationState state : LocationState.values()) {
            LocationPrivateDtoResponse location = LocationPrivateDtoResponse.builder()
                    .id(1L)
                    .name("Location for " + state)
                    .state(state)
                    .build();
            List<LocationPrivateDtoResponse> locations = List.of(location);

            when(locationService.getAllLocationsByFilter(anyLong(), any(LocationPrivateFilter.class)))
                    .thenReturn(locations);

            mockMvc.perform(get("/users/{userId}/locations", userId)
                            .param("state", state.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].state").value(state.name()));

            verify(locationService, times(1)).getAllLocationsByFilter(eq(userId), any(LocationPrivateFilter.class));
            reset(locationService);
        }
    }

    @Test
    void shouldGetAllLocationsWithoutFilters() throws Exception {
        Long userId = 456L;

        LocationPrivateDtoResponse location = LocationPrivateDtoResponse.builder()
                .id(2L)
                .name("Another User Location")
                .state(LocationState.PENDING)
                .build();
        List<LocationPrivateDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByFilter(anyLong(), any(LocationPrivateFilter.class)))
                .thenReturn(locations);

        mockMvc.perform(get("/users/{userId}/locations", userId))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].id").value(2))
                        .andExpect(jsonPath("$[0].state").value("PENDING"));

        ArgumentCaptor<LocationPrivateFilter> filterCaptor = ArgumentCaptor.forClass(LocationPrivateFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(eq(userId), filterCaptor.capture());

        LocationPrivateFilter capturedFilter = filterCaptor.getValue();
        assertNull(capturedFilter.getText());
        assertNull(capturedFilter.getState());
        assertNull(capturedFilter.getZone());
        assertEquals(0, capturedFilter.getOffset());
        assertEquals(10, capturedFilter.getLimit());
    }

    @Test
    void shouldDeleteLocationSuccessfully() throws Exception {
        Long userId = 1L;
        Long locationId = 1L;

        mockMvc.perform(delete("/users/{userId}/locations/{id}", userId, locationId))
                .andExpect(status().isNoContent());

        verify(locationService, times(1)).deleteLocation(locationId, userId);
    }

    @Test
    void shouldValidateLatitudeWhenCreatingLocation() throws Exception {
        Long userId = 1L;
        NewLocationDto invalidDto = NewLocationDto.builder()
                .name("Invalid Location")
                .address("Invalid Address")
                .latitude(91.0) // недопустимое значение широты (> 90°)
                .longitude(37.618423)
                .build();

        mockMvc.perform(post("/users/{userId}/locations", userId)
                        .contentType("application/json")
                        .content(asJsonString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(locationService, never()).createLocation(any(), any());
    }

    @Test
    void shouldValidateLongitudeWhenCreatingLocation() throws Exception {
        Long userId = 1L;
        NewLocationDto invalidDto = NewLocationDto.builder()
                .name("Invalid Location")
                .address("Invalid Address")
                .latitude(55.751244)
                .longitude(181.0) // недопустимое значение долготы
                .build();

        mockMvc.perform(post("/users/{userId}/locations", userId)
                        .contentType("application/json")
                        .content(asJsonString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(locationService, never()).createLocation(any(), any());
    }

    @Test
    void shouldCreateLocationWithMinimalRequiredData() throws Exception {
        Long userId = 2L;
        NewLocationDto minimalDto = NewLocationDto.builder()
                .name("Minimal User Location")
                .address("Minimal Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        LocationPrivateDtoResponse expectedResponse = LocationPrivateDtoResponse.builder()
                .id(3L)
                .name("Minimal User Location")
                .address("Minimal Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.PENDING)
                .build();

        // Используем матчеры Mockito для корректной заглушки
        when(locationService.createLocation(
                eq(userId),
                any(NewLocationDto.class)
        )).thenReturn(expectedResponse);

        mockMvc.perform(post("/users/{userId}/locations", userId)
                        .contentType("application/json")
                        .content(asJsonString(minimalDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Minimal User Location"))
                .andExpect(jsonPath("$.lat").value(55.751244))
                .andExpect(jsonPath("$.lon").value(37.618423));

        // В verify также используем матчеры для корректной проверки вызова
        verify(locationService, times(1)).createLocation(
                eq(userId),
                any(NewLocationDto.class)
        );
    }

    @Test
    void shouldFilterLocationsByTextOnly() throws Exception {
        Long userId = 789L;
        String searchText = "park";

        LocationPrivateDtoResponse location = LocationPrivateDtoResponse.builder()
                .id(4L)
                .name("Central Park")
                .state(LocationState.APPROVED)
                .build();
        List<LocationPrivateDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByFilter(anyLong(), any(LocationPrivateFilter.class)))
                .thenReturn(locations);

        mockMvc.perform(get("/users/{userId}/locations", userId)
                        .param("text", searchText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Central Park"));

        ArgumentCaptor<LocationPrivateFilter> filterCaptor = ArgumentCaptor.forClass(LocationPrivateFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(eq(userId), filterCaptor.capture());

        LocationPrivateFilter capturedFilter = filterCaptor.getValue();
        assertEquals(searchText, capturedFilter.getText());
        assertNull(capturedFilter.getState());
        assertNull(capturedFilter.getZone());
    }

    // Вспомогательный метод для преобразования объекта в JSON строку
    private String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}