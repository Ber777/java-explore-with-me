package ewm.event.model;

import ewm.location.model.Zone;

import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDateTime;

import static ewm.Constants.DATE_TIME_FORMAT;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EventAdminFilter {
    private List<Long> users;
    private List<Long> categories;
    private List<EventState> states;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeEnd;

    private Zone zone;
    private Long locationId;

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "id";

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;

    public Pageable getPageable() {
        Sort sort = Sort.by(sortDirection, sortBy);
        int page = from / size;
        return PageRequest.of(page, size, sort);
    }
}