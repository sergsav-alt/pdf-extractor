package org.example.pdfextractor.controller;

import org.example.pdfextractor.exception.ExtractionException;
import org.example.pdfextractor.model.ExtractionResult;
import org.example.pdfextractor.service.DocxExportService;
import org.example.pdfextractor.service.ProcessingPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * REST controller for PDF extraction and OCR processing.
 */
@RestController
@RequestMapping("/api/v1/extract")
public class ExtractionController {

    private static final Logger log = LoggerFactory.getLogger(ExtractionController.class);

    private final ProcessingPipelineService pipelineService;
    private final DocxExportService docxExportService;

    public ExtractionController(ProcessingPipelineService pipelineService,
                                DocxExportService docxExportService) {
        this.pipelineService = pipelineService;
        this.docxExportService = docxExportService;
    }

    /**
     * Upload a PDF, run OCR, and download the resulting DOCX.
     *
     * @param file the uploaded PDF file
     * @return DOCX file as a downloadable response
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> extractPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "document.pdf");
        log.info("Received PDF: {} ({} bytes)", originalFilename, file.getSize());

        try {
            // Save uploaded file to a temp location
            Path tempPdf = Files.createTempFile("upload-", ".pdf");
            file.transferTo(tempPdf.toFile());

            // Run full pipeline: extract + preprocess + OCR → get pages with text
            ExtractionResult result = pipelineService.processPdfTextOnly(tempPdf);

            // Export to DOCX bytes
            byte[] docxBytes = docxExportService.exportToDocxBytes(result);

            // Clean up temp PDF
            Files.deleteIfExists(tempPdf);

            // Build DOCX filename
            String docxFilename = originalFilename.replaceAll("(?i)\\.pdf$", "") + ".docx";

            log.info("Returning DOCX: {} ({} pages, {} bytes)",
                    docxFilename, result.totalPages(), docxBytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + docxFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(docxBytes);

        } catch (IOException e) {
            log.error("Failed to process PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
