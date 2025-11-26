package org.example.exceptions;

public class LogoNotFoundException extends RuntimeException {
    public LogoNotFoundException(String message) {
        super(message);
    }
}
