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
import java.util.Map;
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
     * Upload a PDF, run OCR, and save the resulting DOCX to ~/Downloads.
     *
     * @param file     the uploaded PDF file
     * @param filename optional desired output filename (without extension)
     * @return JSON with the saved file name and its absolute path
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> extractPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {

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

            // Determine output filename
            String baseName = (filename != null && !filename.isBlank())
                    ? filename
                    : originalFilename.replaceAll("(?i)\\.pdf$", "");
            String docxFilename = baseName + ".docx";

            // Save to ~/Downloads
            Path downloadsDir = Path.of(System.getProperty("user.home"), "Downloads");
            Path outputPath = downloadsDir.resolve(docxFilename);

            // Export to DOCX file
            docxExportService.exportToDocx(result, outputPath);

            // Clean up temp PDF
            Files.deleteIfExists(tempPdf);

            log.info("DOCX saved: {} ({} pages, {} bytes)",
                    outputPath, result.totalPages(), Files.size(outputPath));

            return ResponseEntity.ok(Map.of(
                    "fileName", docxFilename,
                    "filePath", outputPath.toAbsolutePath().toString()
            ));

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
