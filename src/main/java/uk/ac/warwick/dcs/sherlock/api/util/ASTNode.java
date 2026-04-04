package uk.ac.warwick.dcs.sherlock.api.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Generic base class representing a node in an Abstract Syntax Tree (AST).
 *
 * <p>Parameterised on {@code K}, the language-specific node-kind enum.
 * All structural algorithms (fingerprinting, weight, height, traversal)
 * live here; language-specific subclasses supply only their kind enum
 * and the set of kinds whose values should be abstracted away.</p>
 *
 * <p>Based on concepts from "Syntax tree fingerprinting" (Chilowicz et al., 2009)
 * and "Fine-grained source code differencing" (Falleri et al., 2014).</p>
 *
 * @param <K> a language-specific enum implementing {@link NodeKind}
 */
public abstract class ASTNode<K extends Enum<K> & ASTNode.NodeKind> {

    // -------------------------------------------------------------------------
    // Marker interface that every language-kind enum must implement
    // -------------------------------------------------------------------------

    /**
     * Marker interface for node-kind enums.
     * Implementing this allows the generic bound {@code K extends ASTNode.NodeKind}
     * to be checked at compile time.
     */
    public interface NodeKind {}

    // -------------------------------------------------------------------------
    // Core fields
    // -------------------------------------------------------------------------

    private final K kind;
    private String value;
    private ASTNode<?> parent;
    // Stored as ASTNode<?> so getChildren() returns List<ASTNode<?>> directly,
    // avoiding Java's invariant-generics capture problem at every call site.
    // Type safety is maintained by addChild(), which only accepts ASTNode<K>.
    private final List<ASTNode<?>> children = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>(); // "startLine", "startChar", "endLine", "endChar"

    // Fingerprinting (Chilowicz et al. 2009)
    private String strictFingerprint;   // MD5 of subtree with all values
    private String abstractFingerprint; // MD5 of subtree with selected values abstracted
    private int weight;                 // Number of nodes in subtree
    private int height;                 // Height of subtree
    private Set<ASTNode<?>> descendants = null;

    // -------------------------------------------------------------------------
    // Abstract contract for subclasses
    // -------------------------------------------------------------------------

    /**
     * Returns the set of node kinds whose {@code value} field should be
     * ignored (abstracted away) when computing the abstract fingerprint.
     *
     * <p>Typically includes identifiers, declarations, and literals —
     * anything a student could trivially rename without changing structure.</p>
     */
    protected abstract Set<K> abstractValueKinds();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected ASTNode(K kind) {
        this(kind, "");
    }

    protected ASTNode(K kind, String value) {
        this.kind   = kind;
        this.value  = value != null ? value : "";
        this.weight = 1;
        this.height = 1;
    }

    // -------------------------------------------------------------------------
    // Fingerprinting
    // -------------------------------------------------------------------------

    /**
     * Computes (or returns cached) fingerprint for this subtree using MD5.
     *
     * <p>Algorithm: {@code F(x) = H(H(V(x)) · t1 · t2 · … · ti)}
     * where {@code ti} are the fingerprints of direct children
     * (Chilowicz et al., 2009).</p>
     *
     * @param useAbstraction {@code true}  → abstract fingerprint (semantic isomorphism)<br>
     *                       {@code false} → strict fingerprint   (exact isomorphism)
     */
    public String computeFingerprint(boolean useAbstraction) {
        if (useAbstraction  && abstractFingerprint != null) return abstractFingerprint;
        if (!useAbstraction && strictFingerprint   != null) return strictFingerprint;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            String nodeValue;
            if (!useAbstraction) {
                nodeValue = kind.toString() + ":" + value;
            } else {
                // Abstract: drop value for kinds listed by the subclass
                nodeValue = abstractValueKinds().contains(kind)
                        ? kind.toString() + ":"
                        : kind.toString() + ":" + value;
            }
            md.update(nodeValue.getBytes());

            // Concatenate children fingerprints (post-order, bottom-up)
            StringBuilder childHashes = new StringBuilder();
            for (ASTNode<?> child : children) {
                childHashes.append(child.computeFingerprint(useAbstraction));
            }
            if (childHashes.length() > 0) {
                md.update(childHashes.toString().getBytes());
            }

            String fingerprint = bytesToHex(md.digest());

            if (useAbstraction) {
                abstractFingerprint = fingerprint;
            } else {
                strictFingerprint = fingerprint;
            }
            return fingerprint;

        } catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed by the JVM spec; this branch is a safety net only
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /** Invalidates cached fingerprints for this node (not its subtree). */
    public void invalidateFingerprints() {
        strictFingerprint   = null;
        abstractFingerprint = null;
    }

    // -------------------------------------------------------------------------
    // Weight & height
    // -------------------------------------------------------------------------

    /**
     * Computes (and caches) the weight of this subtree.
     * {@code w(x) = 1 + Σ w(xi)} for all children {@code xi}.
     */
    public int computeWeight() {
        weight = 1;
        for (ASTNode<?> child : children) {
            weight += child.computeWeight();
        }
        return weight;
    }

    /**
     * Computes (and caches) the height of this subtree.
     * {@code height(leaf) = 1},  {@code height(internal) = max(height(children)) + 1}.
     */
    public int computeHeight() {
        if (children.isEmpty()) {
            height = 1;
        } else {
            int max = 0;
            for (ASTNode<?> child : children) {
                max = Math.max(max, child.computeHeight());
            }
            height = max + 1;
        }
        return height;
    }

    // -------------------------------------------------------------------------
    // Tree mutation
    // -------------------------------------------------------------------------

    public void addChild(ASTNode<K> child) {
        if (child != null) {
            children.add(child);  // safe: widening ASTNode<K> to ASTNode<?>
            child.parent = this;
        }
    }

    // -------------------------------------------------------------------------
    // Descendant set
    // -------------------------------------------------------------------------

    /**
     * Returns all descendants of this node (including itself).
     * Result is cached after the first call (populate via {@code preprocessTree}).
     */
    public Set<ASTNode<?>> getDescendants() {
        if (descendants != null) return descendants;
        Set<ASTNode<?>> result = new HashSet<>();
        result.add(this);
        for (ASTNode<?> child : children) {
            result.addAll(child.getDescendants());
        }
        descendants = result;
        return descendants;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public K      getKind()     { return kind; }
    public String getValue()    { return value; }
    public void   setValue(String value) { this.value = value; }

    public ASTNode<?>            getParent()             { return parent; }
    public void                  setParent(ASTNode<?> p) { this.parent = p; }

    /** Returns children as {@code List<ASTNode<?>>} — usable anywhere without capture errors. */
    public List<ASTNode<?>>      getChildren()            { return Collections.unmodifiableList(children); }

    public ASTNode<?>            getChild(int index)      { return children.get(index); }

    public int getWeight() { return weight; }
    public int getHeight() { return height; }

    /** Returns the cached fingerprint without recomputing it. */
    public String getFingerprint(boolean useAbstraction) {
        return useAbstraction ? abstractFingerprint : strictFingerprint;
    }

    // Metadata
    public void   setMetadata(String key, Object value) { metadata.put(key, value); }
    public Object getMetadata(String key)               { return metadata.get(key); }
    public <T> T  getMetadata(String key, Class<T> type) {
        Object v = metadata.get(key);
        return type.isInstance(v) ? type.cast(v) : null;
    }
    public Map<String, Object> getAllMetadata() { return Collections.unmodifiableMap(metadata); }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return kind.toString()
                + (value.isEmpty() ? "" : ":" + value)
                + " [w=" + weight + ", h=" + height + "]";
    }

    /**
     * Pretty-prints the subtree rooted at this node.
     *
     * @param prefix connector string built up by the caller (start with {@code ""})
     * @param isLast whether this node is the last child of its parent
     */
    public void printTree(String prefix, boolean isLast) {
        Integer startLine = getMetadata("startLine", Integer.class);
        Integer startChar = getMetadata("startChar", Integer.class);
        Integer endLine   = getMetadata("endLine",   Integer.class);
        Integer endChar   = getMetadata("endChar",   Integer.class);

        String meta = (startLine != null)
                ? "  @ " + startLine + ":" + startChar + " -> " + endLine + ":" + endChar
                : "";

        System.out.println(prefix + (isLast ? "+-- " : "|-- ") + this + meta);

        for (int i = 0; i < children.size(); i++) {
            children.get(i).printTree(
                    prefix + (isLast ? "    " : "|   "),
                    i == children.size() - 1
            );
        }
    }

    // -------------------------------------------------------------------------
    // Internal utilities
    // -------------------------------------------------------------------------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}