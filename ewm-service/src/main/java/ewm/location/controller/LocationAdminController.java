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
@RequestMapping("/admin/locations")
public class LocationAdminController {
    private final LocationService locationService;

    // Создаем локацию админом (сразу переводится в статус одобрено)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationFullDtoResponse createLocation(@RequestBody @Valid NewLocationDto dto) {
        log.debug("Запрос на добавление локации админом: {}", dto);
        return locationService.createLocationByAdmin(dto);
    }

    // Обновляем существующую локацию админом
    @PatchMapping("/{id}")
    public LocationFullDtoResponse updateLocation(@PathVariable @Min(1) Long id,
                                     @RequestBody @Valid LocationUpdateAdminDto dto) {
        log.debug("request for update location id: {} by admin", id);
        return locationService.updateLocationByAdmin(id, dto);
    }

    // Получаем список локакий админом
    @GetMapping
    public Collection<LocationFullDtoResponse> getAllLocations(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Long user,
            @RequestParam(required = false) LocationState state,
            @RequestParam(required = false) @DecimalMin("-90.0")  @DecimalMax("90.0")  Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(required = false) Integer minEvents,
            @RequestParam(required = false) Integer maxEvents,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("Запрос на поиск локаций админом");
        LocationAdminFilter filter = LocationAdminFilter.builder()
                .text(text)
                .creator(user)
                .state(state)
                .minEvents(minEvents)
                .maxEvents(maxEvents)
                .offset(offset)
                .limit(limit)
                .build();
        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.getAllLocationsByAdminFilter(filter);
    }

    // Получаем локацию по айди
    @GetMapping("/{id}")
    public LocationFullDtoResponse getLocation(@PathVariable @Min(1) Long id) {
        log.debug("Запрос на получение локации с id:{} админом", id);
        return locationService.getByIdForAdmin(id);
    }

    // Удаляем существующую локацию, не имеющей мероприятий
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable @Min(1) Long id) {
        log.debug("Запрос на удаление локации с id:{} админом", id);
        locationService.deleteLocationByAdmin(id);
    }
}
