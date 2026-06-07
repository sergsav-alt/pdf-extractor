package org.example.pdfextractor.service.impl;

import org.example.pdfextractor.model.ExtractedPage;
import org.example.pdfextractor.model.ExtractionResult;
import org.example.pdfextractor.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full PDF processing pipeline:
 * extraction → preprocessing → OCR → DOCX export.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingPipelineServiceImpl implements ProcessingPipelineService {

    private final PdfExtractionService pdfExtractionService;
    private final ImagePreprocessor imagePreprocessor;
    private final OcrService ocrService;
    private final DocxExportService docxExportService;

    @Override
    public ExtractionResult processPdf(Path pdfPath, Path docxPath, boolean translateEn) {
        log.info("Starting processing pipeline for PDF: {} (translate={})", pdfPath, translateEn);

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
        docxExportService.exportToDocx(result, docxPath, translateEn);

        log.info("Pipeline complete. Output: {}", docxPath);
        return result;
    }

    @Override
    public ExtractionResult processPdf(Path pdfPath, Path docxPath) {
        return processPdf(pdfPath, docxPath, false);
    }

    @Override
    public ExtractionResult processPdfToBytes(Path pdfPath, boolean translateEn) {
        log.info("Starting in-memory processing pipeline for PDF: {} (translate={})", pdfPath, translateEn);

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
        byte[] docxBytes = docxExportService.exportToDocxBytes(result, translateEn);

        log.info("In-memory pipeline complete. DOCX size: {} bytes", docxBytes.length);
        return result;
    }

    @Override
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
