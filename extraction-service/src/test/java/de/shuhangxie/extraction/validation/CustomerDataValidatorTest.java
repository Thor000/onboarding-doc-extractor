package de.shuhangxie.extraction.validation;

import de.shuhangxie.extraction.TestData;
import de.shuhangxie.extraction.domain.CustomerData;
import de.shuhangxie.extraction.domain.Severity;
import de.shuhangxie.extraction.domain.ValidationIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDataValidatorTest {

    private final CustomerDataValidator validator = new CustomerDataValidator();

    @Test
    void acceptsCompleteAndGroundedData() {
        assertThat(validator.validate(TestData.validCustomer(), TestData.OCR_TEXT)).isEmpty();
    }

    @Test
    void reportsWrongVatIdCheckDigit() {
        List<ValidationIssue> issues = validator.validate(TestData.withVatId("DE812345674"), "DE812345674 " + TestData.OCR_TEXT);

        assertThat(issues).extracting(ValidationIssue::field, ValidationIssue::code, ValidationIssue::severity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("vatId", "INVALID_CHECKSUM", Severity.ERROR));
    }

    @Test
    void flagsValuesThatAreNotInTheDocument() {
        CustomerData invented = TestData.withVatId("DE136695976");

        List<ValidationIssue> issues = validator.validate(invented, TestData.OCR_TEXT);

        assertThat(issues).extracting(ValidationIssue::field, ValidationIssue::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("vatId", "NOT_IN_SOURCE"));
    }

    @Test
    void missingRequiredFieldsAreErrors() {
        CustomerData empty = new CustomerData("", null, null, null, null, null, " ", null, null, null, null);

        List<ValidationIssue> issues = validator.validate(empty, "");

        assertThat(issues).filteredOn(i -> i.severity() == Severity.ERROR)
                .extracting(ValidationIssue::field)
                .containsExactlyInAnyOrder("companyName", "contactPerson", "email");
    }

    @Test
    void validatesEmailAndGermanPostalCode() {
        CustomerData c = TestData.validCustomer();
        CustomerData broken = new CustomerData(c.companyName(), c.street(), "4512", c.city(), "Deutschland",
                c.vatId(), c.contactPerson(), "a.becker(at)nordlicht", c.phone(), c.product(), c.standards());

        List<ValidationIssue> issues = validator.validate(broken, TestData.OCR_TEXT + " 4512 a.becker(at)nordlicht");

        assertThat(issues).extracting(ValidationIssue::field, ValidationIssue::code)
                .contains(org.assertj.core.groups.Tuple.tuple("postalCode", "INVALID_FORMAT"),
                        org.assertj.core.groups.Tuple.tuple("email", "INVALID_FORMAT"));
    }

    @Test
    void foreignPostalCodesAreNotCheckedAgainstGermanFormat() {
        CustomerData c = TestData.validCustomer();
        CustomerData austrian = new CustomerData(c.companyName(), c.street(), "1010", "Wien", "Österreich",
                "ATU12345678", c.contactPerson(), c.email(), c.phone(), c.product(), c.standards());

        List<ValidationIssue> issues = validator.validate(austrian, TestData.OCR_TEXT + " 1010 Wien ATU12345678");

        assertThat(issues).isEmpty();
    }
}
