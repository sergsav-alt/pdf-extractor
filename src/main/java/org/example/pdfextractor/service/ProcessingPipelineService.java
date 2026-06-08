package org.example.pdfextractor.service;

import org.example.pdfextractor.exception.ExtractionException;
import org.example.pdfextractor.model.ExtractionResult;

import java.nio.file.Path;

/**
 * Orchestrates the full PDF processing pipeline:
 * extraction → preprocessing → OCR → DOCX export.
 */
public interface ProcessingPipelineService {

    /**
     * Processes a PDF file: extracts page images, preprocesses them,
     * runs OCR, and exports the result to DOCX.
     *
     * @param pdfPath     path to the source PDF file
     * @param docxPath    path where the output DOCX file will be written
     * @param translateEn whether to add Russian translation after each English text
     * @return the complete extraction result
     * @throws ExtractionException if any step fails
     */
    ExtractionResult processPdf(Path pdfPath, Path docxPath, boolean translateEn);

    /**
     * Processes a PDF file and exports to DOCX without translation.
     *
     * @param pdfPath  path to the source PDF file
     * @param docxPath path where the output DOCX file will be written
     * @return the complete extraction result
     */
    ExtractionResult processPdf(Path pdfPath, Path docxPath);

    /**
     * Processes a PDF file and returns the DOCX as a byte array (no file written).
     *
     * @param pdfPath     path to the source PDF file
     * @param translateEn whether to add Russian translation after each English text
     * @return the complete extraction result with DOCX bytes
     */
    ExtractionResult processPdfToBytes(Path pdfPath, boolean translateEn);

    /**
     * Processes a PDF file and returns only the recognized text (no DOCX export).
     *
     * @param pdfPath path to the source PDF file
     * @return the extraction result with text but no DOCX output
     */
    ExtractionResult processPdfTextOnly(Path pdfPath);
}
