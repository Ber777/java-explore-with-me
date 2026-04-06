package ewm.location;

import ewm.exception.*;
import ewm.location.dto.*;
import ewm.location.model.*;
import ewm.location.service.*;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.location.repository.LocationRepository;

import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;
import org.springframework.data.domain.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTests {
    private static final Long USER_ID = 1L;
    private static final Long LOCATION_ID = 1L;
    private static final double NEARBY_RADIUS = 50.0;

    @InjectMocks
    private LocationServiceImpl locationService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private EventRepository eventRepository;

    @Test
    void shouldCreateLocationSuccessfully() {
        NewLocationDto dto = getNewLocationDto();
        User user = getUser();
        Location location = getLocation();
        LocationPrivateDtoResponse expectedResponse = getLocationPrivateDtoResponse();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(locationRepository.findDuplicates(
                eq(dto.getName()),
                eq(dto.getLatitude()),
                eq(dto.getLongitude()),
                anyDouble()
        )).thenReturn(Optional.empty());

        when(locationRepository.save(any(Location.class))).thenReturn(location);

        LocationPrivateDtoResponse actualResponse = locationService.createLocation(USER_ID, dto);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getName(), actualResponse.getName());
        assertEquals(expectedResponse.getAddress(), actualResponse.getAddress());
        assertEquals(expectedResponse.getLatitude(), actualResponse.getLatitude());
        assertEquals(expectedResponse.getLongitude(), actualResponse.getLongitude());

        verify(locationRepository, times(1)).findDuplicates(
                eq(dto.getName()),
                eq(dto.getLatitude()),
                eq(dto.getLongitude()),
                anyDouble()
        );

        verify(locationRepository, times(1)).save(argThat(savedLocation -> {
            assertEquals(dto.getName(), savedLocation.getName());
            assertEquals(dto.getAddress(), savedLocation.getAddress());
            assertEquals(dto.getLatitude(), savedLocation.getLatitude());
            assertEquals(dto.getLongitude(), savedLocation.getLongitude());
            assertEquals(user, savedLocation.getCreator());
            assertEquals(LocationState.PENDING, savedLocation.getState());
            return true;
        }));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFoundOnCreate() {
        NewLocationDto dto = getNewLocationDto();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> locationService.createLocation(USER_ID, dto),
                "Ожидалось исключение NotFoundException при отсутствии пользователя"
        );

        assertEquals("Пользователь с id: " + USER_ID + " не найден(-а)", exception.getMessage());
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateLocationsExceptionWhenDuplicateExists() {
        NewLocationDto dto = getNewLocationDto();
        User user = getUser();
        Location duplicate = getLocation();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(locationRepository.findDuplicates(eq(dto.getName()), eq(dto.getLatitude()), eq(dto.getLongitude()), anyDouble()))
                .thenReturn(Optional.of(duplicate));

        DuplicateLocationsException exception = assertThrows(
                DuplicateLocationsException.class,
                () -> locationService.createLocation(USER_ID, dto),
                "Ожидалось исключение DuplicateLocationsException при наличии дубликата"
        );
        System.out.println(exception.getMessage());
        assertTrue(exception.getMessage().contains("Запрос на создание данной локации уже существует (id=1). Пожалуйста, дождитесь одобрения"));
    }

    @Test
    void shouldCreateLocationByAdminSuccessfully() {
        NewLocationDto dto = getNewLocationDto();
        Location location = getLocation();
        location.setState(LocationState.APPROVED);
        LocationFullDtoResponse expectedResponse = getLocationFullDtoResponse();

        when(locationRepository.findDuplicates(
                eq(dto.getName()),
                eq(dto.getLatitude()),
                eq(dto.getLongitude()),
                anyDouble()
        )).thenReturn(Optional.empty());

        when(locationRepository.save(any(Location.class))).thenReturn(location);

        LocationFullDtoResponse actualResponse = locationService.createLocationByAdmin(dto);

        assertNotNull(actualResponse, "Ответ не должен быть null");

        assertEquals(expectedResponse.getId(), actualResponse.getId(),
                "ID локации должен совпадать с ожидаемым");
        assertEquals(expectedResponse.getName(), actualResponse.getName(),
                "Название локации должно совпадать с ожидаемым");
        assertEquals(expectedResponse.getAddress(), actualResponse.getAddress(),
                "Адрес локации должен совпадать с ожидаемым");
        assertEquals(expectedResponse.getLatitude(), actualResponse.getLatitude(),
                "Широта локации должна совпадать с ожидаемым значением");
        assertEquals(expectedResponse.getLongitude(), actualResponse.getLongitude(),
                "Долгота локации должна совпадать с ожидаемым значением");
        assertEquals(LocationState.APPROVED, actualResponse.getState(),
                "Статус локации должен быть APPROVED для админского создания");

        // Верификация вызовов репозиториев
        verify(locationRepository, times(1)).findDuplicates(
                eq(dto.getName()),
                eq(dto.getLatitude()),
                eq(dto.getLongitude()),
                anyDouble()
        );

        verify(locationRepository, times(1)).save(argThat(savedLocation -> {
            // Проверка корректности сохранения локации
            assertEquals(dto.getName(), savedLocation.getName(),
                    "Сохранённое имя локации должно совпадать с DTO");
            assertEquals(dto.getAddress(), savedLocation.getAddress(),
                    "Сохранённый адрес локации должен совпадать с DTO");
            assertEquals(dto.getLatitude(), savedLocation.getLatitude(),
                    "Сохранённая широта должна совпадать с DTO");
            assertEquals(dto.getLongitude(), savedLocation.getLongitude(),
                    "Сохранённая долгота должна совпадать с DTO");
            assertEquals(LocationState.APPROVED, savedLocation.getState(),
                    "Состояние локации при админском создании должно быть APPROVED");
            return true;
        }));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLocationNotFoundOnUpdateByAdmin() {
        LocationUpdateAdminDto dto = getLocationUpdateAdminDto();

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> locationService.updateLocationByAdmin(LOCATION_ID, dto));
    }

    @Test
    void shouldThrowConditionNotMetExceptionWhenUpdatingNonPendingLocation() {
        LocationUpdateUserDto dto = getLocationUpdateUserDto();
        Location existingLocation = getLocation();
        existingLocation.setState(LocationState.APPROVED);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));

        ConditionNotMetException exception = assertThrows(
                ConditionNotMetException.class,
                () -> locationService.updateLocation(LOCATION_ID, USER_ID, dto),
                "Ожидалось исключение ConditionNotMetException при обновлении опубликованной локации"
        );

        assertTrue(exception.getMessage().contains("Невозможно обновить опубликованную/отменённую локацию"));
    }

    @Test
    void shouldThrowNoAccessExceptionWhenUserIsNotCreator() {
        LocationUpdateUserDto dto = getLocationUpdateUserDto();
        Location existingLocation = getLocation();
        existingLocation.setCreator(User.builder().id(2L).build());

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));

        NoAccessException exception = assertThrows(
                NoAccessException.class,
                () -> locationService.updateLocation(LOCATION_ID, USER_ID, dto),
                "Ожидалось исключение NoAccessException при попытке редактирования чужой локации"
        );

        assertTrue(exception.getMessage().contains("Только создатель может редактировать данную локацию"));
    }

    @Test
    void shouldGetApprovedLocationSuccessfully() {
        Location location = getLocation();
        location.setState(LocationState.APPROVED);
        LocationDtoResponse expectedResponse = getLocationDtoResponse();

        when(locationRepository.findByIdAndState(LOCATION_ID, LocationState.APPROVED))
                .thenReturn(Optional.of(location));

        LocationDtoResponse actualResponse = locationService.getApprovedLocations(LOCATION_ID);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenApprovedLocationNotFound() {
        when(locationRepository.findByIdAndState(LOCATION_ID, LocationState.APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> locationService.getApprovedLocations(LOCATION_ID));
    }

    @Test
    void shouldUpdateLocationByAdminSuccessfullyWhenAllFieldsProvided() {
        LocationUpdateAdminDto dto = LocationUpdateAdminDto.builder()
                .name("Updated Name")
                .address("Updated Address")
                .latitude(60.0)
                .longitude(40.0)
                .state(LocationState.REJECTED)
                .build();

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);
        existingLocation.setName("Old Name");
        existingLocation.setAddress("Old Address");
        existingLocation.setState(LocationState.APPROVED);
        existingLocation.setLatitude(50.0);
        existingLocation.setLongitude(30.0);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findDuplicates(
                eq("Updated Name"),
                eq(60.0),
                eq(40.0),
                eq(NEARBY_RADIUS)
        )).thenReturn(Optional.empty());

        LocationFullDtoResponse actualResponse = locationService.updateLocationByAdmin(LOCATION_ID, dto);

        assertNotNull(actualResponse, "Ответ не должен быть null");
        assertEquals("Updated Name", actualResponse.getName(), "Имя локации должно быть обновлено");
        assertEquals("Updated Address", actualResponse.getAddress(), "Адрес локации должен быть обновлён");
        assertEquals(60.0, actualResponse.getLatitude(), "Широта должна быть обновлена");
        assertEquals(40.0, actualResponse.getLongitude(), "Долгота должна быть обновлена");
        assertEquals(LocationState.REJECTED, actualResponse.getState(), "Состояние локации должно быть обновлено");

        verify(locationRepository, times(1)).findById(LOCATION_ID);
        verify(locationRepository, times(1)).findDuplicates(
                eq("Updated Name"), eq(60.0), eq(40.0), eq(NEARBY_RADIUS));

        assertEquals("Updated Name", existingLocation.getName());
        assertEquals("Updated Address", existingLocation.getAddress());
        assertEquals(60.0, existingLocation.getLatitude());
        assertEquals(40.0, existingLocation.getLongitude());
        assertEquals(LocationState.REJECTED, existingLocation.getState());
    }

    @Test
    void shouldUpdateLocationByAdminWhenOnlyNameChanged() {
        LocationUpdateAdminDto dto = LocationUpdateAdminDto.builder()
                .name("New Name")
                .build();

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);
        existingLocation.setName("Old Name");
        existingLocation.setAddress("Old Address");
        existingLocation.setLatitude(50.0);
        existingLocation.setLongitude(30.0);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findDuplicates(
                eq("New Name"),
                eq(50.0),
                eq(30.0),
                eq(NEARBY_RADIUS)
        )).thenReturn(Optional.empty());

        LocationFullDtoResponse actualResponse = locationService.updateLocationByAdmin(LOCATION_ID, dto);

        assertNotNull(actualResponse, "Ответ не должен быть null");
        assertEquals("New Name", actualResponse.getName(), "Имя локации должно быть обновлено");
        assertEquals("Old Address", actualResponse.getAddress(), "Адрес должен остаться прежним");
        assertEquals(50.0, actualResponse.getLatitude(), "Широта должна остаться прежней");
        assertEquals(30.0, actualResponse.getLongitude(), "Долгота должна остаться прежней");

        verify(locationRepository, times(1)).findById(LOCATION_ID);
        verify(locationRepository, times(1)).findDuplicates(
                eq("New Name"), eq(50.0), eq(30.0), eq(NEARBY_RADIUS));

        assertEquals("New Name", existingLocation.getName(),
                "Имя локации в существующем объекте должно быть обновлено до 'New Name'");
        assertEquals("Old Address", existingLocation.getAddress(),
                "Адрес в существующем объекте не должен измениться");
        assertEquals(50.0, existingLocation.getLatitude(),
                "Широта в существующем объекте не должна измениться");
        assertEquals(30.0, existingLocation.getLongitude(),
                "Долгота в существующем объекте не должна измениться");
        assertEquals(LOCATION_ID, existingLocation.getId(),
                "id локации в существующем объекте должен остаться неизменным");
    }

    @Test
    void shouldUpdateLocationSuccessfullyWhenUserIsCreatorAndStateIsPending() {
        LocationUpdateUserDto dto = LocationUpdateUserDto.builder()
                .name("Updated User Location")
                .address("User Address")
                .latitude(65.0)
                .longitude(45.0)
                .build();

        User creator = getUser();
        creator.setId(USER_ID);

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);
        existingLocation.setName("Old Name");
        existingLocation.setAddress("Old Address");
        existingLocation.setState(LocationState.PENDING);
        existingLocation.setLatitude(50.0);
        existingLocation.setLongitude(30.0);
        existingLocation.setCreator(creator);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findDuplicates(
                eq("Updated User Location"), eq(65.0), eq(45.0), eq(NEARBY_RADIUS)
        )).thenReturn(Optional.empty());

        LocationPrivateDtoResponse actualResponse = locationService.updateLocation(LOCATION_ID, USER_ID, dto);

        assertNotNull(actualResponse, "Ответ не должен быть null");
        assertEquals("Updated User Location", actualResponse.getName(), "Имя локации должно быть обновлено");
        assertEquals("User Address", actualResponse.getAddress(), "Адрес локации должен быть обновлён");
        assertEquals(65.0, actualResponse.getLatitude(), "Широта должна быть обновлена");
        assertEquals(45.0, actualResponse.getLongitude(), "Долгота должна быть обновлена");

        verify(locationRepository, times(1)).findById(LOCATION_ID);
        verify(locationRepository, times(1)).findDuplicates(
                eq("Updated User Location"), eq(65.0), eq(45.0), eq(NEARBY_RADIUS));

        assertEquals("Updated User Location", existingLocation.getName(),
                "Имя локации в существующем объекте должно быть обновлено до 'Updated User Location'");
        assertEquals("User Address", existingLocation.getAddress(),
                "Адрес в существующем объекте должен быть обновлён до 'User Address'");
        assertEquals(65.0, existingLocation.getLatitude(),
                "Широта в существующем объекте должна быть обновлена до 65.0");
        assertEquals(45.0, existingLocation.getLongitude(),
                "Долгота в существующем объекте должна быть обновлена до 45.0");

        assertEquals(LocationState.PENDING, existingLocation.getState(),
                "Состояние локации в существующем объекте должно остаться PENDING");

        assertEquals(USER_ID, existingLocation.getCreator().getId(),
                "Создатель локации в существующем объекте должен остаться неизменным");

        assertEquals(LOCATION_ID, existingLocation.getId(),
                "id локации в существующем объекте должен остаться неизменным");
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenUserIsNotCreator() {
        LocationUpdateUserDto dto = LocationUpdateUserDto.builder()
                .name("New Name")
                .address("New Address")
                .latitude(55.0)
                .longitude(35.0)
                .build();

        User otherUser = getUser();
        otherUser.setId(200L); // Другой пользователь

        User actualCreator = getUser();
        actualCreator.setId(USER_ID); // Настоящий создатель

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);
        existingLocation.setState(LocationState.PENDING);
        existingLocation.setCreator(actualCreator); // Создатель — другой пользователь

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));

        NoAccessException exception = assertThrows(NoAccessException.class,
                () -> locationService.updateLocation(LOCATION_ID, otherUser.getId(), dto));

        assertEquals("Только создатель может редактировать данную локацию", exception.getMessage(),
                "Сообщение об ошибке должно указывать на отсутствие прав доступа");

        // Верификация: save() не должен вызываться при ошибке доступа
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLocationDoesNotExist() {
        LocationUpdateUserDto dto = LocationUpdateUserDto.builder()
                .name("New Name")
                .address("New Address")
                .latitude(55.0)
                .longitude(35.0)
                .build();

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> locationService.updateLocation(LOCATION_ID, USER_ID, dto));

        // Верификация: никаких дальнейших вызовов при отсутствии локации
        verify(locationRepository, never()).findDuplicates(any(), anyDouble(), anyDouble(), anyDouble());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldThrowDuplicateExceptionWhenDuplicateLocationFound() {
        LocationUpdateUserDto dto = LocationUpdateUserDto.builder()
                .name("Duplicate Name")
                .address("Duplicate Address")
                .latitude(70.0)
                .longitude(50.0)
                .build();

        User creator = getUser();
        creator.setId(USER_ID);

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);
        existingLocation.setState(LocationState.PENDING);
        existingLocation.setCreator(creator);

        Location duplicateLocation = getLocation();
        duplicateLocation.setId(2L);
        duplicateLocation.setState(LocationState.APPROVED);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findDuplicates(
                eq("Duplicate Name"), eq(70.0), eq(50.0), eq(NEARBY_RADIUS)
        )).thenReturn(Optional.of(duplicateLocation));

        assertThrows(DuplicateLocationsException.class,
                () -> locationService.updateLocation(LOCATION_ID, USER_ID, dto));

        // Верификация: save() не должен вызываться при обнаружении дубликата
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void shouldThrowDuplicateExceptionWhenDuplicateFound() {
        LocationUpdateAdminDto dto = LocationUpdateAdminDto.builder()
                .name("Duplicate Name")
                .latitude(60.0)
                .longitude(40.0)
                .build();

        Location existingLocation = getLocation();
        existingLocation.setId(LOCATION_ID);

        Location duplicateLocation = getLocation();
        duplicateLocation.setId(2L);
        duplicateLocation.setState(LocationState.APPROVED);

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.findDuplicates(
                eq("Duplicate Name"), eq(60.0), eq(40.0), eq(NEARBY_RADIUS)
        )).thenReturn(Optional.of(duplicateLocation));

        assertThrows(DuplicateLocationsException.class,
                () -> locationService.updateLocationByAdmin(LOCATION_ID, dto));

        // Верификация: save() не должен вызываться при обнаружении дубликата
        verify(locationRepository, never()).save(any());
    }

    @Test
    void shouldDeleteLocationByAdminSuccessfully() {
        when(eventRepository.existsByLocationId(LOCATION_ID)).thenReturn(false);

        assertDoesNotThrow(() -> locationService.deleteLocationByAdmin(LOCATION_ID));

        verify(eventRepository, times(1)).existsByLocationId(LOCATION_ID);
        verify(locationRepository, times(1)).deleteById(LOCATION_ID);

        // другие методы не вызывались
        verifyNoMoreInteractions(eventRepository, locationRepository);
    }

    @Test
    void shouldThrowConditionNotMetExceptionWhenLocationHasEvents() {
        when(eventRepository.existsByLocationId(LOCATION_ID)).thenReturn(true);

        ConditionNotMetException exception = assertThrows(
                ConditionNotMetException.class,
                () -> locationService.deleteLocationByAdmin(LOCATION_ID),
                "Ожидалось исключение ConditionNotMetException при наличии событий у локации"
        );

        String actualMessage = exception.getMessage();
        assertNotNull(actualMessage, "Сообщение исключения не должно быть null");

        boolean containsExpectedText = actualMessage.contains(
                "Невозможно удалить локацию: есть связанные события"
        );

        assertTrue(containsExpectedText);
        verify(eventRepository, times(1)).existsByLocationId(LOCATION_ID);
        verify(locationRepository, never()).deleteById(LOCATION_ID);
    }

    @Test
    void shouldGetAllLocationsByAdminFilterSuccessfully() {
        LocationAdminFilter filter = getLocationAdminFilter();
        List<Location> locations = List.of(getLocation());

        // Создаём спецификации для каждого условия фильтра
        Specification<Location> textSpec = LocationSpecifications.withTextContains(filter.getText());
        Specification<Location> stateSpec = LocationSpecifications.withState(filter.getState());
        Specification<Location> eventsCountSpec = LocationSpecifications.withEventsCount(
                filter.getMinEvents(),
                filter.getMaxEvents()
        );
        Specification<Location> coordinatesSpec = LocationSpecifications.withCoordinates(filter.getZone());

        // Композируем спецификации с помощью and(), фильтруя null-значения
        Specification<Location> expectedSpec = Specification.unrestricted();

        if (textSpec != null) {
            expectedSpec = expectedSpec.and(textSpec);
        }
        if (stateSpec != null) {
            expectedSpec = expectedSpec.and(stateSpec);
        }

        expectedSpec = expectedSpec.and(eventsCountSpec);

        if (coordinatesSpec != null) {
            expectedSpec.and(coordinatesSpec);
        }

        // Используем ArgumentCaptor для перехвата аргументов вызова
        ArgumentCaptor specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // Настройка мока с использованием матчеров
        when(locationRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(locations));

        // Выполнение тестируемого метода
        Collection<LocationFullDtoResponse> responses = locationService.getAllLocationsByAdminFilter(filter);

        // Проверка результатов
        assertFalse(responses.isEmpty(), "Ответ не должен быть пустым");
        assertEquals(1, responses.size(), "Должно быть возвращено 1 локация");

        // Верификация вызовов репозитория с перехватом аргументов
        verify(locationRepository, times(1)).findAll(
                (Specification<Location>) specCaptor.capture(),
                pageableCaptor.capture()
        );

        // Дополнительная проверка перехваченных аргументов
        specCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        // Проверяем, что Pageable настроен корректно (например, страница 0, размер 10)
        assertEquals(0, capturedPageable.getPageNumber(), "Номер страницы должен быть 0");
        assertEquals(10, capturedPageable.getPageSize(), "Размер страницы должен быть 10");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFoundOnPrivateFilter() {
        LocationPrivateFilter filter = getLocationPrivateFilter();

        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> locationService.getAllLocationsByFilter(USER_ID, filter));
    }

    @Test
    void shouldGetLocationByIdForAdminSuccessfully() {
        Location location = getLocation();
        LocationFullDtoResponse expectedResponse = getLocationFullDtoResponse();

        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));

        LocationFullDtoResponse actualResponse = locationService.getByIdForAdmin(LOCATION_ID);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLocationNotFoundForAdmin() {
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> locationService.getByIdForAdmin(LOCATION_ID));
    }

    @Test
    void shouldGetAllLocationsByPrivateFilterSuccessfully() {
        LocationPrivateFilter filter = getLocationPrivateFilter();
        List<Location> locations = List.of(getLocation());

        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // Создаём спецификации для каждого условия фильтра
        Specification<Location> textSpec = LocationSpecifications.withTextContains(filter.getText());
        Specification<Location> creatorSpec = LocationSpecifications.withCreator(USER_ID);
        Specification<Location> stateSpec = LocationSpecifications.withState(filter.getState());

        // Композируем спецификации с помощью and(), фильтруя null-значения
        Specification<Location> expectedSpec = Specification.unrestricted();

        if (textSpec != null) {
            expectedSpec = expectedSpec.and(textSpec);
        }
        if (creatorSpec != null) {
            expectedSpec = expectedSpec.and(creatorSpec);
        }
        if (stateSpec != null) {
            expectedSpec.and(stateSpec);
        }

        // Используем ArgumentCaptor для перехвата аргументов вызова
        ArgumentCaptor<Specification<Location>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // Настройка мока с использованием матчеров
        when(locationRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(locations));

        // Выполнение тестируемого метода
        Collection<LocationPrivateDtoResponse> responses = locationService.getAllLocationsByFilter(USER_ID, filter);

        // Проверка результатов
        assertFalse(responses.isEmpty(), "Ответ не должен быть пустым");
        assertEquals(1, responses.size(), "Должно быть возвращено 1 локация");

        // Проверяем первый элемент
        LocationPrivateDtoResponse actualResponse = responses.iterator().next();
        LocationPrivateDtoResponse expectedResponse = getLocationPrivateDtoResponse();
        assertEquals(expectedResponse.getId(), actualResponse.getId(), "ID локации должен совпадать");
        assertEquals(expectedResponse.getName(), actualResponse.getName(), "Имя локации должно совпадать");
        assertEquals(expectedResponse.getAddress(), actualResponse.getAddress(), "Адрес локации должен совпадать");

        // Верификация вызовов репозиториев с захватом аргументов
        verify(userRepository, times(1)).existsById(USER_ID);
        verify(locationRepository, times(1)).findAll(
                specCaptor.capture(),
                pageableCaptor.capture()
        );

        // Дополнительная проверка перехваченных аргументов
        specCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        // Проверяем, что Pageable настроен корректно
        assertEquals(0, capturedPageable.getPageNumber(), "Номер страницы должен быть 0");
        assertEquals(10, capturedPageable.getPageSize(), "Размер страницы должен быть 10");
    }

    @Test
    void shouldGetAllLocationsByPublicFilterSuccessfully() {
        LocationPublicFilter filter = getLocationPublicFilter();
        List<Location> locations = List.of(getLocation());

        // Создаём спецификации для каждого условия фильтра
        Specification<Location> textSpec = LocationSpecifications.withTextContains(filter.getText());
        Specification<Location> coordinatesSpec = LocationSpecifications.withCoordinates(filter.getZone());

        // Композируем спецификации с помощью and(), фильтруя null-значения
        Specification<Location> expectedSpec = Specification.unrestricted();

        if (textSpec != null) {
            expectedSpec = expectedSpec.and(textSpec);
        }
        if (coordinatesSpec != null) {
            expectedSpec.and(coordinatesSpec);
        }

        // Используем ArgumentCaptor для перехвата аргументов вызова
        ArgumentCaptor<Specification<Location>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // Настройка мока с использованием матчеров
        when(locationRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(locations));

        // Выполнение тестируемого метода
        Collection<LocationDtoResponse> responses = locationService.getAllLocationsByFilter(filter);

        // Проверка результатов
        assertFalse(responses.isEmpty(), "Ответ не должен быть пустым");
        assertEquals(1, responses.size(), "Должно быть возвращено 1 локация");

        // Проверяем первый элемент
        LocationDtoResponse actualResponse = responses.iterator().next();
        LocationDtoResponse expectedResponse = getLocationDtoResponse();
        assertEquals(expectedResponse.getId(), actualResponse.getId(), "ID локации должен совпадать");
        assertEquals(expectedResponse.getName(), actualResponse.getName(), "Имя локации должно совпадать");
        assertEquals(expectedResponse.getAddress(), actualResponse.getAddress(), "Адрес локации должен совпадать");

        // Верификация вызовов репозитория с захватом аргументов
        verify(locationRepository, times(1)).findAll(
                specCaptor.capture(),
                pageableCaptor.capture()
        );

        // Дополнительная проверка перехваченных аргументов
        specCaptor.getValue();
        Pageable capturedPageable = pageableCaptor.getValue();

        // Проверяем, что Pageable настроен корректно
        assertEquals(0, capturedPageable.getPageNumber(), "Номер страницы должен быть 0");
        assertEquals(10, capturedPageable.getPageSize(), "Размер страницы должен быть 10");
    }

    // Вспомогательные методы
    private NewLocationDto getNewLocationDto() {
        return NewLocationDto.builder()
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();
    }

    private User getUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Test User");
        user.setEmail("user@test.com");
        return user;
    }

    private Location getLocation() {
        return Location.builder()
                .id(LOCATION_ID)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.PENDING)
                .build();
    }

    private LocationPrivateDtoResponse getLocationPrivateDtoResponse() {
        return LocationPrivateDtoResponse.builder()
                .id(LOCATION_ID)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();
    }

    private LocationFullDtoResponse getLocationFullDtoResponse() {
        return LocationFullDtoResponse.builder()
                .id(LOCATION_ID)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .state(LocationState.PENDING)
                .build();
    }

    private LocationDtoResponse getLocationDtoResponse() {
        return LocationDtoResponse.builder()
                .id(LOCATION_ID)
                .name("Test Location")
                .address("Test Address")
                .latitude(55.751244)
                .longitude(37.618423)
                .build();
    }

    private LocationUpdateAdminDto getLocationUpdateAdminDto() {
        return LocationUpdateAdminDto.builder()
                .name("Updated Location")
                .address("Updated Address")
                .latitude(60.0)
                .longitude(40.0)
                .state(LocationState.APPROVED)
                .build();
    }

    private LocationUpdateUserDto getLocationUpdateUserDto() {
        return LocationUpdateUserDto.builder()
                .name("Updated User Location")
                .address("Updated User Address")
                .latitude(65.0)
                .longitude(45.0)
                .build();
    }

    private LocationAdminFilter getLocationAdminFilter() {
        return LocationAdminFilter.builder()
                .text("test")
                .creator(null)
                .state(LocationState.PENDING)
                .zone(null)
                .minEvents(null)
                .maxEvents(null)
                .offset(0)
                .limit(10)
                .pageable(null)
                .build();
    }

    private LocationPrivateFilter getLocationPrivateFilter() {
        return LocationPrivateFilter.builder()
                .text("test")
                .state(LocationState.PENDING)
                .zone(null)
                .offset(0)
                .limit(10)
                .pageable(null)
                .build();
    }

    private LocationPublicFilter getLocationPublicFilter() {
        Zone zone = Zone.builder()
                .latitude(55.751244)
                .longitude(37.618423)
                .radius(1000.0)
                .build();

        return LocationPublicFilter.builder()
                .text("test")
                .zone(zone)
                .offset(0)
                .limit(10)
                .pageable(null)
                .build();
    }
}
