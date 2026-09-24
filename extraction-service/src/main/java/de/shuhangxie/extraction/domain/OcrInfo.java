package de.shuhangxie.extraction.domain;

/** Kennzahlen aus dem OCR-Schritt, damit nachvollziehbar bleibt, wie gut die Textgrundlage war. */
public record OcrInfo(String method, Double meanConfidence, int pages, int characters) {
}
