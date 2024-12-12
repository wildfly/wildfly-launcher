/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.core.launcher.logger;

import static java.security.AccessController.doPrivileged;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
 * Copied the logging Messages to use 0 dependencies for this module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Messages {

    private Messages() {
    }

    /**
     * Get a message bundle of the given type.
     *
     * @param type the bundle type class
     *
     * @return the bundle
     */
    public static <T> T getBundle(final Class<T> type) {
        if (System.getSecurityManager() == null) {
            try {
                final Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                return doGetBundle(lookup, type);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("This library does not have private access to " + type);
            }
        } else {
            return doPrivileged((PrivilegedAction<T>) () -> {
                try {
                    final Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                    return doGetBundle(lookup, type);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("This library does not have private access to " + type);
                }
            });
        }
    }

    private static String join(final String interfaceName, final String lang, final String country, final String variant) {
        final StringBuilder build = new StringBuilder();
        build.append(interfaceName).append('_').append("$bundle");
        if (lang != null && !lang.isEmpty()) {
            build.append('_');
            build.append(lang);
        }
        if (country != null && !country.isEmpty()) {
            build.append('_');
            build.append(country);
        }
        if (variant != null && !variant.isEmpty()) {
            build.append('_');
            build.append(variant);
        }
        return build.toString();
    }

    private static <T> T doGetBundle(final MethodHandles.Lookup lookup, final Class<T> type) {
        final Locale locale = LoggingLocale.getLocale();
        final String language = locale.getLanguage();
        final String country = locale.getCountry();
        final String variant = locale.getVariant();

        Class<? extends T> bundleClass = null;
        if (!variant.isEmpty()) {
            try {
                bundleClass = lookup.findClass(join(type.getName(), language, country, variant))
                        .asSubclass(type);
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("The given lookup does not have access to the implementation class");
            }
        }
        if (bundleClass == null && !country.isEmpty()) {
            try {
                bundleClass = lookup.findClass(join(type.getName(), language, country, null))
                        .asSubclass(type);
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("The given lookup does not have access to the implementation class");
            }
        }
        if (bundleClass == null && !language.isEmpty()) {
            try {
                bundleClass = lookup.findClass(join(type.getName(), language, null, null)).asSubclass(type);
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("The given lookup does not have access to the implementation class");
            }
        }
        if (bundleClass == null) {
            try {
                bundleClass = lookup.findClass(join(type.getName(), null, null, null)).asSubclass(type);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Invalid bundle " + type + " (implementation not found)");
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("The given lookup does not have access to the implementation class");
            }
        }
        final MethodHandle getter;
        try {
            getter = lookup.findStaticGetter(bundleClass, "INSTANCE", bundleClass);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Bundle implementation " + bundleClass + " has no instance field");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "The given lookup does not have access to the implementation class instance field");
        }
        try {
            return type.cast(getter.invoke());
        } catch (Throwable e) {
            throw new IllegalArgumentException("Bundle implementation " + bundleClass + " could not be instantiated", e);
        }
    }
}
