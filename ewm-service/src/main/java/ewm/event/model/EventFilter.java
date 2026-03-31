package ewm.event.model;

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
public class EventFilter {
    private String text;
    private List<Long> categories;
    private Boolean paid;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeEnd;

    @Builder.Default
    private Boolean onlyAvailable = false;

    @Builder.Default
    private String sort = "EVENT_DATE";

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private EventState state = EventState.PUBLISHED;

    private Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.DESC,
                    this.sort.equals("VIEWS") ? "views" : "eventDate");
            int page = from / size;
            this.pageable = PageRequest.of(page, size, sort);
        }
        return pageable;
    }
}
