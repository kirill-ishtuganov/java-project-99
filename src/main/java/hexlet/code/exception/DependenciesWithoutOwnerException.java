package hexlet.code.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class DependenciesWithoutOwnerException extends RuntimeException {
    public DependenciesWithoutOwnerException(String message) {
        super(message);
    }
}
