package ewm.location.model;

import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class LocationAdminFilter {
    private String text;
    private Long creator;
    private LocationState state;
    private Zone zone;
    private Integer minEvents;
    private Integer maxEvents;
    private Integer offset;
    private Integer limit;
    private Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.ASC, "id");
            int page = offset / limit;
            this.pageable = PageRequest.of(page, limit, sort);
        }
        return pageable;
    }
}
