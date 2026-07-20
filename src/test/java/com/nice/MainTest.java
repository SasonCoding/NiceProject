package com.nice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void mainRunsWithoutThrowing() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
