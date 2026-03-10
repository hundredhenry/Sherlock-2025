package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.*;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParserBaseListener;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParserBaseVisitor;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class ASTGeneratorJava implements IAdvancedPreProcessor<JavaLexer> {

    @Override
    public ASTArtifact process(JavaLexer lexer) {

        lexer.reset();
        JavaParser parser = new JavaParser(new CommonTokenStream(lexer));

        ParseTree tree = parser.compilationUnit();

        ASTNode ast = new ASTNode(ASTNode.NodeKind.FUNCTION_DECL);

        JavaASTBuilder astBuilder = new JavaASTBuilder();

        ASTNode astRoot = astBuilder.visit(tree);

        return new ASTArtifact(astRoot);
    }
}

class JavaASTBuilder extends JavaParserBaseVisitor<ASTNode>{
    private static final Map<String, ASTNode.NodeKind> RULE_MAP = Map.ofEntries(
            Map.entry("compilationUnit", ASTNode.NodeKind.PROGRAM),
            Map.entry("classDeclaration", ASTNode.NodeKind.CLASS_DECL),
            Map.entry("interfaceDeclaration", ASTNode.NodeKind.INTERFACE_DECL),
            Map.entry("enumDeclaration", ASTNode.NodeKind.ENUM_DECL),
            Map.entry("recordDeclaration", ASTNode.NodeKind.RECORD_DECL),
            Map.entry("methodDeclaration", ASTNode.NodeKind.FUNCTION_DECL),
            Map.entry("constructorDeclaration", ASTNode.NodeKind.CONSTRUCTOR_DECL),
            Map.entry("fieldDeclaration", ASTNode.NodeKind.VARIABLE_DECL),
            Map.entry("localVariableDeclaration", ASTNode.NodeKind.VARIABLE_DECL),
            Map.entry("formalParameter", ASTNode.NodeKind.PARAMETER),
            Map.entry("block", ASTNode.NodeKind.BLOCK),
            Map.entry("identifier", ASTNode.NodeKind.IDENTIFIER),
            Map.entry("typeType", ASTNode.NodeKind.TYPE),
            Map.entry("ifStatement", ASTNode.NodeKind.IF_STATEMENT),
            Map.entry("forStatement", ASTNode.NodeKind.FOR_LOOP),
            Map.entry("whileStatement", ASTNode.NodeKind.WHILE_LOOP),
            Map.entry("doWhileStatement", ASTNode.NodeKind.DO_WHILE_LOOP),
            Map.entry("tryStatement", ASTNode.NodeKind.TRY_STATEMENT),
            Map.entry("catchClause", ASTNode.NodeKind.CATCH),
            Map.entry("finallyBlock", ASTNode.NodeKind.FINALLY),
            Map.entry("switchStatement", ASTNode.NodeKind.SWITCH_EXPR),
            Map.entry("switchExpression", ASTNode.NodeKind.SWITCH_EXPR),
            Map.entry("switchLabeledRule", ASTNode.NodeKind.CASE_GROUP),
            Map.entry("switchLabel", ASTNode.NodeKind.CASE_LABEL),
            Map.entry("returnStatement", ASTNode.NodeKind.RETURN),
            Map.entry("breakStatement", ASTNode.NodeKind.BREAK),
            Map.entry("continueStatement", ASTNode.NodeKind.CONTINUE),
            Map.entry("throwStatement", ASTNode.NodeKind.THROW),
            Map.entry("assertStatement", ASTNode.NodeKind.ASSERT),
    );

    private static final Map<Integer, ASTNode.NodeKind> TOKEN_MAP = Map.of(
            JavaLexer.IDENTIFIER, ASTNode.NodeKind.IDENTIFIER,
            JavaLexer.STRING_LITERAL, ASTNode.NodeKind.STRING_LITERAL,
            JavaLexer.CHAR_LITERAL, ASTNode.NodeKind.STRING_LITERAL,
            JavaLexer.DECIMAL_LITERAL, ASTNode.NodeKind.NUMBER_LITERAL,
            JavaLexer.FLOAT_LITERAL, ASTNode.NodeKind.NUMBER_LITERAL,
            JavaLexer.BOOL_LITERAL, ASTNode.NodeKind.BOOL_LITERAL,
            JavaLexer.NULL_LITERAL, ASTNode.NodeKind.NULL_LITERAL
    );

    @Override
    public ASTNode visitChildren(RuleNode node) {

        ParserRuleContext ctx = (ParserRuleContext) node;
        String ruleName = JavaParser.ruleNames[ctx.getRuleIndex()];
        ASTNode.NodeKind kind = RULE_MAP.get(ruleName);

        List<ASTNode> children = new ArrayList<>();

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            ASTNode childAST = child.accept(this);
            if (childAST != null) {
                children.add(childAST);
            }
        }

        if (kind == null) {
            if (children.isEmpty()) return null;
            if (children.size() == 1) return children.get(0);

            ASTNode wrapper = new ASTNode(ASTNode.NodeKind.UNKNOWN);
            for (ASTNode child : children) addChildWithParent(wrapper, child);
            return wrapper;
        }

        ASTNode nodeAST = new ASTNode(kind);
        for (ASTNode child : children) addChildWithParent(nodeAST, child);
        attachMetadata(nodeAST, ctx);

        return nodeAST;
    }

    @Override
    public ASTNode visitTerminal(TerminalNode node) {
        int type = node.getSymbol().getType();
        ASTNode.NodeKind kind = TOKEN_MAP.get(type);
        if (kind == null) return null;

        ASTNode ast = new ASTNode(kind, node.getText());
        return ast;
    }

    @Override
    public ASTNode visitBinaryOperatorExpression(JavaParser.BinaryOperatorExpressionContext ctx) {
        ASTNode expr = new ASTNode(ASTNode.NodeKind.BINARY_EXPR, ctx.bop.getText());
        addChildWithParent(expr, visit(ctx.expression(0)));
        addChildWithParent(expr, visit(ctx.expression(1)));
        attachMetadata(expr, ctx);
        return expr;
    }

    @Override
    public ASTNode visitUnaryOperatorExpression(JavaParser.UnaryOperatorExpressionContext ctx) {
        ASTNode expr = new ASTNode(ASTNode.NodeKind.UNARY_EXPR, ctx.prefix.getText());
        addChildWithParent(expr, visit(ctx.expression()));
        attachMetadata(expr, ctx);
        return expr;
    }

    @Override
    public ASTNode visitMethodCallExpression(JavaParser.MethodCallExpressionContext ctx) {
        ASTNode call = new ASTNode(ASTNode.NodeKind.CALL_EXPR);
        call.setValue(ctx.methodCall().getText());

        JavaParser.ExpressionListContext exprList = ctx.methodCall().arguments().expressionList();
        if (exprList != null) {
            for (JavaParser.ExpressionContext arg : exprList.expression()) {
                addChildWithParent(call, visit(arg));
            }
        }

        attachMetadata(call, ctx);
        return call;
    }

    @Override
    public ASTNode visitLambdaExpression(JavaParser.LambdaExpressionContext ctx) {
        ASTNode lambda = new ASTNode(ASTNode.NodeKind.LAMBDA_EXPR);
        addChildWithParent(lambda, visit(ctx.lambdaBody()));
        attachMetadata(lambda, ctx);
        return lambda;
    }

    private void attachMetadata(ASTNode node, ParserRuleContext ctx) {

        Token start = ctx.getStart();
        Token stop = ctx.getStop();

        node.setMetadata("startLine", start.getLine());
        node.setMetadata("startChar", start.getCharPositionInLine());

        node.setMetadata("endLine", stop.getLine());
        node.setMetadata("endChar", stop.getCharPositionInLine());
    }

    private void addChildWithParent(ASTNode parent, ASTNode child) {
        if (child != null) {
            parent.addChild(child);
            child.setParent(parent);
        }
    }
}