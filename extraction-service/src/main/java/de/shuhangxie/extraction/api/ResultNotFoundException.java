package de.shuhangxie.extraction.api;

import java.util.UUID;

public class ResultNotFoundException extends RuntimeException {

    public ResultNotFoundException(UUID id) {
        super("Kein Ergebnis mit der ID " + id);
    }
}
