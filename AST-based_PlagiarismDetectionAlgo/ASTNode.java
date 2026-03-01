import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a node in an Abstract Syntax Tree (AST)
 * Based on concepts from "Syntax tree fingerprinting" and "Fine-grained source code differencing"
 */

public class ASTNode {
    private String label;           // Node type (e.g., "MethodDeclaration", "IfStatement")
    private String value;           // Optional value (e.g., identifier name, literal value)
    private List<ASTNode> children; // Child nodes
    private ASTNode parent;         // Parent node
    
    // Fingerprinting properties (from Chilowicz et al. 2009)
    private String strictFingerprint;    // MD5 hash of subtree (label + value)
    private String abstractFingerprint;  // (label + value for literals), (label otherwise)
    private int weight;                  // Number of nodes in subtree
    private int height;                  // Height of subtree

    
    // Labels whose values should be abstracted away (ignored) for plagiarism detection
    private static final Set<String> ABSTRACT_VALUE_LABELS = Set.of(
        // Identifiers
        "SimpleName", "Identifier", "MethodDeclaration", "ConstructorDeclaration",
        "ClassDeclaration", "InterfaceDeclaration", "EnumDeclaration", "RecordDeclaration",
        "TypeDeclaration", "VariableDeclarationFragment", "SingleVariableDeclaration",
        "FieldAccess", "SuperFieldAccess", "MethodInvocation", "SuperMethodInvocation",
        "EnumConstantDeclaration", "LabeledStatement", "TypeParameter", "QualifiedName",
        // Literals
        "NumberLiteral", "StringLiteral", "BooleanLiteral", 
        "CharacterLiteral", "NullLiteral"
    );

    public ASTNode(String label, String value) {
        this.label = label;
        this.value = value != null ? value : ""; // (ternary conditional, if null set to "")
        this.children = new ArrayList<>();
        this.parent = null;
        this.weight = 1;
        this.height = 1;
        this.strictFingerprint = null;
        this.abstractFingerprint = null;
    }
    
    public ASTNode(String label) { // overloaded constructor for e.g. control-flow nodes
        this(label, "");
    }
    
    // Add child node
    public void addChild(ASTNode child) {
        children.add(child);
        child.parent = this;
    }
    
    /**
     * Compute fingerprint for this subtree using MD5 hashing
     * Based on Algorithm from "Syntax tree fingerprinting" (Chilowicz et al.)
     * F(x) = H(H(V(x)) · t1 · t2 · ... · ti) where ti are hash values of direct children
     * @param useAbstraction if true, abstract values
     *                       if false, include all values (strict matching)
     */
    public String computeFingerprint(boolean useAbstraction) {
        // Check cache (preprocessing should be bottom-up so children are already computed)
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
                nodeValue = label + ":" + value; // strict: (label + value) --> STRICT ISOMORPHISM
            } else { // abstract: (label + value for specific labels only) --> SEMANTIC ISOMORPHISM
                if (ABSTRACT_VALUE_LABELS.contains(label)) {
                    nodeValue = label + ":";
                } else {
                    nodeValue = label + ":" + value; // Keep value (operators, types, etc.)
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


////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Check if two subtrees are isomorphic (structurally identical, so values don't have to match)
     * NOTE: Expensive operation, supplementary only (use fingerprints)
     */
    public boolean isIsomorphic(ASTNode other) {
        if (!this.label.equals(other.label)) { // match LABELS
            return false;
        }
        if (this.children.size() != other.children.size()) { // match NO. CHILDREN
            return false;
        }
        for (int i = 0; i < children.size(); i++) { // match CHILDREN IN ORDER
            if (!children.get(i).isIsomorphic(other.children.get(i))) {
                return false;
            }
        }
        
        return true;
    }
////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////// 

    ///// GETTERS & SETTERS /////

    /**
     * Get all descendant nodes (including this node) O(n^2) DFS traversal
     */
    public Set<ASTNode> getDescendants() {
        Set<ASTNode> descendants = new HashSet<>();
        descendants.add(this);
        for (ASTNode child : children) {
            descendants.addAll(child.getDescendants());
        }
        return descendants;
    }
    public String getLabel() { return label; }
    public String getValue() { return value; }
    public List<ASTNode> getChildren() { return children; }
    public ASTNode getChild(int index) {
        return children.get(index);
    }
    public ASTNode getParent() { return parent; }
    public String getFingerprint(boolean useAbstraction) { 
        if (useAbstraction) {
            return abstractFingerprint;
        } else {
            return strictFingerprint;
        }
    }
    public int getWeight() { return weight; }
    public int getHeight() { return height; }
    
    public void setParent(ASTNode parent) { this.parent = parent; }
    
    @Override // override implicit Object toString()
    public String toString() {
        return label + (value.isEmpty() ? "" : ":" + value) + 
               " [w=" + weight + ", h=" + height + "]";
    }
    
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
}