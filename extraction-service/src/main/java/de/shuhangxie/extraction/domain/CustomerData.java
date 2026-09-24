package de.shuhangxie.extraction.domain;

import java.util.List;
import java.util.Objects;

/**
 * Die Kundendaten, die aus einem Prüfauftrag oder einer Anfrage extrahiert werden.
 * Leere Strings aus dem Sprachmodell werden als "nicht vorhanden" (null) behandelt.
 */
public record CustomerData(
        String companyName,
        String street,
        String postalCode,
        String city,
        String country,
        String vatId,
        String contactPerson,
        String email,
        String phone,
        String product,
        List<String> standards
) {
    public CustomerData {
        companyName = blankToNull(companyName);
        street = blankToNull(street);
        postalCode = blankToNull(postalCode);
        city = blankToNull(city);
        country = blankToNull(country);
        vatId = blankToNull(vatId);
        contactPerson = blankToNull(contactPerson);
        email = blankToNull(email);
        phone = blankToNull(phone);
        product = blankToNull(product);
        standards = standards == null ? List.of() : standards.stream()
                .map(CustomerData::blankToNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
