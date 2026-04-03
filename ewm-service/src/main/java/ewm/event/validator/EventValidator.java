package ewm.event.validator;

import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.exception.NoAccessException;
import ewm.exception.ConditionNotMetException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventValidator {
    private static final int MIN_TIME_TO_PUBLISHED_EVENT = 1;
    private static final int MIN_TIME_TO_UNPUBLISHED_EVENT = 2;

    public void validateEventDate(LocalDateTime eventDate, EventState state) {
        if (eventDate == null) {
            throw new IllegalArgumentException("Дата события отсутствует");
        }

        int hours = state == EventState.PUBLISHED ? MIN_TIME_TO_PUBLISHED_EVENT : MIN_TIME_TO_UNPUBLISHED_EVENT;

        // дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (409 ошибка)
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            String message = "Дата события должна быть не ранее, чем %d часов от %s времени"
                    .formatted(hours, state == EventState.PUBLISHED ? "publishing" : "current");
            throw new ConditionNotMetException(message);
        }
    }

    public void validateInitiatorAccess(Long userId, Event event) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NoAccessException("Только создатель может редактировать событие");
        }
    }

    public void validateUpdatePublishedEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionNotMetException("Не удалось обновить опубликованное событие");
        }
    }

    public void validateRejectPublishedEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionNotMetException("Опубликованные события не могут быть отклонены");
        }
    }

    public void validatePrePublishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConditionNotMetException("События должны быть в статусе ожидание, чтобы была возможность их опубликовть");
        }
    }
}
