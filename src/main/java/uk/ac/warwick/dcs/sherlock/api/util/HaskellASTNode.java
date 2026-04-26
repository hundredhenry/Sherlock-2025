package uk.ac.warwick.dcs.sherlock.api.util;

import java.util.Set;

public class HaskellASTNode extends ASTNode<HaskellASTNode.Kind> {

    public enum Kind implements ASTNode.NodeKind {
        // -- Top-level module structure
        MODULE,
        MODULE_CONTENT,
        IMPORT,
        EXPORT,
        BLOCK,

        // -- Declarations
        FUNCTION_DECL,
        PATTERN_BIND,
        TYPE_SIG,
        TYPE_DECL,
        DATA_DECL,
        NEWTYPE_DECL,
        CLASS_DECL,
        INSTANCE_DECL,
        DERIVING,
        FIXITY_DECL,

        // -- Data constructors
        CONSTRUCTOR,
        RECORD_FIELD,

        // -- Function equations & guards
        EQUATION,
        GUARD_EXPR,
        WHERE_BLOCK,

        // -- Patterns (LHS of equations, case alternatives, let bindings)
        PAT_WILDCARD,
        PAT_CONSTRUCTOR,
        PAT_TUPLE,
        PAT_LIST,
        PAT_AS,
        PAT_IRREFUTABLE,

        // -- Expressions
        APP_EXPR,
        INFIX_EXPR,
        NEGATE_EXPR,
        LAMBDA_EXPR,
        LET_EXPR,
        IF_EXPR,
        CASE_EXPR,
        DO_EXPR,
        TUPLE_EXPR,
        LIST_EXPR,
        ARITHMETIC_SEQ,
        LIST_COMPREHENSION,
        SECTION_LEFT,
        SECTION_RIGHT,
        TYPE_ANNOTATION,

        // -- Do-notation statements
        DO_BIND,
        DO_LET,
        DO_STMT,

        // -- List comprehension qualifiers
        COMP_BIND,
        COMP_GUARD,
        COMP_LET,

        // -- Types
        TYPE_APP,
        TYPE_FUN,
        TYPE_TUPLE,
        TYPE_LIST,
        TYPE_FORALL,
        TYPE_CONSTRAINT,
        KIND_SIG,

        // -- Terminals / leaves
        VARID,
        CONID,
        MODID,
        OPERATOR,
        INTEGER_LITERAL,
        FLOAT_LITERAL,
        CHAR_LITERAL,
        STRING_LITERAL,

        UNKNOWN
    }

    private static final Set<Kind> ABSTRACT_KINDS = Set.of(
            // Declarations whose names should be abstracted
            Kind.FUNCTION_DECL,
            Kind.TYPE_DECL,
            Kind.NEWTYPE_DECL,
            Kind.DATA_DECL,
            Kind.CLASS_DECL,
            Kind.INSTANCE_DECL,
            Kind.TYPE_SIG,
            Kind.PATTERN_BIND,

            // Name/identifier nodes
            Kind.VARID,
            Kind.CONID,
            Kind.MODID,

            // Literals (values abstracted, structure retained)
            Kind.CHAR_LITERAL,
            Kind.INTEGER_LITERAL,
            Kind.FLOAT_LITERAL,
            Kind.STRING_LITERAL
    );

    @Override
    protected Set<Kind> abstractValueKinds() {
        return ABSTRACT_KINDS;
    }

    public HaskellASTNode(Kind kind) {
        super(kind);
    }

    public HaskellASTNode(Kind kind, String value) {
        super(kind, value);
    }

}