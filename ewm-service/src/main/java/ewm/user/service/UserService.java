package ewm.user.service;

import ewm.user.dto.UserDto;
import ewm.user.dto.NewUserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserDto request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}

