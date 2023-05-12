/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

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

/**
 * A painless tree is composed of the node classes found in this package.
 * <p>
 * The following are the types of nodes:
 * A* (abstract) - These are the abstract nodes that are the superclasses for the other types.
 * I* (interface) - These are marker interfaces to denote a property of the node.
 * S* (statement) - These are nodes that represent a statement in Painless.
 * E* (expression) - These are nodes that represent an expression in Painless.
 * P* (postfix) - These are nodes that represent a postfix of a variable chain.
 * E/P* (storeable) - These are nodes that are allowed to store a value to memory.
 * *Sub* (sub) - These are partial nodes with a parent (S/E/P)* node used to split up logic into smaller pieces.
 * <p>
 * The following is a brief description of each node:
 * {@link com.colasoft.opensearch.painless.node.AExpression} - The superclass for all E* (expression) and P* (postfix) nodes.
 * {@link com.colasoft.opensearch.painless.node.ANode} - The superclass for all nodes.
 * {@link com.colasoft.opensearch.painless.node.AStatement} - The superclass for all S* (statement) nodes.
 * {@link com.colasoft.opensearch.painless.node.EAssignment} - Represents an assignment with the lhs and rhs as child nodes.
 * {@link com.colasoft.opensearch.painless.node.EBinary} - Represents a binary math expression.
 * {@link com.colasoft.opensearch.painless.node.EBooleanComp} - Represents a boolean expression.
 * {@link com.colasoft.opensearch.painless.node.EBooleanConstant} - Represents a boolean constant.
 * {@link com.colasoft.opensearch.painless.node.ECallLocal} - Represents a user-defined call.
 * {@link com.colasoft.opensearch.painless.node.EComp} - Represents a comparison expression.
 * {@link com.colasoft.opensearch.painless.node.EConditional} - Represents a conditional expression.
 * {@link com.colasoft.opensearch.painless.node.EDecimal} - Represents a decimal constant.
 * {@link com.colasoft.opensearch.painless.node.EExplicit} - Represents an explicit cast.
 * {@link com.colasoft.opensearch.painless.node.EFunctionRef} - Represents a function reference (non-capturing).
 * {@link com.colasoft.opensearch.painless.node.EInstanceof} - Represents an instanceof check.
 * {@link com.colasoft.opensearch.painless.node.ELambda} - Represents a lambda function.
 * {@link com.colasoft.opensearch.painless.node.EListInit} - Represents a list initialization shortcut.
 * {@link com.colasoft.opensearch.painless.node.EMapInit} - Represents a map initialization shortcut.
 * {@link com.colasoft.opensearch.painless.node.ENewArray} - Represents an array instantiation.
 * {@link com.colasoft.opensearch.painless.node.ENewObj} - Represents and object instantiation.
 * {@link com.colasoft.opensearch.painless.node.ENull} - Represents a null constant.
 * {@link com.colasoft.opensearch.painless.node.ENumeric} - Represents a non-decimal numeric constant.
 * {@link com.colasoft.opensearch.painless.node.ERegex} - Represents a regular expression constant.
 * {@link com.colasoft.opensearch.painless.node.EString} - Represents a string constant.
 * {@link com.colasoft.opensearch.painless.node.EUnary} - Represents a unary math expression.
 * {@link com.colasoft.opensearch.painless.node.ESymbol} - Represents a variable load/store.
 * {@link com.colasoft.opensearch.painless.node.EBrace} - Represents an array load/store and defers to a child subnode.
 * {@link com.colasoft.opensearch.painless.node.ECall} - Represents a method call and defers to a child subnode.
 * {@link com.colasoft.opensearch.painless.node.EDot} - Represents a field load/store and defers to a child subnode.
 * {@link com.colasoft.opensearch.painless.node.SBlock} - Represents a set of statements as a branch of control-flow.
 * {@link com.colasoft.opensearch.painless.node.SBreak} - Represents a break statement.
 * {@link com.colasoft.opensearch.painless.node.SCatch} - Represents a catch block as part of a try-catch block.
 * {@link com.colasoft.opensearch.painless.node.SContinue} - Represents a continue statement.
 * {@link com.colasoft.opensearch.painless.node.SDeclaration} - Represents a single variable declaration.
 * {@link com.colasoft.opensearch.painless.node.SDeclBlock} - Represents a series of declarations.
 * {@link com.colasoft.opensearch.painless.node.SDo} - Represents a do-while loop.
 * {@link com.colasoft.opensearch.painless.node.SEach} - Represents a for-each loop and defers to subnodes depending on type.
 * {@link com.colasoft.opensearch.painless.node.SExpression} - Represents the top-level node for an expression as a statement.
 * {@link com.colasoft.opensearch.painless.node.SFor} - Represents a for loop.
 * {@link com.colasoft.opensearch.painless.node.SFunction} - Represents a user-defined function.
 * {@link com.colasoft.opensearch.painless.node.SIf} - Represents an if block.
 * {@link com.colasoft.opensearch.painless.node.SIfElse} - Represents an if/else block.
 * {@link com.colasoft.opensearch.painless.node.SReturn} - Represents a return statement.
 * {@link com.colasoft.opensearch.painless.node.SClass} - The root of all Painless trees.  Contains a series of statements.
 * {@link com.colasoft.opensearch.painless.node.SThrow} - Represents a throw statement.
 * {@link com.colasoft.opensearch.painless.node.STry} - Represents the try block as part of a try-catch block.
 * {@link com.colasoft.opensearch.painless.node.SWhile} - Represents a while loop.
 * <p>
 * Note that internal nodes are generated during the analysis phase by modifying the tree on-the-fly
 * for clarity of development and convenience during the writing phase.
 * <p>
 * All Painless trees must start with an SClass node at the root.  Each node has a constructor that requires
 * all of its values and children be passed in at the time of instantiation.  This means that Painless trees
 * are build bottom-up; however, this helps enforce tree structure correctness and fits naturally with a
 * standard recursive-descent parser.
 * <p>
 * Generally, statement nodes have member data that evaluate legal control-flow during the analysis phase.
 * The typical order for statement nodes is for each node to call analyze on it's children during the analysis phase
 * and write on it's children during the writing phase.
 * <p>
 * Generally, expression nodes have member data that evaluate static and def types.  The typical order for an expression node
 * during the analysis phase looks like the following:
 * <pre>{@code
 * For known expected types:
 *
 * expression.child.expected = expectedType      // set the known expected type
 *
 * expression.child.analyze(...)                 // analyze the child node to set the child's actual type
 *
 * expression.child = expression.child.cast(...) // add an implicit cast node if the child node's
 *                                               // actual type is not the expected type and set the
 *                                               // expression's child to the implicit cast node
 *
 * For unknown expected types that need promotion:
 *
 * expression.child.analyze(...)                 // analyze the child node to set the child's actual type
 *
 * Type promote = Caster.promote(...)            // get the promotion type for the child based on
 *                                               // the current operation and child's actual type
 *
 * expression.child.expected = promote           // set the expected type to the promotion type
 *
 * expression.child = expression.child.cast(...) // add an implicit cast node if the child node's
 *                                               // actual type is not the expected type and set the
 *                                               // expression's child to the implicit cast node
 * }</pre>
 * Expression nodes just call each child during the writing phase.
 * <p>
 * Postfix nodes represent postfixes in a variable/method chain including braces, calls, or fields.
 * Postfix nodes will always have a prefix node that is the prior piece of the variable/method chain.
 * Analysis of a postfix node will cause a chain of analysis calls to happen where the prefix will
 * be analyzed first and continue until the prefix is not a postfix node.  Writing out a series of
 * loads from a postfix node works in the same fashion.  Stores work somewhat differently as
 * described by later documentation.
 * <p>
 * Storebable nodes have three methods for writing -- setup, load, and store.  These methods
 * are used in conjunction with a parent node aware of the storeable node (lhs) that has a node
 * representing a value to store (rhs). The setup method is always once called before a store
 * to give storeable nodes a chance to write any prefixes they may have and any values such as
 * array indices before the store happens.  Load is called on a storeable node that must also
 * be read from, and store is called to write a value to memory.
 * <p>
 * Sub nodes are partial nodes that require a parent to work correctly.  These nodes can really
 * represent anything the parent node would like to split up into logical pieces and don't really
 * have any distinct set of rules.  The currently existing subnodes all have ANode as a super class
 * somewhere in their class hierarchy so the parent node can defer some analysis and writing to
 * the sub node.
 */
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.painless.node;
