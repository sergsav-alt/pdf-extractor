package org.example.pdfextractor.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.pdfextractor.service.ImagePreprocessor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

/**
 * Preprocesses page images before OCR to improve recognition accuracy.
 * Applies grayscale conversion, binarization (thresholding), and noise reduction.
 */
@Slf4j
@Service
public class ImagePreprocessorImpl implements ImagePreprocessor {

    @Override
    public BufferedImage preprocess(BufferedImage original) {
        log.debug("Preprocessing image: {}x{}", original.getWidth(), original.getHeight());

        // 1. Convert to grayscale
        BufferedImage gray = toGrayscale(original);

        // 2. Apply binary threshold (Otsu-style adaptive)
        BufferedImage binary = binarize(gray);

        // 3. Light noise reduction (despeckle)
        BufferedImage cleaned = despeckle(binary);

        log.debug("Preprocessing complete");
        return cleaned;
    }

    /**
     * Converts a BufferedImage to grayscale using ICC color conversion.
     */
    private BufferedImage toGrayscale(BufferedImage source) {
        var grayImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        var op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(source, grayImage);
        return grayImage;
    }

    /**
     * Applies binary thresholding using a simple global threshold.
     * Pixels above threshold become white (255), below become black (0).
     * Uses a computed mean-based threshold for robustness.
     */
    private BufferedImage binarize(BufferedImage gray) {
        var binary = new BufferedImage(
                gray.getWidth(),
                gray.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        int threshold = computeThreshold(gray);
        log.debug("Computed binarization threshold: {}", threshold);

        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int pixel = gray.getRaster().getSample(x, y, 0);
                int binaryValue = (pixel < threshold) ? 0 : 255;
                binary.getRaster().setSample(x, y, 0, binaryValue);
            }
        }

        return binary;
    }

    /**
     * Computes a global binarization threshold using a simple histogram-based method
     * (iterative mean threshold, similar to basic Otsu approximation).
     */
    private int computeThreshold(BufferedImage gray) {
        int[] histogram = new int[256];
        int total = gray.getWidth() * gray.getHeight();

        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int pixel = gray.getRaster().getSample(x, y, 0);
                histogram[pixel]++;
            }
        }

        // Iterative mean threshold
        int sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        int threshold = sum / total;
        int newThreshold;
        int iterations = 0;
        int maxIterations = 100;

        do {
            newThreshold = threshold;
            int sumLower = 0, countLower = 0;
            int sumUpper = 0, countUpper = 0;

            for (int i = 0; i <= newThreshold; i++) {
                sumLower += i * histogram[i];
                countLower += histogram[i];
            }
            for (int i = newThreshold + 1; i < 256; i++) {
                sumUpper += i * histogram[i];
                countUpper += histogram[i];
            }

            int meanLower = (countLower > 0) ? sumLower / countLower : 0;
            int meanUpper = (countUpper > 0) ? sumUpper / countUpper : 255;
            threshold = (meanLower + meanUpper) / 2;

            iterations++;
            if (iterations > maxIterations) break;
        } while (threshold != newThreshold);

        return threshold;
    }

    /**
     * Simple despeckle: removes isolated black pixels (surrounded by white).
     * This reduces salt-and-pepper noise from scanning.
     */
    private BufferedImage despeckle(BufferedImage binary) {
        int w = binary.getWidth();
        int h = binary.getHeight();
        var result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = binary.getRaster().getSample(x, y, 0);
                if (pixel == 0) {
                    // Count black neighbors
                    int blackNeighbors = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                if (binary.getRaster().getSample(nx, ny, 0) == 0) {
                                    blackNeighbors++;
                                }
                            }
                        }
                    }
                    // Keep pixel black only if it has at least 2 black neighbors
                    result.getRaster().setSample(x, y, 0, (blackNeighbors >= 2) ? 0 : 255);
                } else {
                    result.getRaster().setSample(x, y, 0, 255);
                }
            }
        }

        return result;
    }
}
