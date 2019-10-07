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

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.ClassWriter;
import org.elasticsearch.painless.CompilerSettings;
import org.elasticsearch.painless.Globals;
import org.elasticsearch.painless.Locals;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.MethodWriter;
import org.elasticsearch.painless.ScriptRoot;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * Represents an if/else block.
 */
public final class SIfElse extends AStatement {

    private AExpression condition;
    private final SBlock ifblock;
    private final SBlock elseblock;

    public SIfElse(Location location, AExpression condition, SBlock ifblock, SBlock elseblock) {
        super(location);

        this.condition = Objects.requireNonNull(condition);
        this.ifblock = ifblock;
        this.elseblock = elseblock;
    }

    @Override
    void storeSettings(CompilerSettings settings) {
        condition.storeSettings(settings);

        if (ifblock != null) {
            ifblock.storeSettings(settings);
        }

        if (elseblock != null) {
            elseblock.storeSettings(settings);
        }
    }

    @Override
    void extractVariables(Set<String> variables) {
        condition.extractVariables(variables);

        if (ifblock != null) {
            ifblock.extractVariables(variables);
        }

        if (elseblock != null) {
            elseblock.extractVariables(variables);
        }
    }

    @Override
    void analyze(ScriptRoot scriptRoot, Locals locals) {
        condition.expected = boolean.class;
        condition.analyze(scriptRoot, locals);
        condition = condition.cast(scriptRoot, locals);

        if (condition.constant != null) {
            throw createError(new IllegalArgumentException("Extraneous if statement."));
        }

        if (ifblock == null) {
            throw createError(new IllegalArgumentException("Extraneous if statement."));
        }

        ifblock.lastSource = lastSource;
        ifblock.inLoop = inLoop;
        ifblock.lastLoop = lastLoop;

        ifblock.analyze(scriptRoot, Locals.newLocalScope(locals));

        anyContinue = ifblock.anyContinue;
        anyBreak = ifblock.anyBreak;
        statementCount = ifblock.statementCount;

        if (elseblock == null) {
            throw createError(new IllegalArgumentException("Extraneous else statement."));
        }

        elseblock.lastSource = lastSource;
        elseblock.inLoop = inLoop;
        elseblock.lastLoop = lastLoop;

        elseblock.analyze(scriptRoot, Locals.newLocalScope(locals));

        methodEscape = ifblock.methodEscape && elseblock.methodEscape;
        loopEscape = ifblock.loopEscape && elseblock.loopEscape;
        allEscape = ifblock.allEscape && elseblock.allEscape;
        anyContinue |= elseblock.anyContinue;
        anyBreak |= elseblock.anyBreak;
        statementCount = Math.max(ifblock.statementCount, elseblock.statementCount);
    }

    @Override
    void write(ClassWriter classWriter, MethodWriter methodWriter, Globals globals) {
        methodWriter.writeStatementOffset(location);

        Label fals = new Label();
        Label end = new Label();

        condition.write(classWriter, methodWriter, globals);
        methodWriter.ifZCmp(Opcodes.IFEQ, fals);

        ifblock.continu = continu;
        ifblock.brake = brake;
        ifblock.write(classWriter, methodWriter, globals);

        if (!ifblock.allEscape) {
            methodWriter.goTo(end);
        }

        methodWriter.mark(fals);

        elseblock.continu = continu;
        elseblock.brake = brake;
        elseblock.write(classWriter, methodWriter, globals);

        methodWriter.mark(end);
    }

    @Override
    public String toString() {
        return multilineToString(singleton(condition), Arrays.asList(ifblock, elseblock));
    }
}
