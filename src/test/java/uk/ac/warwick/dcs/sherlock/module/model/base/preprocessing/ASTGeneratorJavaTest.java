package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;

import static org.junit.jupiter.api.Assertions.*;

class JavaASTBuilderTest {

    @Test
    void buildSimpleClassAST() {

        String code = """
                class Test {
                    int x = 5;

                    void foo() {
                        x = x + 1;
                        if (x == 5){
                            x = x + 1;
                        }
                    }
                }
                """;

        JavaLexer lexer = new JavaLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        ParseTree tree = parser.compilationUnit();

        JavaASTBuilder builder = new JavaASTBuilder();
        ASTNode ast = builder.visit(tree);

        assertNotNull(ast);
        assertEquals(ASTNode.NodeKind.PROGRAM, ast.getKind());

        ASTNode classNode = ast.getChildren().get(0);
        assertEquals(ASTNode.NodeKind.CLASS_DECL, classNode.getKind());

        assertEquals(ast, classNode.getParent());
    }

    @Test
    void loopAndIfAST() {

        String code = """
                class SimpleLoop {
                    for (int i = 1; i <= 5; i++) {
                        if (i % 2 == 0) {
                            System.out.println(i + " is even");
                        } else {
                            System.out.println(i + " is odd");
                        }
                    }
                }
                """;

        JavaLexer lexer = new JavaLexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);

        ParseTree tree = parser.compilationUnit();

        JavaASTBuilder builder = new JavaASTBuilder();
        ASTNode ast = builder.visit(tree);

        assertNotNull(ast);

        ASTNode classNode = ast.getChildren().get(0);
        /*assertEquals(ASTNode.NodeKind.PROGRAM, ast.getKind());

        ASTNode classNode = ast.getChildren().get(0);
        assertEquals(ASTNode.NodeKind.CLASS_DECL, classNode.getKind());

        assertEquals(ast, classNode.getParent());*/
    }
}