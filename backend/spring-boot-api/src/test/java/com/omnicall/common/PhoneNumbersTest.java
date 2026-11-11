package com.omnicall.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void stripsSpacesFromInternationalNumbers() {
        assertEquals("+919876543210", PhoneNumbers.normalize("+91 98765 43210"));
    }

    @Test
    void rejectsLocalNumbersWithoutACountryCode() {
        assertThrows(ApiException.class, () -> PhoneNumbers.normalize("9876543210"));
    }
}
