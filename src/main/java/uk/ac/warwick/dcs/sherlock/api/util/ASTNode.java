package uk.ac.warwick.dcs.sherlock.api.util;

import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a node in an Abstract Syntax Tree (AST)
 * Based on concepts from "Syntax tree fingerprinting" (2009) and "Fine-grained source code differencing" (2014)
 */

public class ASTNode {

    private final NodeKind kind;                                // Node type (e.g., FUNCTION_DECL, IF_STATEMENT)
    private String value;                                       // Optional value (e.g., identifier name, literal value)
    private ASTNode parent;                                   
    private ArrayList<ASTNode> children = new ArrayList<>();   
    private Map<String, Object> metadata = new HashMap<>();     // "startLine", "startChar", "endLine" & "endChar" for said node

    // Fingerprinting properties (from Chilowicz et al. 2009)
    private String strictFingerprint;    // MD5 hash of subtree (kind + value)
    private String abstractFingerprint;  // (kind + value for literals), (kind otherwise)
    private int weight;                  // Number of nodes in subtree
    private int height;                  // Height of subtree

    // lablels whose values should be abstracted away (ignored) for plagiarism detection
    private static final Set<NodeKind> ABSTRACT_VALUE_LABELS = Set.of(
        // Identifiers
        NodeKind.IDENTIFIER,
        // Declarations
        NodeKind.CLASS_DECL,
        NodeKind.INTERFACE_DECL,
        NodeKind.ENUM_DECL,
        NodeKind.RECORD_DECL,
        NodeKind.FUNCTION_DECL,
        NodeKind.CONSTRUCTOR_DECL,
        NodeKind.VARIABLE_DECL,
        NodeKind.PARAMETER,
        //Literals
            NodeKind.STRING_LITERAL,
        NodeKind.NUMBER_LITERAL,
        NodeKind.BOOL_LITERAL,
        NodeKind.NULL_LITERAL
    );



    public ASTNode(NodeKind kind) {
        this(kind, "");
    }

    public ASTNode(NodeKind kind, String value) {
        this.kind = kind;
        this.value = value != null ? value : ""; // (ternary conditional, if null set to "")
        this.children = new ArrayList<>();
        this.parent = null;
        this.weight = 1;
        this.height = 1;
        this.strictFingerprint = null;
        this.abstractFingerprint = null;
    }


    /**
     * Compute fingerprint for this subtree using MD5 hashing
     * Based on Algorithm from "Syntax tree fingerprinting" (Chilowicz et al.)
     * F(x) = H(H(V(x)) · t1 · t2 · ... · ti) where ti are hash values of direct children
     * @param useAbstraction if true, abstract values
     *                       if false, include all values (strict matching)
     */
    public String computeFingerprint(boolean useAbstraction) {
        // Check cache (preprocessTree should be called first to populate)
        if (useAbstraction && abstractFingerprint != null){
            return abstractFingerprint;
        }
        if (!useAbstraction && strictFingerprint != null) {
            return strictFingerprint;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            String nodeValue;
            if (!useAbstraction) {
                nodeValue = kind.toString() + ":" + value; // strict: (kind + value) --> STRICT ISOMORPHISM
            } else { // abstract: (kind + value for specific kinds only) --> SEMANTIC ISOMORPHISM
                if (ABSTRACT_VALUE_LABELS.contains(kind.toString())) {
                    nodeValue = kind.toString() + ":";
                } else {
                    nodeValue = kind.toString() + ":" + value; // Keep value (operators, types, etc.)
                }
            }
            md.update(nodeValue.getBytes());
            
            // Concatenate and hash children fingerprints
            StringBuilder childHashes = new StringBuilder();
            for (ASTNode child : children) {
                childHashes.append(child.computeFingerprint(useAbstraction)); // post-order bottom-up appending
            }
            if (childHashes.length() > 0) {
                md.update(childHashes.toString().getBytes());
            }
            
            // Convert to hex string
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b); // 0xff to ensure non-negative hex values
                if (hex.length() == 1) hexString.append('0'); // padding to ensure two-digit hex
                hexString.append(hex);
            }
            
            String fingerprint = hexString.toString();
            if (useAbstraction) {
                abstractFingerprint = fingerprint;
            } else {
                strictFingerprint = fingerprint;
            }
            return fingerprint;
            
        } catch (NoSuchAlgorithmException e) { // shouldn't be thrown, here in case MD5 is replaced with something unavailable
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }   

    /**
     * Compute weight (number of nodes in subtree)
     * w(x) = 1 + Σ w(xi) for all children xi
     */
    public int computeWeight() {
        weight = 1;
        for (ASTNode child : children) {
            weight += child.computeWeight();
        }
        return weight;
    }
    
    /**
     * Compute height of subtree
     * height(leaf) = 1
     * height(internal) = max(height(children)) + 1
     */
    public int computeHeight() {
        if (children.isEmpty()) { // leaf node
            height = 1;
        } else { // internal node
            int maxChildHeight = 0;
            for (ASTNode child : children) {
                maxChildHeight = Math.max(maxChildHeight, child.computeHeight());
            }
            height = maxChildHeight + 1;
        }
        return height;
    }

    public void addChild(ASTNode child){
        if(child != null){
            children.add(child);
            child.setParent(this);
        }
    }


    //////////// GETTERS & SETTERS ////////////
    
    // Get all descendant nodes (including this node) O(n^2) DFS traversal
    public Set<ASTNode> getDescendants() {
        Set<ASTNode> descendants = new HashSet<>();
        descendants.add(this);
        for (ASTNode child : children) {
            descendants.addAll(child.getDescendants());
        }
        return descendants;
    }   
    public String getFingerprint(boolean useAbstraction) {
        if (useAbstraction) {
            return abstractFingerprint;
        } else {
            return strictFingerprint;
        }
    }
    public int getWeight() { return weight; }
    public int getHeight() { return height; }

    public NodeKind getKind() { return kind; }
    public String getValue() { return value; }
    public List<ASTNode> getChildren() { return children; }
    public ASTNode getChild(int index) { return children.get(index); }
    public ASTNode getParent() { return parent; }
    public void setParent(ASTNode parent) { this.parent = parent; }
    public void setValue(String value) { this.value = value; }
    public void setMetadata(String key, Object value) { metadata.put(key, value); }
    public Object getMetadata(String key) { return metadata.get(key); }
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    public Map<String, Object> getAllMetadata() { return Collections.unmodifiableMap(metadata);}



    /**
     * Pretty print the AST structure
     */
    public void printTree(String prefix, boolean isLast) {
        System.out.println(prefix + (isLast ? "└── " : "├── ") + this.toString());
        
        for (int i = 0; i < children.size(); i++) {
            children.get(i).printTree(
                prefix + (isLast ? "    " : "│   "),
                i == children.size() - 1
            );
        }
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
