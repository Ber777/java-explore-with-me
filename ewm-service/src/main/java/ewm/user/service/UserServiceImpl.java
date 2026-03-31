package ewm.user.service;

import ewm.user.UserMapper;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;

import ewm.user.dto.NewUserDto;
import ewm.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserDto request) {
        User user = UserMapper.fromNewUserDto(request);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAllWithPageable(pageable);
        } else {
            users = userRepository.findByIdInWithPageable(ids, pageable);
        }

        return users.stream().map(UserMapper::toUserDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
