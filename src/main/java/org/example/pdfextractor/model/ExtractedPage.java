package org.example.pdfextractor.model;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Represents a single extracted page from a PDF document,
 * containing the page image and its OCR-recognized text.
 */
public final class ExtractedPage {

    private final int pageNumber;
    private final BufferedImage image;
    private final String text;
    private final double confidence;

    public ExtractedPage(int pageNumber, BufferedImage image, String text, double confidence) {
        this.pageNumber = pageNumber;
        this.image = Objects.requireNonNull(image, "image must not be null");
        this.text = Objects.requireNonNullElse(text, "");
        this.confidence = confidence;
    }

    public int pageNumber() {
        return pageNumber;
    }

    public BufferedImage image() {
        return image;
    }

    public String text() {
        return text;
    }

    public double confidence() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractedPage that)) return false;
        return pageNumber == that.pageNumber
                && Double.compare(confidence, that.confidence) == 0
                && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, text, confidence);
    }

    @Override
    public String toString() {
        return "ExtractedPage{" +
                "pageNumber=" + pageNumber +
                ", textLength=" + text.length() +
                ", confidence=" + confidence +
                '}';
    }
}
