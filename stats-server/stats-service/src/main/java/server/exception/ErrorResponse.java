package server.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private final String error;
}
