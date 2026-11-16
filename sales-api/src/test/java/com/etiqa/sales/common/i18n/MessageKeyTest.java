package com.etiqa.sales.common.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MessageKeyTest {

    @Test
    void everyKeyHasText() throws IOException {
        Properties messages = new Properties();
        try (InputStream stream = getClass().getResourceAsStream("/messages.properties")) {
            assertThat(stream).isNotNull();
            messages.load(stream);
        }

        Stream<String> enumKeys = Arrays.stream(MessageKey.values()).map(MessageKey::getKey);
        Stream<String> validationKeys = Stream.of(
                MessageKey.VALIDATION_USERNAME_REQUIRED,
                MessageKey.VALIDATION_USERNAME_LENGTH,
                MessageKey.VALIDATION_USERNAME_CHARACTERS,
                MessageKey.VALIDATION_PASSWORD_REQUIRED,
                MessageKey.VALIDATION_PASSWORD_LENGTH,
                MessageKey.VALIDATION_SALE_QUANTITY_MIN
        ).map(template -> template.substring(1, template.length() - 1));

        assertThat(Stream.concat(enumKeys, validationKeys)).allSatisfy(key ->
                assertThat(messages.getProperty(key)).as("text for %s", key).isNotBlank());
    }
}
