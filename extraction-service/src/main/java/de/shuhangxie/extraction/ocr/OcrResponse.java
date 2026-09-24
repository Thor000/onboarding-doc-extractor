package de.shuhangxie.extraction.ocr;

import java.util.List;

/** Antwort des Python-OCR-Services (siehe ocr-service/app/main.py). */
public record OcrResponse(
        String filename,
        String text,
        List<Page> pages,
        Double meanConfidence,
        long durationMs
) {
    public record Page(int number, String method, Double confidence, int characters) {
    }

    /** "ocr", "text-layer" oder "mixed", wenn ein PDF beides enthält. */
    public String method() {
        if (pages == null || pages.isEmpty()) {
            return "unknown";
        }
        List<String> methods = pages.stream().map(Page::method).distinct().toList();
        return methods.size() == 1 ? methods.get(0) : "mixed";
    }
}
