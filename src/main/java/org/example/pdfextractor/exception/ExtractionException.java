package org.example.pdfextractor.exception;

/**
 * Unchecked exception thrown when PDF extraction or OCR processing fails.
 */
public class ExtractionException extends RuntimeException {

    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
