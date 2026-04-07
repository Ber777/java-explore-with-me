package ewm.location.controller;

import ewm.location.model.*;
import ewm.location.service.LocationService;
import ewm.location.dto.LocationDtoResponse;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationPublicController {
    private final LocationService locationService;

    // Получить список одобренных локаций (по имени, координатам)
    @GetMapping
    public Collection<LocationDtoResponse> getAllLocation(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) @DecimalMin("-90.0")  @DecimalMax("90.0")  Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("Запрос на получение одобренных локаций");
        LocationPublicFilter filter = LocationPublicFilter.builder()
                .text(text)
                .offset(offset)
                .limit(limit)
                .build();

        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.getAllLocationsByFilter(filter);
    }

    @GetMapping("/{id}")
    public LocationDtoResponse getLocation(@PathVariable @Min(1) Long id) {
        log.debug("Запрос на получение локации с id:{}", id);
        return locationService.getApprovedLocations(id);
    }
}

