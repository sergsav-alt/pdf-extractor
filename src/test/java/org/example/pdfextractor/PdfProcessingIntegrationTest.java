package org.example.pdfextractor;

import org.example.pdfextractor.model.ExtractedPage;
import org.example.pdfextractor.model.ExtractionResult;
import org.example.pdfextractor.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that runs the full pipeline on the actual encyclopedia PDF.
 */
@SpringBootTest
class PdfProcessingIntegrationTest {

    @Autowired
    private ProcessingPipelineService pipelineService;

    @Autowired
    private PdfExtractionService pdfExtractionService;

    @Autowired
    private ImagePreprocessor imagePreprocessor;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DocxExportService docxExportService;

    @Test
    void testPdfExtractionService() {
        Path pdfPath = Paths.get("Энциклопедия САСШ 2.PDF");
        List<ExtractedPage> pages = pdfExtractionService.extractPages(pdfPath);
        assertNotNull(pages);
        assertFalse(pages.isEmpty(), "PDF should have at least one page");
        System.out.println("Total pages extracted: " + pages.size());
        for (ExtractedPage page : pages) {
            assertNotNull(page.image());
            assertTrue(page.image().getWidth() > 0);
            assertTrue(page.image().getHeight() > 0);
        }
    }

    @Test
    void testImagePreprocessor() {
        Path pdfPath = Paths.get("Энциклопедия САСШ 2.PDF");
        List<ExtractedPage> pages = pdfExtractionService.extractPages(pdfPath);
        ExtractedPage firstPage = pages.get(0);
        var preprocessed = imagePreprocessor.preprocess(firstPage.image());
        assertNotNull(preprocessed);
        assertEquals(firstPage.image().getWidth(), preprocessed.getWidth());
        assertEquals(firstPage.image().getHeight(), preprocessed.getHeight());
    }

    @Test
    void testFullPipelineTextOnly() {
        Path pdfPath = Paths.get("Энциклопедия САСШ 2.PDF");
        ExtractionResult result = pipelineService.processPdfTextOnly(pdfPath);

        assertNotNull(result);
        assertNotNull(result.pages());
        assertFalse(result.pages().isEmpty());

        System.out.println("\n=== OCR RESULTS ===");
        System.out.println("Total pages: " + result.totalPages());
        for (ExtractedPage page : result.pages()) {
            System.out.printf("Page %d (confidence: %.2f): %d chars%n",
                    page.pageNumber(), page.confidence(), page.text().length());
            if (!page.text().isBlank()) {
                System.out.println("  First 200 chars: " + page.text().substring(0, Math.min(200, page.text().length())));
            }
        }
    }

    @Test
    void testFullPipelineWithDocxExport() throws Exception {
        Path pdfPath = Paths.get("Энциклопедия САСШ 2.PDF");
        Path docxPath = Paths.get("target/Энциклопедия_САСШ_2.docx");

        ExtractionResult result = pipelineService.processPdf(pdfPath, docxPath);

        assertNotNull(result);
        assertTrue(java.nio.file.Files.exists(docxPath));
        long fileSize = java.nio.file.Files.size(docxPath);
        assertTrue(fileSize > 0);
        System.out.println("DOCX exported: " + docxPath.toAbsolutePath() + " (" + fileSize + " bytes)");
    }

    @Test
    void testOcrService() {
        Path pdfPath = Paths.get("Энциклопедия САСШ 2.PDF");
        List<ExtractedPage> pages = pdfExtractionService.extractPages(pdfPath);
        ExtractedPage firstPage = pages.get(0);

        var preprocessed = imagePreprocessor.preprocess(firstPage.image());
        OcrService.OcrResult result = ocrService.recognizeTextWithConfidence(preprocessed);

        assertNotNull(result);
        assertNotNull(result.text());
        System.out.println("OCR confidence: " + result.confidence());
        System.out.println("OCR text length: " + result.text().length());
        System.out.println("First 300 chars:");
        System.out.println(result.text().substring(0, Math.min(300, result.text().length())));
    }
}
