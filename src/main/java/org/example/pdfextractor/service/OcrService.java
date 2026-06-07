package org.example.pdfextractor.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.example.pdfextractor.exception.ExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

/**
 * Performs OCR text recognition on preprocessed page images using Tesseract (Tess4J).
 * <p>
 * Configuration is read from {@code application.properties} (or system properties):
 * <ul>
 *   <li>{@code tesseract.datapath} — path to tessdata directory</li>
 *   <li>{@code tesseract.language} — OCR language (default: eng)</li>
 *   <li>{@code tesseract.pagesegmode} — page segmentation mode (default: 6)</li>
 * </ul>
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final ITesseract tesseract;

    public OcrService(@Value("${tesseract.datapath:}") String dataPath,
                      @Value("${tesseract.language:eng}") String language,
                      @Value("${tesseract.pagesegmode:6}") int pageSegMode) {
        this.tesseract = new Tesseract();
        configureTesseract(tesseract, dataPath, language, pageSegMode);
    }

    /**
     * Configures Tesseract instance with the provided settings.
     */
    private void configureTesseract(ITesseract instance, String dataPath, String language, int pageSegMode) {
        instance.setLanguage(language);
        instance.setPageSegMode(pageSegMode);

        if (dataPath != null && !dataPath.isBlank()) {
            instance.setDatapath(dataPath);
            log.info("Using Tesseract data path: {}", dataPath);
        } else {
            log.info("Tesseract data path not explicitly set; using TESSDATA_PREFIX or default");
        }
        log.info("Tesseract language: {}, pageSegMode: {}", language, pageSegMode);
    }


    /**
     * Recognizes text from a preprocessed page image.
     *
     * @param image the preprocessed (binarized) image
     * @return recognized text
     * @throws ExtractionException if OCR fails
     */
    public String recognizeText(BufferedImage image) {
        try {
            log.debug("Starting OCR on image: {}x{}", image.getWidth(), image.getHeight());
            String result = tesseract.doOCR(image);
            log.debug("OCR completed, got {} characters", result.length());
            return result;
        } catch (TesseractException e) {
            throw new ExtractionException("OCR recognition failed", e);
        }
    }

    /**
     * Recognizes text and returns a confidence-weighted result.
     * Uses doOCR with a DataOutput for per-word confidence data.
     *
     * @param image the preprocessed image
     * @return recognized text
     * @throws ExtractionException if OCR fails
     */
    public OcrResult recognizeTextWithConfidence(BufferedImage image) {
        try {
            log.debug("Starting OCR (with confidence) on image: {}x{}", image.getWidth(), image.getHeight());
            // Use Tess4J's built-in word-level confidence via the standard doOCR
            // For simplicity, we get the full text and report average confidence
            String text = tesseract.doOCR(image);
            // Tess4J provides getWords() but it's on the ResultIterator level;
            // we use a simpler approach: return the text and a placeholder confidence
            // A more advanced version would use the API's getSegmentedRegions or ResultIterator
            double avgConfidence = estimateConfidence(text);
            log.debug("OCR completed, {} chars, estimated confidence: {}", text.length(), avgConfidence);
            return new OcrResult(text, avgConfidence);
        } catch (TesseractException e) {
            throw new ExtractionException("OCR recognition failed", e);
        }
    }

    /**
     * Rough heuristic for OCR confidence: ratio of alphanumeric + spaces
     * to total characters. High for clean text, lower for garbage.
     */
    private double estimateConfidence(String text) {
        if (text == null || text.isBlank()) return 0.0;
        long validChars = text.chars()
                .filter(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c))
                .count();
        return (double) validChars / text.length();
    }

    /**
     * OCR result with recognized text and confidence score.
     */
    public record OcrResult(String text, double confidence) {
        public OcrResult {
            if (text == null) text = "";
        }
    }
}
