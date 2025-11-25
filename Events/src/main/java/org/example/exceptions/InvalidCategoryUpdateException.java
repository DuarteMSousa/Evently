package org.example.exceptions;

public class InvalidCategoryUpdateException extends RuntimeException {
    public InvalidCategoryUpdateException(String message) {
        super(message);
    }
}
