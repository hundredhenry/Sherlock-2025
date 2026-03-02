import java.util.*;

/**
 * GumTree-inspired AST Matcher
 * Based on "Fine-grained and accurate source code differencing" (Falleri et al. 2014)
 * 
 * Implements two-phase matching:
 * 1. Top-down phase: Find isomorphic subtrees (anchor mappings)
 * 2. Bottom-up phase: Find container nodes with similar descendants
 */
public class GumTreeMatcher {
    private static final int MIN_HEIGHT = 2;
    private static final double MIN_DICE = 0.5;
    
    private ASTNode tree1;
    private ASTNode tree2;
    private Set<NodeMapping> mappings; // Node-mappings found by this matcher 
    private Set<ASTNode> anchorMatched1;
    private Set<ASTNode> anchorMatched2;
    private Set<ASTNode> containerMatched1;
    private Set<ASTNode> containerMatched2;

    
    public GumTreeMatcher(ASTNode tree1, ASTNode tree2) {
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.mappings = new HashSet<>();
        this.anchorMatched1 = new HashSet<>();
        this.anchorMatched2 = new HashSet<>();
        this.containerMatched1 = new HashSet<>();
        this.containerMatched2 = new HashSet<>();
    }


    /**
     * Run the complete GumTree matching algorithm
     */
    public Set<NodeMapping> match() {
        // Phase 1: Top-down (Anchor Mappings)
        topDownPhase();

        // Phase 2: Bottom-up (Container Mappings)
        bottomUpPhase();

        // Clean up: Delete all anchor-mappings made due to addAnchorDescentMapping()
        deleteAnchorDescendantMappings();
        
         // Return all mappings found by this matcher (both anchor and container)
        
        return mappings;
    }
    
    /**
     * Phase 1: Top-down greedy search for isomorphic subtrees
     * Algorithm 1 from GumTree paper
     */
    private void topDownPhase() {
        // Group nodes by height
        Map<Integer, List<ASTNode>> heightMap1 = groupByHeight(tree1);
        Map<Integer, List<ASTNode>> heightMap2 = groupByHeight(tree2);
        
        // Get all heights in descending order (essentially a reverse check from max height node down to leaves, using TreeSet for robustness based on how the heights are defined by ASTNode class)
        Set<Integer> allHeights = new TreeSet<>(Collections.reverseOrder()); // O(logn) search, order-aware
        allHeights.addAll(heightMap1.keySet());
        allHeights.addAll(heightMap2.keySet());
        
        for (int height : allHeights) { // Top-down to maximise subtree coverage and prevent redundant matches
            if (height < MIN_HEIGHT) break; // skip loop completely if subtree height too small to be meaningful
            
            List<ASTNode> nodes1 = heightMap1.getOrDefault(height, Collections.emptyList());
            List<ASTNode> nodes2 = heightMap2.getOrDefault(height, Collections.emptyList());
            
            // find fingerprint matches  --> optimised by comparing only nodes of the same height
            for (ASTNode n1 : nodes1) {
                if (anchorMatched1.contains(n1)) continue;
                
                for (ASTNode n2 : nodes2) {
                    if (anchorMatched2.contains(n2)) continue;
                    
                    if (n1.getFingerprint(true).equals(n2.getFingerprint(true))) { // ABSTRACT
                        // Add mapping for this subtree
                        addAnchorMapping(n1, n2);
                        break; // Move to next node from tree1
                    }
                }
            }
        }
    }
    
    /**
     * Add an anchor mapping and all its descendant mappings (2 separate code to get rid of root node redundancy check in recursion)
     */
    private void addAnchorMapping(ASTNode n1, ASTNode n2) {
        NodeMapping mapping = new NodeMapping(n1, n2, NodeMapping.MappingType.ANCHOR);
        mappings.add(mapping);
        anchorMatched1.add(n1);
        anchorMatched2.add(n2);
        
        // Also add mappings for all descendants (they're isomorphic too)
        addAnchorDescendantMappings(n1, n2);
    }
    
    /**
     * Recursively add mappings for isomorphic descendants (done DFS)
     */
    private void addAnchorDescendantMappings(ASTNode n1, ASTNode n2) {
        List<ASTNode> children1 = n1.getChildren();
        List<ASTNode> children2 = n2.getChildren();
        
        for (int i = 0; i < children1.size(); i++) { 
            ASTNode c1 = children1.get(i);
            ASTNode c2 = children2.get(i);
            
            NodeMapping childMapping = new NodeMapping(c1, c2, NodeMapping.MappingType.ANCHOR);
            mappings.add(childMapping);
            anchorMatched1.add(c1);
            anchorMatched2.add(c2);
            
            addAnchorDescendantMappings(c1, c2);
        }
    }
    
    /**
     * Group nodes by their height (computed BFS)
     */
    private Map<Integer, List<ASTNode>> groupByHeight(ASTNode root) {
        Map<Integer, List<ASTNode>> heightMap = new HashMap<>();
        Queue<ASTNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            ASTNode node = queue.poll();
            int height = node.getHeight();
            
            heightMap.putIfAbsent(height, new ArrayList<>());
            heightMap.get(height).add(node);
            
            queue.addAll(node.getChildren());
        }
        
        return heightMap;
    }

    /**
     * Phase 2: Bottom-up search for container nodes
     * Algorithm 2 from GumTree paper
     */
    private void bottomUpPhase() {
        // Process nodes in post-order (children before parents)
        List<ASTNode> postOrder1 = postOrder(tree1);
        Map<ASTNode, ASTNode> anchorMap = buildAnchorMap(); // BUILD ONCE for O(1) lookup during container matching
        
        for (ASTNode n1 : postOrder1) {
            if (anchorMatched1.contains(n1)) continue; // If already anchor-mapped, skip
            if (n1.getChildren().isEmpty()) continue; // container-type mappings cannot be leaves
            
            // Check if this node has any anchor-mapped-matched descendants
            if (!hasAnchorMatchedDescendants(n1)) continue; // If has no anchor-mapped descendants, also skip
            
            // Find candidate matches in tree2
            Object[] bestMatchAndDice = findBestContainerMatch(n1, anchorMap);
            
            if (bestMatchAndDice != null) {
                ASTNode bestMatch = (ASTNode) bestMatchAndDice[0]; // Retrieve ASTNode match
                double dice = (double) bestMatchAndDice[1]; // Retrieve Dice coefficient score
                if (dice >= MIN_DICE) { 
                    NodeMapping mapping = new NodeMapping(n1, bestMatch, dice, 
                                                         NodeMapping.MappingType.CONTAINER);
                    mappings.add(mapping);
                    containerMatched1.add(n1);
                    containerMatched2.add(bestMatch);
                }
            }
        }
    }
    
    /**
     * Find the best container match for a node (for tree1 from tree2)
     */
    private Object[] findBestContainerMatch(ASTNode n1, Map<ASTNode, ASTNode> anchorMap) {
        List<ASTNode> candidates = findCandidates(n1, tree2);
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Select candidate with highest dice coefficient
        ASTNode bestMatch = null;
        double bestDice = 0;
        
        for (ASTNode candidate : candidates) {
            double dice = computeDice(n1, candidate, anchorMap);
            if (dice > bestDice) {
                bestDice = dice;
                bestMatch = candidate;
            }
        }
        
        return bestMatch != null ? new Object[]{bestMatch, bestDice} : null; // Return both the best match and its dice score if a match was found
    }
    
    /**
     * Find candidate nodes from tree2 that could match n1 (BFS)
     */
    private List<ASTNode> findCandidates(ASTNode n1, ASTNode root2) {
        List<ASTNode> candidates = new ArrayList<>();
        Queue<ASTNode> queue = new LinkedList<>();
        queue.add(root2); // BFS process root2 level by level
        
        while (!queue.isEmpty()) {
            ASTNode n2 = queue.poll();
            
            // Must have same label (node type) and be unmatched (NOT fingerprint matched)
            if (n2.getLabel().equals(n1.getLabel()) && !anchorMatched2.contains(n2)) {
                // Must have some matching anchor-mapped descendants
                if (hasCommonAnchorMappedDescendants(n1, n2)) {
                    candidates.add(n2);
                }
            }
            
            queue.addAll(n2.getChildren());
        }
        
        return candidates;
    }
    
    /**
     * Compute Dice coefficient between two nodes
     * dice(t1, t2) = 2 * |common_descendants| / (|desc(t1)| + |desc(t2)|)
     */
    private double computeDice(ASTNode n1, ASTNode n2, Map<ASTNode, ASTNode> anchorMap) {
        Set<ASTNode> desc1 = n1.getDescendants(); // O(d1)
        
        int commonCount = 0;
        for (ASTNode d1 : desc1) { // O(d1)
            ASTNode partner = anchorMap.get(d1); // O(1)
            if (partner != null && d1.getFingerprint(true).equals(partner.getFingerprint(true))) { // GumTree paper specifies anchor-mapped node equality but isomorphism suffices (plus fingerprint is configurable)
                commonCount++;
            }
        }
        return (2.0 * commonCount) / (n1.getWeight() + n2.getWeight());
    }
    
    /**
     * Check if a node has any anchor-mapped descendants -> O(1) lookup
     */
    private boolean hasAnchorMatchedDescendants(ASTNode node) {
        for (ASTNode desc : node.getDescendants()) {
            if (anchorMatched1.contains(desc)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if two nodes have common anchor-mapped descendants O(d1 * m)
     */
    private boolean hasCommonAnchorMappedDescendants(ASTNode n1, ASTNode n2) {
        Set<ASTNode> desc1 = n1.getDescendants(); // O(n) DFS traversal
        Set<ASTNode> desc2 = n2.getDescendants();
        
        for (ASTNode d1 : desc1) {
            if (anchorMatched1.contains(d1)) { // check every anchor-mapped descendant of n1 for a direct mapping to a descendant of n2
                for (NodeMapping m : mappings) { // check for anchor-mapping 
                    if (m.getType() == NodeMapping.MappingType.ANCHOR && m.getNode1() == d1 && desc2.contains(m.getNode2())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Get nodes in post-order (children before parents)
     */
    private List<ASTNode> postOrder(ASTNode root) {
        List<ASTNode> result = new ArrayList<>();
        postOrderHelper(root, result);
        return result;
    }
    
    private void postOrderHelper(ASTNode node, List<ASTNode> result) {
        for (ASTNode child : node.getChildren()) {
            postOrderHelper(child, result);
        }
        result.add(node);
    } 

/**
 * Remove descendant mappings created by addAnchorDescendantMappings()
 * Keep only the root anchor mappings for reporting (code blocks)
 */
private void deleteAnchorDescendantMappings() {
    Set<NodeMapping> anchorRoots = new HashSet<>();
    // Step 1: Find all anchor root mappings (those whose parents aren't anchor-mapped)
    for (NodeMapping m : mappings) {
        if (m.getType() == NodeMapping.MappingType.ANCHOR) {
            if (isAnchorRoot(m.getNode1())) {
                anchorRoots.add(m);
            }
        }
    }
    // Step 2: Remove all descendants of anchor roots
    Set<NodeMapping> toRemove = new HashSet<>();
    for (NodeMapping root : anchorRoots) {
        Set<ASTNode> descendants = root.getNode1().getDescendants();
        for (NodeMapping m : mappings) {
            if (m != root && 
                m.getType() == NodeMapping.MappingType.ANCHOR &&
                descendants.contains(m.getNode1())) {
                toRemove.add(m);
            }
        }
    }
    mappings.removeAll(toRemove);
}
/**
 * Build a map of anchor-mapped nodes for O(1) lookup during container matching
 */
private Map<ASTNode, ASTNode> buildAnchorMap() {
    Map<ASTNode, ASTNode> anchorMap = new HashMap<>();
    for (NodeMapping m : mappings) {
        if (m.getType() == NodeMapping.MappingType.ANCHOR) {
            anchorMap.put(m.getNode1(), m.getNode2());
        }
    }
    return anchorMap;
}

/**
 * Check if a node is the root of an anchor-mapped subtree
 * (i.e., its parent is NOT anchor-mapped)
 */
private boolean isAnchorRoot(ASTNode node) {
    ASTNode parent = node.getParent();
    if (parent == null) { // Definitely a root
        return true;
    }
    // If parent is anchor-mapped, this node is NOT a root
    return !anchorMatched1.contains(parent);
}



    /////////////////// GETTERS //////////////////////
    public Set<NodeMapping> getMappings() {
        return mappings;
    }
    public Set<ASTNode> getAnchorMatched1() {
        return anchorMatched1;
    }   
    public Set<ASTNode> getAnchorMatched2() {
        return anchorMatched2;
    }
    public Set<ASTNode> getContainerMatched1() {
        return containerMatched1;
    }
    public Set<ASTNode> getContainerMatched2() {
        return containerMatched2;
    }
}