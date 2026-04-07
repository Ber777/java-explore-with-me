package ewm.event.dto;

import ewm.user.dto.UserDto;
import ewm.event.model.EventState;
import ewm.location.dto.LocationDtoResponse;
import ewm.category.dto.CategoryDtoResponse;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import static ewm.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDtoResponse {
    private Long id;
    private String title;
    private String annotation;
    private String description;
    private CategoryDtoResponse category;
    private UserDto initiator;
    private LocationDtoResponse location;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime publishedOn;

    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;
    private Integer confirmedRequests;

    @Builder.Default
    private Integer views = 0;
}
