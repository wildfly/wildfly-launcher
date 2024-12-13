/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher.logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Tag("new-jvm")
//
abstract class LocalizationTest {
    private final String bcp47Tag;

    LocalizationTest(final String bcp47Tag) {
        System.setProperty("org.jboss.logging.locale", bcp47Tag);
        this.bcp47Tag = bcp47Tag;
    }

    @Test
    void className() {
        final String className = LauncherMessages.MESSAGES.getClass().getSimpleName();
        final String expectedClassName = String.format("LauncherMessages_$bundle_%s", bcp47Tag.replace('-', '_'));
        // Check that we loaded the correct class based on the locale
        Assertions.assertEquals(expectedClassName, className);
    }

    @Test
    void checkMessage() throws Exception {
        final Properties properties = loadProperties();
        final Path pathName = Path.of("path");
        // Compare the message from the translation file to the message from the message interface
        Assertions.assertEquals(String.format("WFLYLNCHR0001: " + properties.getProperty("pathDoesNotExist"), pathName),
                LauncherMessages.MESSAGES.pathDoesNotExist(pathName).getMessage());
    }

    private Properties loadProperties() throws IOException {
        // Load the properties file which contains the translations.
        String resourceName = String.format("LauncherMessages.i18n_%s.properties", bcp47Tag.replace('-', '_'));
        try (InputStream stream = getClass().getResourceAsStream(resourceName)) {
            Assertions.assertNotNull(stream, "Could not find translations for " + resourceName);
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        }
    }
}
