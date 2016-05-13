/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless;

import org.elasticsearch.bootstrap.BootstrapInfo;
import org.elasticsearch.painless.Variables.Reserved;
import org.elasticsearch.painless.antlr.Walker;
import org.elasticsearch.painless.node.SSource;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;

import static org.elasticsearch.painless.WriterConstants.CLASS_NAME;

/**
 * The Compiler is the entry point for generating a Painless script.  The compiler will receive a Painless
 * tree based on the type of input passed in (currently only ANTLR).  Two passes will then be run over the tree,
 * one for analysis using the {@link Analyzer} and another to generate the actual byte code using ASM in
 * the {@link Writer}.
 */
final class Compiler {

    /**
     * The maximum number of characters allowed in the script source.
     */
    static int MAXIMUM_SOURCE_LENGTH = 16384;

    /**
     * Define the class with lowest privileges.
     */
    private static final CodeSource CODESOURCE;

    /**
     * Setup the code privileges.
     */
    static {
        try {
            // Setup the code privileges.
            CODESOURCE = new CodeSource(new URL("file:" + BootstrapInfo.UNTRUSTED_CODEBASE), (Certificate[]) null);
        } catch (final MalformedURLException impossible) {
            throw new RuntimeException(impossible);
        }
    }

    /**
     * A secure class loader used to define Painless scripts.
     */
    static final class Loader extends SecureClassLoader {
        /**
         * @param parent The parent ClassLoader.
         */
        Loader(final ClassLoader parent) {
            super(parent);
        }

        /**
         * Generates a Class object from the generated byte code.
         * @param name The name of the class.
         * @param bytes The generated byte code.
         * @return A Class object extending {@link Executable}.
         */
        Class<? extends Executable> define(final String name, final byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length, CODESOURCE).asSubclass(Executable.class);
        }
    }

    /**
     * Runs the two-pass compiler to generate a Painless script.
     * @param loader The ClassLoader used to define the script.
     * @param name The name of the script.
     * @param source The source code for the script.
     * @param settings The CompilerSettings to be used during the compilation.
     * @return An {@link Executable} Painless script.
     */
    static Executable compile(final Loader loader, final String name, final String source, final CompilerSettings settings) {
        byte[] bytes = compile(name, source, settings);

        return createExecutable(loader, name, source, bytes);
    }

    /**
     * Runs the two-pass compiler to generate a Painless script.  (Used by the debugger.)
     * @param source The source code for the script.
     * @param settings The CompilerSettings to be used during the compilation.
     * @return The bytes for compilation.
     */
    static byte[] compile(String name, String source, CompilerSettings settings) {
        if (source.length() > MAXIMUM_SOURCE_LENGTH) {
            throw new IllegalArgumentException("Scripts may be no longer than " + MAXIMUM_SOURCE_LENGTH +
                " characters.  The passed in script is " + source.length() + " characters.  Consider using a" +
                " plugin if a script longer than this length is a requirement.");
        }

        final Reserved reserved = new Reserved();
        final SSource root = Walker.buildPainlessTree(source, reserved);
        final Variables variables = Analyzer.analyze(settings, Definition.INSTANCE, reserved, root);

        return Writer.write(settings, Definition.INSTANCE, name, source, variables, root);
    }

    /**
     * Generates an {@link Executable} that can run a Painless script.
     * @param loader The {@link Loader} to define the script's class file.
     * @param name The name of the script.
     * @param source The source text of the script.
     * @param bytes The ASM generated byte code to define the class with.
     * @return A Painless {@link Executable} script.
     */
    private static Executable createExecutable(final Loader loader, final String name, final String source, final byte[] bytes) {
        try {
            final Class<? extends Executable> clazz = loader.define(CLASS_NAME, bytes);
            final java.lang.reflect.Constructor<? extends Executable> constructor = clazz.getConstructor(String.class, String.class);

            return constructor.newInstance(name, source);
        } catch (final Exception exception) { // Catch everything to let the user know this is something caused internally.
            throw new IllegalStateException(
                    "An internal error occurred attempting to define the script [" + name + "].", exception);
        }
    }

    /**
     * All methods in the compiler should be static.
     */
    private Compiler() {}
}
