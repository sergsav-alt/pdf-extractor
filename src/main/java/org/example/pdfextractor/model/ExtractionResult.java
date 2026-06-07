package org.example.pdfextractor.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds the complete result of processing a single PDF document:
 * the list of extracted pages and the output file paths.
 */
public final class ExtractionResult {

    private final Path sourcePdf;
    private final List<ExtractedPage> pages;
    private final Path docxOutput;

    public ExtractionResult(Path sourcePdf, List<ExtractedPage> pages, Path docxOutput) {
        this.sourcePdf = Objects.requireNonNull(sourcePdf, "sourcePdf must not be null");
        this.pages = List.copyOf(pages);
        this.docxOutput = docxOutput;
    }

    public Path sourcePdf() {
        return sourcePdf;
    }

    public List<ExtractedPage> pages() {
        return Collections.unmodifiableList(pages);
    }

    public Path docxOutput() {
        return docxOutput;
    }

    public int totalPages() {
        return pages.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExtractionResult that)) return false;
        return sourcePdf.equals(that.sourcePdf)
                && pages.equals(that.pages)
                && Objects.equals(docxOutput, that.docxOutput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePdf, pages, docxOutput);
    }

    @Override
    public String toString() {
        return "ExtractionResult{" +
                "sourcePdf=" + sourcePdf +
                ", pages=" + pages.size() +
                ", docxOutput=" + docxOutput +
                '}';
    }
}
