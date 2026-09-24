package de.shuhangxie.extraction.validation;

import de.shuhangxie.extraction.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTextMatcherTest {

    private final SourceTextMatcher matcher = new SourceTextMatcher(TestData.OCR_TEXT);

    @Test
    void findsValuesDespiteSpacingAndCase() {
        assertThat(matcher.contains("DE812345673")).isTrue();
        assertThat(matcher.contains("nordlicht funktechnik gmbh")).isTrue();
        assertThat(matcher.contains("EN 62368-1")).isTrue();
    }

    @Test
    void toleratesMissingUmlautsFromOcr() {
        // OCR liest "Prüfauftrag" als "Prufauftrag"
        assertThat(matcher.contains("Prüfauftrag")).isTrue();
    }

    @Test
    void detectsInventedValues() {
        assertThat(matcher.contains("Südwind Elektronik AG")).isFalse();
        assertThat(matcher.contains("DE999999999")).isFalse();
    }

    @Test
    void comparesPhoneNumbersByTrailingDigits() {
        assertThat(matcher.containsPhone("0201 5550182")).isTrue();
        assertThat(matcher.containsPhone("+49 201 555 9999")).isFalse();
    }
}
