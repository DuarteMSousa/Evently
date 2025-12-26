package org.evently.reviews.exceptions;

public class InvalidPageNumberException extends RuntimeException {
  public InvalidPageNumberException(String message) {
    super(message);
  }
}
