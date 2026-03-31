package ewm.request;

import ewm.request.model.UserRequest;
import ewm.request.dto.UserRequestDto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserRequestMapper {
    public static UserRequestDto toUserRequestDto(UserRequest ur) {
        return UserRequestDto.builder()
                .id(ur.getId())
                .created(ur.getCreated())
                .event(ur.getEvent().getId())
                .requester(ur.getRequester().getId())
                .status(ur.getStatus().name())
                .build();
    }
}
