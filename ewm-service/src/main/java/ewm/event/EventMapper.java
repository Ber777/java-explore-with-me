package ewm.event;

import ewm.event.dto.*;
import ewm.user.UserMapper;
import ewm.event.model.Event;
import ewm.category.CategoryMapper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventMapper {
    public static EventDtoResponse toEventDto(Event event) {
        return EventDtoResponse.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .description(event.getDescription())
                .initiator(UserMapper.toUserDto(event.getInitiator()))
                .createdOn(event.getCreatedAt())
                .state(event.getState())
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .location(new LocationDto(
                        event.getLocationLat(),
                        event.getLocationLon()))
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    public static EventShortDtoResponse toShortEventDto(Event event) {
        return EventShortDtoResponse.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .title(event.getTitle())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserDto(event.getInitiator()))
                .confirmedRequests(event.getConfirmedRequests())
                .views(event.getViews())
                .build();
    }

    public static Event fromEventDto(EventDto eventDto) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .title(eventDto.getTitle())
                .paid(eventDto.getPaid())
                .eventDate(eventDto.getEventDate())
                .description(eventDto.getDescription())
                .locationLat(eventDto.getLocation().getLat())
                .locationLon(eventDto.getLocation().getLon())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .build();
    }
}
