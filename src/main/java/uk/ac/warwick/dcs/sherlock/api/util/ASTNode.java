import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// AST Node class, for building ASTs
public class ASTNode {

    private final NodeKind kind;
    private String value;
    private final List<ASTNode> children = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();

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
}

// Enum to encode the type of AST node, can be expanded with new languages added
public enum NodeKind {
    PROGRAM,

    FUNCTION_DECL,
    PARAMETER,
    BLOCK,

    VARIABLE_DECL,
    ASSIGNMENT,

    IF_STATEMENT,
    WHILE_LOOP,
    FOR_LOOP,
    RETURN,

    BINARY_EXPR,
    UNARY_EXPR,
    CALL_EXPR,

    IDENTIFIER,
    LITERAL,

    TYPE,

    UNKNOWN
    }