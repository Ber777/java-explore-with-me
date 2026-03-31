package ewm.event.dto;

import ewm.event.model.StateAction;

import lombok.Data;
import lombok.Builder;
import lombok.ToString;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

import static ewm.Constants.DATE_TIME_FORMAT;

@Data
@Builder
@ToString
public class EventUpdateAdminDto {

    @Size(min = 3, max = 120, message = "Заголовок должен содержать минимум 3 и максимум 120 символов")
    private String title;

    @Size(min = 20, max = 2000, message = "Аннотация должна содержать минимум 20 и максимум 2000 символов")
    private String annotation;

    @Size(min = 20, max = 7000, message = "Описание должно содержать минимум 20 и максимум 7000 символов")
    private String description;

    @JsonProperty("category")
    private Long categoryId;

    @Future(message = "Дата мероприятия должна быть в будущем")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    private LocationDto location;
    private Boolean paid;

    @Min(0)
    private Integer participantLimit;

    private Boolean requestModeration;
    private StateAction stateAction;
}
