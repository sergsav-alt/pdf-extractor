package org.example.pdfextractor.service;

import java.awt.image.BufferedImage;

/**
 * Preprocesses page images before OCR to improve recognition accuracy.
 */
public interface ImagePreprocessor {

    /**
     * Preprocesses an image for OCR: converts to grayscale, applies binary threshold,
     * and performs light noise reduction.
     *
     * @param original the raw page image
     * @return preprocessed (binarized) image ready for OCR
     */
    BufferedImage preprocess(BufferedImage original);
}
