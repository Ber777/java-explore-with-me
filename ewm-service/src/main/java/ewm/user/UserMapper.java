package ewm.user;

import ewm.user.model.User;
import ewm.user.dto.UserDto;
import ewm.user.dto.NewUserDto;

public class UserMapper {
    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User fromNewUserDto(NewUserDto request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }
}
