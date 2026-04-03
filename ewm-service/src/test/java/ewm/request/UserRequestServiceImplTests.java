package ewm.request;

import ewm.exception.*;
import ewm.request.dto.*;
import ewm.event.model.*;
import ewm.request.model.*;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.request.service.UserRequestServiceImpl;
import ewm.request.repository.UserRequestRepository;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserRequestServiceImplTests {

    @InjectMocks
    private UserRequestServiceImpl userRequestService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRequestRepository requestRepository;

    @Test
    void shouldCreateRequestSuccessfully() {
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("Test User").email("test@example.com").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(User.builder().id(2L).build())
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(false);
        when(requestRepository.countByEventIdAndStatus(eventId, UserRequestStatus.CONFIRMED)).thenReturn(5);

        UserRequest savedRequest = new UserRequest();
        savedRequest.setId(100L);
        savedRequest.setRequester(user);
        savedRequest.setEvent(event);
        savedRequest.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        savedRequest.setStatus(UserRequestStatus.PENDING);

        when(requestRepository.save(any(UserRequest.class))).thenReturn(savedRequest);

        UserRequestDto result = userRequestService.createUserRequest(userId, eventId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(userId, result.getRequester());
        assertEquals(eventId, result.getEvent());
        assertEquals("PENDING", result.getStatus());

        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).existsByRequesterIdAndEventId(userId, eventId);
        verify(requestRepository, times(1)).save(any(UserRequest.class));
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        Long userId = 999L;
        Long eventId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userRequestService.createUserRequest(userId, eventId));

        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, never()).findById(any());
    }

    @Test
    void shouldConfirmAndRejectBasedOnSlots() {
        Long userId = 2L; // инициатор события
        Long eventId = 10L;

        // Создаём пользователя‑заявителя для запросов
        User requester1 = User.builder()
                .id(3L)
                .name("Requester 1")
                .email("requester1@example.com")
                .build();

        User requester2 = User.builder()
                .id(4L)
                .name("Requester 2")
                .email("requester2@example.com")
                .build();

        // Создаём инициатора события
        User initiator = User.builder()
                .id(userId)
                .name("Initiator")
                .email("initiator@example.com")
                .build();

        Event event = Event.builder()
                .id(eventId)
                .initiator(initiator)
                .participantLimit(2)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .build();

        // Инициализируем запросы с полными данными
        UserRequest request1 = new UserRequest();
        request1.setId(1L);
        request1.setStatus(UserRequestStatus.PENDING);
        request1.setEvent(event);
        request1.setRequester(requester1);
        request1.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        UserRequest request2 = new UserRequest();
        request2.setId(2L);
        request2.setStatus(UserRequestStatus.PENDING);
        request2.setEvent(event);
        request2.setRequester(requester2);
        request2.setCreated(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MILLIS));

        List<Long> requestIds = List.of(1L, 2L);
        EventRequestStatusUpdateDto updateDto = new EventRequestStatusUpdateDto(requestIds, "CONFIRMED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(requestIds)).thenReturn(List.of(request1, request2));
        when(requestRepository.countByEventIdAndStatus(eventId, UserRequestStatus.CONFIRMED)).thenReturn(1); // уже 1 подтверждённый

        EventRequestStatusUpdateResponse result = userRequestService.updateRequestStatus(userId, eventId, updateDto);

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(1, result.getRejectedRequests().size());

        // Проверяем статусы в результатах
        assertEquals("CONFIRMED", result.getConfirmedRequests().getFirst().getStatus());
        assertEquals("REJECTED", result.getRejectedRequests().getFirst().getStatus());

        // Проверяем ID запросов в результатах
        assertEquals(1L, result.getConfirmedRequests().getFirst().getId());
        assertEquals(2L, result.getRejectedRequests().getFirst().getId());

        // Проверяем ID событий в результатах
        assertEquals(eventId, result.getConfirmedRequests().getFirst().getEvent());
        assertEquals(eventId, result.getRejectedRequests().getFirst().getEvent());

        // Проверяем ID заявителей в результатах
        assertEquals(3L, result.getConfirmedRequests().getFirst().getRequester());
        assertEquals(4L, result.getRejectedRequests().getFirst().getRequester());

        // Проверяем временные метки
        assertNotNull(result.getConfirmedRequests().getFirst().getCreated());
        assertNotNull(result.getRejectedRequests().getFirst().getCreated());

        // Изменяем верификацию: ожидаем 2 вызова saveAll()
        verify(requestRepository, times(2)).saveAll(anyList());

        // Дополнительно проверяем, что статусы действительно изменились в сохранённых объектах
        ArgumentCaptor<List<UserRequest>> savedRequestsCaptor = ArgumentCaptor.forClass(List.class);
        verify(requestRepository, atLeast(1)).saveAll(savedRequestsCaptor.capture());

        List<List<UserRequest>> allSavedRequests = savedRequestsCaptor.getAllValues();
        assertEquals(2, allSavedRequests.size(), "Должно быть 2 вызова saveAll");

        // Объединяем все сохранённые запросы для проверки
        List<UserRequest> allRequests = allSavedRequests.stream()
                .flatMap(List::stream)
                .toList();

        assertEquals(2, allRequests.size());

        // Проверяем новые статусы сохранённых запросов
        boolean hasConfirmed = allRequests.stream()
                .anyMatch(r -> r.getId().equals(1L) && r.getStatus() == UserRequestStatus.CONFIRMED);
        boolean hasRejected = allRequests.stream()
                .anyMatch(r -> r.getId().equals(2L) && r.getStatus() == UserRequestStatus.REJECTED);

        assertTrue(hasConfirmed, "Один запрос должен быть подтверждён");
        assertTrue(hasRejected, "Один запрос должен быть отклонён");
    }

    @Test
    void shouldReturnUserRequests() {
        Long userId = 1L;

        // Создаём пользователя-заявителя
        User requester = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .build();

        // Создаём событие для привязки к запросам
        Event event = Event.builder()
                .id(10L)
                .title("Test Event")
                .build();

        UserRequest request1 = new UserRequest();
        request1.setId(1L);
        request1.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        request1.setStatus(UserRequestStatus.PENDING);
        request1.setEvent(event);      // Добавляем событие
        request1.setRequester(requester); // Добавляем заявителя

        UserRequest request2 = new UserRequest();
        request2.setId(2L);
        request2.setCreated(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MILLIS));
        request2.setStatus(UserRequestStatus.CONFIRMED);
        request2.setEvent(event);      // Добавляем событие
        request2.setRequester(requester); // Добавляем заявителя

        when(userRepository.existsById(userId)).thenReturn(true);
        when(requestRepository.findAllByRequesterId(userId)).thenReturn(List.of(request1, request2));

        List<UserRequestDto> result = userRequestService.getUserRequests(userId);

        assertEquals(2, result.size());

        // Проверяем первый запрос
        assertEquals("PENDING", result.getFirst().getStatus());
        assertEquals(1L, result.getFirst().getId());
        assertEquals(10L, result.getFirst().getEvent()); // ID события
        assertEquals(userId, result.getFirst().getRequester()); // ID заявителя

        // Проверяем второй запрос
        assertEquals("CONFIRMED", result.get(1).getStatus());
        assertEquals(2L, result.get(1).getId());
        assertEquals(10L, result.get(1).getEvent()); // ID события
        assertEquals(userId, result.get(1).getRequester()); // ID заявителя

        assertNotNull(result.get(0).getCreated());
        assertNotNull(result.get(1).getCreated());

        verify(userRepository, times(1)).existsById(userId);
        verify(requestRepository, times(1)).findAllByRequesterId(userId);
    }

    @Test
    void shouldReturnRequestsForEventInitiator() {
        Long eventId = 10L;
        Long userId = 2L; // инициатор

        // Создаём пользователя‑заявителя для запроса
        User requester = User.builder()
                .id(3L)
                .name("Requester")
                .email("requester@example.com")
                .build();

        // Создаём инициатора события
        User initiator = User.builder()
                .id(userId)
                .name("Initiator")
                .email("initiator@example.com")
                .build();

        Event event = Event.builder()
                .id(eventId)
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .build();

        // Инициализируем запрос с полными данными
        UserRequest request = new UserRequest();
        request.setId(1L);
        request.setStatus(UserRequestStatus.PENDING);
        request.setEvent(event);
        request.setRequester(requester); // Добавляем заявителя
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        when(userRepository.findById(userId)).thenReturn(Optional.of(initiator));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(List.of(request));

        List<UserRequestDto> result = userRequestService.getRequestsForEvent(eventId, userId);

        assertEquals(1, result.size());

        // Проверяем статус в результате
        assertEquals("PENDING", result.getFirst().getStatus());

        // Проверяем ID запроса
        assertEquals(1L, result.getFirst().getId());

        // Проверяем, что ID события корректно передаётся в DTO
        assertEquals(eventId, result.getFirst().getEvent());

        // Проверяем ID заявителя
        assertEquals(3L, result.getFirst().getRequester());

        // Проверяем временную метку
        assertNotNull(result.getFirst().getCreated());

        // Проверяем, что все зависимости были вызваны ожидаемое количество раз
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllByEventId(eventId);
    }

    @Test
    void shouldThrowWhenNotInitiator() {
        Long eventId = 10L;
        Long userId = 3L; // не инициатор
        User otherUser = User.builder().id(userId).build();

        Event event = Event.builder()
                .id(eventId)
                .initiator(User.builder().id(2L).build()) // другой инициатор
                .state(EventState.PUBLISHED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(otherUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(NoAccessException.class,
                () -> userRequestService.getRequestsForEvent(eventId, userId));

        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void shouldCancelUserRequest() {
        Long userId = 1L;
        Long requestId = 100L;

        // Создаём событие для привязки к запросу
        Event event = Event.builder()
                .id(10L)
                .title("Test Event")
                .build();

        User user = User.builder().id(userId).name("Test User").email("test@example.com").build();
        UserRequest request = new UserRequest();
        request.setId(requestId);
        request.setRequester(user);
        request.setStatus(UserRequestStatus.PENDING);
        request.setEvent(event); // Добавляем событие
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        UserRequest savedRequest = new UserRequest();
        savedRequest.setId(requestId);
        savedRequest.setStatus(UserRequestStatus.CANCELED);
        savedRequest.setEvent(event);      // Сохраняем связь с событием
        savedRequest.setRequester(user); // Сохраняем связь с пользователем
        savedRequest.setCreated(request.getCreated()); // Сохраняем временную метку

        when(requestRepository.save(any(UserRequest.class))).thenReturn(savedRequest);

        UserRequestDto result = userRequestService.cancelRequest(userId, requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getId());
        assertEquals("CANCELED", result.getStatus());

        // Проверяем ID события в результате
        assertEquals(10L, result.getEvent());

        // Проверяем ID заявителя в результате
        assertEquals(userId, result.getRequester());

        // Проверяем временную метку
        assertNotNull(result.getCreated());

        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, times(1)).save(any(UserRequest.class));
    }

    @Test
    void shouldThrowWhenRequestNotFound() {
        Long userId = 1L;
        Long requestId = 999L;

        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userRequestService.cancelRequest(userId, requestId));

        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNotRequestOwner() {
        Long userId = 2L; // другой пользователь
        Long requestId = 100L;

        User otherUser = User.builder().id(3L).build(); // владелец запроса
        UserRequest request = new UserRequest();
        request.setId(requestId);
        request.setRequester(otherUser);
        request.setStatus(UserRequestStatus.PENDING);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ForbiddenException.class,
                () -> userRequestService.cancelRequest(userId, requestId));

        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void shouldRejectAllRequests() {
        Long userId = 2L;
        Long eventId = 10L;

        // Создаём пользователя‑заявителя для запросов
        User requester1 = User.builder()
                .id(3L)
                .name("Requester 1")
                .email("requester1@example.com")
                .build();

        User requester2 = User.builder()
                .id(4L)
                .name("Requester 2")
                .email("requester2@example.com")
                .build();

        // Создаём инициатора события
        User initiator = User.builder()
                .id(userId)
                .name("Initiator")
                .email("initiator@example.com")
                .build();

        Event event = Event.builder()
                .id(eventId)
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .build();

        // Инициализируем запросы с полными данными
        UserRequest request1 = new UserRequest();
        request1.setId(1L);
        request1.setStatus(UserRequestStatus.PENDING);
        request1.setEvent(event);
        request1.setRequester(requester1); // Добавляем заявителя
        request1.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        UserRequest request2 = new UserRequest();
        request2.setId(2L);
        request2.setStatus(UserRequestStatus.PENDING);
        request2.setEvent(event);
        request2.setRequester(requester2); // Добавляем заявителя
        request2.setCreated(LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.MILLIS));

        List<Long> requestIds = List.of(1L, 2L);
        EventRequestStatusUpdateDto updateDto = new EventRequestStatusUpdateDto(requestIds, "REJECTED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(requestIds)).thenReturn(List.of(request1, request2));

        EventRequestStatusUpdateResponse result = userRequestService.updateRequestStatus(userId, eventId, updateDto);

        assertEquals(0, result.getConfirmedRequests().size());
        assertEquals(2, result.getRejectedRequests().size());

        // Проверяем статусы в результатах
        assertEquals("REJECTED", result.getRejectedRequests().get(0).getStatus());
        assertEquals("REJECTED", result.getRejectedRequests().get(1).getStatus());

        // Проверяем ID запросов в результатах
        assertEquals(1L, result.getRejectedRequests().get(0).getId());
        assertEquals(2L, result.getRejectedRequests().get(1).getId());

        // Проверяем ID событий в результатах
        assertEquals(eventId, result.getRejectedRequests().get(0).getEvent());
        assertEquals(eventId, result.getRejectedRequests().get(1).getEvent());

        // Проверяем ID заявителей в результатах
        assertEquals(3L, result.getRejectedRequests().get(0).getRequester());
        assertEquals(4L, result.getRejectedRequests().get(1).getRequester());

        // Проверяем временные метки
        assertNotNull(result.getRejectedRequests().get(0).getCreated());
        assertNotNull(result.getRejectedRequests().get(1).getCreated());

        // Проверяем, что все запросы были сохранены (через saveAll)
        verify(requestRepository, times(1)).saveAll(anyList());

        // Дополнительно проверяем, что статусы действительно изменились в сохранённых объектах
        ArgumentCaptor<List<UserRequest>> savedRequestsCaptor = ArgumentCaptor.forClass(List.class);
        verify(requestRepository).saveAll(savedRequestsCaptor.capture());

        List<UserRequest> savedRequests = savedRequestsCaptor.getValue();
        assertEquals(2, savedRequests.size());

        // Проверяем новые статусы сохранённых запросов
        boolean allRejected = savedRequests.stream()
                .allMatch(r -> r.getStatus() == UserRequestStatus.REJECTED);

        assertTrue(allRejected, "Все запросы должны быть отклонены");

        // Проверяем, что ID сохранённых запросов соответствуют ожидаемым
        Set<Long> savedIds = savedRequests.stream()
                .map(UserRequest::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(1L, 2L), savedIds);
    }

    @Test
    void shouldThrowForInvalidStatus() {
        Long userId = 2L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .initiator(User.builder().id(userId).build())
                .state(EventState.PUBLISHED)
                .build();

        List<Long> requestIds = List.of(1L);
        EventRequestStatusUpdateDto updateDto = new EventRequestStatusUpdateDto(requestIds, "INVALID_STATUS");

        // Мокаем репозитории, чтобы событие находилось
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        // Для метода findAllById возвращаем пустой список — это нормально для теста некорректного статуса
        when(requestRepository.findAllById(requestIds)).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userRequestService.updateRequestStatus(userId, eventId, updateDto));

        assertTrue(exception.getMessage().contains("Некорректный статус"));

        // Проверяем, что репозитории были вызваны (событие найдено, запросы получены)
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllById(requestIds);
    }

    @Test
    void shouldGetUserRequestThrowWhenUserNotFound() {
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> userRequestService.getUserRequests(userId));

        verify(userRepository, times(1)).existsById(userId);
        verify(requestRepository, never()).findAllByRequesterId(any());
    }

    @Test
    void shouldThrowWhenRequestAlreadyExists() {
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).build();
        Event event = Event.builder().id(eventId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(userId, eventId)).thenReturn(true);

        assertThrows(ConditionNotMetException.class,
                () -> userRequestService.createUserRequest(userId, eventId));

        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).existsByRequesterIdAndEventId(userId, eventId);
        verify(requestRepository, never()).save(any());
    }
}
