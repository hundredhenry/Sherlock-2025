package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.api.util.JavaASTNode;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for JavaASTBuilder.
 */
class JavaASTBuilderTest {

    private JavaASTNode buildAST(String source) {
        JavaLexer lexer = new JavaLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        ParseTree tree = parser.compilationUnit();
        return new JavaASTBuilder().visit(tree);
    }

    private JavaASTNode findFirst(ASTNode<?> node, JavaASTNode.Kind kind) {
        if (node == null) return null;
        if (node.getKind() == kind) return (JavaASTNode) node;
        for (ASTNode<?> child : node.getChildren()) {
            JavaASTNode result = findFirst(child, kind);
            if (result != null) return result;
        }
        return null;
    }

    private int countKind(ASTNode<?> node, JavaASTNode.Kind kind) {
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
    void programStructureBasics() {
        JavaASTNode ast = buildAST("class A { void m() {} }");

        assertAll(
                () -> assertEquals(JavaASTNode.Kind.PROGRAM, ast.getKind()),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.CLASS_DECL)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.FUNCTION_DECL)),
                () -> assertTrue(ast.computeWeight() >= 3),
                () -> assertTrue(ast.computeHeight() >= 2)
        );
    }

    @Test
    void classWeightGrowsWithMembers() {
        int empty = buildAST("class A {}").computeWeight();
        int filled = buildAST("class A { int x; void m() {} }").computeWeight();
        assertTrue(filled > empty);
    }


    @Test
    void controlFlowNodesExist() {
        JavaASTNode ast = buildAST("""
            class A {
                void m() {
                    if (true) {}
                    for(;;) {}
                    while(true) {}
                    do {} while(true);
                    try {} catch(Exception e) {} finally {}
                }
            }
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.IF_STATEMENT)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.FOR_LOOP)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.WHILE_LOOP)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.DO_WHILE_LOOP)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.TRY_STATEMENT)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.CATCH)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.FINALLY))
        );
    }

    @Test
    void nestedControlFlowProducesDepth() {
        JavaASTNode ast = buildAST("""
            class A {
                void m() {
                    for(;;) {
                        while(true) {
                            if(true) { break; }
                        }
                    }
                }
            }
        """);

        assertTrue(ast.computeHeight() >= 7);
    }


    @Test
    void switchStructureExists() {
        JavaASTNode ast = buildAST("""
            class A {
                void m(int x) {
                    switch (x) {
                        case 1: break;
                        default: break;
                    }
                }
            }
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.SWITCH_EXPR)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.CASE_LABEL)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.BREAK))
        );
    }


    @Test
    void leafStatementsExist() {
        JavaASTNode ast = buildAST("""
            class A {
                void m() {
                    for(;;) {
                        break;
                        continue;
                    }
                    return;
                    throw new RuntimeException();
                }
            }
        """);

        assertAll(
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.BREAK)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.CONTINUE)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.RETURN)),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.THROW))
        );
    }


    @Test
    void binaryAndUnaryExpressions() {
        JavaASTNode ast = buildAST("""
            class A {
                void m() {
                    int x = a + b;
                    int y = -x;
                }
            }
        """);

        JavaASTNode bin = findFirst(ast, JavaASTNode.Kind.BINARY_EXPR);
        JavaASTNode unary = findFirst(ast, JavaASTNode.Kind.UNARY_EXPR);

        assertAll(
                () -> assertNotNull(bin),
                () -> assertEquals("+", bin.getValue()),
                () -> assertEquals(2, bin.getChildren().size()),
                () -> assertNotNull(unary),
                () -> assertEquals(1, unary.getChildren().size())
        );
    }

    @Test
    void methodCallAndLambda() {
        JavaASTNode ast = buildAST("""
            class A {
                void m() {
                    foo(a, b, c);
                    Runnable r = () -> {};
                }
            }
        """);

        JavaASTNode call = findFirst(ast, JavaASTNode.Kind.CALL_EXPR);

        assertAll(
                () -> assertNotNull(call),
                () -> assertEquals(3, call.getChildren().size()),
                () -> assertNotNull(findFirst(ast, JavaASTNode.Kind.LAMBDA_EXPR))
        );
    }


    @Test
    void literalsAndMetadata() {
        JavaASTNode lit = findFirst(buildAST("""
            class A { int x = 42; String s = "hi"; boolean b = true; }
        """), JavaASTNode.Kind.NUMBER_LITERAL);

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
        JavaASTNode method = findFirst(buildAST("""
            class A {
                void m() {
                    int x = 1;
                }
            }
        """), JavaASTNode.Kind.FUNCTION_DECL);

        int start = (int) method.getMetadata("startLine");
        int end   = (int) method.getMetadata("endLine");

        assertTrue(end > start);
    }

    @Test
    void typedMetadataAccess() {
        JavaASTNode cls = findFirst(buildAST("class A {}"), JavaASTNode.Kind.CLASS_DECL);

        assertAll(
                () -> assertNotNull(cls.getMetadata("startLine", Integer.class)),
                () -> assertNull(cls.getMetadata("startLine", String.class))
        );
    }

    @Test
    void weightAndHeightInvariants() {
        JavaASTNode method = findFirst(buildAST("""
            class A { void m(int x) { return; } }
        """), JavaASTNode.Kind.FUNCTION_DECL);

        int childWeight = method.getChildren().stream().mapToInt(ASTNode::computeWeight).sum();
        int maxHeight = method.getChildren().stream().mapToInt(ASTNode::computeHeight).max().orElse(0);

        assertAll(
                () -> assertEquals(1 + childWeight, method.computeWeight()),
                () -> assertEquals(1 + maxHeight, method.computeHeight())
        );
    }

    @Test
    void parentLinksAreValid() {
        assertParentLinksValid(buildAST("""
            class A {
                void m() {
                    int x = 1;
                    if (x > 0) { return; }
                }
            }
        """));
    }

    @Test
    void childrenListIsImmutable() {
        List<ASTNode<?>> children = buildAST("class A {}").getChildren();

        assertThrows(UnsupportedOperationException.class,
                () -> children.add(new JavaASTNode(JavaASTNode.Kind.UNKNOWN)));
    }
}