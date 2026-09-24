package de.shuhangxie.extraction.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt und JSON-Schema für die Extraktion.
 * <p>
 * Das Schema wird an Ollama als {@code format} übergeben ("Structured Outputs"). Das Modell
 * kann dann nur noch JSON erzeugen, das exakt diesem Schema entspricht – freie Texte oder
 * fehlende Klammern sind damit ausgeschlossen.
 */
public final class ExtractionPrompt {

    public static final String SYSTEM = """
            Du extrahierst Kundendaten aus Dokumenten eines Prüflabors (Prüfaufträge, Anfragen, Formulare).
            Der Text stammt aus einer OCR und kann Lesefehler enthalten.

            Regeln:
            - Übernimm nur Informationen, die im Dokument stehen. Erfinde nichts.
            - Fehlt eine Information, gib einen leeren String "" zurück (bei standards eine leere Liste).
            - Übernimm Werte in der Schreibweise des Dokuments; korrigiere nur offensichtliche OCR-Fehler bei Umlauten.
            - vatId: Umsatzsteuer-Identifikationsnummer ohne Leerzeichen, z. B. DE123456789.
            - contactPerson: Name der Ansprechperson ohne Anrede, Titel wie "Dr." bleiben erhalten.
            - country: Land der Firmenadresse. Ist es nicht angegeben, aber aus PLZ und Ort eindeutig, nutze den deutschen Ländernamen.
            - standards: Liste der genannten Prüfnormen, z. B. ["EN 300 328", "EN 301 489-1"].
            - Feldbezeichnungen aus dem Formular ("Firma", "Produkt", "PLZ / Ort", "E-Mail") gehören nicht in den Wert.
            - Ankreuzfelder vor einem Wert (☒, ☐, "Kl", "KI", "X") gehören nicht dazu.
            - PLZ und Ort stehen oft in einer Zeile ("45127 Essen", "… 88 · 40212 Düsseldorf"): Die Ziffernfolge ist die PLZ, der Rest der Ort.
            """;

    private static final List<String> TEXT_FIELDS = List.of(
            "companyName", "street", "postalCode", "city", "country",
            "vatId", "contactPerson", "email", "phone", "product");

    private ExtractionPrompt() {
    }

    public static String userMessage(String documentText) {
        return "Extrahiere die Kundendaten aus folgendem Dokument:\n\n<dokument>\n"
                + documentText
                + "\n</dokument>";
    }

    public static Map<String, Object> jsonSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        TEXT_FIELDS.forEach(field -> properties.put(field, Map.of("type", "string")));
        properties.put("standards", Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        return schema;
    }
}
