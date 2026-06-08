package org.example.pdfextractor.service;

import org.example.pdfextractor.model.ExtractionResult;

import java.nio.file.Path;

/**
 * Service for exporting OCR results to DOCX files.
 */
public interface DocxExportService {

    /**
     * Exports the extraction result to a DOCX file at the given output path.
     *
     * @param result      the full extraction result (pages with text and images)
     * @param outputPath  where to write the DOCX file
     * @param translateEn whether to add Russian translation after each English text
     */
    void exportToDocx(ExtractionResult result, Path outputPath, boolean translateEn);

    /**
     * Exports to DOCX and returns the byte array (e.g. for download response).
     *
     * @param result      the full extraction result
     * @param translateEn whether to add Russian translation after each English text
     * @return DOCX file content as byte array
     */
    byte[] exportToDocxBytes(ExtractionResult result, boolean translateEn);

    /**
     * Exports without translation (backwards-compatible).
     */
    void exportToDocx(ExtractionResult result, Path outputPath);

    /**
     * Exports to bytes without translation (backwards-compatible).
     */
    byte[] exportToDocxBytes(ExtractionResult result);
}
