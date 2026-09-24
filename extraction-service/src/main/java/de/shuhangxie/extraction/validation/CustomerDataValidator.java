package de.shuhangxie.extraction.validation;

import de.shuhangxie.extraction.domain.CustomerData;
import de.shuhangxie.extraction.domain.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Deterministische Prüfung der extrahierten Daten.
 * <p>
 * Das Sprachmodell liefert einen Vorschlag – ob der Datensatz ins Kernsystem darf,
 * entscheidet dieser Validator mit festen, nachvollziehbaren Regeln.
 */
public class CustomerDataValidator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");
    private static final Pattern GERMAN_POSTAL_CODE = Pattern.compile("^\\d{5}$");
    private static final Pattern STANDARD = Pattern.compile("^(?:[A-Z]{2,5}\\s)+[0-9][0-9 .\\-:/]*[0-9]$");
    private static final Set<String> GERMANY = Set.of("deutschland", "germany", "de", "brd");

    /** Felder, die gegen den Dokumenttext geprüft werden. "country" fehlt bewusst: das Land darf erschlossen werden. */
    private static final Map<String, Function<CustomerData, String>> GROUNDED_FIELDS = Map.of(
            "companyName", CustomerData::companyName,
            "street", CustomerData::street,
            "postalCode", CustomerData::postalCode,
            "city", CustomerData::city,
            "vatId", CustomerData::vatId,
            "contactPerson", CustomerData::contactPerson,
            "email", CustomerData::email,
            "product", CustomerData::product
    );

    public List<ValidationIssue> validate(CustomerData data, String sourceText) {
        List<ValidationIssue> issues = new ArrayList<>();
        checkRequired(data, issues);
        checkFormats(data, issues);
        checkGrounding(data, new SourceTextMatcher(sourceText), issues);
        return issues;
    }

    private void checkRequired(CustomerData data, List<ValidationIssue> issues) {
        requireError("companyName", data.companyName(), issues);
        requireError("contactPerson", data.contactPerson(), issues);
        requireError("email", data.email(), issues);
        requireWarning("street", data.street(), issues);
        requireWarning("postalCode", data.postalCode(), issues);
        requireWarning("city", data.city(), issues);
        requireWarning("vatId", data.vatId(), issues);
        if (data.standards().isEmpty()) {
            issues.add(ValidationIssue.warning("standards", "MISSING", "Keine Prüfnorm angegeben"));
        }
    }

    private void checkFormats(CustomerData data, List<ValidationIssue> issues) {
        if (data.email() != null && !EMAIL.matcher(data.email()).matches()) {
            issues.add(ValidationIssue.error("email", "INVALID_FORMAT", "Keine gültige E-Mail-Adresse: " + data.email()));
        }

        if (data.postalCode() != null && isGermany(data.country())
                && !GERMAN_POSTAL_CODE.matcher(data.postalCode()).matches()) {
            issues.add(ValidationIssue.error("postalCode", "INVALID_FORMAT",
                    "Deutsche Postleitzahlen haben fünf Ziffern: " + data.postalCode()));
        }

        if (data.vatId() != null) {
            switch (VatIdValidator.check(data.vatId())) {
                case INVALID_FORMAT -> issues.add(ValidationIssue.error("vatId", "INVALID_FORMAT",
                        "Ungültiges Format der USt-IdNr.: " + data.vatId()));
                case INVALID_CHECKSUM -> issues.add(ValidationIssue.error("vatId", "INVALID_CHECKSUM",
                        "Prüfziffer der USt-IdNr. stimmt nicht – Lesefehler oder Tippfehler im Dokument: " + data.vatId()));
                case VALID -> { }
            }
        }

        for (String standard : data.standards()) {
            if (!STANDARD.matcher(standard).matches()) {
                issues.add(ValidationIssue.warning("standards", "UNUSUAL_FORMAT",
                        "Ungewöhnliche Normbezeichnung: " + standard));
            }
        }
    }

    private void checkGrounding(CustomerData data, SourceTextMatcher matcher, List<ValidationIssue> issues) {
        GROUNDED_FIELDS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String value = entry.getValue().apply(data);
                    if (!matcher.contains(value)) {
                        issues.add(notInSource(entry.getKey(), value));
                    }
                });
        if (!matcher.containsPhone(data.phone())) {
            issues.add(notInSource("phone", data.phone()));
        }
        for (String standard : data.standards()) {
            if (!matcher.contains(standard)) {
                issues.add(notInSource("standards", standard));
            }
        }
    }

    private static ValidationIssue notInSource(String field, String value) {
        return ValidationIssue.warning(field, "NOT_IN_SOURCE",
                "Wert kommt im Dokument nicht vor – möglicherweise vom Modell ergänzt: " + value);
    }

    private static boolean isGermany(String country) {
        return country == null || GERMANY.contains(country.toLowerCase(Locale.ROOT));
    }

    private static void requireError(String field, String value, List<ValidationIssue> issues) {
        if (value == null) {
            issues.add(ValidationIssue.error(field, "MISSING", "Pflichtfeld fehlt"));
        }
    }

    private static void requireWarning(String field, String value, List<ValidationIssue> issues) {
        if (value == null) {
            issues.add(ValidationIssue.warning(field, "MISSING", "Feld nicht im Dokument gefunden"));
        }
    }
}
