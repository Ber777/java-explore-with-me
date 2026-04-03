package ewm.request.service;

import ewm.exception.*;
import ewm.event.model.*;
import ewm.request.dto.*;
import ewm.request.model.*;
import ewm.user.model.User;
import ewm.request.UserRequestMapper;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.request.repository.UserRequestRepository;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static ewm.request.model.UserRequestStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRequestServiceImpl implements UserRequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserRequestRepository requestRepository;

    @Transactional
    public UserRequestDto createUserRequest(Long userId, Long eventId) {
        log.info("Пользователь {} пытается создать запрос на участие в событии {}", userId, eventId);

        User user = getUserById(userId);
        Event event = getEventById(eventId);

        validateRequestCreation(userId, event);

        UserRequestStatus status = calculateRequestStatus(event);

        UserRequest request = new UserRequest();
        request.setRequester(user);
        request.setEvent(event);
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        request.setStatus(status);

        log.info("Создана заявка от пользователя {} на событие {} со статусом {}", userId, eventId, status);
        return UserRequestMapper.toUserRequestDto(requestRepository.save(request));
    }

    @Transactional
    public EventRequestStatusUpdateResponse updateRequestStatus(Long userId,
                                                                Long eventId,
                                                                EventRequestStatusUpdateDto request) {
        Event event = getEventWithCheck(userId, eventId);

        List<UserRequest> requests = getPendingRequestsOrThrow(request.getRequestIds());

        return switch (request.getStatus()) {
            case "CONFIRMED" -> confirmRequests(event, requests);
            case "REJECTED" -> rejectRequests(requests);
            default -> throw new IllegalArgumentException("Некорректный статус: " + request.getStatus());
        };
    }

    @Override
    public List<UserRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(UserRequestMapper::toUserRequestDto)
                .toList();
    }

    @Override
    public List<UserRequestDto> getRequestsForEvent(Long eventId, Long userId) {
        log.debug("getRequestsForEvent: {} пользователя: {}", eventId, userId);

        getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NoAccessException("Только создатель может смотреть запросы события");
        }

        List<UserRequest> allByEventId = requestRepository.findAllByEventId(eventId);

        return allByEventId.stream()
                .map(UserRequestMapper::toUserRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public UserRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь {} отменяет заявку с id {}", userId, requestId);

        UserRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("Только создатель события может его отменить.");
        }

        request.setStatus(CANCELED);
        return UserRequestMapper.toUserRequestDto(requestRepository.save(request));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));
    }

    // Определяем будет ли заявка сразу подтверждена или в статусе ожидания (зависит от настроек события).
    private UserRequestStatus calculateRequestStatus(Event event) {
        return (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0)
                ? UserRequestStatus.CONFIRMED
                : UserRequestStatus.PENDING;
    }

    private Event getEventWithCheck(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        validateEventAccessAndState(userId, event);

        return event;
    }

    private List<UserRequest> getPendingRequestsOrThrow(List<Long> requestIds) {
        List<UserRequest> requests = requestRepository.findAllById(requestIds);
        boolean isNotPending = requests.stream()
                .anyMatch(r -> r.getStatus() != UserRequestStatus.PENDING);

        if (isNotPending) {
            throw new ConditionNotMetException("Запрос должен находиться в статусе ожидание");
        }

        return requests;
    }

    private int calculateAvailableSlots(Event event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), CONFIRMED);
        return limit == 0 ? Integer.MAX_VALUE : limit - (int) confirmedCount;
    }

    private EventRequestStatusUpdateResponse confirmRequests(Event event, List<UserRequest> requests) {
        checkIfLimitAvailableOrThrow(event);

        int availableSlots = calculateAvailableSlots(event);
        List<UserRequest> confirmed = new ArrayList<>();
        List<UserRequest> rejected = new ArrayList<>();

        for (UserRequest request : requests) {
            if (shouldAutoConfirm(event) || availableSlots > 0) {
                confirmRequest(request, confirmed);
                availableSlots--;
            } else {
                rejectRequest(request, rejected);
            }
        }

        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);

        return new EventRequestStatusUpdateResponse(
                confirmed.stream().map(UserRequestMapper::toUserRequestDto).toList(),
                rejected.stream().map(UserRequestMapper::toUserRequestDto).toList()
        );
    }

    private void checkIfLimitAvailableOrThrow(Event event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepository.countByEventIdAndStatus(event.getId(), UserRequestStatus.CONFIRMED);
        if (limit != 0 && Boolean.TRUE.equals(event.getRequestModeration()) && confirmedCount >= limit) {
            throw new ConditionNotMetException("Лимит на участие в событии достигнут");
        }
    }

    private boolean shouldAutoConfirm(Event event) {
        return event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration());
    }

    private void confirmRequest(UserRequest request, List<UserRequest> confirmed) {
        request.setStatus(UserRequestStatus.CONFIRMED);
        confirmed.add(request);
    }

    private void rejectRequest(UserRequest request, List<UserRequest> rejected) {
        request.setStatus(UserRequestStatus.REJECTED);
        rejected.add(request);
    }

    private EventRequestStatusUpdateResponse rejectRequests(List<UserRequest> requests) {
        requests.forEach(request -> request.setStatus(REJECTED));
        requestRepository.saveAll(requests);

        List<UserRequestDto> rejectedDtos = requests.stream()
                .map(UserRequestMapper::toUserRequestDto)
                .toList();

        return new EventRequestStatusUpdateResponse(List.of(), rejectedDtos);
    }

    private void validateEventAccessAndState(Long userId, Event event) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Только создатель может управлять запросами события");
        }
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConditionNotMetException("Событие должно быть опубликовано");
        }
    }

    // Выбрысываем ошибку, если заявка уже существует
    private void checkRequestNotExists(Long userId, Long eventId) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConditionNotMetException("Заявка на участие уже существует");
        }
    }

    // Проверяем, что создатель события не пытается подать заявку на своё событие.
    private void checkNotEventInitiator(Long userId, Event event) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConditionNotMetException("Автор события не может подать заявку на свое событие");
        }
    }

    // Проверяем, что событие опубликовано.
    private void checkEventIsPublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConditionNotMetException("Нельзя принять участие в неопубликованном событии");
        }
    }

    // Проверяем лимит участников события.
    private void checkParticipantLimit(Event event, Long eventId) {
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConditionNotMetException("Лимит на участие в событии достигнут");
        }
    }

    // Единый метод проверки
    private void validateRequestCreation(Long userId, Event event) {
        checkRequestNotExists(userId, event.getId());
        checkNotEventInitiator(userId, event);
        checkEventIsPublished(event);
        checkParticipantLimit(event, event.getId());
    }
}
