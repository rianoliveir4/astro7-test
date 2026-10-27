package com.rian.appointment_api.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ProblemDetail handleAppointmentNotFound(AppointmentNotFoundException appointmentNotFoundException) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, appointmentNotFoundException.getMessage());
        problemDetail.setTitle("Atendimento não encontrado");
        return problemDetail;
    }

    @ExceptionHandler(ActiveAppointmentAlreadyExistsException.class)
    public ProblemDetail handleActiveAppointmentAlreadyExists(
            ActiveAppointmentAlreadyExistsException activeAppointmentAlreadyExistsException) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, activeAppointmentAlreadyExistsException.getMessage());
        problemDetail.setTitle("Atendimento em andamento");
        problemDetail.setProperty(
                "activeAppointmentId", activeAppointmentAlreadyExistsException.getActiveAppointmentId());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception unexpectedException) {
        log.error("Unexpected error while handling request", unexpectedException);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problemDetail.setTitle("Erro interno");
        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException methodArgumentNotValidException,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : methodArgumentNotValidException.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problemDetail.setTitle("Dados inválidos");
        problemDetail.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(problemDetail);
    }
}
