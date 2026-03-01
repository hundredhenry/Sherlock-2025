import java.util.*;

/**
 * Represents the results of plagiarism detection analysis
 * Includes overall similarity score and detailed mapping information
 */
public class SimilarityReport {
    private ASTNode tree1;
    private ASTNode tree2;
    private Set<NodeMapping> allMappings;
    private double overallSimilarity;
    private Map<NodeMapping.MappingType, Integer> mappingCounts;
 
    public SimilarityReport(ASTNode tree1, ASTNode tree2, Set<NodeMapping> mappings) {
        this.tree1 = tree1;
        this.tree2 = tree2;
        this.allMappings = mappings;
        this.mappingCounts = new EnumMap<>(NodeMapping.MappingType.class);
        
        computeStatistics();
    }
    
    /**
     * Compute all similarity statistics
     */
    private void computeStatistics() {
        
        // Initialize counts
        for (NodeMapping.MappingType type : NodeMapping.MappingType.values()) {
            mappingCounts.put(type, 0);
        }
        
        // Track matched nodes AND their weighted similarity
        Map<ASTNode, Double> nodeWeights1 = new HashMap<>(); 
        Map<ASTNode, Double> nodeWeights2 = new HashMap<>();

        for (NodeMapping mapping : allMappings) { 
            ASTNode n1 = mapping.getNode1();
            ASTNode n2 = mapping.getNode2();
            NodeMapping.MappingType type = mapping.getType();
            mappingCounts.put(type, mappingCounts.get(type) + 1);
            double similarity = mapping.getSimilarity(); 

            // For ANCHOR & BFS: all descendants have similarity 1.0
            if (type == NodeMapping.MappingType.ANCHOR || type == NodeMapping.MappingType.BFS) { // BFS mappings also get full similarity
                nodeWeights1.merge(n1, 1.0, Math::max); // Add root
                nodeWeights2.merge(n2, 1.0, Math::max);
                // All descendants also get 1.0 similarity
                for (ASTNode desc : n1.getDescendants()) {
                    nodeWeights1.merge(desc, 1.0, Math::max);
                }
                for (ASTNode desc : n2.getDescendants()) {
                    nodeWeights2.merge(desc, 1.0, Math::max);
                }
            } 
            // For container mappings: use dice similarity
            else if (type == NodeMapping.MappingType.CONTAINER) {
                nodeWeights1.merge(n1, similarity, Math::max);
                nodeWeights2.merge(n2, similarity, Math::max);
                // Descendants inherit the container's similarity score (if higher similarity then replace)
                for (ASTNode desc : n1.getDescendants()) {
                    nodeWeights1.merge(desc, similarity, Math::max);
                }
                for (ASTNode desc : n2.getDescendants()) {
                    nodeWeights2.merge(desc, similarity, Math::max);
                }
            }
        }
        
        // Compute WEIGHTED similarity
        int totalNodes1 = tree1.getWeight();
        int totalNodes2 = tree2.getWeight();
        // Sum of all similarity weights
        double totalSimilarity1 = nodeWeights1.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalSimilarity2 = nodeWeights2.values().stream().mapToDouble(Double::doubleValue).sum();
        // Average weighted similarity as percentage
        double avgSimilarity = (totalSimilarity1 / totalNodes1 + totalSimilarity2 / totalNodes2) / 2.0; // symmetric average
        // assymSimilarity = max(totalSimilarity1/totalNodes1, totalSimilarity2/totalNodes2) // alternatively use assymetric maximum 
        overallSimilarity = avgSimilarity * 100.0;
    }
    
    /**
     * Get mappings grouped by type and sorted by weight
     */
    public Map<NodeMapping.MappingType, List<NodeMapping>> getMappingsByType() {
        Map<NodeMapping.MappingType, List<NodeMapping>> grouped = new EnumMap<>(NodeMapping.MappingType.class);
        
        for (NodeMapping.MappingType type : NodeMapping.MappingType.values()) { // initialise
            grouped.put(type, new ArrayList<>());
        }
        for (NodeMapping mapping : allMappings) { 
            grouped.get(mapping.getType()).add(mapping);
        }
        
        // Sort each list by weight (largest first)
        for (List<NodeMapping> list : grouped.values()) {
            list.sort((m1, m2) -> Integer.compare( // Lambda comparator to define ordering rule (descending order)
                m2.getNode1().getWeight(), 
                m1.getNode1().getWeight()
            ));
        }
        
        return grouped;
    }
    
    /**
     * Generate a detailed textual report
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=" .repeat(80)).append("\n");
        sb.append("AST PLAGIARISM DETECTION REPORT\n");
        sb.append("=".repeat(80)).append("\n\n");
        
        // Overall similarity
        sb.append(String.format("OVERALL SIMILARITY: %.2f%%\n\n", overallSimilarity));
        
        // Tree statistics
        sb.append("TREE STATISTICS:\n");
        sb.append(String.format("  Tree 1: %d nodes, height %d\n", tree1.getWeight(), tree1.getHeight()));
        sb.append(String.format("  Tree 2: %d nodes, height %d\n", tree2.getWeight(), tree2.getHeight()));
        sb.append(String.format("  Total mappings found: %d\n\n", allMappings.size()));
        
        // Breakdown by mapping type
        sb.append("MAPPING BREAKDOWN:\n");
        for (NodeMapping.MappingType type : NodeMapping.MappingType.values()) {
            int count = mappingCounts.get(type);
            if (count > 0) {
                sb.append(String.format("  %s: %d mappings \n", type, count));
            }
        }
        sb.append("\n");
        
        // Detailed mapping information per type
        Map<NodeMapping.MappingType, List<NodeMapping>> grouped = getMappingsByType();
        
        for (NodeMapping.MappingType type : NodeMapping.MappingType.values()) {
            List<NodeMapping> mappings = grouped.get(type);
            if (mappings.isEmpty()) continue;
            
            sb.append(String.format("\n%s MAPPINGS:\n", type));
            sb.append("-".repeat(80)).append("\n");
            
            // Show top 10 largest mappings of each type
            int shown = 0;
            for (NodeMapping mapping : mappings) {
                if (shown++ >= 10) break;
                
                ASTNode n1 = mapping.getNode1();
                ASTNode n2 = mapping.getNode2();
                
                sb.append(String.format("  Weight: %d | Similarity: %.2f\n", 
                    n1.getWeight(), mapping.getSimilarity()));
                sb.append(String.format("    Tree1: %s\n", n1));
                sb.append(String.format("    Tree2: %s\n", n2));
                sb.append("\n");
            }
            
            if (mappings.size() > 10) {
                sb.append(String.format("  ... and %d more\n", mappings.size() - 10));
            }
        }
        
        sb.append("=".repeat(80)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Get similarity classification
     */
    public String getSimilarityClassification() {
        if (overallSimilarity >= 80) {
            return "VERY HIGH - Likely plagiarism";
        } else if (overallSimilarity >= 60) {
            return "HIGH - Suspicious similarity";
        } else if (overallSimilarity >= 40) {
            return "MODERATE - Some shared code";
        } else if (overallSimilarity >= 20) {
            return "LOW - Minor similarities";
        } else {
            return "VERY LOW - Mostly different";
        }
    }
    
    // Getters
    public double getOverallSimilarity() {
        return overallSimilarity;
    }
    
    public Set<NodeMapping> getAllMappings() {
        return allMappings;
    }
    
    public Map<NodeMapping.MappingType, Integer> getMappingCounts() {
        return mappingCounts;
    }
}