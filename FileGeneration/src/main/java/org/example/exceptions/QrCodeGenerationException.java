package org.example.exceptions;

public class QrCodeGenerationException extends RuntimeException {
    public QrCodeGenerationException(String message) {
        super(message);
    }
}
