package de.shuhangxie.extraction.api;

import de.shuhangxie.extraction.llm.LlmException;
import de.shuhangxie.extraction.ocr.OcrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Einheitliche Fehlerantworten im Format RFC 9457 (Problem Details). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResultNotFoundException.class)
    ProblemDetail notFound(ResultNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", e.getMessage());
    }

    @ExceptionHandler({InvalidUploadException.class, MissingServletRequestPartException.class})
    ProblemDetail badRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Ungültiger Upload", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Datei zu groß", "Maximal 20 MB pro Dokument");
    }

    @ExceptionHandler(OcrException.class)
    ProblemDetail ocrFailed(OcrException e) {
        log.warn("OCR fehlgeschlagen", e);
        return problem(HttpStatus.BAD_GATEWAY, "OCR-Service-Fehler", e.getMessage());
    }

    @ExceptionHandler(LlmException.class)
    ProblemDetail llmFailed(LlmException e) {
        log.warn("Extraktion durch Sprachmodell fehlgeschlagen", e);
        return problem(HttpStatus.BAD_GATEWAY, "Sprachmodell-Fehler", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
