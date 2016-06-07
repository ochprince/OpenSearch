// ANTLR GENERATED CODE: DO NOT EDIT
package org.elasticsearch.painless.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PainlessParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
interface PainlessParserVisitor<T> extends ParseTreeVisitor<T> {
  /**
   * Visit a parse tree produced by {@link PainlessParser#source}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSource(PainlessParser.SourceContext ctx);
  /**
   * Visit a parse tree produced by the {@code if}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitIf(PainlessParser.IfContext ctx);
  /**
   * Visit a parse tree produced by the {@code while}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitWhile(PainlessParser.WhileContext ctx);
  /**
   * Visit a parse tree produced by the {@code do}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDo(PainlessParser.DoContext ctx);
  /**
   * Visit a parse tree produced by the {@code for}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFor(PainlessParser.ForContext ctx);
  /**
   * Visit a parse tree produced by the {@code each}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitEach(PainlessParser.EachContext ctx);
  /**
   * Visit a parse tree produced by the {@code decl}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDecl(PainlessParser.DeclContext ctx);
  /**
   * Visit a parse tree produced by the {@code continue}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitContinue(PainlessParser.ContinueContext ctx);
  /**
   * Visit a parse tree produced by the {@code break}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBreak(PainlessParser.BreakContext ctx);
  /**
   * Visit a parse tree produced by the {@code return}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitReturn(PainlessParser.ReturnContext ctx);
  /**
   * Visit a parse tree produced by the {@code try}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTry(PainlessParser.TryContext ctx);
  /**
   * Visit a parse tree produced by the {@code throw}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitThrow(PainlessParser.ThrowContext ctx);
  /**
   * Visit a parse tree produced by the {@code expr}
   * labeled alternative in {@link PainlessParser#statement}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitExpr(PainlessParser.ExprContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#trailer}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTrailer(PainlessParser.TrailerContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#block}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBlock(PainlessParser.BlockContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#empty}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitEmpty(PainlessParser.EmptyContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#initializer}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitInitializer(PainlessParser.InitializerContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#afterthought}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAfterthought(PainlessParser.AfterthoughtContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#declaration}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclaration(PainlessParser.DeclarationContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#decltype}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDecltype(PainlessParser.DecltypeContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#funcref}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFuncref(PainlessParser.FuncrefContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#declvar}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDeclvar(PainlessParser.DeclvarContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#trap}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTrap(PainlessParser.TrapContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#delimiter}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDelimiter(PainlessParser.DelimiterContext ctx);
  /**
   * Visit a parse tree produced by the {@code single}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSingle(PainlessParser.SingleContext ctx);
  /**
   * Visit a parse tree produced by the {@code comp}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitComp(PainlessParser.CompContext ctx);
  /**
   * Visit a parse tree produced by the {@code bool}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBool(PainlessParser.BoolContext ctx);
  /**
   * Visit a parse tree produced by the {@code conditional}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitConditional(PainlessParser.ConditionalContext ctx);
  /**
   * Visit a parse tree produced by the {@code assignment}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAssignment(PainlessParser.AssignmentContext ctx);
  /**
   * Visit a parse tree produced by the {@code binary}
   * labeled alternative in {@link PainlessParser#expression}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBinary(PainlessParser.BinaryContext ctx);
  /**
   * Visit a parse tree produced by the {@code pre}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPre(PainlessParser.PreContext ctx);
  /**
   * Visit a parse tree produced by the {@code post}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPost(PainlessParser.PostContext ctx);
  /**
   * Visit a parse tree produced by the {@code read}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitRead(PainlessParser.ReadContext ctx);
  /**
   * Visit a parse tree produced by the {@code numeric}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNumeric(PainlessParser.NumericContext ctx);
  /**
   * Visit a parse tree produced by the {@code true}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTrue(PainlessParser.TrueContext ctx);
  /**
   * Visit a parse tree produced by the {@code false}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFalse(PainlessParser.FalseContext ctx);
  /**
   * Visit a parse tree produced by the {@code null}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNull(PainlessParser.NullContext ctx);
  /**
   * Visit a parse tree produced by the {@code operator}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitOperator(PainlessParser.OperatorContext ctx);
  /**
   * Visit a parse tree produced by the {@code cast}
   * labeled alternative in {@link PainlessParser#unary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCast(PainlessParser.CastContext ctx);
  /**
   * Visit a parse tree produced by the {@code dynamic}
   * labeled alternative in {@link PainlessParser#chain}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitDynamic(PainlessParser.DynamicContext ctx);
  /**
   * Visit a parse tree produced by the {@code static}
   * labeled alternative in {@link PainlessParser#chain}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitStatic(PainlessParser.StaticContext ctx);
  /**
   * Visit a parse tree produced by the {@code newarray}
   * labeled alternative in {@link PainlessParser#chain}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNewarray(PainlessParser.NewarrayContext ctx);
  /**
   * Visit a parse tree produced by the {@code exprprec}
   * labeled alternative in {@link PainlessParser#primary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitExprprec(PainlessParser.ExprprecContext ctx);
  /**
   * Visit a parse tree produced by the {@code chainprec}
   * labeled alternative in {@link PainlessParser#primary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitChainprec(PainlessParser.ChainprecContext ctx);
  /**
   * Visit a parse tree produced by the {@code string}
   * labeled alternative in {@link PainlessParser#primary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitString(PainlessParser.StringContext ctx);
  /**
   * Visit a parse tree produced by the {@code variable}
   * labeled alternative in {@link PainlessParser#primary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitVariable(PainlessParser.VariableContext ctx);
  /**
   * Visit a parse tree produced by the {@code newobject}
   * labeled alternative in {@link PainlessParser#primary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNewobject(PainlessParser.NewobjectContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#secondary}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitSecondary(PainlessParser.SecondaryContext ctx);
  /**
   * Visit a parse tree produced by the {@code callinvoke}
   * labeled alternative in {@link PainlessParser#dot}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitCallinvoke(PainlessParser.CallinvokeContext ctx);
  /**
   * Visit a parse tree produced by the {@code fieldaccess}
   * labeled alternative in {@link PainlessParser#dot}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitFieldaccess(PainlessParser.FieldaccessContext ctx);
  /**
   * Visit a parse tree produced by the {@code braceaccess}
   * labeled alternative in {@link PainlessParser#brace}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBraceaccess(PainlessParser.BraceaccessContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#arguments}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitArguments(PainlessParser.ArgumentsContext ctx);
  /**
   * Visit a parse tree produced by {@link PainlessParser#argument}.
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitArgument(PainlessParser.ArgumentContext ctx);
}
