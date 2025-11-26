package org.example.exceptions;

public class FileGenerationException extends RuntimeException {
    public FileGenerationException(String message) {
        super(message);
    }
}
