package org.example.pdfextractor.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Holds the complete result of processing a single PDF document:
 * the list of extracted pages and the output file paths.
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public final class ExtractionResult {

    private final Path sourcePdf;
    private final List<ExtractedPage> pages;
    private final Path docxOutput;

    public ExtractionResult(Path sourcePdf, List<ExtractedPage> pages, Path docxOutput) {
        this.sourcePdf = Objects.requireNonNull(sourcePdf, "sourcePdf must not be null");
        this.pages = List.copyOf(pages);
        this.docxOutput = docxOutput;
    }

    public int totalPages() {
        return pages.size();
    }
}
