package ewm.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Long id) {
        super(entity + " с id: " + id + " не найден(-а)");
    }
}
