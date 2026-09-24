package de.shuhangxie.extraction.domain;

public record ValidationIssue(String field, Severity severity, String code, String message) {

    public static ValidationIssue error(String field, String code, String message) {
        return new ValidationIssue(field, Severity.ERROR, code, message);
    }

    public static ValidationIssue warning(String field, String code, String message) {
        return new ValidationIssue(field, Severity.WARNING, code, message);
    }
}
