package com.mindhaven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MindHavenApplicationTests {

    @Test
    void applicationClassIsAvailable() {
        assertThat(MindHavenApplication.class).isNotNull();
    }
}
