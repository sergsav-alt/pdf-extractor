package org.example.pdfextractor.service;

import org.example.pdfextractor.exception.ExtractionException;
import org.example.pdfextractor.model.ExtractedPage;
import org.example.pdfextractor.model.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full PDF processing pipeline:
 * extraction → preprocessing → OCR → DOCX export.
 */
@Service
public class ProcessingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingPipelineService.class);

    private final PdfExtractionService pdfExtractionService;
    private final ImagePreprocessor imagePreprocessor;
    private final OcrService ocrService;
    private final DocxExportService docxExportService;

    public ProcessingPipelineService(PdfExtractionService pdfExtractionService,
                                     ImagePreprocessor imagePreprocessor,
                                     OcrService ocrService,
                                     DocxExportService docxExportService) {
        this.pdfExtractionService = pdfExtractionService;
        this.imagePreprocessor = imagePreprocessor;
        this.ocrService = ocrService;
        this.docxExportService = docxExportService;
    }

    /**
     * Processes a PDF file: extracts page images, preprocesses them,
     * runs OCR, and exports the result to DOCX.
     *
     * @param pdfPath  path to the source PDF file
     * @param docxPath path where the output DOCX file will be written
     * @return the complete extraction result
     * @throws ExtractionException if any step fails
     */
    public ExtractionResult processPdf(Path pdfPath, Path docxPath) {
        log.info("Starting processing pipeline for PDF: {}", pdfPath);

        // Step 1: Extract page images from PDF
        List<ExtractedPage> rawPages = pdfExtractionService.extractPages(pdfPath);
        log.info("Step 1 — Extracted {} raw page images", rawPages.size());

        // Step 2 & 3: Preprocess each image and run OCR
        List<ExtractedPage> processedPages = new ArrayList<>(rawPages.size());
        for (ExtractedPage rawPage : rawPages) {
            BufferedImage preprocessed = imagePreprocessor.preprocess(rawPage.image());
            OcrService.OcrResult ocrResult = ocrService.recognizeTextWithConfidence(preprocessed);
            processedPages.add(new ExtractedPage(
                    rawPage.pageNumber(),
                    rawPage.image(),   // keep original image for DOCX embedding
                    ocrResult.text(),
                    ocrResult.confidence()
            ));
            log.debug("Page {} OCR confidence: {}", rawPage.pageNumber(), ocrResult.confidence());
        }

        // Step 4: Build result and export to DOCX
        var result = new ExtractionResult(pdfPath, processedPages, docxPath);
        docxExportService.exportToDocx(result, docxPath);

        log.info("Pipeline complete. Output: {}", docxPath);
        return result;
    }

    /**
     * Processes a PDF file and returns the DOCX as a byte array (no file written).
     *
     * @param pdfPath path to the source PDF file
     * @return the complete extraction result with DOCX bytes
     */
    public ExtractionResult processPdfToBytes(Path pdfPath) {
        log.info("Starting in-memory processing pipeline for PDF: {}", pdfPath);

        List<ExtractedPage> rawPages = pdfExtractionService.extractPages(pdfPath);
        log.info("Step 1 — Extracted {} raw page images", rawPages.size());

        List<ExtractedPage> processedPages = new ArrayList<>(rawPages.size());
        for (ExtractedPage rawPage : rawPages) {
            BufferedImage preprocessed = imagePreprocessor.preprocess(rawPage.image());
            OcrService.OcrResult ocrResult = ocrService.recognizeTextWithConfidence(preprocessed);
            processedPages.add(new ExtractedPage(
                    rawPage.pageNumber(),
                    rawPage.image(),
                    ocrResult.text(),
                    ocrResult.confidence()
            ));
        }

        var result = new ExtractionResult(pdfPath, processedPages, null);
        byte[] docxBytes = docxExportService.exportToDocxBytes(result);

        log.info("In-memory pipeline complete. DOCX size: {} bytes", docxBytes.length);
        return result;
    }

    /**
     * Processes a PDF file and returns only the recognized text (no DOCX export).
     *
     * @param pdfPath path to the source PDF file
     * @return the extraction result with text but no DOCX output
     */
    public ExtractionResult processPdfTextOnly(Path pdfPath) {
        log.info("Starting text-only processing for PDF: {}", pdfPath);

        List<ExtractedPage> rawPages = pdfExtractionService.extractPages(pdfPath);

        List<ExtractedPage> processedPages = new ArrayList<>(rawPages.size());
        for (ExtractedPage rawPage : rawPages) {
            BufferedImage preprocessed = imagePreprocessor.preprocess(rawPage.image());
            OcrService.OcrResult ocrResult = ocrService.recognizeTextWithConfidence(preprocessed);
            processedPages.add(new ExtractedPage(
                    rawPage.pageNumber(),
                    rawPage.image(),
                    ocrResult.text(),
                    ocrResult.confidence()
            ));
        }

        return new ExtractionResult(pdfPath, processedPages, null);
    }
}
