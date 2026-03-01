/**
 * Represents a mapping between two AST nodes
 * Used to track which nodes from AST1 correspond to nodes in AST2
 */
public class NodeMapping {
    private ASTNode node1;
    private ASTNode node2;
    private double similarity;
    private MappingType type;
    
    public enum MappingType {
        ANCHOR,      // Top-down phase: exact isomorphic match
        CONTAINER,   // Bottom-up phase: container with matching descendants
        BFS          // Breadth-first search match
    }
    
    public NodeMapping(ASTNode node1, ASTNode node2, MappingType type) {
        this.node1 = node1;
        this.node2 = node2;
        this.type = type;
        this.similarity = 1.0; // Default to exact match
    }
    
    public NodeMapping(ASTNode node1, ASTNode node2, double similarity, MappingType type) {
        this.node1 = node1;
        this.node2 = node2;
        this.similarity = similarity;
        this.type = type;
    }
    
    public ASTNode getNode1() { return node1; }
    public ASTNode getNode2() { return node2; }
    public double getSimilarity() { return similarity; }
    public MappingType getType() { return type; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s <-> %s (sim: %.2f)", 
            type, node1, node2, similarity);
    }
}