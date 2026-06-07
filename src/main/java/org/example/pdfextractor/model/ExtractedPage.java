package org.example.pdfextractor.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Represents a single extracted page from a PDF document,
 * containing the page image and its OCR-recognized text.
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(exclude = {"image"})
@ToString(exclude = {"image"})
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
}
