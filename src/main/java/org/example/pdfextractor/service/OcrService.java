package org.example.pdfextractor.service;

import org.example.pdfextractor.exception.ExtractionException;

import java.awt.image.BufferedImage;

/**
 * Service for performing OCR text recognition on preprocessed page images.
 */
public interface OcrService {

    /**
     * Recognizes text from a preprocessed page image.
     *
     * @param image the preprocessed (binarized) image
     * @return recognized text
     * @throws ExtractionException if OCR fails
     */
    String recognizeText(BufferedImage image);

    /**
     * Recognizes text and returns a confidence-weighted result.
     *
     * @param image the preprocessed image
     * @return OCR result with text and confidence score
     * @throws ExtractionException if OCR fails
     */
    OcrResult recognizeTextWithConfidence(BufferedImage image);

    /**
     * OCR result with recognized text and confidence score.
     */
    record OcrResult(String text, double confidence) {
        public OcrResult {
            if (text == null) text = "";
        }
    }
}
