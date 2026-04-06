package ewm.location.controller;

import ewm.location.dto.*;
import ewm.location.model.*;
import ewm.location.service.LocationService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/locations")
public class LocationPrivateController {
    private final LocationService locationService;

    // Создаем локацию пользователем (сразу переводится в статус на рассмотрении)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationPrivateDtoResponse createLocation(@PathVariable @Min(1) Long userId,
                                        @RequestBody @Valid NewLocationDto dto) {
        log.debug("Запрос на создание локации {} пользователем: {}", dto, userId);
        return locationService.createLocation(userId, dto);
    }

    // Обновляем существующую локацию от пользователя (если локация в статусе на рассмотрении)
    @PatchMapping("/{id}")
    public LocationPrivateDtoResponse updateLocation(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid LocationUpdateUserDto dto) {
        log.debug("Запрос на обновление локации с id: {} пользователем:{}", id, userId);
        return locationService.updateLocation(id, userId, dto);
    }

    // Получаем список локакий текущего пользователя
    @GetMapping
    public Collection<LocationPrivateDtoResponse> getAllLocations(
            @PathVariable @Min(1) Long userId,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) LocationState state,
            @RequestParam(required = false) @DecimalMin("-90.0")  @DecimalMax("90.0")  Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("request for search locations by user: {}", userId);
        LocationPrivateFilter filter = LocationPrivateFilter.builder()
                .text(text)
                .state(state)
                .offset(offset)
                .limit(limit)
                .build();

        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.getAllLocationsByFilter(userId, filter);
    }

    // Удаляем существующую неопубликованную локацию
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable @Min(1) Long userId,
                       @PathVariable @Min(1) Long id) {
        log.debug("Запрос на удаление локации с id: {} пользователем:{}", id, userId);
        locationService.deleteLocation(id, userId);
    }
}
