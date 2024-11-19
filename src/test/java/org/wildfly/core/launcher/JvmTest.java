/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JvmTest {

    @ParameterizedTest
    @MethodSource("testReleases")
    void releaseFile(final String version, final boolean expectedValue) throws Exception {
        final Path javaHome = createFakeJavaHome(version);
        try {
            assertEquals(expectedValue, Jvm.of(javaHome).isSecurityManagerSupported(), () ->
                    String.format("Expected version %s to %s support the security manager", version, (expectedValue ? "" : "not")));
        } finally {
            Files.walkFileTree(javaHome, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    static Stream<Arguments> testReleases() {
        return Stream.of(
                arguments("", false),
                arguments("9", true),
                arguments("9.0", true),
                arguments("9.0.1", true),
                arguments("10", true),
                arguments("10.0", true),
                arguments("10.0.2", true),
                arguments("11", true),
                arguments("11.0.1", true),
                arguments("21.0.5", true),
                arguments("23.0.3", true),
                arguments("24", false),
                arguments("25.0.1", false)
        );
    }

    private static Path createFakeJavaHome(final String version) throws IOException {
        final Path javaHome = Files.createTempDirectory("fake-java-home");
        Files.createFile(Files.createDirectory(javaHome.resolve("bin"))
                .resolve(Environment.isWindows() ? "java.exe" : "java"));
        final Path releaseFile = javaHome.resolve("release");
        Files.write(releaseFile, Collections.singleton(String.format("JAVA_VERSION=\"%s\"%n", version)), StandardCharsets.UTF_8);
        return javaHome;
    }
}
