package ewm.event.service;

import ewm.event.dto.*;
import ewm.event.model.*;

import java.util.Collection;

public interface EventService {
    EventDtoResponse createEvent(Long userId, EventDto eventDto);

    EventDtoResponse updateEventByUser(Long userId, Long eventId, EventUpdateDto updateRequest);

    EventDtoResponse updateEventByAdmin(Long eventId, EventUpdateAdminDto eventDto);

    EventDtoResponse getPublished(Long eventId);

    EventDtoResponse getEvent(Long userId, Long eventId);

    Collection<EventShortDtoResponse> getShortEventsBy(EventFilter filter);

    Collection<EventDtoResponse> getFullEventsBy(EventAdminFilter filter);

    Collection<EventShortDtoResponse> getByUserId(Long userId, Integer offset, Integer limit);
}
