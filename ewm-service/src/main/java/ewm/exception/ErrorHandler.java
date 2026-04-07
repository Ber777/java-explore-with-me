package ewm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final String DEFAULT_REASON = "Внутренняя ошибка сервера";
    private static final String DEFAULT_ERROR_MESSAGE = "Произошла непредвиденная ошибка";

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Ошибка валидации");

        return buildErrorResponse(errorMessage, "Ошибка валидации аргумента метода", HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), "Запрашиваемый объект не найден", HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({ConditionNotMetException.class, IllegalStateException.class, DuplicateLocationsException.class})
    public ErrorResponse handleConflictExceptions(RuntimeException ex) {
        return buildErrorResponse(ex.getMessage(), "Условие не выполнено", HttpStatus.CONFLICT);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(NoAccessException.class)
    public ErrorResponse handleNoAccessException(NoAccessException ex) {
        return buildErrorResponse(ex.getMessage(), "Нет доступа", HttpStatus.FORBIDDEN);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        String message = "Параметр '" + ex.getParameterName() + "' отсутствует";
        return buildErrorResponse(message, "Обязательный параметр запроса отсутствует", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @SuppressWarnings("ConstantConditions")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage() != null ? v.getMessage() : "Нарушение ограничения целостности данных")
                .orElse("Нарушение валидации");
        return buildErrorResponse(message, "Нарушение валидации", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException.class)
    public ErrorResponse handleInvalidRequestException(InvalidRequestException ex) {
        return buildErrorResponse(ex.getMessage(), "Неверный запрос", HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse onDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.warn("Нарушение целостности данных: {}", e.getMessage());
        String rootCause = Optional.ofNullable(e.getRootCause())
                .map(Throwable::getMessage)
                .orElse("Неизвестная причина нарушения целостности данных");
        return buildErrorResponse(e.getMessage(), rootCause, HttpStatus.CONFLICT);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ErrorResponse handleForbiddenException(ForbiddenException ex) {
        return buildErrorResponse(ex.getMessage(), "В доступе отказано", HttpStatus.FORBIDDEN);
    }

    @ResponseBody
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(final Exception e) {
        log.error("Неожиданная ошибка на сервере", e);
        return buildErrorResponse(DEFAULT_ERROR_MESSAGE, DEFAULT_REASON, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Вспомогательный метод для уменьшения дублирования кода
    private ErrorResponse buildErrorResponse(String message, String reason, HttpStatus status) {
        return ErrorResponse.builder()
                .message(message)
                .reason(reason)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}