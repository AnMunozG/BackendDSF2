package cl.veritrust.v1.Exception;
 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.Instant;
import java.util.Map;
 
@ControllerAdvice
public class RestExceptionHandler {
 
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("timestamp", Instant.now(), "message", ex.getMessage()));
    }
 
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<?> handleFileErr(FileStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("timestamp", Instant.now(), "message", ex.getMessage()));
    }
 
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("timestamp", Instant.now(), "message", ex.getMessage()));
    }
}
