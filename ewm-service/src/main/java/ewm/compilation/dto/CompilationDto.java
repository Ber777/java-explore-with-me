package ewm.compilation.dto;

import ewm.event.dto.EventShortDtoResponse;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompilationDto {
    private Long id;
    private String title;
    private Boolean pinned;
    private List<EventShortDtoResponse> events;
}
