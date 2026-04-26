package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.api.util.HaskellASTNode;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParserBaseVisitor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for HaskellASTBuilder.
 *
 */
class HaskellASTBuilderTest {

    private HaskellASTNode buildAST(String source) {
        HaskellLexer lexer = new HaskellLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HaskellParser parser = new HaskellParser(tokens);
        ParseTree tree = parser.module();
        return new HaskellASTBuilder().visit(tree);
    }

    private HaskellASTNode findFirst(ASTNode<?> node, HaskellASTNode.Kind kind) {
        if (node == null) return null;
        if (node.getKind() == kind) return (HaskellASTNode) node;
        for (ASTNode<?> child : node.getChildren()) {
            HaskellASTNode result = findFirst(child, kind);
            if (result != null) return result;
        }
        return null;
    }

    private int countKind(ASTNode<?> node, HaskellASTNode.Kind kind) {
        if (node == null) return 0;
        int count = node.getKind() == kind ? 1 : 0;
        for (ASTNode<?> child : node.getChildren()) {
            count += countKind(child, kind);
        }
        return count;
    }

    private void assertParentLinksValid(ASTNode<?> node) {
        for (ASTNode<?> child : node.getChildren()) {
            assertSame(node, child.getParent());
            assertParentLinksValid(child);
        }
    }

    @Test
    void moduleStructureBasics() {
        HaskellASTNode ast = buildAST("""
            module A (foo) where
            import Data.List
            foo x = x + 1
        """);

        assertAll(
                () -> assertEquals(HaskellASTNode.Kind.MODULE, ast.getKind()),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.IMPORT)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.EXPORT)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.FUNCTION_DECL)),
                () -> assertTrue(ast.computeWeight() >= 3),
                () -> assertTrue(ast.computeHeight() >= 2)
        );
    }

    @Test
    void moduleWeightGrowsWithDeclarations() {
        int empty = buildAST("module A where\nx = 1\n").computeWeight();
        int filled = buildAST("""
            module A where
            x = 1
            y = 2
            z = 3
        """).computeWeight();

        assertTrue(filled > empty);
    }

    @Test
    void typeAndDataDeclarationsExist() {
        HaskellASTNode ast = buildAST("""
            module A where
            type Name = String
            data Color = Red | Green
            newtype Wrapper a = Wrapper a
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.TYPE_DECL)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.DATA_DECL)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.NEWTYPE_DECL)),
                () -> assertTrue(countKind(ast, HaskellASTNode.Kind.CONSTRUCTOR) >= 2)
        );
    }

    @Test
    void classAndInstanceDeclarationsExist() {
        HaskellASTNode ast = buildAST("""
            module A where
            class C a where f :: a -> a
            instance C Int where f x = x
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.CLASS_DECL)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.INSTANCE_DECL))
        );
    }

    @Test
    void coreExpressionsExist() {
        HaskellASTNode ast = buildAST("""
            module A where
            f x =
                if x > 0 then
                    let y = x in y + 1
                else
                    case x of
                        0 -> 0
                        _ -> x
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.IF_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.LET_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.CASE_EXPR))
        );
    }

    @Test
    void lambdaAndApplication() {
        HaskellASTNode ast = buildAST("""
            module A where
            f = map (\\x -> x + 1) [1,2,3]
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.LAMBDA_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.APP_EXPR))
        );
    }

    @Test
    void listAndTupleExpressions() {
        HaskellASTNode ast = buildAST("""
            module A where
            f = ([1..10], [x | x <- [1..10]])
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.TUPLE_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.ARITHMETIC_SEQ)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.LIST_COMPREHENSION))
        );
    }

    @Test
    void infixAndUnaryExpressions() {
        HaskellASTNode ast = buildAST("""
            module A where
            f x y = -x + y
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.INFIX_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.NEGATE_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.OPERATOR))
        );
    }

    @Test
    void doNotationContainsAllStatementKinds() {
        HaskellASTNode ast = buildAST("""
            module A where
            f = do
              x <- getLine
              let y = x
              putStrLn y
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.DO_EXPR)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.DO_BIND)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.DO_LET)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.DO_STMT))
        );
    }

    @Test
    void guardsExist() {
        HaskellASTNode ast = buildAST("""
            module A where
            abs' x
              | x < 0 = -x
              | otherwise = x
        """);

        assertTrue(countKind(ast, HaskellASTNode.Kind.GUARD_EXPR) >= 2);
    }

    @Test
    void typeFeaturesExist() {
        HaskellASTNode ast = buildAST("""
            {-# LANGUAGE RankNTypes #-}
            module A where
            f :: forall a. (Show a) => a -> String
            f x = show x
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.TYPE_FUN)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.TYPE_CONSTRAINT)),
                () -> assertNotNull(findFirst(ast, HaskellASTNode.Kind.TYPE_FORALL))
        );
    }

    @Test
    void literalsAndMetadata() {
        HaskellASTNode lit = findFirst(
                buildAST("module A where\nn = 42\n"),
                HaskellASTNode.Kind.INTEGER_LITERAL
        );

        assertAll(
                () -> assertNotNull(lit),
                () -> assertEquals("42", lit.getValue()),
                () -> assertEquals(1, lit.computeWeight()),
                () -> assertEquals(1, lit.computeHeight()),
                () -> assertNotNull(lit.getMetadata("startLine")),
                () -> assertNotNull(lit.getMetadata("endChar"))
        );
    }

    @Test
    void metadataLineSpans() {
        HaskellASTNode fn = findFirst(buildAST("""
            module A where
            foo x =
              x + 1
        """), HaskellASTNode.Kind.FUNCTION_DECL);

        int start = (int) fn.getMetadata("startLine");
        int end   = (int) fn.getMetadata("endLine");

        assertTrue(end >= start);
    }

    @Test
    void typedMetadataAccess() {
        HaskellASTNode fn = findFirst(
                buildAST("module A where\nfoo x = x\n"),
                HaskellASTNode.Kind.FUNCTION_DECL
        );

        assertAll(
                () -> assertNotNull(fn.getMetadata("startLine", Integer.class)),
                () -> assertNull(fn.getMetadata("startLine", String.class))
        );
    }

    @Test
    void weightAndHeightInvariants() {
        HaskellASTNode fn = findFirst(
                buildAST("module A where\nfoo x = x + 1\n"),
                HaskellASTNode.Kind.FUNCTION_DECL
        );

        int childWeight = fn.getChildren().stream().mapToInt(ASTNode::computeWeight).sum();
        int maxHeight = fn.getChildren().stream().mapToInt(ASTNode::computeHeight).max().orElse(0);

        assertAll(
                () -> assertEquals(1 + childWeight, fn.computeWeight()),
                () -> assertEquals(1 + maxHeight, fn.computeHeight())
        );
    }

    @Test
    void nestedExpressionsProduceDepth() {
        int shallow = buildAST("module A where\nf = 1\n").computeHeight();
        int deep = buildAST("""
            module A where
            f = if True then (let x = [1..10] in map (\\y -> y + 1) x) else []
        """).computeHeight();

        assertTrue(deep > shallow);
    }

    @Test
    void parentLinksAreValid() {
        assertParentLinksValid(buildAST("""
            module A where
            foo x = case x of
                0 -> "zero"
                _ -> "other"
        """));
    }

    @Test
    void childrenListIsImmutable() {
        List<ASTNode<?>> children = buildAST("module A where\nfoo x = x + 1\n").getChildren();

        assertThrows(UnsupportedOperationException.class,
                () -> children.add(new HaskellASTNode(HaskellASTNode.Kind.UNKNOWN)));
    }

    @Test
    void descendantsIncludeAllNodes() {
        HaskellASTNode ast = buildAST("module A where\nfoo x = x + 1\n");

        assertAll(
                () -> assertTrue(ast.getDescendants().contains(ast)),
                () -> assertTrue(ast.getDescendants().size() >= ast.computeWeight())
        );
    }
}