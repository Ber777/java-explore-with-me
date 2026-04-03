package ewm.compilation;

import ewm.compilation.dto.*;
import ewm.event.EventMapper;
import ewm.event.model.Event;
import ewm.compilation.model.Compilation;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class CompilationMapper {
    public CompilationDto toCompilationDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(EventMapper::toShortEventDto)
                        .toList())
                .build();
    }

    public Compilation fromCompilationDto(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null && dto.getPinned())
                .events(events)
                .build();
    }
}
