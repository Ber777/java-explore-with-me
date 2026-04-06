package ewm.location;

import ewm.location.dto.*;
import ewm.user.UserMapper;
import ewm.location.model.Location;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LocationMapper {
    public static LocationDtoResponse toLocationDto(Location location) {
        return LocationDtoResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }

    public static Location fromLocationDto(NewLocationDto dto) {
        return Location.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public static LocationFullDtoResponse toFullLocationDto(Location location) {
        return LocationFullDtoResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .creator(location.getCreator() == null ? null : UserMapper.toUserDto(location.getCreator()))
                .state(location.getState())
                .build();
    }

    public static LocationPrivateDtoResponse toPrivateLocationDto(Location location) {
        return LocationPrivateDtoResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .state(location.getState())
                .build();
    }
}
