import java.util.*;

/**
 * Breadth-First Search based AST Matcher
 * Based on "Source Code Plagiarism Detection Based on Abstract Syntax Tree Fingerprintings" (2022)
 * 
 * Uses fingerprinting with BFS traversal to detect similar code sections
 */
public class BFSMatcher {
    private ASTNode tree1;
    private ASTNode tree2;
    private Set<NodeMapping> mappings;
    private Set<ASTNode> matched1;
    private Set<ASTNode> matched2;
    private Map<String, List<ASTNode>> fingerprintIndex2;

    public BFSMatcher(ASTNode tree1, ASTNode tree2) {
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.mappings = new HashSet<>(); // Node-mappings found by this matcher 
        this.matched1 = new HashSet<>(); // Matched nodes from tree1
        this.matched2 = new HashSet<>(); // Matched nodes from tree2
        this.fingerprintIndex2 = new HashMap<>(); // Fingerprint index for tree2 (for fast O(1) lookup against tree1)

        // Build fingerprint index for tree2 for fast lookup
        buildFingerprintIndex();
    }
    
    /**
     * Build an index mapping fingerprints to nodes in tree2
     * This allows O(1) lookup when matching nodes from tree1
     */
    private void buildFingerprintIndex() {
        Queue<ASTNode> queue = new LinkedList<>();
        queue.add(tree2);
        
        while (!queue.isEmpty()) {
            ASTNode node = queue.poll(); // Remove and return node at front of queue
            String fp = node.getFingerprint(true); // ABSTRACT, label-focused
            
            fingerprintIndex2.putIfAbsent(fp, new ArrayList<>());
            fingerprintIndex2.get(fp).add(node); // The node itself is added so can be uniquely identified
            
            queue.addAll(node.getChildren());
        }
    }
    
    /**
     * Run BFS matching algorithm
     * Traverses tree1 in breadth-first search (BFS) order, looking for matches in tree2
     */
    public Set<NodeMapping> match() {
        Queue<ASTNode> queue = new LinkedList<>();
        queue.add(tree1);
        
        while (!queue.isEmpty()) {
            ASTNode node1 = queue.poll();
            
            // Skip if already matched
            if (matched1.contains(node1)) {
                continue;
            }
            
            // Look for exact fingerprint match
            String fp1 = node1.getFingerprint(true); // ABSTRACT
            List<ASTNode> candidates = fingerprintIndex2.get(fp1);
            
            if (candidates != null && !candidates.isEmpty()) {
                // Find the best unmatched candidate
                ASTNode bestMatch = findBestUnmatchedCandidate(candidates); // Also avoids double-counting by only checking unmatched candidates in the event of many-to-many matches
                
                if (bestMatch != null) {
                    // Create mapping and mark as matched
                    NodeMapping mapping = new NodeMapping(node1, bestMatch, 
                                                         NodeMapping.MappingType.BFS); // Default similarity of 1.0
                    mappings.add(mapping);
                    matched1.add(node1);
                    matched2.add(bestMatch);
                    
                    // Mark descendants in matched2 to prevent tree2 reuse (no need to do for matched1 since BFS search)
                    matched2.addAll(bestMatch.getDescendants());
                    // Skip adding children to queue since subtree is matched
                    continue;
                }
            }
            
            // If no match found, add children to queue to continue search
            queue.addAll(node1.getChildren());
        }
        
        return mappings;
    }
    
    /**
     * Find the BEST UNMATCHED candidate from a list
     * Prefers candidates with larger weight (more nodes)
     */
    private ASTNode findBestUnmatchedCandidate(List<ASTNode> candidates) {
        ASTNode best = null;
        int maxWeight = 0;
        
        for (ASTNode candidate : candidates) {
            if (!matched2.contains(candidate) && candidate.getWeight() > maxWeight) { 
                best = candidate;
                maxWeight = candidate.getWeight();
            }
        }
        
        return best;
    }
    
    /////////////////// GETTERS //////////////////////
    public Set<NodeMapping> getMappings() {
        return mappings;
    }
    public Set<ASTNode> getMatched1() {
        return matched1;
    }
    public Set<ASTNode> getMatched2() {
        return matched2;
    }
}