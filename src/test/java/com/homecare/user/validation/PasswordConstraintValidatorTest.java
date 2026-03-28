package com.homecare.user.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordConstraintValidator — password policy enforcement")
class PasswordConstraintValidatorTest {

    private PasswordConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordConstraintValidator();
    }

    @Test
    @DisplayName("null password → valid (handled by @NotBlank)")
    void nullPassword_isValid() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    @DisplayName("blank password → valid (handled by @NotBlank)")
    void blankPassword_isValid() {
        assertTrue(validator.isValid("   ", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "StrongP@ss1",
            "MyP@ssw0rd",
            "Hello1234!",
            "Abcdefg1@",
            "Test#Pass9"
    })
    @DisplayName("valid passwords pass validation")
    void validPasswords(String password) {
        assertTrue(validator.isValid(password, null), "Should be valid: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",          // < 8 chars
            "nouppercase1!",    // no uppercase
            "NoDigits!!",       // no digit
            "NoSpecial1a",      // no special char
            "12345678",         // no uppercase, no special
            "ABCDEFGH",         // no digit, no special
    })
    @DisplayName("invalid passwords fail validation")
    void invalidPasswords(String password) {
        assertFalse(validator.isValid(password, null), "Should be invalid: " + password);
    }

    @Test
    @DisplayName("exactly 8 characters with all requirements → valid")
    void minLengthPassword() {
        assertTrue(validator.isValid("Abcdef1!", null));
    }

    @Test
    @DisplayName("password with spaces is invalid (not in allowed char set)")
    void passwordWithSpaces() {
        assertFalse(validator.isValid("Pass word1!", null));
    }
}

