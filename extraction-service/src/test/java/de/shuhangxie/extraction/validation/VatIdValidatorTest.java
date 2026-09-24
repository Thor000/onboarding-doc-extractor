package de.shuhangxie.extraction.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VatIdValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "DE136695976, VALID",
            "DE812345673, VALID",
            "'DE 812 345 673', VALID",
            "de812345673, VALID",
            "DE293746105, INVALID_CHECKSUM",
            "DE81234567, INVALID_FORMAT",
            "DE8123456X3, INVALID_FORMAT",
            "ATU12345678, VALID",
            "12345, INVALID_FORMAT"
    })
    void checksGermanAndEuVatIds(String vatId, VatIdValidator.Result expected) {
        assertThat(VatIdValidator.check(vatId)).isEqualTo(expected);
    }
}
