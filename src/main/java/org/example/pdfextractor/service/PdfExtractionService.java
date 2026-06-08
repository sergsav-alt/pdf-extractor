package org.example.pdfextractor.service;

import org.example.pdfextractor.exception.ExtractionException;
import org.example.pdfextractor.model.ExtractedPage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Service for extracting page images from PDF documents.
 */
public interface PdfExtractionService {

    /**
     * Extracts all pages from a PDF file as high-resolution images.
     *
     * @param pdfPath path to the PDF file
     * @return list of extracted pages with images
     * @throws ExtractionException if reading or rendering fails
     */
    List<ExtractedPage> extractPages(Path pdfPath);

    /**
     * Extracts pages from a PDF provided as an input stream.
     *
     * @param inputStream PDF data stream
     * @return list of extracted pages with images
     * @throws ExtractionException if reading or rendering fails
     */
    List<ExtractedPage> extractPages(InputStream inputStream);
}
