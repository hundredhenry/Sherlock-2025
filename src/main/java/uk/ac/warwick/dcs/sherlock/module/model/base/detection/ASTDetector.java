package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGenerator;

import java.util.*;

/**
 * AST-based plagiarism detector.
 * Parses source files into abstract syntax trees (ASTs) and compares
 * their structural similarity.
 */
public class ASTDetector extends PairwiseDetector<ASTDetector.ASTDetectorWorker> {

    @AdjustableParameter(
            name = "Minimum Height",
            defaultValue = 2f,
            minimumBound = 1f,
            maximumBound = 10f,
            step = 1f,
            description = "Minimum height for two AST subtrees to be considered a semantic match."
    )
    public float MIN_HEIGHT;

    @AdjustableParameter(
            name = "Minimum Dice Threshold",
            defaultValue = 0.5f,
            minimumBound = 0f,
            maximumBound = 1f,
            step = 0.05f,
            description = "Minimum similarity value between two AST subtrees to be considered a relative match (based on Dice Coefficient)"
    )
    public float MIN_DICE;


    @AdjustableParameter(
        name = "Abstract Matching?",
        defaultValue = 1, // AdjustableParameter only supports float and int, so use 1 and 0 for boolean
        minimumBound = 0,
        maximumBound = 1,
        step = 1,
        description = "When true (1), ignores differences in variable/method names and literal values. When false (0), requires exact syntactic matches."
    )
    public int ABSTRACT_MATCHING;


    public ASTDetector() {
        super("AST Detector", "Detects plagiarism by comparing the abstract syntax tree structures of source files",
                ASTDetectorWorker.class, PreProcessingStrategy.of("parseTree", ParseTreeGenerator.class));
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
                node.getDescendants(); // Precompute descendants for O(1) retrieval later 
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

    // Get nodes in post-order (children before parents)
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
    * The main processing method used in the detector
    */
    public class ASTDetectorWorker extends PairwiseDetectorWorker<ASTRawResult> {
        
        private final boolean useAbstraction;

        private Set<ASTNode> anchorMatched1;
        private Set<ASTNode> anchorMatched2;
        private Set<ASTNode> containerMatched1;
        private Set<ASTNode> containerMatched2;
        private Map<ASTNode, ASTNode> anchorMap; 

        public ASTDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
            super(parent, file1Data, file2Data);
            this.useAbstraction = ABSTRACT_MATCHING == 1;
        }

        // Recursively add mappings for isomorphic descendants of descendant anchor-mappings
        private void addAnchorDescendantMappings(Map<ASTNode, ASTNode> anchorMap, ASTNode n1, ASTNode n2){
            List<ASTNode> children1 = n1.getChildren();
            List<ASTNode> children2 = n2.getChildren();
            for (int i = 0; i < children1.size(); i++) {
                ASTNode c1 = children1.get(i);
                ASTNode c2 = children2.get(i);
                anchorMatched1.add(c1);
                anchorMatched2.add(c2);
                anchorMap.put(c1, c2);
                addAnchorDescendantMappings(anchorMap, c1, c2); // recurse
            }
        }

        // helper function for adding to metadata output mappings
        private void addToRawResult(ASTRawResult res, ASTNode n1, ASTNode n2, float similarityScore){
            if (n1.getMetadata("startLine") == null || n2.getMetadata("startLine") == null) {
                System.out.println("Invalid metadata: ref --> " + n1 + " and " + n2);
                return; 
            }   

            int refStart = n1.getMetadata("startLine", Integer.class);
            int refEnd = n1.getMetadata("endLine", Integer.class);
            int checkStart = n2.getMetadata("startLine", Integer.class);
            int checkEnd = n2.getMetadata("endLine", Integer.class);
            ASTMatch match = new ASTMatch(refStart, refEnd, checkStart, checkEnd, similarityScore, this.file1.getFile(), this.file2.getFile());
            res.addMatch(match);   
        }

        // Find candidate nodes from tree2 that could match n1 (BFS)
        private List<ASTNode> findCandidates(ASTNode n1, ASTNode root2) {
            List<ASTNode> candidates = new ArrayList<>();
            Queue<ASTNode> queue = new LinkedList<>();
            queue.add(root2); // BFS process root2 level by level

            while (!queue.isEmpty()) {
                ASTNode n2 = queue.poll();
                
                // Must have same kind (node type) and be unmatched (NOT NECESSARILY FINGERPRINT MATCHED)
                if (n2.getKind().equals(n1.getKind()) && !anchorMatched2.contains(n2)) {
                    // Must have some matching anchor-mapped descendants
                    boolean hasCommonAnchorMappedDescendants = false;
                    Set<ASTNode> desc1 = n1.getDescendants(); // Now O(1) from cache
                    Set<ASTNode> desc2 = n2.getDescendants();

                    for (ASTNode d1 : desc1) {
                        if (anchorMatched1.contains(d1)) { // check every anchor-mapped descendant of n1 for a direct mapping to a descendant of n2
                            if (anchorMap.containsKey(d1) && desc2.contains(anchorMap.get(d1))) {
                                hasCommonAnchorMappedDescendants = true;
                                break;
                            }   
                        }
                    }
                    // Must have some matching anchor-mapped descendants
                    if (hasCommonAnchorMappedDescendants) {
                        candidates.add(n2);
                    }
                }
                queue.addAll(n2.getChildren());
            }
            return candidates;
        }




        /**
         * Core execution method.
         * Parses both files into ASTs, compares their structures, and records matches (GumTree anchor/container mappings)
         */
        @Override
        public void execute() {
            // Temp data-structure initialisation
            this.anchorMatched1 = new HashSet<>();
            this.anchorMatched2 = new HashSet<>();
            this.containerMatched1 = new HashSet<>();
            this.containerMatched2 = new HashSet<>();
            this.anchorMap = new HashMap<>();

            // Gets each line as a string in the list, as returned by the specified preprocessor
            ASTArtifact artiF1 = (ASTArtifact) this.file1.getPreProcessedArtifact("ast");
            ASTArtifact artiF2 = (ASTArtifact) this.file2.getPreProcessedArtifact("ast");
            // Get the root node of each AST
            ASTNode tree1 = artiF1.ast();
            ASTNode tree2 = artiF2.ast();
            
            // Preprocess: compute fingerprints, weights, and heights
            preprocessTree(tree1);
            preprocessTree(tree2);

            // make raw result output container of "node mappings"
            ASTRawResult res = new ASTRawResult(this.file1.getFile(), this.file2.getFile(), tree1, tree2);





            /* ### PLAGIARISM DETECTION ALGORITHM BASED ON GUM-TREE DIFFING ### */
             
            // PHASE 1: Top-down greedy search for isomorphic subtrees (anchors)
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
                    
                    for (ASTNode n2 : nodes2) {
                        if (anchorMatched2.contains(n2)) continue;

                        // Anchor-mapping conditions depend on parametrised matching strictness
                        if (n1.getFingerprint(useAbstraction).equals(n2.getFingerprint(useAbstraction))) { 
                            // Add mapping for this subtree
                            addToRawResult(res, n1, n2, 1.0f);
                            anchorMatched1.add(n1);
                            anchorMatched2.add(n2);
                            anchorMap.put(n1, n2); // Build a map of anchor-mapped nodes for O(1) lookup during container matching

                            // Also add anchor-mappings for all descendants (they're isomorphic too)
                            addAnchorDescendantMappings(anchorMap, n1, n2);
                            break; // Move to the next node from tree1
                        }
                    }
                }
            }




            // PHASE 2: Bottom-up search for similar subtrees (containers) based on Dice similarity of their children
            List<ASTNode> postOrder1 = postOrder(tree1);
            for (ASTNode n1 : postOrder1) { // 
                if (anchorMatched1.contains(n1)) continue; // If already anchor-mapped, skip
                if (n1.getChildren().isEmpty()) continue; // container-type mappings cannot be leaves

                // Check if this node has any anchor-mapped descendants 
                boolean hasAnchorMatchedDescendants = false;
                for (ASTNode desc : n1.getDescendants()) {
                    if (anchorMatched1.contains(desc)) hasAnchorMatchedDescendants = true;
                } 
                if (!hasAnchorMatchedDescendants) continue;
                

                // Find the candidate matches in tree2
                List<ASTNode> candidates = findCandidates(n1, tree2);

                // Find the BEST container match for a node (for tree1 from tree2)
                Object[] bestMatchAndDice = null;
                System.out.println("\n");
                System.out.println("CANDIDATES: " + candidates + " for node " + n1 + " empty?: " + candidates.isEmpty());

                if (!candidates.isEmpty()){
                    // Select candidate with highest dice coefficient
                    ASTNode bestMatch = null;
                    float bestDice = 0;
                    for (ASTNode candidate : candidates) { // Compute Dice coefficient between n1 and candidate node from tree2
                        // dice(t1, t2) = 2 * |common_descendants| / (|desc(t1)| + |desc(t2)|)
                        Set<ASTNode> desc1 = n1.getDescendants(); // O(d1)
                        int commonCount = 0;
                        for (ASTNode d1 : desc1) { // O(d1)
                            ASTNode partner = anchorMap.get(d1); // O(1)
                            if (partner != null && d1.getFingerprint(useAbstraction).equals(partner.getFingerprint(useAbstraction))) { // Gumtree paper specifies "anchor-mapped node equality" but for our detector, we can choose if isomorphism (abstract fingerprints) suffices
                                commonCount++;
                            }
                        }
                        float dice = (2f * commonCount) / (n1.getWeight() + candidate.getWeight());
            
                        System.out.println("Candidate: " + candidate + " with commonCount: " + commonCount + " and dice: " + dice);
                        if (dice > bestDice) {
                            bestDice = dice;
                            bestMatch = candidate;
                        }
                    }
                    if (bestMatch != null){
                        bestMatchAndDice = new Object[]{bestMatch, bestDice};
                    }
                }

                if (bestMatchAndDice != null) {
                    ASTNode bestMatch = (ASTNode) bestMatchAndDice[0]; // Retrieve the best ASTNode match
                    float dice = (float) bestMatchAndDice[1]; // Retrieve Dice coefficient score
                    if (dice >= MIN_DICE) {
                        addToRawResult(res, n1, bestMatch, dice); // Add container mapping as a match
                    }
                }
            }

            // Debugging
            System.out.println("FILE REPRESENTATIONS:");
            tree1.printTree("", false);
            tree2.printTree("", false);
            System.out.println("\nDETECTED MATCHES:");
            System.out.println(res.toString());


            this.result = res;

        }
    }

}