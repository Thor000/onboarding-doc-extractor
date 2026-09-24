package de.shuhangxie.extraction.api;

import de.shuhangxie.extraction.domain.ExtractionResult;
import de.shuhangxie.extraction.service.ExtractionService;
import de.shuhangxie.extraction.service.ResultRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final ExtractionService extractionService;
    private final ResultRepository repository;

    public DocumentController(ExtractionService extractionService, ResultRepository repository) {
        this.extractionService = extractionService;
        this.repository = repository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractionResult> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidUploadException("Die hochgeladene Datei ist leer");
        }
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        ExtractionResult result = extractionService.process(filename, file.getBytes());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{id}")
    public ExtractionResult get(@PathVariable UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResultNotFoundException(id));
    }

    @GetMapping
    public List<ExtractionResult> list() {
        return repository.findAllNewestFirst();
    }
}
