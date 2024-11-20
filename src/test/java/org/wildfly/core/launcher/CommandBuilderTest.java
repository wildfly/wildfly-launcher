/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.wildfly.core.launcher.Arguments.Argument;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class CommandBuilderTest {

    private static final Path WILDFLY_HOME;
    private static final Path WILDFLY_BOOTABLE_JAR;

    static {
        WILDFLY_HOME = Paths.get(System.getProperty("jboss.home")).toAbsolutePath().normalize();
        WILDFLY_BOOTABLE_JAR = Paths.get(System.getProperty("wildfly.launcher.bootable.jar")).toAbsolutePath().normalize();
    }

    @Test
    void jBossModulesBuilder() {
        final int featureVersion = Runtime.version().feature();
        // Set up a standalone command builder
        final JBossModulesCommandBuilder commandBuilder = JBossModulesCommandBuilder.of(WILDFLY_HOME, "org.jboss.as.launcher.test")
                .addJavaOption("-Djava.net.preferIPv4Stack=true")
                .addJavaOption("-Djava.net.preferIPv4Stack=false")
                .addModuleOption("-javaagent:test-agent1.jar")
                .addServerArgument("--server=test");

        if (featureVersion < 24) {
            commandBuilder.addJavaOption("-Djava.security.manager");
        } else {
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.addJavaOption("-Djava.security.manager"));
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.setUseSecurityManager(true));
        }

        // Get all the commands
        List<String> commands = commandBuilder.buildArguments();

        assertEquals(featureVersion < 24, commands.contains("-secmgr"), "Missing -secmgr option");

        assertTrue(commands.stream().anyMatch(entry -> entry.matches("-javaagent:.*jboss-modules.jar$")), "Missing jboss-modules.jar");
        assertTrue(commands.contains("-javaagent:test-agent1.jar"), "Missing test-agent1.jar");
        assertTrue(commands.contains("--server=test"), "Missing --server=test");

        // If we're using Java 9+ ensure the modular JDK options were added
        testModularJvmArguments(commands, 1);

        // A system property should only be added ones
        long count = 0L;
        for (String s : commandBuilder.getJavaOptions()) {
            if (s.contains("java.net.preferIPv4Stack")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one java.net.preferIPv4Stack system property");

        // The value saved should be the last value added
        assertTrue(commandBuilder.getJavaOptions().contains("-Djava.net.preferIPv4Stack=false"), "java.net.preferIPv4Stack should be set to false");
    }

    @Test
    void standaloneBuilder() {
        final int featureVersion = Runtime.version().feature();
        // Set up a standalone command builder
        final StandaloneCommandBuilder commandBuilder = StandaloneCommandBuilder.of(WILDFLY_HOME)
                .setAdminOnly()
                .setBindAddressHint("0.0.0.0")
                .setDebug(true, 5005)
                .setServerConfiguration("standalone-full.xml")
                .addJavaOption("-Djava.net.preferIPv4Stack=true")
                .addJavaOption("-Djava.net.preferIPv4Stack=false")
                .addModuleOption("-javaagent:test-agent1.jar")
                .setBindAddressHint("management", "0.0.0.0");

        if (featureVersion < 24) {
            commandBuilder.addJavaOption("-Djava.security.manager");
        } else {
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.addJavaOption("-Djava.security.manager"));
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.setUseSecurityManager(true));
        }

        // Get all the commands
        List<String> commands = commandBuilder.buildArguments();

        assertTrue(commands.contains("--admin-only"), "--admin-only is missing");

        assertTrue(commands.contains("-b=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains("-bmanagement=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains(String.format(StandaloneCommandBuilder.DEBUG_FORMAT, "y", 5005)), "Missing debug argument");

        assertTrue(commands.contains("-c=standalone-full.xml"), "Missing server configuration file override");

        assertEquals(featureVersion < 24, commands.contains("-secmgr"), "Missing -secmgr option");

        assertTrue(commands.stream().anyMatch(entry -> entry.matches("-javaagent:.*jboss-modules.jar$")), "Missing jboss-modules.jar");
        assertTrue(commands.contains("-javaagent:test-agent1.jar"), "Missing test-agent1.jar");

        // If we're using Java 9+ ensure the modular JDK options were added
        testModularJvmArguments(commands, 1);

        // A system property should only be added ones
        long count = 0L;
        for (String s : commandBuilder.getJavaOptions()) {
            if (s.contains("java.net.preferIPv4Stack")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one java.net.preferIPv4Stack system property");

        // The value saved should be the last value added
        assertTrue(commandBuilder.getJavaOptions().contains("-Djava.net.preferIPv4Stack=false"), "java.net.preferIPv4Stack should be set to false");

        // Rename the binding address
        commandBuilder.setBindAddressHint(null);
        commands = commandBuilder.buildArguments();
        assertFalse(commands.contains("-b=0.0.0.0"), "Binding address should have been removed");
    }

    @Test
    void bootableJarBuilder() {
        // Set up a bootable command builder
        final BootableJarCommandBuilder commandBuilder = BootableJarCommandBuilder.of(WILDFLY_BOOTABLE_JAR)
                .setInstallDir(Paths.get("foo"))
                .setInstallDir(Paths.get("bar"))
                .setBindAddressHint("0.0.0.0")
                .setDebug(true, 5005)
                .addJavaOption("-Djava.security.manager")
                .addJavaOption("-Djava.net.preferIPv4Stack=true")
                .addJavaOption("-Djava.net.preferIPv4Stack=false")
                .setBindAddressHint("management", "0.0.0.0")
                .setYamlFiles(Path.of("bad.yml"))
                .setYamlFiles(Path.of("dummy.yml"));

        // Get all the commands
        List<String> commands = commandBuilder.buildArguments();

        assertTrue(commands.contains("--install-dir=bar"), "--install-dir is missing");

        assertTrue(commands.contains("-b=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains("-bmanagement=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains(String.format(StandaloneCommandBuilder.DEBUG_FORMAT, "y", 5005)), "Missing debug argument");

        assertTrue(commands.contains("--yaml=" + Path.of("dummy.yml").toFile().getAbsolutePath()), "--yaml is missing");

        // If we're using Java 12+. the enhanced security manager option must be set.
        testEnhancedSecurityManager(commands, 1);
        // Bootable JAR handles JPMS arguments thanks to its Manifest file.
        testJPMSArguments(commands, 0);
        // A system property should only be added ones
        long count = 0L;
        for (String s : commandBuilder.getJavaOptions()) {
            if (s.contains("java.net.preferIPv4Stack")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one java.net.preferIPv4Stack system property");

        // Install dir should be added once.
        count = 0L;
        for (String s : commandBuilder.getServerArguments()) {
            if (s.contains("--install-dir")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one --install-dir");

        // Install dir should be added once.
        count = 0L;
        for (String s : commandBuilder.getServerArguments()) {
            if (s.contains("--yaml")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one --yaml");

        // Rename the binding address
        commandBuilder.setBindAddressHint(null);
        commands = commandBuilder.buildArguments();
        assertFalse(commands.contains("-b=0.0.0.0"), "Binding address should have been removed");
    }

    @Test
    void domainBuilder() {
        final int featureVersion = Runtime.version().feature();
        // Set up a standalone command builder
        final DomainCommandBuilder commandBuilder = DomainCommandBuilder.of(WILDFLY_HOME)
                .setAdminOnly()
                .setBindAddressHint("0.0.0.0")
                .setMasterAddressHint("0.0.0.0")
                .setDomainConfiguration("domain.xml")
                .setHostConfiguration("host.xml")
                .setBindAddressHint("management", "0.0.0.0");

        if (featureVersion < 24) {
            commandBuilder.addJavaOption("-Djava.security.manager");
        } else {
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.addJavaOption("-Djava.security.manager"));
            assertThrows(IllegalArgumentException.class, () -> commandBuilder.setUseSecurityManager(true));
        }

        // Get all the commands
        List<String> commands = commandBuilder.buildArguments();

        assertTrue(commands.contains("--admin-only"), "--admin-only is missing");

        assertTrue(commands.contains("-b=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains("--primary-address=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains("-bmanagement=0.0.0.0"), "Missing -b=0.0.0.0");

        assertTrue(commands.contains("-c=domain.xml"), "Missing server configuration file override");

        assertEquals(featureVersion < 24, commands.contains("-secmgr"), "Missing -secmgr option");

        // If we're using Java 9+ ensure the modular JDK options were added
        testModularJvmArguments(commands, 2);

        // Rename the binding address
        commandBuilder.setBindAddressHint(null);
        commands = commandBuilder.buildArguments();
        assertFalse(commands.contains("-b=0.0.0.0"), "Binding address should have been removed");
    }

    @Test
    void cliBuilder() {
        // Set up a standalone command builder
        final CliCommandBuilder commandBuilder = CliCommandBuilder.asModularLauncher(WILDFLY_HOME)
                .addJavaOption("-Djava.net.preferIPv4Stack=true")
                .addJavaOption("-Djava.net.preferIPv4Stack=false");

        // Get all the commands
        final List<String> commands = commandBuilder.buildArguments();

        // If we're using Java 9+ ensure the modular JDK options were added
        testModularJvmArguments(commands, 1);

        // A system property should only be added ones
        long count = 0L;
        for (String s : commandBuilder.getJavaOptions()) {
            if (s.contains("java.net.preferIPv4Stack")) {
                count++;
            }
        }
        assertEquals(1, count, "There should be only one java.net.preferIPv4Stack system property");

        // The value saved should be the last value added
        assertTrue(commandBuilder.getJavaOptions().contains("-Djava.net.preferIPv4Stack=false"), "java.net.preferIPv4Stack should be set to false");
    }

    @Test
    void arguments() {
        final Arguments arguments = new Arguments();
        arguments.add("-Dkey=value");
        arguments.add("-X");
        arguments.add("-X");
        arguments.set("single-key", "single-value");
        arguments.set("single-key", "single-value");
        arguments.addAll("-Dprop1=value1", "-Dprop2=value2", "-Dprop3=value3");

        // Validate the arguments
        Iterator<Argument> iter = arguments.getArguments("key").iterator();
        assertTrue(iter.hasNext(), "Missing 'key' entry");
        assertEquals("value", arguments.get("key"));
        assertEquals("-Dkey=value", iter.next().asCommandLineArgument());

        // -X should have been added twice
        assertEquals(2, arguments.getArguments("-X").size());

        // Using set should only add the value once
        assertEquals(1, arguments.getArguments("single-key").size(), "Should not be more than one 'single-key' argument");

        // Convert the arguments to a list and ensure each entry has been added in the format expected
        final List<String> stringArgs = arguments.asList();
        assertEquals(7, stringArgs.size());
        assertTrue(stringArgs.contains("-Dkey=value"), "Missing -Dkey=value");
        assertTrue(stringArgs.contains("-X"), "Missing -X");
        assertTrue(stringArgs.contains("single-key=single-value"), "Missing single-key=single-value");
        assertTrue(stringArgs.contains("-Dprop1=value1"), "Missing -Dprop1=value1");
        assertTrue(stringArgs.contains("-Dprop2=value2"), "Missing -Dprop2=value2");
        assertTrue(stringArgs.contains("-Dprop3=value3"), "Missing -Dprop3=value3");
    }

    private void testEnhancedSecurityManager(final Collection<String> command, final int expectedCount) {
        // If we're using Java 12+, but less than 24 ensure enhanced security manager option was added
        if (Jvm.current().enhancedSecurityManagerAvailable()) {
            assertArgumentExists(command, "-Djava.security.manager=allow", expectedCount);
        } else {
            assertFalse(command.contains("-Djava.security.manager=allow"),
                    "Did not expect \"-Djava.security.manager=allow\" to be in the command list");
        }
    }

    private void testJPMSArguments(final Collection<String> command, final int expectedCount) {
        // Check exports and opens
        assertArgumentExists(command, "--add-exports=java.desktop/sun.awt=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-exports=java.naming/com.sun.jndi.url.ldap=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-exports=java.naming/com.sun.jndi.url.ldaps=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED", expectedCount);
        if (getJavaVersion() <= 12) {
            // for condition see WFCORE-4296 - java.base/com.sun.net.ssl.internal.ssl isn't available since JDK13
            assertArgumentExists(command, "--add-opens=java.base/com.sun.net.ssl.internal.ssl=ALL-UNNAMED", expectedCount);
        }
        assertArgumentExists(command, "--add-opens=java.base/java.lang=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.io=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.net=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.security=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.util=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.management/javax.management=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-opens=java.naming/javax.naming=ALL-UNNAMED", expectedCount);
        assertArgumentExists(command, "--add-modules=java.se", expectedCount);
    }

    private static int getJavaVersion() {
        return Runtime.version().feature();
    }

    private void testModularJvmArguments(final Collection<String> command, final int expectedCount) {
        testEnhancedSecurityManager(command, expectedCount);
        testJPMSArguments(command, expectedCount);
    }

    private static void assertArgumentExists(final Collection<String> args, final String arg, final int expectedCount) {
        int count = 0;
        for (String value : args) {
            if (value.equals(arg)) {
                count++;
            }
        }
        assertEquals(expectedCount, count, String.format("Expected %d %s arguments, found %d", expectedCount, arg, count));
    }

}
