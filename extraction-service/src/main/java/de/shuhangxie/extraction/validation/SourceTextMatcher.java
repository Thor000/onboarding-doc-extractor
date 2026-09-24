package de.shuhangxie.extraction.validation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Prüft, ob ein vom Sprachmodell gelieferter Wert tatsächlich im Dokument steht.
 * <p>
 * Sprachmodelle ergänzen gelegentlich plausible, aber erfundene Werte ("Halluzinationen").
 * Weil jeder extrahierte Wert aus dem Dokument stammen muss, lässt sich das einfach
 * gegenprüfen: Wert und OCR-Text werden normalisiert und verglichen. Kleine OCR-Fehler
 * werden über einen Token-Vergleich toleriert.
 */
public class SourceTextMatcher {

    private static final double MIN_TOKEN_SHARE = 0.75;
    private static final int PHONE_DIGITS_COMPARED = 8;

    private final String normalizedSource;
    private final String sourceDigits;

    public SourceTextMatcher(String sourceText) {
        String source = sourceText == null ? "" : sourceText;
        this.normalizedSource = normalize(source);
        this.sourceDigits = source.replaceAll("\\D", "");
    }

    public boolean contains(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty() || normalizedSource.contains(normalizedValue)) {
            return true;
        }
        List<String> tokens = Arrays.stream(fold(value).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .toList();
        if (tokens.isEmpty()) {
            return false;
        }
        long found = tokens.stream().filter(normalizedSource::contains).count();
        return (double) found / tokens.size() >= MIN_TOKEN_SHARE;
    }

    /**
     * Telefonnummern werden vom Modell oft umformatiert (0211 … → +49 211 …).
     * Verglichen werden daher nur die letzten Ziffern.
     */
    public boolean containsPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return true;
        }
        String digits = phone.replaceAll("\\D", "");
        String tail = digits.length() > PHONE_DIGITS_COMPARED
                ? digits.substring(digits.length() - PHONE_DIGITS_COMPARED)
                : digits;
        return sourceDigits.contains(tail);
    }

    static String normalize(String text) {
        return fold(text).replaceAll("[^a-z0-9]", "");
    }

    /** Kleinschreibung, ß → ss, Umlaute und Akzente → Grundbuchstabe (OCR liefert oft "a" statt "ä"). */
    private static String fold(String text) {
        String lower = text.toLowerCase(Locale.GERMAN).replace("ß", "ss");
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
