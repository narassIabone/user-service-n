package ru.cinimex.userservice.exception;

public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException(String message) { super(message); }
}
