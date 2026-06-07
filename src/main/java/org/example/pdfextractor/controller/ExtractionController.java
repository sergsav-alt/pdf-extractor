package org.example.pdfextractor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pdfextractor.model.ExtractionResult;
import org.example.pdfextractor.service.DocxExportService;
import org.example.pdfextractor.service.ProcessingPipelineService;
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
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/extract")
public class ExtractionController {

    private final ProcessingPipelineService pipelineService;
    private final DocxExportService docxExportService;

    /**
     * Upload a PDF, run OCR, and save the resulting DOCX to ~/Downloads.
     *
     * @param file      the uploaded PDF file
     * @param filename  optional desired output filename (without extension)
     * @param translate optional flag; if "1", adds Russian translation after each English text
     * @return JSON with the saved file name and its absolute path
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> extractPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "translate", required = false) String translate) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "document.pdf");
        log.info("Received PDF: {} ({} bytes), translate={}", originalFilename, file.getSize(), translate);

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

            // Whether to add Russian translation
            boolean translateEn = "1".equals(translate);

            // Export to DOCX file
            docxExportService.exportToDocx(result, outputPath, translateEn);

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
