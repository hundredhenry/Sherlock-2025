package uk.ac.warwick.dcs.sherlock.api.util;

import java.util.Set;

public class JavaASTNode extends ASTNode<JavaASTNode.Kind> {

    public enum Kind implements ASTNode.NodeKind {
        PROGRAM,

        CLASS_DECL,
        INTERFACE_DECL,
        ENUM_DECL,
        RECORD_DECL,

        FUNCTION_DECL,
        CONSTRUCTOR_DECL,

        VARIABLE_DECL,
        PARAMETER,

        BLOCK,

        IF_STATEMENT,
        FOR_LOOP,
        WHILE_LOOP,
        DO_WHILE_LOOP,

        RETURN,
        BREAK,
        CONTINUE,
        THROW,
        ASSERT,

        TRY_STATEMENT,
        CATCH,
        FINALLY,

        SWITCH_EXPR,
        CASE_GROUP,
        CASE_LABEL,

        BINARY_EXPR,
        UNARY_EXPR,
        POSTFIX_EXPR,
        TERNARY_EXPR,
        CAST_EXPR,

        CALL_EXPR,

        NEW_OBJECT,
        NEW_ARRAY,

        LAMBDA_EXPR,

        IDENTIFIER,
        STRING_LITERAL,
        NUMBER_LITERAL,
        BOOL_LITERAL,
        NULL_LITERAL,

        TYPE,
        PATTERN,

        UNKNOWN
    }

    private static final Set<Kind> ABSTRACT_KINDS = Set.of(
            Kind.IDENTIFIER,
            Kind.CLASS_DECL,
            Kind.INTERFACE_DECL,
            Kind.ENUM_DECL,
            Kind.RECORD_DECL,
            Kind.FUNCTION_DECL,
            Kind.CONSTRUCTOR_DECL,
            Kind.VARIABLE_DECL,
            Kind.PARAMETER,
            Kind.STRING_LITERAL,
            Kind.NUMBER_LITERAL,
            Kind.BOOL_LITERAL,
            Kind.NULL_LITERAL
    );

    @Override
    protected Set<Kind> abstractValueKinds() {
        return ABSTRACT_KINDS;
    }

    public JavaASTNode(Kind kind) {
        super(kind);
    }

    public JavaASTNode(Kind kind, String value) {
        super(kind, value);
    }
}