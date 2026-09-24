package de.shuhangxie.extraction.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Prüft Umsatzsteuer-Identifikationsnummern.
 * <p>
 * Für deutsche Nummern (DE + 9 Ziffern) wird zusätzlich die Prüfziffer nach dem
 * Verfahren ISO 7064, MOD 11,10 berechnet. Damit fallen Lese- und Tippfehler auf,
 * die ein Sprachmodell oder die OCR nicht bemerkt.
 */
public final class VatIdValidator {

    private static final Pattern GERMAN = Pattern.compile("DE\\d{9}");
    private static final Pattern EU_GENERIC = Pattern.compile("[A-Z]{2}[0-9A-Z+*]{2,12}");

    public enum Result { VALID, INVALID_FORMAT, INVALID_CHECKSUM }

    private VatIdValidator() {
    }

    /** Entfernt Leerzeichen, Punkte und Bindestriche und wandelt in Großbuchstaben. */
    public static String normalize(String vatId) {
        return vatId == null ? null : vatId.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }

    public static Result check(String vatId) {
        String normalized = normalize(vatId);
        if (normalized == null) {
            return Result.INVALID_FORMAT;
        }
        if (normalized.startsWith("DE")) {
            if (!GERMAN.matcher(normalized).matches()) {
                return Result.INVALID_FORMAT;
            }
            return hasValidGermanCheckDigit(normalized.substring(2)) ? Result.VALID : Result.INVALID_CHECKSUM;
        }
        // Für andere EU-Länder nur eine grobe Formatprüfung – jedes Land hat eigene Prüfverfahren.
        return EU_GENERIC.matcher(normalized).matches() ? Result.VALID : Result.INVALID_FORMAT;
    }

    /** ISO 7064, MOD 11,10 über die ersten 8 Ziffern; die 9. Ziffer ist die Prüfziffer. */
    static boolean hasValidGermanCheckDigit(String nineDigits) {
        int product = 10;
        for (int i = 0; i < 8; i++) {
            int sum = (Character.getNumericValue(nineDigits.charAt(i)) + product) % 10;
            if (sum == 0) {
                sum = 10;
            }
            product = (2 * sum) % 11;
        }
        int checkDigit = 11 - product;
        if (checkDigit == 10) {
            checkDigit = 0;
        }
        return checkDigit == Character.getNumericValue(nineDigits.charAt(8));
    }
}
