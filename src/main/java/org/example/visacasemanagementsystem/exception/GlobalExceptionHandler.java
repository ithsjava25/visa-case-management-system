package org.example.visacasemanagementsystem.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseBody
    public String handleUnauthorizedException(UnauthorizedException exception) {
        return "Access Denied: " + exception.getMessage();
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class})
    @ResponseBody
    public String handleNotFoundException(RuntimeException exception) {
        return exception.getMessage();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public String handleIllegalArgumentException(IllegalArgumentException exception) {
        return "Invalid Request: " + exception.getMessage();
    }
}
