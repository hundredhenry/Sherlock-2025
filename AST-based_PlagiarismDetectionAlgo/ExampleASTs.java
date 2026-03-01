// ExampleASTs.java
public class ExampleASTs {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // AST 1
    // public void foo(int x) { if (x > 0) { return x; } }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ASTNode createExampleAST1() {
        ASTNode root = new ASTNode("CompilationUnit");

        ASTNode classDecl = new ASTNode("ClassDeclaration", "MyClass");
        root.addChild(classDecl);

        ASTNode methodDecl = new ASTNode("MethodDeclaration", "foo");
        classDecl.addChild(methodDecl);

        methodDecl.addChild(new ASTNode("Modifier", "public"));
        methodDecl.addChild(new ASTNode("PrimitiveType", "void"));

        ASTNode param = new ASTNode("SingleVariableDeclaration");
        methodDecl.addChild(param);
        param.addChild(new ASTNode("PrimitiveType", "int"));
        param.addChild(new ASTNode("SimpleName", "x"));

        ASTNode body = new ASTNode("Block");
        methodDecl.addChild(body);

        ASTNode ifStmt = new ASTNode("IfStatement");
        body.addChild(ifStmt);

        ASTNode condition = new ASTNode("InfixExpression", ">");
        ifStmt.addChild(condition);
        condition.addChild(new ASTNode("SimpleName", "x"));
        condition.addChild(new ASTNode("NumberLiteral", "0"));

        ASTNode thenBlock = new ASTNode("Block");
        ifStmt.addChild(thenBlock);

        ASTNode returnStmt = new ASTNode("ReturnStatement");
        thenBlock.addChild(returnStmt);
        returnStmt.addChild(new ASTNode("SimpleName", "x"));

        return root;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // AST 2
    // public void bar(int y) { if (y > 0) { return y; } }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ASTNode createExampleAST2() {
        ASTNode root = new ASTNode("CompilationUnit");

        ASTNode classDecl = new ASTNode("ClassDeclaration", "YourClass");
        root.addChild(classDecl);

        ASTNode methodDecl = new ASTNode("MethodDeclaration", "bar");
        classDecl.addChild(methodDecl);

        methodDecl.addChild(new ASTNode("Modifier", "public"));
        methodDecl.addChild(new ASTNode("PrimitiveType", "void"));

        ASTNode param = new ASTNode("SingleVariableDeclaration");
        methodDecl.addChild(param);
        param.addChild(new ASTNode("PrimitiveType", "int"));
        param.addChild(new ASTNode("SimpleName", "y"));

        ASTNode body = new ASTNode("Block");
        methodDecl.addChild(body);

        ASTNode ifStmt = new ASTNode("IfStatement");
        body.addChild(ifStmt);

        ASTNode condition = new ASTNode("InfixExpression", ">");
        ifStmt.addChild(condition);
        condition.addChild(new ASTNode("SimpleName", "y"));
        condition.addChild(new ASTNode("NumberLiteral", "0"));

        ASTNode thenBlock = new ASTNode("Block");
        ifStmt.addChild(thenBlock);

        ASTNode returnStmt = new ASTNode("ReturnStatement");
        thenBlock.addChild(returnStmt);
        returnStmt.addChild(new ASTNode("SimpleName", "y"));

        return root;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // AST 3
    // private int calculate(int a, int b) { return a + b; }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static ASTNode createExampleAST3() {
        ASTNode root = new ASTNode("CompilationUnit");

        ASTNode classDecl = new ASTNode("ClassDeclaration", "Calculator");
        root.addChild(classDecl);

        ASTNode methodDecl = new ASTNode("MethodDeclaration", "calculate");
        classDecl.addChild(methodDecl);

        methodDecl.addChild(new ASTNode("Modifier", "private"));
        methodDecl.addChild(new ASTNode("PrimitiveType", "int"));

        ASTNode param1 = new ASTNode("SingleVariableDeclaration");
        methodDecl.addChild(param1);
        param1.addChild(new ASTNode("PrimitiveType", "int"));
        param1.addChild(new ASTNode("SimpleName", "a"));

        ASTNode param2 = new ASTNode("SingleVariableDeclaration");
        methodDecl.addChild(param2);
        param2.addChild(new ASTNode("PrimitiveType", "int"));
        param2.addChild(new ASTNode("SimpleName", "b"));

        ASTNode body = new ASTNode("Block");
        methodDecl.addChild(body);

        ASTNode returnStmt = new ASTNode("ReturnStatement");
        body.addChild(returnStmt);

        ASTNode addExpr = new ASTNode("InfixExpression", "+");
        returnStmt.addChild(addExpr);
        addExpr.addChild(new ASTNode("SimpleName", "a"));
        addExpr.addChild(new ASTNode("SimpleName", "b"));

        return root;
    }
}
