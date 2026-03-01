import java.util.*;

/**
 * Main plagiarism detection system combining multiple matching strategies
 * 
 * Implements:
 * 1. GumTree-style matching (Falleri et al. 2014)
 * 2. BFS-based matching (Suttichaya et al. 2022)
 * 
 * Based on research papers:
 * - "Fine-grained and accurate source code differencing" (Falleri et al., 2014)
 * - "Syntax tree fingerprinting" (Chilowicz et al., 2009)
 * - "Source Code Plagiarism Detection Based on AST Fingerprintings" (Suttichaya et al., 2022)
 */
public class ASTPlagiarismDetector {
    
    public enum MatchingStrategy {
        GUMTREE,     // GumTree-style anchor + container mapping
        BFS,         // Breadth-first search with fingerprinting
    }
    
    /**
     * Detect plagiarism between two ASTs using specified strategy
     */
    public SimilarityReport detectPlagiarism(ASTNode tree1, ASTNode tree2, 
                                             MatchingStrategy strategy) {
        // Preprocess: compute fingerprints, weights, and heights
        preprocessTree(tree1);
        preprocessTree(tree2);
        
        Set<NodeMapping> allMappings = new HashSet<>();
        
        switch (strategy) {
            case GUMTREE:
                allMappings = runGumTreeMatching(tree1, tree2);
                break;
            case BFS:
                allMappings = runBFSMatching(tree1, tree2);
                break;
        }

        return new SimilarityReport(tree1, tree2, allMappings);
    }
    
    /**
     * Preprocess tree: compute fingerprints, weights, and heights
     */
    private void preprocessTree(ASTNode root) {
        // Post-order traversal to compute bottom-up
        Stack<ASTNode> stack = new Stack<>();
        Set<ASTNode> visited = new HashSet<>();
        stack.push(root);
        
        while (!stack.isEmpty()) {
            ASTNode node = stack.peek();
            
            // Check if all children have been processed
            boolean childrenReady = true;
            for (ASTNode child : node.getChildren()) {
                if (!visited.contains(child)) {
                    stack.push(child);
                    childrenReady = false;
                }
            }
            
            if (childrenReady) {
                // Process this node
                node.computeWeight();
                node.computeHeight();
                node.computeFingerprint(false); // STRICT
                node.computeFingerprint(true); // ABSTRACT
                visited.add(node);
                stack.pop();
            }
        }
    }
    
    /**
     * Run GumTree matching (anchor + container mappings)
     */
    private Set<NodeMapping> runGumTreeMatching(ASTNode tree1, ASTNode tree2) {
        GumTreeMatcher matcher = new GumTreeMatcher(tree1, tree2);
        return matcher.match();
    }
    
    /**
     * Run BFS matching with fingerprinting
     */
    private Set<NodeMapping> runBFSMatching(ASTNode tree1, ASTNode tree2) {
        BFSMatcher matcher = new BFSMatcher(tree1, tree2);
        return matcher.match();
    }

}