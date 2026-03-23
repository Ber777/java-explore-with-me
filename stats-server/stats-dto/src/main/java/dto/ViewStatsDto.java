package dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {
    @NotBlank
    private String app;
    @NotBlank
    private String uri;
    @NotNull
    private Long hits;
}
