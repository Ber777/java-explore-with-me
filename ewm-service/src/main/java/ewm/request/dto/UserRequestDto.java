package ewm.request.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {
    private Long id;
    private LocalDateTime created;
    private Long event;
    private Long requester;
    private String status;
}