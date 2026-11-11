package com.matriz;

import com.matriz.api.ApiModels.CreateTaskRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTitleValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsTitleAtMinimumLength() {
        assertFalse(hasTitleViolation("Fix"));
    }

    @Test
    void rejectsWhitespaceOnlyTitle() {
        assertTrue(hasTitleViolation("   "));
    }

    @Test
    void enforcesMaximumTitleLength() {
        assertFalse(hasTitleViolation("T".repeat(150)));
        assertTrue(hasTitleViolation("T".repeat(151)));
    }

    private static boolean hasTitleViolation(String title) {
        return validator.validate(new CreateTaskRequest(UUID.randomUUID(), title, null, null, null))
                .stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("title"));
    }
}
