package de.shuhangxie.extraction.service;

import de.shuhangxie.extraction.domain.ExtractionResult;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-Memory-Speicher für den Prototyp. In Produktion wäre das eine Tabelle (JPA). */
@Repository
public class ResultRepository {

    private final Map<UUID, ExtractionResult> results = new ConcurrentHashMap<>();

    public ExtractionResult save(ExtractionResult result) {
        results.put(result.id(), result);
        return result;
    }

    public Optional<ExtractionResult> findById(UUID id) {
        return Optional.ofNullable(results.get(id));
    }

    public List<ExtractionResult> findAllNewestFirst() {
        return results.values().stream()
                .sorted(Comparator.comparing(ExtractionResult::createdAt).reversed())
                .toList();
    }
}
