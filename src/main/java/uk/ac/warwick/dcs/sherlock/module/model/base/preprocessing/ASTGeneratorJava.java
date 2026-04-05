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
import uk.ac.warwick.dcs.sherlock.api.util.JavaASTNode;

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

        JavaASTNode ast = new JavaASTNode(JavaASTNode.Kind.FUNCTION_DECL);

        JavaASTBuilder astBuilder = new JavaASTBuilder();

        JavaASTNode astRoot = astBuilder.visit(tree);

        return new ASTArtifact(astRoot);
    }
}

class JavaASTBuilder extends JavaParserBaseVisitor<JavaASTNode>{
    private static final Map<String, JavaASTNode.Kind> RULE_MAP = Map.ofEntries(
            Map.entry("compilationUnit", JavaASTNode.Kind.PROGRAM),
            Map.entry("classDeclaration", JavaASTNode.Kind.CLASS_DECL),
            Map.entry("interfaceDeclaration", JavaASTNode.Kind.INTERFACE_DECL),
            Map.entry("enumDeclaration", JavaASTNode.Kind.ENUM_DECL),
            Map.entry("recordDeclaration", JavaASTNode.Kind.RECORD_DECL),
            Map.entry("methodDeclaration", JavaASTNode.Kind.FUNCTION_DECL),
            Map.entry("constructorDeclaration", JavaASTNode.Kind.CONSTRUCTOR_DECL),
            Map.entry("variableDeclarator", JavaASTNode.Kind.VARIABLE_DECL),
            Map.entry("formalParameter", JavaASTNode.Kind.PARAMETER),
            Map.entry("block", JavaASTNode.Kind.BLOCK),
            Map.entry("typeType", JavaASTNode.Kind.TYPE),
            Map.entry("ifStatement", JavaASTNode.Kind.IF_STATEMENT),
            Map.entry("forStatement", JavaASTNode.Kind.FOR_LOOP),
            Map.entry("whileStatement", JavaASTNode.Kind.WHILE_LOOP),
            Map.entry("doWhileStatement", JavaASTNode.Kind.DO_WHILE_LOOP),
            Map.entry("tryStatement", JavaASTNode.Kind.TRY_STATEMENT),
            Map.entry("catchClause", JavaASTNode.Kind.CATCH),
            Map.entry("finallyBlock", JavaASTNode.Kind.FINALLY),
            Map.entry("switchStatement", JavaASTNode.Kind.SWITCH_EXPR),
            Map.entry("switchExpression", JavaASTNode.Kind.SWITCH_EXPR),
            Map.entry("switchLabeledRule", JavaASTNode.Kind.CASE_GROUP),
            Map.entry("switchLabel", JavaASTNode.Kind.CASE_LABEL),
            Map.entry("returnStatement", JavaASTNode.Kind.RETURN),
            Map.entry("breakStatement", JavaASTNode.Kind.BREAK),
            Map.entry("continueStatement", JavaASTNode.Kind.CONTINUE),
            Map.entry("throwStatement", JavaASTNode.Kind.THROW),
            Map.entry("assertStatement", JavaASTNode.Kind.ASSERT),
            Map.entry("classBody", JavaASTNode.Kind.BLOCK)
    );

    private static final Map<Integer, JavaASTNode.Kind> TOKEN_MAP = Map.of(
            JavaLexer.IDENTIFIER, JavaASTNode.Kind.IDENTIFIER,
            JavaLexer.STRING_LITERAL, JavaASTNode.Kind.STRING_LITERAL,
            JavaLexer.CHAR_LITERAL, JavaASTNode.Kind.STRING_LITERAL,
            JavaLexer.DECIMAL_LITERAL, JavaASTNode.Kind.NUMBER_LITERAL,
            JavaLexer.FLOAT_LITERAL, JavaASTNode.Kind.NUMBER_LITERAL,
            JavaLexer.BOOL_LITERAL, JavaASTNode.Kind.BOOL_LITERAL,
            JavaLexer.NULL_LITERAL, JavaASTNode.Kind.NULL_LITERAL
    );

    @Override
    public JavaASTNode visitChildren(RuleNode node) {

        ParserRuleContext ctx = (ParserRuleContext) node;
        String ruleName = JavaParser.ruleNames[ctx.getRuleIndex()];
        JavaASTNode.Kind kind = RULE_MAP.get(ruleName);

        List<JavaASTNode> children = new ArrayList<>();

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            JavaASTNode childAST = child.accept(this);
            if (childAST != null) {
                children.add(childAST);
            }
        }

        if (kind == null) {
            if (children.isEmpty()) return null;
            if (children.size() == 1) return children.get(0);

            JavaASTNode wrapper = new JavaASTNode(JavaASTNode.Kind.UNKNOWN);
            for (JavaASTNode child : children) addChildWithParent(wrapper, child);
            attachMetadata(wrapper, ctx);
            return wrapper;
        }

        JavaASTNode nodeAST = new JavaASTNode(kind);
        for (JavaASTNode child : children) addChildWithParent(nodeAST, child);
        attachMetadata(nodeAST, ctx);

        return nodeAST;
    }

    @Override
    public JavaASTNode visitTerminal(TerminalNode node) {
        int type = node.getSymbol().getType();
        JavaASTNode.Kind kind = TOKEN_MAP.get(type);
        if (kind == null) return null;

        JavaASTNode ast = new JavaASTNode(kind, node.getText());

        // Setup metadata for terminal nodes
        Token token = node.getSymbol();
        ast.setMetadata("startLine", token.getLine());
        ast.setMetadata("startChar", token.getCharPositionInLine());
        ast.setMetadata("endLine", token.getLine());
        ast.setMetadata("endChar", token.getCharPositionInLine() + token.getText().length());


        return ast;
    }

    @Override
    public JavaASTNode visitBinaryOperatorExpression(JavaParser.BinaryOperatorExpressionContext ctx) {
        JavaASTNode expr = new JavaASTNode(JavaASTNode.Kind.BINARY_EXPR, ctx.bop.getText());
        addChildWithParent(expr, visit(ctx.expression(0)));
        addChildWithParent(expr, visit(ctx.expression(1)));
        attachMetadata(expr, ctx);
        return expr;
    }

    @Override
    public JavaASTNode visitUnaryOperatorExpression(JavaParser.UnaryOperatorExpressionContext ctx) {
        JavaASTNode expr = new JavaASTNode(JavaASTNode.Kind.UNARY_EXPR, ctx.prefix.getText());
        addChildWithParent(expr, visit(ctx.expression()));
        attachMetadata(expr, ctx);
        return expr;
    }

    @Override
    public JavaASTNode visitMethodCallExpression(JavaParser.MethodCallExpressionContext ctx) {
        JavaASTNode call = new JavaASTNode(JavaASTNode.Kind.CALL_EXPR);
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
    public JavaASTNode visitStatement(JavaParser.StatementContext ctx) {
        if (ctx == null || ctx.getChildCount() == 0) return null;

        ParseTree first = ctx.getChild(0);
        JavaASTNode node = null;

        if (first.getText().equals("if")) {  // first token is 'if'
            node = new JavaASTNode(JavaASTNode.Kind.IF_STATEMENT);
        } else if (first.getText().equals("while")) {
            node = new JavaASTNode(JavaASTNode.Kind.WHILE_LOOP);
        } else if (first.getText().equals("for")) {
            node = new JavaASTNode(JavaASTNode.Kind.FOR_LOOP);
        } else if (first.getText().equals("try")) {
            node = new JavaASTNode(JavaASTNode.Kind.TRY_STATEMENT);
        }

        // If this isn't a control flow statement, just unwrap it
        if (node == null) {
            return visitChildren(ctx);
        }

        // Attach meaningful children only once
        for (int i = 0; i < ctx.getChildCount(); i++) {

            ParseTree child = ctx.getChild(i);

            if (child instanceof ParserRuleContext) {
                JavaASTNode childNode = visit(child);
                addChildWithParent(node, childNode);
            }
        }

        attachMetadata(node, ctx);
        return node;
    }

    @Override
    public JavaASTNode visitLambdaExpression(JavaParser.LambdaExpressionContext ctx) {
        JavaASTNode lambda = new JavaASTNode(JavaASTNode.Kind.LAMBDA_EXPR);
        addChildWithParent(lambda, visit(ctx.lambdaBody()));
        attachMetadata(lambda, ctx);
        return lambda;
    }

    private void attachMetadata(JavaASTNode node, ParserRuleContext ctx) {

        Token start = ctx.getStart();
        Token stop = ctx.getStop();

        node.setMetadata("startLine", start.getLine());
        node.setMetadata("startChar", start.getCharPositionInLine());

        node.setMetadata("endLine", stop.getLine());
        node.setMetadata("endChar", stop.getCharPositionInLine());
    }

    private void addChildWithParent(JavaASTNode parent, JavaASTNode child) {
        if (child != null) {
            parent.addChild(child);
            child.setParent(parent);
        }
    }
}