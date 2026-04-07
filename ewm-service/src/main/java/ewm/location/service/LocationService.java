package ewm.location.service;

import ewm.location.dto.*;
import ewm.location.model.*;

import java.util.Collection;

public interface LocationService {

    LocationPrivateDtoResponse createLocation(Long userId, NewLocationDto dto);

    LocationFullDtoResponse createLocationByAdmin(NewLocationDto dto);

    LocationFullDtoResponse updateLocationByAdmin(Long id, LocationUpdateAdminDto dto);

    LocationPrivateDtoResponse updateLocation(Long id, Long userId, LocationUpdateUserDto dto);

    LocationDtoResponse getApprovedLocations(Long id);

    Collection<LocationFullDtoResponse> getAllLocationsByAdminFilter(LocationAdminFilter filter);

    Collection<LocationPrivateDtoResponse> getAllLocationsByFilter(Long userId, LocationPrivateFilter filter);

    Collection<LocationDtoResponse> getAllLocationsByFilter(LocationPublicFilter filter);

    void deleteLocationByAdmin(Long id);

    void deleteLocation(Long id, Long userId);

    Location getOrCreateLocation(LocationDto location);

    LocationFullDtoResponse getByIdForAdmin(Long id);
}
