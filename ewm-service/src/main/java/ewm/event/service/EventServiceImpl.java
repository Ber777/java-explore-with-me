package ewm.event.service;

import ewm.event.dto.*;
import ewm.exception.*;
import ewm.event.model.*;
import dto.ViewStatsDto;
import client.StatsClient;
import ewm.user.model.User;
import ewm.event.EventMapper;
import ewm.location.model.Location;
import ewm.location.service.LocationService;
import client.StatsClientException;
import ewm.category.model.Category;
import ewm.event.validator.EventValidator;
import ewm.request.model.UserRequestCount;
import ewm.user.repository.UserRepository;
import ewm.event.repository.EventRepository;
import ewm.category.repository.CategoryRepository;
import ewm.request.repository.UserRequestRepository;

import java.util.*;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static ewm.Constants.STATS_EVENTS_URL;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final StatsClient statsClient;
    private final EventValidator eventValidator;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRequestRepository requestRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public EventDtoResponse createEvent(Long userId, EventDto eventDto) {
        eventValidator.validateEventDate(eventDto.getEventDate(), EventState.PENDING);
        Category category = getCategory(eventDto.getCategoryId());
        User user = getUser(userId);
        Location location = locationService.getOrCreateLocation(eventDto.getLocation());

        Event event = EventMapper.fromEventDto(eventDto);
        event.setCategory(category);
        event.setInitiator(user);
        event.setLocation(location);

        event = eventRepository.save(event);

        return EventMapper.toEventDto(event);
    }

    @Override
    @Transactional
    public EventDtoResponse updateEventByUser(Long userId, Long eventId, EventUpdateDto eventDto) {
        Event event = getEvent(eventId);

        eventValidator.validateInitiatorAccess(userId, event);
        eventValidator.validateUpdatePublishedEvent(event);

        Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
            Location location = locationService.getOrCreateLocation(eventDto.getLocation());
            event.setLocation(location);
        });
        Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

        if (eventDto.getCategoryId() != null && !eventDto.getCategoryId().equals(event.getCategory().getId())) {
            Category category = categoryRepository.findById(eventDto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Категория", eventDto.getCategoryId()));
            event.setCategory(category);
        }

        if (eventDto.getEventDate() != null) {
            eventValidator.validateEventDate(eventDto.getEventDate(), event.getState());
            event.setEventDate(eventDto.getEventDate());
        }

        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW  -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);
        return EventMapper.toEventDto(updated);
    }

    @Override
    @Transactional
    public EventDtoResponse updateEventByAdmin(Long eventId, EventUpdateAdminDto eventDto) {
        Event event = getEvent(eventId);

        Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
            Location location = locationService.getOrCreateLocation(eventDto.getLocation());
            event.setLocation(location);
        });
        Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

        if (eventDto.getEventDate() != null) {
            eventValidator.validateEventDate(eventDto.getEventDate(), event.getState());
            event.setEventDate(eventDto.getEventDate());
        }

        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case PUBLISH_EVENT -> publishEvent(event);
                case REJECT_EVENT -> rejectEvent(event);
            }
        }

        return EventMapper.toEventDto(event);
    }

    @Override
    public EventDtoResponse getPublished(Long eventId) {
        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));

        enrichEventsData(List.of(event));

        return EventMapper.toEventDto(event);
    }

    @Override
    public EventDtoResponse getEvent(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }

        Event event = getEvent(eventId);

        eventValidator.validateInitiatorAccess(userId, event);

        enrichEventsData(List.of(event));

        return EventMapper.toEventDto(event);
    }

    public Specification<Event> buildSpecification(EventAdminFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withUsers(filter.getUsers())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withStatesIn(filter.getStates())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd())),
                        optionalSpec(EventSpecifications.withLocationId(filter.getLocationId())),
                        optionalSpec(EventSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    public Specification<Event> buildSpecification(EventFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withTextContains(filter.getText())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withPaid(filter.getPaid())),
                        optionalSpec(EventSpecifications.withState(filter.getState())),
                        optionalSpec(EventSpecifications.withLocationId(filter.getLocationId())),
                        optionalSpec(EventSpecifications.withCoordinates(filter.getZone())),
                        optionalSpec(EventSpecifications.withOnlyAvailable(filter.getOnlyAvailable())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private static <T> Specification<T> optionalSpec(Specification<T> spec) {
        return spec;
    }

    @Override
    public Collection<EventShortDtoResponse> getShortEventsBy(EventFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(EventMapper::toShortEventDto)
                .toList();
    }

    @Override
    public Collection<EventDtoResponse> getFullEventsBy(EventAdminFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(EventMapper::toEventDto)
                .toList();
    }

    private Collection<Event> findBy(Specification<Event> spec, Pageable pageable) {
        Collection<Event> events = eventRepository.findAll(spec, pageable).getContent();
        enrichEventsData(events);

        return events;
    }

    @Override
    public Collection<EventShortDtoResponse> getByUserId(Long userId, Integer offset, Integer limit) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь", userId);
        }

        Collection<Event> events = eventRepository.findByUserId(userId, offset, limit);
        enrichEventsData(events);

        return events.stream()
                .map(EventMapper::toShortEventDto)
                .toList();
    }

    private void enrichEventsData(Collection<Event> events) {
        if (events.isEmpty()) return;

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        // Получаем оба набора данных за один проход по БД
        Map<Long, Integer> confirmedRequests = getConfirmedRequestsCount(eventIds);
        Map<Long, Integer> views = getViewsCount(eventIds);

        // Однократно проходим по коллекции событий и обогащаем данными
        events.forEach(event -> {
            Long eventId = event.getId();
            event.setConfirmedRequests(confirmedRequests.getOrDefault(eventId, 0));
            event.setViews(views.getOrDefault(eventId, 0));
        });
    }

    private Map<Long, Integer> getConfirmedRequestsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<UserRequestCount> results = requestRepository.countConfirmedRequestsForEvents(eventIds);

        if (results == null || results.isEmpty()) {
            return new HashMap<>();
        }

        return results.stream()
                .collect(Collectors.toMap(
                        UserRequestCount::getId,
                        UserRequestCount::getCount,
                        (existing, replacement) -> existing // стратегия разрешения коллизий
                ));
    }

    private Map<Long, Integer> getStatistics(Collection<Long> eventIds) {
        try {
            List<ViewStatsDto> stats = (List<ViewStatsDto>) statsClient.getStats(
                    LocalDateTime.now().minusYears(10),
                    LocalDateTime.now().plusYears(10),
                    eventIds.stream().map(id -> STATS_EVENTS_URL + id).toList(),
                    true);

            Map<String, Long> hits = stats.stream()
                    .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));

            return eventIds.stream()
                    .collect(Collectors.toMap(
                            id -> id,
                            id -> Math.toIntExact(hits.getOrDefault(STATS_EVENTS_URL + id, 0L))
                    ));
        } catch (StatsClientException ex) {
            log.error("Ошибка получения статистики", ex);
            return Map.of();
        }
    }

    private Map<Long, Integer> getViewsCount(List<Long> eventIds) {
        return getStatistics(eventIds);
    }

    @SuppressWarnings("UnusedReturnValue")
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория", categoryId));
    }

    @SuppressWarnings("UnusedReturnValue")
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь", userId));
    }

    @SuppressWarnings("UnusedReturnValue")
    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие", eventId));
    }

    private void publishEvent(Event event) {
        eventValidator.validatePrePublishEvent(event);
        eventValidator.validateEventDate(event.getEventDate(), EventState.PUBLISHED);
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
    }

    private void rejectEvent(Event event) {
        eventValidator.validateRejectPublishedEvent(event);
        event.setState(EventState.CANCELED);
    }
}
