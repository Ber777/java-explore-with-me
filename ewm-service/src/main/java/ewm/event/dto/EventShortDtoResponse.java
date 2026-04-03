package ewm.event.dto;

import ewm.user.dto.UserDto;
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
public class EventShortDtoResponse {
    private Long id;
    private String title;
    private String annotation;
    private CategoryDtoResponse category;
    private UserDto initiator;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    private Boolean paid;
    private Integer confirmedRequests;

    @Builder.Default
    private Integer views = 0;
}
