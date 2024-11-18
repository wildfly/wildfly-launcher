/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class LauncherTest {

    private Path stdout;

    @BeforeEach
    void setup() throws IOException {
        stdout = Files.createTempFile("stdout", ".txt");
    }

    @AfterEach
    void deleteStdout() throws IOException {
        if (stdout != null) {
            Files.deleteIfExists(stdout);
        }
    }

    @Test
    void checkSingleNullEnvironmentVariable() throws Exception {
        final TestCommandBuilder commandBuilder = new TestCommandBuilder();
        checkProcess(Launcher.of(commandBuilder).addEnvironmentVariable("TEST", null));
    }

    @Test
    void checkNullEnvironmentVariables() throws Exception {
        final TestCommandBuilder commandBuilder = new TestCommandBuilder();
        final Map<String, String> env = new HashMap<>();
        env.put("TEST", null);
        env.put("TEST_2", "test2");
        checkProcess(Launcher.of(commandBuilder).addEnvironmentVariables(env));
    }

    private void checkProcess(final Launcher launcher) throws IOException, InterruptedException {
        Process process = null;
        try {
            process = launcher.setRedirectErrorStream(true).redirectOutput(stdout).launch();
            assertNotNull(process, "Process should not be null");
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Process should have exited within 5 seconds");
            assertEquals(0, process.exitValue(), String.format("Process should have exited with an exit code of 0:%n%s", Files.readString(stdout)));
        } finally {
            ProcessHelper.destroyProcess(process);
        }
    }

    /**
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    private static class TestCommandBuilder implements CommandBuilder {
        @Override
        public List<String> buildArguments() {
            return List.of();
        }

        @Override
        public List<String> build() {
            return List.of(Jvm.current().getCommand(), "-version");
        }
    }
}
