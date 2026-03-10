package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.TrimWhitespaceOnly;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;

/**
 * AST-based plagiarism detector.
 * Parses source files into abstract syntax trees (ASTs) and compares
 * their structural similarity.
 */
public class ASTDetector extends PairwiseDetector<ASTDetector.ASTDetectorWorker> {

    @AdjustableParameter(
            name = "Minimum Height",
            defaultValue = 2,
            minimumBound = 1,
            maximumBound = 10,
            step = 1,
            description = "Minimum height for two AST subtrees to be considered a semantic match."
    )
    public float MIN_HEIGHT;

    @AdjustableParameter(
            name = "Minimum Dice Threshold",
            defaultValue = 5,
            minimumBound = 1,
            maximumBound = 50,
            step = 1,
            description = "Minimum similarity value between two AST subtrees to be considered a relative match (based on Dice Coefficient)"
    )
    public int MIN_DICE;

    private Set<ASTNode> anchorMatched1;
    private Set<ASTNode> anchorMatched2;
    private Set<ASTNode> containerMatched1;
    private Set<ASTNode> containerMatched2;

    @AdjustableParameter(
        name = "Abstract Matching?",
        defaultValue = true,
        description = "When true, ignores differences in variable/method names and literal values. When false, requires exact matches."
    )
    public boolean ABSTRACT_MATCHING;



    public ASTDetector() {
        super("AST Detector",
                "Detects plagiarism by comparing abstract syntax tree structures of source files",
                ASTDetectorWorker.class,
                PreProcessingStrategy.of("no_whitespace", TrimWhitespaceOnly.class));
    }




    // Preprocess tree: compute fingerprints, weights, and heights
    private void preprocessTree(ASTNode root){
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

    // Group nodes in a subtree by height (computed BFS)
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

    // helper function for adding to metadata output mappings
    private void addToRawResult(ASTRawResult<ASTMatch> res, ASTNode n1, ASTNode n2, float similarityScore){
        int refStart = n1.getMetaData("startLine");
        int refEnd = n1.getMetaData("endLine");
        int checkStart = n2.getMetaData("startLine");
        int checkEnd = n2.getMetaData("endLine");  
        ASTMatch match = new ASTMatch(refStart, refEnd, checkStart, checkEnd, similarityScore, this.file1.getFile(), this.file2.getFile());
        res.addMatch(match);   
    }

    // Recursively add mappings for isomorphic descendants of descendant anchor-mappings
    private void addAnchorDescendantMappings(ASTRawResult<ASTMatch> res, ASTNode n1, ASTNode n2){
        List<ASTNode> children1 = n1.getChildren();
        List<ASTNode> children2 = n2.getChildren();
        for (int i = 0; i < children1.size(); i++) {
            ASTNode c1 = children.get(i);
            ASTNode c2 = children2.get(i);
            addToRawResult(res, n1, n2, 1.0);
            anchorMatched1.add(c1);
            anchorMatched2.add(c2);
            addAnchorDescendantMappings(c1, c2); // recurse
        }
    }





    /**
    * The main processing method used in the detector
    */
    public class ASTDetectorWorker extends PairwiseDetectorWorker<ASTRawResult> {

        public ASTDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
            super(parent, file1Data, file2Data);
        }

        /**
         * Core execution method.
         * Parses both files into ASTs, compares their structures, and records matches (GumTree anchor/container mappings)
         */
        @Override
        public void execute() {
            // Gets each line as a string in the list, as returned by the specified preprocessor
            ASTArtifact artiF1 = (ASTArtifact) this.file1.getPreProcessedArtifact("ast");
            ASTArtifact artiF2 = (ASTArtifact) this.file2.getPreProcessedArtifact("ast");
            // Get the root node of each AST
            ASTNode tree1 = artiF1.ast();
            ASTNode tree2 = artiF2.ast();
            // make raw result output container of "node mappings"
            ASTRawResult<ASTMatch> res = new ASTRawResult<>(this.file1.getFile(), this.file2.getFile());
            // Preprocess: compute fingerprints, weights, and heights
            preprocessTree(tree1);
            preprocessTree(tree2);


            /* ### PLAGIARISM DETECTION ALGORITHM BASED ON GUM-TREE DIFFING ### */
             
            

            // PHASE 1: Top-down greedy search for isomorpihc subtrees (anchors)
            Map<Integer, List<ASTNode>> heightMap1 = groupByHeight(tree1);
            Map<Integer, List<ASTNode>> heightMap2 = groupByHeight(tree2);

            // Get all heights in descending order 
            Set<Integer> allHeights = new TreeSet<>(Collections.reverseOrder()); // O(logn) search, order-aware
            allHeights.addAll(heightMap1.keySet());
            allHeights.addAll(heightMap2.keySet());

            for (int height : allHeights) { // Top-down traversal to maximise subtree coverage and prevent redundant matches
                if (height < MIN_HEIGHT) break; // skip if subtree height too small to be meaningful

                List<ASTNode> nodes1 = heightMap1.getOrDefault(height, Collections.emptyList());
                List<ASTNode> nodes2 = heightMap2.getOrDefault(height, Collections.emptyList());

                // find fingerprint matches --> optimised by comparing only nodes with same height
                for (ASTNode n1 : nodes1) {
                    if (anchorMatched1.contains(n1)) continue;
                    
                    for (ASTNode n : nodes2) {
                        if (anchorMatched2.contains(n2)) continue;

                        // Anchor-mapping conditions depend on parametrised matching strictness
                        if (n1.getFingerprint(ABSTRACT_MATCHING).equals(n2.getFingerprint(ABSTRACT_MATCHING))) { 
                            // Add mapping for this subtree
                            addToRawResult(res, n1, n2, 1.0);
                            anchorMatched1.add(n1);
                            anchorMatched2.add(n2);
                            // Also add mappings for all descendants (they're isomorphic too)
                            addAnchorDescendantMappings(n1, n2);
                            break; // Move to the next node from tree1
                        }
                    }
                }
            }

            // PHASE 2: Bottom-up search for similar subtrees (containers) based on Dice similarity of their children
            //List<ASTNode> postOrder1 = postOrder(tree1);
            
            this.result = res;

        }

    }

}