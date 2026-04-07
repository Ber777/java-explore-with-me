package ewm.location.service;

import ewm.exception.*;
import ewm.location.dto.*;
import ewm.location.model.*;
import ewm.user.model.User;
import ewm.location.LocationMapper;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.location.repository.LocationRepository;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {
    private static final double NEARBY_RADIUS = 50; // meters

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public LocationPrivateDtoResponse createLocation(Long userId, NewLocationDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));

        // При создании новой локации excludeId = null
        checkForDuplicate(dto.getName(), dto.getLatitude(), dto.getLongitude(), null);

        Location location = LocationMapper.fromLocationDto(dto);
        location.setCreator(user);
        Location saved = locationRepository.save(location);
        return LocationMapper.toPrivateLocationDto(saved);
    }

    @Override
    @Transactional
    public LocationFullDtoResponse createLocationByAdmin(NewLocationDto dto) {
        // При создании новой локации excludeId = null
        checkForDuplicate(dto.getName(), dto.getLatitude(), dto.getLongitude(), null);

        Location location = LocationMapper.fromLocationDto(dto);
        location.setState(LocationState.APPROVED);
        return LocationMapper.toFullLocationDto(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationFullDtoResponse updateLocationByAdmin(Long id, LocationUpdateAdminDto dto) {
        log.debug("Попытка обновить локацию админом: {}", dto);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Локация", id));

        // Проверяем изменения, требующие проверки дубликатов
        boolean needToCheckDuplicates =
                (dto.getName() != null && !dto.getName().equals(location.getName())) ||
                        (dto.getLatitude() != null && !dto.getLatitude().equals(location.getLatitude())) ||
                        (dto.getLongitude() != null && !dto.getLongitude().equals(location.getLongitude()));

        if (needToCheckDuplicates) {
            final String name = Optional.ofNullable(dto.getName()).orElse(location.getName());
            final Double lat = Optional.ofNullable(dto.getLatitude()).orElse(location.getLatitude());
            final Double lon = Optional.ofNullable(dto.getLongitude()).orElse(location.getLongitude());
            // Передаём id текущей локации для исключения из проверки
            checkForDuplicate(name, lat, lon, id);
        }

        Optional.ofNullable(dto.getName()).ifPresent(location::setName);
        Optional.ofNullable(dto.getAddress()).ifPresent(location::setAddress);
        Optional.ofNullable(dto.getLatitude()).ifPresent(location::setLatitude);
        Optional.ofNullable(dto.getLongitude()).ifPresent(location::setLongitude);
        Optional.ofNullable(dto.getState()).ifPresent(state -> changeLocationState(location, state));

        return LocationMapper.toFullLocationDto(location);
    }

    @Override
    @Transactional
    public LocationPrivateDtoResponse updateLocation(Long id, Long userId, LocationUpdateUserDto dto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Локация", id));

        if (location.getState() != LocationState.PENDING) {
            throw new ConditionNotMetException("Невозможно обновить опубликованную/отменённую локацию");
        }

        if (location.getCreator() == null || !location.getCreator().getId().equals(userId)) {
            throw new NoAccessException("Только создатель может редактировать данную локацию");
        }

        boolean needToCheckDuplicates =
                (dto.getName() != null && !dto.getName().equals(location.getName())) ||
                        (dto.getLatitude() != null && !dto.getLatitude().equals(location.getLatitude())) ||
                        (dto.getLongitude() != null && !dto.getLongitude().equals(location.getLongitude()));

        if (needToCheckDuplicates) {
            final String name = Optional.ofNullable(dto.getName()).orElse(location.getName());
            final Double lat = Optional.ofNullable(dto.getLatitude()).orElse(location.getLatitude());
            final Double lon = Optional.ofNullable(dto.getLongitude()).orElse(location.getLongitude());
            // Передаём id текущей локации для исключения из проверки дубликатов
            checkForDuplicate(name, lat, lon, id);
        }

        Optional.ofNullable(dto.getName()).ifPresent(location::setName);
        Optional.ofNullable(dto.getAddress()).ifPresent(location::setAddress);
        Optional.ofNullable(dto.getLatitude()).ifPresent(location::setLatitude);
        Optional.ofNullable(dto.getLongitude()).ifPresent(location::setLongitude);

        return LocationMapper.toPrivateLocationDto(location);
    }

    // Учитываем, что при обновлении локации поиск дубликатов может вернуть ту же самую локацию, которую мы обновляем
    private void checkForDuplicate(String name, Double lat, Double lon, Long excludeId) {
        log.debug("Проверка дубликатов для локации: name={}, lat={}, lon={}", name, lat, lon);

        Optional<Location> existing = locationRepository.findDuplicates(name, lat, lon, NEARBY_RADIUS);

        if (existing.isPresent() && !Objects.equals(existing.get().getId(), excludeId)) {
            log.warn("Найден дубликат локации: {}", existing.get());
            throw new DuplicateLocationsException(getDuplicateErrorMessage(existing.get()));
        }
    }

    @Override
    public LocationDtoResponse getApprovedLocations(Long id) {
        Location location = locationRepository.findByIdAndState(id, LocationState.APPROVED)
                .orElseThrow(() -> new NotFoundException("Location", id));

        return LocationMapper.toLocationDto(location);
    }

    private void changeLocationState(Location location, LocationState state) {
        log.debug("Изменение локации id:{} состояние: {} -> {}", location.getId(), location.getState(), state);
        if (location.getState() == state)
            return;

        if (state == LocationState.PENDING || state == LocationState.AUTO_GENERATED) {
            throw new ConditionNotMetException(
                    String.format("Невозможно изменить состояние %s в %s", location.getState(), state));
        }
        location.setState(state);
    }

    private static String getDuplicateErrorMessage(@NotNull Location existing) {
        Long id = existing.getId();
        switch (existing.getState()) {
            case LocationState.APPROVED -> {
                return String.format("Пожалуйста, используйте существующую локацию (id=%d)", id);
            }
            case LocationState.PENDING -> {
                return String.format("Запрос на создание данной локации уже существует (id=%d). Пожалуйста, дождитесь одобрения", id);
            }
            case LocationState.REJECTED -> {
                return  "Запрос на создание данной локации был ранее отклонен. Пожалуйста, обратитесь к администратору";
            }
        }
        return "";
    }

    @Override
    public Collection<LocationFullDtoResponse> getAllLocationsByAdminFilter(LocationAdminFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toFullLocationDto)
                .toList();
    }

    @Override
    public Collection<LocationPrivateDtoResponse> getAllLocationsByFilter(Long userId, LocationPrivateFilter filter) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("Пользователь", userId);

        Specification<Location> spec = buildSpecification(userId, filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toPrivateLocationDto)
                .toList();
    }

    @Override
    public Collection<LocationDtoResponse> getAllLocationsByFilter(LocationPublicFilter filter) {
        Specification<Location> spec = buildSpecification(filter);
        List<Location> locations = locationRepository.findAll(spec, filter.getPageable()).getContent();
        return locations.stream()
                .map(LocationMapper::toLocationDto)
                .toList();
    }

    @Override
    public LocationFullDtoResponse getByIdForAdmin(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Локация", id));
        return LocationMapper.toFullLocationDto(location);
    }

    @Override
    @Transactional
    public void deleteLocationByAdmin(Long id) {
        if (eventRepository.existsByLocationId(id)) {
            throw new ConditionNotMetException("Невозможно удалить локацию: есть связанные события");
        }
        locationRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteLocation(Long id, Long userId) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Локация", id));

        if (location.getState() == LocationState.APPROVED) {
            throw new ConditionNotMetException("Невозможно удалить опубликованную локацию");
        }

        if (location.getCreator() == null || !location.getCreator().getId().equals(userId)) {
            throw new NoAccessException("Только создатель может удалить данную локацию");
        }

        if (eventRepository.existsByLocationId(id)) {
            throw new ConditionNotMetException("События в этой локации");
        }

        locationRepository.deleteById(id);
    }

    @Override
    public Location getOrCreateLocation(LocationDto location) {
        if (location.getId() != null) {
            return locationRepository.findByIdAndState(location.getId(), LocationState.APPROVED)
                    .orElseThrow(() -> new NotFoundException("Локация", location.getId()));
        }

        if (location.getLatitude() != null && location.getLongitude() != null) {
            Optional<Location> nearByAutoGenerated = locationRepository.findNearByAutoGenerated(
                    location.getLatitude(), location.getLongitude());

            return nearByAutoGenerated.orElseGet(()
                    -> createAutoGeneratedLocation(location.getLatitude(), location.getLongitude()));
        }

        throw new ConditionNotMetException("Неверная локация");
    }

    @Transactional
    private Location createAutoGeneratedLocation(Double lat, Double lon) {
        Location location = Location.builder()
                .latitude(lat)
                .longitude(lon)
                .state(LocationState.AUTO_GENERATED)
                .build();
        return locationRepository.save(location);
    }

    private Specification<Location> buildSpecification(LocationAdminFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCreator(filter.getCreator())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone())),
                        optionalSpec(LocationSpecifications.withState(filter.getState())),
                        optionalSpec(LocationSpecifications.withEventsCount(filter.getMinEvents(), filter.getMaxEvents()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Location> buildSpecification(Long userId, LocationPrivateFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withCreator(userId)),
                        optionalSpec(LocationSpecifications.withState(filter.getState())),
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Location> buildSpecification(LocationPublicFilter filter) {
        return Stream.of(
                        optionalSpec(LocationSpecifications.withState(LocationState.APPROVED)),
                        optionalSpec(LocationSpecifications.withTextContains(filter.getText())),
                        optionalSpec(LocationSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private static <T> Specification<T> optionalSpec(Specification<T> spec) {
        return spec;
    }
}

