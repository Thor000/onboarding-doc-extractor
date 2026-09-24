package de.shuhangxie.extraction;

import de.shuhangxie.extraction.domain.CustomerData;

import java.util.List;

/** Testdaten passend zu samples/pruefauftrag_scan.png. */
public final class TestData {

    public static final String OCR_TEXT = """
            Prufauftrag - Kundendaten
            1. Auftraggeber
            Firma Nordlicht Funktechnik GmbH
            StraBe / Nr. Hafenstraße 12
            PLZ / Ort 45127 Essen
            Land Deutschland
            USt-IdNr. DE 812 345 673
            Ansprechpartner/in Dr. Anna Becker
            E-Mail a.becker@nordlicht-funk.example
            Telefon +49 201 555 0182
            2. Prifgegenstand
            Produkt NLT-Sensor 868 (LoRa-Funkmodul)
            Gewunschte Normen Kl EN 300 220-2
            Kl EN 301 489-3
            Kl EN 62368-1
            """;

    private TestData() {
    }

    public static CustomerData validCustomer() {
        return new CustomerData("Nordlicht Funktechnik GmbH", "Hafenstraße 12", "45127", "Essen", "Deutschland",
                "DE812345673", "Dr. Anna Becker", "a.becker@nordlicht-funk.example", "+49 201 555 0182",
                "NLT-Sensor 868 (LoRa-Funkmodul)", List.of("EN 300 220-2", "EN 301 489-3", "EN 62368-1"));
    }

    public static CustomerData withVatId(String vatId) {
        CustomerData c = validCustomer();
        return new CustomerData(c.companyName(), c.street(), c.postalCode(), c.city(), c.country(), vatId,
                c.contactPerson(), c.email(), c.phone(), c.product(), c.standards());
    }
}
