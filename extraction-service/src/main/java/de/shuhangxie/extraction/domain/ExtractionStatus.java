package de.shuhangxie.extraction.domain;

public enum ExtractionStatus {
    /** Alle Prüfungen bestanden – kann automatisch übernommen werden. */
    VALID,
    /** Mindestens eine Auffälligkeit – ein Mensch sollte vor der Übernahme draufschauen. */
    NEEDS_REVIEW
}
