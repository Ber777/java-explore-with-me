package ewm.location;

import ewm.location.dto.*;
import ewm.location.model.*;
import ewm.location.service.LocationService;
import ewm.location.controller.LocationAdminController;

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
class LocationAdminControllerTests {

    @InjectMocks
    private LocationAdminController locationAdminController;

    @Mock
    private LocationService locationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(locationAdminController).build();
    }

    @Test
    void shouldCreateLocationSuccessfully() throws Exception {
        NewLocationDto newLocationDto = NewLocationDto.builder()
                .name("New Location")
                .address("New Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        LocationFullDtoResponse expectedResponse = LocationFullDtoResponse.builder()
                .id(1L)
                .name("New Location")
                .address("New Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.APPROVED)
                .build();

        when(locationService.createLocationByAdmin(newLocationDto)).thenReturn(expectedResponse);

        mockMvc.perform(post("/admin/locations")
                        .contentType("application/json")
                        .content(asJsonString(newLocationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Location"))
                .andExpect(jsonPath("$.state").value("APPROVED"));

        verify(locationService, times(1)).createLocationByAdmin(newLocationDto);
    }

    @Test
    void shouldValidateLatitudeWhenCreatingLocation() throws Exception {
        NewLocationDto invalidDto = NewLocationDto.builder()
                .name("Invalid Location")
                .address("Invalid Address")
                .latitude(91.0) // недопустимое значение
                .longitude(37.618423)
                .build();

        mockMvc.perform(post("/admin/locations")
                        .contentType("application/json")
                        .content(asJsonString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(locationService, never()).createLocationByAdmin(any());
    }

    @Test
    void shouldUpdateLocationSuccessfully() throws Exception {
        Long locationId = 1L;
        LocationUpdateAdminDto updateDto = LocationUpdateAdminDto.builder()
                .name("Updated Name")
                .build();

        LocationFullDtoResponse updatedResponse = LocationFullDtoResponse.builder()
                .id(locationId)
                .name("Updated Name")
                .build();

        when(locationService.updateLocationByAdmin(locationId, updateDto)).thenReturn(updatedResponse);

        mockMvc.perform(patch("/admin/locations/{id}", locationId)
                        .contentType("application/json")
                        .content(asJsonString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId))
                .andExpect(jsonPath("$.name").value("Updated Name"));

        verify(locationService, times(1)).updateLocationByAdmin(locationId, updateDto);
    }

    @Test
    void shouldGetAllLocationsWithFilters() throws Exception {
        String text = "test";
        Long user = 123L;
        LocationState state = LocationState.APPROVED;
        Double lat = 55.751244;
        Double lon = 37.618423;
        Double radius = 10.0;
        int minEvents = 5;
        int maxEvents = 20;
        int offset = 0;
        int limit = 10;

        LocationFullDtoResponse location = LocationFullDtoResponse.builder()
                .id(1L)
                .name("Test Location")
                .build();
        List<LocationFullDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByAdminFilter(any(LocationAdminFilter.class)))
                .thenReturn(locations);

        mockMvc.perform(get("/admin/locations")
                        .param("text", text)
                        .param("user", user.toString())
                        .param("state", state.name())
                        .param("lat", lat.toString())
                        .param("lon", lon.toString())
                        .param("radius", radius.toString())
                        .param("minEvents", Integer.toString(minEvents))
                        .param("maxEvents", Integer.toString(maxEvents))
                        .param("offset", Integer.toString(offset))
                        .param("limit", Integer.toString(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        ArgumentCaptor<LocationAdminFilter> filterCaptor = ArgumentCaptor.forClass(LocationAdminFilter.class);
        verify(locationService, times(1)).getAllLocationsByAdminFilter(filterCaptor.capture());

        LocationAdminFilter capturedFilter = filterCaptor.getValue();
        assertEquals(text, capturedFilter.getText());
        assertEquals(user, capturedFilter.getCreator());
        assertEquals(state, capturedFilter.getState());
        assertNotNull(capturedFilter.getZone());
        assertEquals(lat, capturedFilter.getZone().getLatitude());
        assertEquals(lon, capturedFilter.getZone().getLongitude());
        assertEquals(radius, capturedFilter.getZone().getRadius());
    }

    @Test
    void shouldGetLocationByIdSuccessfully() throws Exception {
        Long locationId = 1L;
        LocationFullDtoResponse expectedLocation = LocationFullDtoResponse.builder()
                .id(locationId)
                .name("Test Location")
                .build();

        when(locationService.getByIdForAdmin(locationId)).thenReturn(expectedLocation);

        mockMvc.perform(get("/admin/locations/{id}", locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId));

        verify(locationService, times(1)).getByIdForAdmin(locationId);
    }

    @Test
    void shouldDeleteLocationSuccessfully() throws Exception {
        Long locationId = 1L;

        mockMvc.perform(delete("/admin/locations/{id}", locationId))
                .andExpect(status().isNoContent());

        verify(locationService, times(1)).deleteLocationByAdmin(locationId);
    }

    @Test
    void shouldGetAllLocationsWithoutFilters() throws Exception {
        LocationFullDtoResponse location = LocationFullDtoResponse.builder()
                .id(1L)
                .name("Test Location")
                .build();
        List<LocationFullDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByAdminFilter(any(LocationAdminFilter.class)))
                .thenReturn(locations);

        mockMvc.perform(get("/admin/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        ArgumentCaptor<LocationAdminFilter> filterCaptor = ArgumentCaptor.forClass(LocationAdminFilter.class);
        verify(locationService, times(1)).getAllLocationsByAdminFilter(filterCaptor.capture());

        LocationAdminFilter capturedFilter = filterCaptor.getValue();
        assertNull(capturedFilter.getText());
        assertNull(capturedFilter.getCreator());
        assertNull(capturedFilter.getState());
        assertNull(capturedFilter.getZone());
        assertEquals(0, capturedFilter.getOffset());
        assertEquals(10, capturedFilter.getLimit());
    }

    @Test
    void shouldCreateLocationWithMinimalRequiredData() throws Exception {
        NewLocationDto minimalDto = NewLocationDto.builder()
                .name("Minimal Location")
                .address("Minimal Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();

        LocationFullDtoResponse expectedResponse = LocationFullDtoResponse.builder()
                .id(2L)
                .name("Minimal Location")
                .address("Minimal Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.APPROVED)
                .build();

        when(locationService.createLocationByAdmin(minimalDto)).thenReturn(expectedResponse);

        mockMvc.perform(post("/admin/locations")
                        .contentType("application/json")
                        .content(asJsonString(minimalDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Minimal Location"))
                .andExpect(jsonPath("$.lat").value(55.751244))
                .andExpect(jsonPath("$.lon").value(37.618423));

        verify(locationService, times(1)).createLocationByAdmin(minimalDto);
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
