package de.shuhangxie.extraction.llm;

import de.shuhangxie.extraction.domain.CustomerData;

/** Abstraktion über das Sprachmodell – austauschbar (Ollama, anderes lokales Modell, Test-Double). */
public interface LlmExtractor {

    CustomerData extract(String documentText);

    /** Name des verwendeten Modells, wird im Ergebnis mitgespeichert. */
    String modelName();
}
