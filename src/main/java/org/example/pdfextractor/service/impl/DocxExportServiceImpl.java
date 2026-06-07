package org.example.pdfextractor.service.impl;

import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.example.pdfextractor.model.ExtractedPage;
import org.example.pdfextractor.model.ExtractionResult;
import org.example.pdfextractor.service.DocxExportService;
import org.example.pdfextractor.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

/**
 * Exports OCR results into a DOCX file using Apache POI.
 * Each page becomes a section with its number, recognized text,
 * and optionally the page image embedded as a thumbnail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxExportServiceImpl implements DocxExportService {

    private final TranslationService translationService;

    @Override
    public void exportToDocx(ExtractionResult result, Path outputPath, boolean translateEn) {
        log.info("Exporting {} pages to DOCX: {} (translate={})",
                result.totalPages(), outputPath, translateEn);

        try (var document = new XWPFDocument()) {
            writeMetadata(document, result);

            for (ExtractedPage page : result.pages()) {
                writePageToDocument(document, page, translateEn);
            }

            // Delete existing file if present to ensure clean overwrite
            Files.deleteIfExists(outputPath);
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                document.write(out);
            }

            log.info("DOCX export complete: {} bytes", Files.size(outputPath));
        } catch (IOException e) {
            throw new org.example.pdfextractor.exception.ExtractionException(
                    "Failed to export DOCX to " + outputPath, e);
        }
    }

    @Override
    public byte[] exportToDocxBytes(ExtractionResult result, boolean translateEn) {
        log.info("Exporting {} pages to DOCX byte array (translate={})",
                result.totalPages(), translateEn);

        try (var document = new XWPFDocument();
             var baos = new ByteArrayOutputStream()) {

            writeMetadata(document, result);

            for (ExtractedPage page : result.pages()) {
                writePageToDocument(document, page, translateEn);
            }

            document.write(baos);
            byte[] bytes = baos.toByteArray();
            log.info("DOCX export complete: {} bytes", bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new org.example.pdfextractor.exception.ExtractionException(
                    "Failed to export DOCX to byte array", e);
        }
    }

    @Override
    public void exportToDocx(ExtractionResult result, Path outputPath) {
        exportToDocx(result, outputPath, false);
    }

    @Override
    public byte[] exportToDocxBytes(ExtractionResult result) {
        return exportToDocxBytes(result, false);
    }

    // --- Private helpers ---

    private void writeMetadata(XWPFDocument document, ExtractionResult result) {
        document.getProperties().getCoreProperties().setCreator("pdf-extractor");
        document.getProperties().getCoreProperties().setDescription(
                "OCR extraction from: " + result.sourcePdf().getFileName());
    }

    private void writePageToDocument(XWPFDocument document, ExtractedPage page, boolean translateEn) {
        // --- Page number heading ---
        XWPFParagraph titlePara = document.createParagraph();
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(14);
        titleRun.setText("Page " + page.pageNumber());
        titleRun.addBreak();

        // --- Recognized text ---
        String text = page.text();
        if (text != null && !text.isBlank()) {
            // English text
            XWPFParagraph textPara = document.createParagraph();
            XWPFRun textRun = textPara.createRun();
            textRun.setFontSize(10);
            textRun.setText(text);

            // Russian translation (if requested) — separated by 3 blank lines
            if (translateEn) {
                // Add three blank lines between English and Russian
                IntStream.range(0, 3)
                        .forEach(i -> textRun.addBreak());

                XWPFParagraph transPara = document.createParagraph();
                XWPFRun transRun = transPara.createRun();
                transRun.setFontSize(10);
                transRun.setItalic(true);
                transRun.setText(translationService.translate(text, "en", "ru"));
            }
        } else {
            XWPFParagraph emptyPara = document.createParagraph();
            XWPFRun emptyRun = emptyPara.createRun();
            emptyRun.setText("[No text recognized on this page]");
        }

        // --- Page image (embedded as PNG thumbnail) ---
        if (page.image() != null) {
            try {
                var pngBytes = new ByteArrayOutputStream();
                ImageIO.write(page.image(), "PNG", pngBytes);

                XWPFParagraph imgPara = document.createParagraph();
                XWPFRun imgRun = imgPara.createRun();
                imgRun.addPicture(
                        new ByteArrayInputStream(pngBytes.toByteArray()),
                        PictureType.PNG,
                        "page-" + page.pageNumber() + ".png",
                        Units.toEMU(400),   // target width in EMU
                        Units.toEMU(300)    // target height in EMU
                );
            } catch (Exception e) {
                log.warn("Failed to embed image for page {}: {}", page.pageNumber(), e.getMessage());
            }
        }

        // Page separator
        XWPFParagraph separator = document.createParagraph();
        XWPFRun sepRun = separator.createRun();
        sepRun.setText("— — — — — — — — — — — — — — —");
        sepRun.addBreak();
    }
}
