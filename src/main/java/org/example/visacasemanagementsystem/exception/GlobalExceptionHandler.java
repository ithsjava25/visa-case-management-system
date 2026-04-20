package org.example.visacasemanagementsystem.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(annotations = Controller.class)
@Slf4j
public class GlobalExceptionHandler {

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorizedException(UnauthorizedException exception, Model model) {
        log.error("Access denied: {}", exception.getMessage());
        model.addAttribute("errorMessage","You do not have permission to perform this action.");
        model.addAttribute("errorTitle", "⚠️Access Denied.");

        // TODO: Fetch user role from SecurityContext and add correct dashboard URL to model
        // model.addAttribute("dashboardUrl", determineDashboardUrl());

        return "error/error";
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class})
    public String handleNotFoundException(RuntimeException exception, Model model) {
        log.error("Resource not found: {}", exception.getMessage());
        model.addAttribute("errorMessage", "The requested resource could not be found.");
        model.addAttribute("errorTitle", "⚠️Not Found.");

        // TODO: Fetch user role from SecurityContext and add correct dashboard URL to model
        // model.addAttribute("dashboardUrl", determineDashboardUrl());

        return "error/error";
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException exception,  Model model) {
        log.error("Bad request: {}", exception.getMessage());
        String userMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "The request contained invalid data.";
        model.addAttribute("errorMessage", userMessage);
        model.addAttribute("errorTitle", "⚠️Bad Request.");

        // TODO: Fetch user role from SecurityContext and add correct dashboard URL to model
        // model.addAttribute("dashboardUrl", determineDashboardUrl());

        return "error/error";
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleAllUncaughtErrors(Exception exception,  Model model) {
        log.error("Unexpected Error: ", exception);
        model.addAttribute("errorMessage","Something went wrong. Try again later.");
        model.addAttribute("errorTitle", "⚠️Internal Server Error.");

        // TODO: Fetch user role from SecurityContext and add correct dashboard URL to model
        // model.addAttribute("dashboardUrl", determineDashboardUrl());

        return "error/error";
    }
}
