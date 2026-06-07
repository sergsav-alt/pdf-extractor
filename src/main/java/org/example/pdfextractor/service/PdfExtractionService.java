package org.example.pdfextractor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.example.pdfextractor.exception.ExtractionException;
import org.example.pdfextractor.model.ExtractedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts page images from a PDF document using Apache PDFBox.
 */
@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    private static final float DPI = 300f;

    /**
     * Extracts all pages from a PDF file as high-resolution images.
     *
     * @param pdfPath path to the PDF file
     * @return list of extracted pages with images
     * @throws ExtractionException if reading or rendering fails
     */
    public List<ExtractedPage> extractPages(Path pdfPath) {
        var pages = new ArrayList<ExtractedPage>();

        try (var document = Loader.loadPDF(pdfPath.toFile())) {
            var renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            log.info("Extracting {} pages from PDF at {} dpi", totalPages, DPI);

            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                pages.add(new ExtractedPage(i + 1, image, "", 0.0));
                log.debug("Rendered page {} ({}x{})", i + 1, image.getWidth(), image.getHeight());
            }

            log.info("Successfully extracted {} pages", pages.size());
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract pages from PDF: " + pdfPath, e);
        }

        return List.copyOf(pages);
    }

    /**
     * Extracts pages from a PDF provided as an input stream.
     *
     * @param inputStream PDF data stream
     * @return list of extracted pages with images
     * @throws ExtractionException if reading or rendering fails
     */
    public List<ExtractedPage> extractPages(InputStream inputStream) {
        var pages = new ArrayList<ExtractedPage>();

        try (var document = Loader.loadPDF(inputStream.readAllBytes())) {
            var renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            log.info("Extracting {} pages from PDF stream at {} dpi", totalPages, DPI);

            for (int i = 0; i < totalPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                pages.add(new ExtractedPage(i + 1, image, "", 0.0));
            }

            log.info("Successfully extracted {} pages from stream", pages.size());
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract pages from PDF stream", e);
        }

        return List.copyOf(pages);
    }
}
