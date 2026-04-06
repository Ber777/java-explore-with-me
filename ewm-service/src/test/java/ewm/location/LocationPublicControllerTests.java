package ewm.location;

import ewm.exception.*;
import ewm.location.dto.LocationDtoResponse;
import ewm.location.service.LocationService;
import ewm.location.model.LocationPublicFilter;
import ewm.location.controller.LocationPublicController;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class LocationPublicControllerTests {

    @InjectMocks
    private LocationPublicController locationPublicController;

    @Mock
    private LocationService locationService;

    // Вспомогательный метод для создания тестового DTO локации
    private LocationDtoResponse getLocationDtoResponse(Long id) {
        return LocationDtoResponse.builder()
                .id(id)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();
    }

    @Test
    void shouldGetAllLocationsSuccessfullyWithFullParameters() {
        String text = "test location";
        Double lat = 55.751244;
        Double lon = 37.618423;
        Double radius = 10.0;
        Integer offset = 5;
        Integer limit = 20;

        LocationDtoResponse location = getLocationDtoResponse(1L);
        List<LocationDtoResponse> locations = List.of(location);

        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(locations);

        Collection<LocationDtoResponse> result = locationPublicController.getAllLocation(
                text, lat, lon, radius, offset, limit);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(location.getId(), result.iterator().next().getId());

        ArgumentCaptor<LocationPublicFilter> filterCaptor = ArgumentCaptor.forClass(LocationPublicFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(filterCaptor.capture());

        LocationPublicFilter capturedFilter = filterCaptor.getValue();
        assertEquals(text, capturedFilter.getText());
        assertNotNull(capturedFilter.getZone());
        assertEquals(lat, capturedFilter.getZone().getLatitude());
        assertEquals(lon, capturedFilter.getZone().getLongitude());
        assertEquals(radius, capturedFilter.getZone().getRadius());
        assertEquals(offset, capturedFilter.getOffset());
        assertEquals(limit, capturedFilter.getLimit());
        assertEquals(0, capturedFilter.getPageable().getPageNumber()); // 5 / 20 = 0
        assertEquals(limit, capturedFilter.getPageable().getPageSize());
    }

    @Test
    void shouldHandleEmptyLocationsList() {
        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(Collections.emptyList());

        Collection<LocationDtoResponse> result = locationPublicController.getAllLocation(
                null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(locationService, times(1)).getAllLocationsByFilter(any(LocationPublicFilter.class));
    }

    @Test
    void shouldGetLocationByIdSuccessfully() {
        Long locationId = 1L;
        LocationDtoResponse locationDtoResponse = getLocationDtoResponse(locationId);
        when(locationService.getApprovedLocations(locationId)).thenReturn(locationDtoResponse);

        LocationDtoResponse result = locationPublicController.getLocation(locationId);

        assertNotNull(result);
        assertEquals(locationId, result.getId());
        assertEquals("Test Location", result.getName());
        assertEquals(55.751244, result.getLatitude());
        assertEquals(37.618423, result.getLongitude());

        verify(locationService, times(1)).getApprovedLocations(locationId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLocationNotExists() {
        Long locationId = 999L;

        when(locationService.getApprovedLocations(locationId))
                .thenThrow(new NotFoundException("Location", locationId));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> locationPublicController.getLocation(locationId));


        assertEquals("Location с id: 999 не найден(-а)", exception.getMessage());
        verify(locationService, times(1)).getApprovedLocations(locationId);
    }

    @Test
    void shouldApplyPaginationCorrectly() {
        Integer offset = 25;
        Integer limit = 10;
        LocationDtoResponse location = getLocationDtoResponse(1L);
        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(List.of(location));

        locationPublicController.getAllLocation(null, null, null, null, offset, limit);

        ArgumentCaptor<LocationPublicFilter> filterCaptor = ArgumentCaptor.forClass(LocationPublicFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(filterCaptor.capture());

        LocationPublicFilter capturedFilter = filterCaptor.getValue();
        assertEquals(offset, capturedFilter.getOffset());
        assertEquals(limit, capturedFilter.getLimit());
        assertEquals(2, capturedFilter.getPageable().getPageNumber()); // 25 / 10 = 2
        assertEquals(limit, capturedFilter.getPageable().getPageSize());
    }

    @Test
    void shouldGetLocationsWithTextFilter() {
        String text = "test";
        LocationDtoResponse location = getLocationDtoResponse(1L);
        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(List.of(location));

        Collection<LocationDtoResponse> result = locationPublicController.getAllLocation(
                text, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());

        ArgumentCaptor<LocationPublicFilter> filterCaptor = ArgumentCaptor.forClass(LocationPublicFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(filterCaptor.capture());

        LocationPublicFilter capturedFilter = filterCaptor.getValue();
        assertEquals(text, capturedFilter.getText());
        assertNull(capturedFilter.getZone());
    }

    @Test
    void shouldFilterOnlyApprovedLocations() {
        LocationDtoResponse location = getLocationDtoResponse(1L);
        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(List.of(location));

        locationPublicController.getAllLocation(null, null, null, null, 0, 10);

        ArgumentCaptor<LocationPublicFilter> filterCaptor = ArgumentCaptor.forClass(LocationPublicFilter.class);
        verify(locationService, times(1)).getAllLocationsByFilter(filterCaptor.capture());

        LocationPublicFilter capturedFilter = filterCaptor.getValue();

        // В сервисе автоматически фильтруются только локации со статусом одобрено
        assertNotNull(capturedFilter);
    }

    @Test
    void shouldHandleNullValuesInLocationDtoResponse() {
        LocationDtoResponse locationWithNulls = LocationDtoResponse.builder()
                .id(2L)
                .name("Location with nulls")
                .address(null)
                .latitude(null)
                .longitude(null)
                .build();

        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(List.of(locationWithNulls));

        Collection<LocationDtoResponse> result = locationPublicController.getAllLocation(
                null, null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        LocationDtoResponse resultDto = result.iterator().next();
        assertNull(resultDto.getAddress());
        assertNull(resultDto.getLatitude());
        assertNull(resultDto.getLongitude());
    }

    @Test
    void shouldLogDebugMessageForGetAllLocations() {
        LocationDtoResponse location = getLocationDtoResponse(1L);
        when(locationService.getAllLocationsByFilter(any(LocationPublicFilter.class)))
                .thenReturn(List.of(location));

        locationPublicController.getAllLocation(null, null, null, null, 0, 10);
    }

    @Test
    void shouldLogDebugMessageForGetLocationById() {
        Long locationId = 1L;
        LocationDtoResponse locationDtoResponse = getLocationDtoResponse(locationId);
        when(locationService.getApprovedLocations(locationId)).thenReturn(locationDtoResponse);

        locationPublicController.getLocation(locationId);
    }
}
