package ru.cinimex.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 Bad Request: Пользователь уже есть
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // 403 Forbidden: Ошибка логина/пароля (Spring Security выбрасывает BadCredentialsException)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleAuthError(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Неверный логин или пароль");
    }

    // 500 Internal Server Error: Любая другая непредвиденная ошибка
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Произошла внутренняя ошибка сервера: " + ex.getMessage());
    }
}