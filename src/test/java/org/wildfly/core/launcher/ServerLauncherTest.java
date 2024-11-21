/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.wildfly.plugin.tools.ConsoleConsumer;
import org.wildfly.plugin.tools.ContainerDescription;
import org.wildfly.plugin.tools.server.ServerManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Tag("non-modular")
class ServerLauncherTest {
    private static final Path JBOSS_HOME = Path.of(System.getProperty("jboss.home"));
    private static final Path BOOTABLE_JAR = Path.of(System.getProperty("wildfly.launcher.bootable.jar"));


    @TestTemplate
    @ExtendWith(ServerLaunchTestTemplateInvocationContextProvider.class)
    void launch(final CommandBuilder commandBuilder, final long timeout, final TestInfo testInfo) throws Exception {
        final Launcher launcher = Launcher.of(commandBuilder);
        Process process = null;
        try {
            process = launcher.launch();
            final Process capturedProcess = process;
            final CapturingOutputStream out = new CapturingOutputStream();
            ConsoleConsumer.start(process, out);
            Assertions.assertTrue(process.isAlive(), () -> String.format("The process has terminated: %d - %s", capturedProcess.exitValue(), out));
            try (
                    ServerManager serverManager = ServerManager.builder()
                            .process(process)
                            .shutdownOnClose(true)
                            .build()
                            .get(timeout, TimeUnit.SECONDS)
            ) {
                ConsoleConsumer.start(process, out);
                Assertions.assertTrue(serverManager.waitFor(timeout, TimeUnit.SECONDS), () -> String.format("Failed to start %s within %d seconds. Process: %s%n%s", testInfo.getDisplayName(), timeout, capturedProcess,
                        out));
                Assertions.assertTrue(serverManager.isRunning(), () -> String.format("Server %s is not running. Process: %s%n%s", testInfo.getDisplayName(), capturedProcess,
                        out));
                final ContainerDescription description = serverManager.containerDescription();
                if (commandBuilder instanceof StandaloneCommandBuilder || commandBuilder instanceof BootableJarCommandBuilder) {
                    Assertions.assertFalse(description.isDomain(), () -> String.format("Expected the server to not be a domain server: " + description));
                } else {
                    Assertions.assertTrue(description.isDomain(), () -> String.format("Expected the server to be a domain server: " + description));
                }
            } catch (Throwable e) {
                Assertions.fail(String.format("Failed starting %s: %s", testInfo.getDisplayName(), out), e);
            }
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    public static class ServerLaunchTestTemplateInvocationContextProvider implements TestTemplateInvocationContextProvider {

        @Override
        public boolean supportsTestTemplate(final ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
            return Stream.of(createTestContext("Standalone", StandaloneCommandBuilder.of(JBOSS_HOME), 60L),
                    createTestContext("Domain", DomainCommandBuilder.of(JBOSS_HOME), 60L),
                    createTestContext("Bootable JAR", BootableJarCommandBuilder.of(BOOTABLE_JAR), 60L)
            );
        }

        @SuppressWarnings("SameParameterValue")
        private TestTemplateInvocationContext createTestContext(final String name, final CommandBuilder commandBuilder, final long timeout) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(final int invocationIndex) {
                    return name;
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return List.of(
                            new ParameterResolver() {
                                @Override
                                public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
                                    return parameterContext.getParameter().getType() == CommandBuilder.class;
                                }

                                @Override
                                public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
                                    return commandBuilder;
                                }
                            },
                            new ParameterResolver() {
                                @Override
                                public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
                                    return parameterContext.getParameter().getType() == long.class;
                                }

                                @Override
                                public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
                                    return timeout;
                                }
                            }
                    );
                }
            };
        }
    }

    private static class CapturingOutputStream extends ByteArrayOutputStream {
        @Override
        public synchronized void write(final int b) {
            try {
                super.write(b);
            } finally {
                System.out.write(b);
            }
        }

        @Override
        public synchronized void write(final byte[] b, final int off, final int len) {
            try {
                super.write(b, off, len);
            } finally {
                System.out.write(b, off, len);
            }
        }

        @Override
        public synchronized void write(final byte[] b) throws IOException {
            try {
                super.write(b);
            } finally {
                System.out.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                super.flush();
            } finally {
                System.out.flush();
            }
        }

        @Override
        public synchronized String toString() {
            return super.toString(StandardCharsets.UTF_8);
        }
    }
}
