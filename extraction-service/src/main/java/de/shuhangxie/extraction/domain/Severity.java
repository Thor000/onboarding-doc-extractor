package de.shuhangxie.extraction.domain;

public enum Severity {
    /** Der Wert ist falsch oder fehlt, obwohl er Pflicht ist – so darf der Datensatz nicht ins Kernsystem. */
    ERROR,
    /** Auffälligkeit, die ein Mensch kurz prüfen sollte. */
    WARNING
}
