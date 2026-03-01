/**
 * Demonstration of AST-based plagiarism detection
 * 
 * This class demonstrates three different matching strategies:
 * 1. GumTree: Top-down + bottom-up matching
 * 2. BFS: Breadth-first search with fingerprinting
 * 3. Hybrid: Combination of both approaches
 */
public class PlagiarismDetectionDemo {
    
    public static void main(String[] args) {
        System.out.println("AST-BASED PLAGIARISM DETECTION SYSTEM");
        System.out.println("=====================================\n");
        // Create example ASTs
        System.out.println("Creating example ASTs...\n");
        ASTNode ast1 = ExampleASTs.createExampleAST1();
        ASTNode ast2 = ExampleASTs.createExampleAST2();
        ASTNode ast3 = ExampleASTs.createExampleAST3();
        // ASTNode astBST = ExampleASTs.createAST_BinarySearchTree();
        // ASTNode astBST_p = ExampleASTs.createAST_BinarySearch_Plagiarized1();
        System.out.println("AST 1 Structure:");
        ast1.printTree("", true);
        System.out.println();
        System.out.println("AST 2 Structure (similar to AST 1):");
        ast2.printTree("", true);
        System.out.println();
        System.out.println("AST 3 Structure (different from AST 1):");
        ast3.printTree("", true);
        // System.out.println("AST BinarySearch.java Structure :");
        // astBST.printTree("", true);
        // System.out.println();
        // System.out.println("AST BinarySearch_Plagiarized1.java Structure :");
        // astBST_p.printTree("", true);
    
        // Create detector
        ASTPlagiarismDetector detector = new ASTPlagiarismDetector();
        
        // Test 1: Compare AST1 vs AST2 (should be similar)
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1: Comparing AST1 (foo) vs AST2 (bar) - Expected: HIGH similarity");
        System.out.println("=".repeat(80));
        runComparisonWithAllStrategies(detector, ast1, ast2);
        
        // Test 2: Compare AST1 vs AST3 (should be different)
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2: Comparing AST1 (foo) vs AST3 (calculate) - Expected: LOW similarity");
        System.out.println("=".repeat(80));
        runComparisonWithAllStrategies(detector, ast1, ast3);

        // //Test BinarySearchTree:
        // System.out.println("\n" + "=".repeat(80));
        // System.out.println("TEST BST: Comparing AST BinarySearch.java vs BinarySearch_Plagiarized1.java");
        // runComparisonWithAllStrategies(detector, astBST, astBST_p);
    }
    
    /**
     * Run comparison using all three strategies and compare results
     */
    private static void runComparisonWithAllStrategies(ASTPlagiarismDetector detector,
                                                       ASTNode tree1, ASTNode tree2) {
        
        // Test with GumTree strategy
        System.out.println("\n--- GUMTREE STRATEGY ---");
        SimilarityReport gumtreeReport = detector.detectPlagiarism(
            tree1, tree2, ASTPlagiarismDetector.MatchingStrategy.GUMTREE);
        System.out.println(gumtreeReport.generateReport());
        
        // Test with BFS strategy
        System.out.println("\n--- BFS STRATEGY ---");
        SimilarityReport bfsReport = detector.detectPlagiarism(
            tree1, tree2, ASTPlagiarismDetector.MatchingStrategy.BFS);
        System.out.println(bfsReport.generateReport());
        
        // Summary comparison
        System.out.println("\n--- STRATEGY COMPARISON ---");
        System.out.println(String.format("GumTree: %.2f%% (%s)", 
            gumtreeReport.getOverallSimilarity(),
            gumtreeReport.getSimilarityClassification()));
        System.out.println(String.format("BFS:     %.2f%% (%s)", 
            bfsReport.getOverallSimilarity(),
            bfsReport.getSimilarityClassification()));
    }
}