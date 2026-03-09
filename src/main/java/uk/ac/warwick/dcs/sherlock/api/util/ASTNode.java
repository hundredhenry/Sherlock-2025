package uk.ac.warwick.dcs.sherlock.api.util;

import java.util.*;

// AST Node class, for building ASTs
public class ASTNode {

    private final NodeKind kind;
    private String value;
    private ASTNode parent;
    private ArrayList<ASTNode> children = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public ASTNode(NodeKind kind) {
        this(kind, null);
    }

    public ASTNode(NodeKind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public NodeKind getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public ASTNode getParent() { return parent; }

    public void addChild(ASTNode child){
        if(child != null){
            children.add(child);
        }
    }

    public void setParent(ASTNode parent) {
        this.parent = parent;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public Map<String, Object> getAllMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    // Enum to encode the type of AST node, can be expanded with new languages added
    public enum NodeKind {

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
}

