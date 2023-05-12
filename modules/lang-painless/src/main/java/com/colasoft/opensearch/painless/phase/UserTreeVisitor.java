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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.painless.phase;

import com.colasoft.opensearch.painless.node.EAssignment;
import com.colasoft.opensearch.painless.node.EBinary;
import com.colasoft.opensearch.painless.node.EBooleanComp;
import com.colasoft.opensearch.painless.node.EBooleanConstant;
import com.colasoft.opensearch.painless.node.EBrace;
import com.colasoft.opensearch.painless.node.ECall;
import com.colasoft.opensearch.painless.node.ECallLocal;
import com.colasoft.opensearch.painless.node.EComp;
import com.colasoft.opensearch.painless.node.EConditional;
import com.colasoft.opensearch.painless.node.EDecimal;
import com.colasoft.opensearch.painless.node.EDot;
import com.colasoft.opensearch.painless.node.EElvis;
import com.colasoft.opensearch.painless.node.EExplicit;
import com.colasoft.opensearch.painless.node.EFunctionRef;
import com.colasoft.opensearch.painless.node.EInstanceof;
import com.colasoft.opensearch.painless.node.ELambda;
import com.colasoft.opensearch.painless.node.EListInit;
import com.colasoft.opensearch.painless.node.EMapInit;
import com.colasoft.opensearch.painless.node.ENewArray;
import com.colasoft.opensearch.painless.node.ENewArrayFunctionRef;
import com.colasoft.opensearch.painless.node.ENewObj;
import com.colasoft.opensearch.painless.node.ENull;
import com.colasoft.opensearch.painless.node.ENumeric;
import com.colasoft.opensearch.painless.node.ERegex;
import com.colasoft.opensearch.painless.node.EString;
import com.colasoft.opensearch.painless.node.ESymbol;
import com.colasoft.opensearch.painless.node.EUnary;
import com.colasoft.opensearch.painless.node.SBlock;
import com.colasoft.opensearch.painless.node.SBreak;
import com.colasoft.opensearch.painless.node.SCatch;
import com.colasoft.opensearch.painless.node.SClass;
import com.colasoft.opensearch.painless.node.SContinue;
import com.colasoft.opensearch.painless.node.SDeclBlock;
import com.colasoft.opensearch.painless.node.SDeclaration;
import com.colasoft.opensearch.painless.node.SDo;
import com.colasoft.opensearch.painless.node.SEach;
import com.colasoft.opensearch.painless.node.SExpression;
import com.colasoft.opensearch.painless.node.SFor;
import com.colasoft.opensearch.painless.node.SFunction;
import com.colasoft.opensearch.painless.node.SIf;
import com.colasoft.opensearch.painless.node.SIfElse;
import com.colasoft.opensearch.painless.node.SReturn;
import com.colasoft.opensearch.painless.node.SThrow;
import com.colasoft.opensearch.painless.node.STry;
import com.colasoft.opensearch.painless.node.SWhile;

public interface UserTreeVisitor<Scope> {

    void visitClass(SClass userClassNode, Scope scope);

    void visitFunction(SFunction userFunctionNode, Scope scope);

    void visitBlock(SBlock userBlockNode, Scope scope);

    void visitIf(SIf userIfNode, Scope scope);

    void visitIfElse(SIfElse userIfElseNode, Scope scope);

    void visitWhile(SWhile userWhileNode, Scope scope);

    void visitDo(SDo userDoNode, Scope scope);

    void visitFor(SFor userForNode, Scope scope);

    void visitEach(SEach userEachNode, Scope scope);

    void visitDeclBlock(SDeclBlock userDeclBlockNode, Scope scope);

    void visitDeclaration(SDeclaration userDeclarationNode, Scope scope);

    void visitReturn(SReturn userReturnNode, Scope scope);

    void visitExpression(SExpression userExpressionNode, Scope scope);

    void visitTry(STry userTryNode, Scope scope);

    void visitCatch(SCatch userCatchNode, Scope scope);

    void visitThrow(SThrow userThrowNode, Scope scope);

    void visitContinue(SContinue userContinueNode, Scope scope);

    void visitBreak(SBreak userBreakNode, Scope scope);

    void visitAssignment(EAssignment userAssignmentNode, Scope scope);

    void visitUnary(EUnary userUnaryNode, Scope scope);

    void visitBinary(EBinary userBinaryNode, Scope scope);

    void visitBooleanComp(EBooleanComp userBooleanCompNode, Scope scope);

    void visitComp(EComp userCompNode, Scope scope);

    void visitExplicit(EExplicit userExplicitNode, Scope scope);

    void visitInstanceof(EInstanceof userInstanceofNode, Scope scope);

    void visitConditional(EConditional userConditionalNode, Scope scope);

    void visitElvis(EElvis userElvisNode, Scope scope);

    void visitListInit(EListInit userListInitNode, Scope scope);

    void visitMapInit(EMapInit userMapInitNode, Scope scope);

    void visitNewArray(ENewArray userNewArrayNode, Scope scope);

    void visitNewObj(ENewObj userNewObjectNode, Scope scope);

    void visitCallLocal(ECallLocal userCallLocalNode, Scope scope);

    void visitBooleanConstant(EBooleanConstant userBooleanConstantNode, Scope scope);

    void visitNumeric(ENumeric userNumericNode, Scope scope);

    void visitDecimal(EDecimal userDecimalNode, Scope scope);

    void visitString(EString userStringNode, Scope scope);

    void visitNull(ENull userNullNode, Scope scope);

    void visitRegex(ERegex userRegexNode, Scope scope);

    void visitLambda(ELambda userLambdaNode, Scope scope);

    void visitFunctionRef(EFunctionRef userFunctionRefNode, Scope scope);

    void visitNewArrayFunctionRef(ENewArrayFunctionRef userNewArrayFunctionRefNode, Scope scope);

    void visitSymbol(ESymbol userSymbolNode, Scope scope);

    void visitDot(EDot userDotNode, Scope scope);

    void visitBrace(EBrace userBraceNode, Scope scope);

    void visitCall(ECall userCallNode, Scope scope);
}
