package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGenerator;

import java.util.*;

/**
 * AST-based plagiarism detector.
 * Parses source files into abstract syntax trees (ASTs) and compares
 * their structural similarity.
 */
public class ASTDetector extends PairwiseDetector<ASTDetector.ASTDetectorWorker> {

    @AdjustableParameter(
            name = "Minimum Height",
            defaultValue = 5f,
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
        name = "Abstract Matching",
        defaultValue = 1, // AdjustableParameter only supports float and int, so use 1 and 0 for boolean
        minimumBound = 0,
        maximumBound = 1,
        step = 1,
        description = "When true (1), ignores differences in variable/method names and literal values. When false (0), requires exact syntactic matches."
    )
    public int ABSTRACT_MATCHING;

    public ASTDetector() {
        super("AST Detector", "Detects plagiarism by comparing the abstract syntax tree structures of source files",
                ASTDetectorWorker.class, PreProcessingStrategy.of("ast", ASTGenerator.class));
    }

    // Preprocess tree: compute fingerprints, weights, and heights
    private void preprocessTree(ASTNode<?> root) {
        // Post-order traversal to compute bottom-up
        Stack<ASTNode<?>> stack = new Stack<>();
        Set<ASTNode<?>> visited = new HashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            ASTNode<?> node = stack.peek();
            // Check if all children have been processed
            boolean childrenReady = true;
            for (ASTNode<?> child : node.getChildren()) {
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
    private Map<Integer, List<ASTNode<?>>> groupByHeight(ASTNode<?> root) {
        Map<Integer, List<ASTNode<?>>> heightMap = new HashMap<>();
        Queue<ASTNode<?>> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            ASTNode<?> node = queue.poll();
            int height = node.getHeight();
            heightMap.putIfAbsent(height, new ArrayList<>());
            heightMap.get(height).add(node);
            queue.addAll(node.getChildren());
        }
        return heightMap;
    }

    // Get nodes in post-order (children before parents)
    private List<ASTNode<?>> postOrder(ASTNode<?> root) {
        List<ASTNode<?>> result = new ArrayList<>();
        postOrderHelper(root, result);
        return result;
    }

    private void postOrderHelper(ASTNode<?> node, List<ASTNode<?>> result) {
        for (ASTNode<?> child : node.getChildren()) {
            postOrderHelper(child, result);
        }
        result.add(node);
    }

    /**
     * The main processing method used in the detector
     */
    public class ASTDetectorWorker extends PairwiseDetectorWorker<ASTRawResult> {

        private final boolean useAbstraction;

        private Set<ASTNode<?>> anchorMatched1;
        private Set<ASTNode<?>> anchorMatched2;
        private Set<ASTNode<?>> containerMatched1;
        private Set<ASTNode<?>> containerMatched2;
        private Map<ASTNode<?>, ASTNode<?>> anchorMap;

        public ASTDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
            super(parent, file1Data, file2Data);
            this.useAbstraction = ABSTRACT_MATCHING == 1;
        }

        // Recursively add mappings for isomorphic descendants of descendant anchor-mappings
        private void addAnchorDescendantMappings(Map<ASTNode<?>, ASTNode<?>> anchorMap, ASTNode<?> n1, ASTNode<?> n2) {
            List<ASTNode<?>> children1 = n1.getChildren();
            List<ASTNode<?>> children2 = n2.getChildren();
            for (int i = 0; i < children1.size(); i++) {
                ASTNode<?> c1 = children1.get(i);
                ASTNode<?> c2 = children2.get(i);
                anchorMatched1.add(c1);
                anchorMatched2.add(c2);
                anchorMap.put(c1, c2);
                addAnchorDescendantMappings(anchorMap, c1, c2); // recurse
            }
        }

        // helper function for adding to metadata output mappings
        private void addToRawResult(ASTRawResult res, ASTNode<?> n1, int subtreeWeight1,ASTNode<?> n2, int subtreeWeight2, float similarityScore) {
            if (n1.getMetadata("startLine") == null || n2.getMetadata("startLine") == null) {
                // Missing line metadata, skip this match
                return;
            }

            int refStart = n1.getMetadata("startLine", Integer.class);
            int refEnd = n1.getMetadata("endLine", Integer.class);
            int checkStart = n2.getMetadata("startLine", Integer.class);
            int checkEnd = n2.getMetadata("endLine", Integer.class);
            ASTMatch match = new ASTMatch(refStart, refEnd, checkStart, checkEnd, similarityScore, this.file1.getFile(), subtreeWeight1, this.file2.getFile(), subtreeWeight2);
            res.addMatch(match);
        }

        // Find candidate nodes from tree2 that could match n1 (BFS)
        private List<ASTNode<?>> findCandidates(ASTNode<?> n1, ASTNode<?> root2) {
            List<ASTNode<?>> candidates = new ArrayList<>();
            Queue<ASTNode<?>> queue = new LinkedList<>();
            queue.add(root2); // BFS process root2 level by level

            while (!queue.isEmpty()) {
                ASTNode<?> n2 = queue.poll();
                
                // Must have same kind (node type) and be unmatched (NOT NECESSARILY FINGERPRINT MATCHED)
                if (n2.getKind().equals(n1.getKind()) && !anchorMatched2.contains(n2)) {
                    // Must have some matching anchor-mapped descendants
                    boolean hasCommonAnchorMappedDescendants = false;
                    Set<ASTNode<?>> desc1 = n1.getDescendants(); // Now O(1) from cache
                    Set<ASTNode<?>> desc2 = n2.getDescendants();

                    for (ASTNode<?> d1 : desc1) {
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
            ASTNode<?> tree1 = artiF1.ast();
            ASTNode<?> tree2 = artiF2.ast();
            
            // Preprocess: compute fingerprints, weights, and heights
            preprocessTree(tree1);
            preprocessTree(tree2);

            // Make raw result output container of "node mappings"
            ASTRawResult res = new ASTRawResult(this.file1.getFile(), this.file2.getFile(), tree1, tree2);

            // PHASE 1: Top-down greedy search for isomorphic subtrees (anchors)
            Map<Integer, List<ASTNode<?>>> heightMap1 = groupByHeight(tree1);
            Map<Integer, List<ASTNode<?>>> heightMap2 = groupByHeight(tree2);

            // Get all heights in descending order
            Set<Integer> allHeights = new TreeSet<>(Collections.reverseOrder()); // O(logn) search, order-aware
            allHeights.addAll(heightMap1.keySet());
            allHeights.addAll(heightMap2.keySet());

            for (int height : allHeights) { // Top-down traversal to maximise subtree coverage and prevent redundant matches
                if (height < MIN_HEIGHT) break; // skip if subtree height too small to be meaningful

                List<ASTNode<?>> nodes1 = heightMap1.getOrDefault(height, Collections.emptyList());
                List<ASTNode<?>> nodes2 = heightMap2.getOrDefault(height, Collections.emptyList());

                // find fingerprint matches --> optimised by comparing only nodes with same height
                for (ASTNode<?> n1 : nodes1) {
                    if (anchorMatched1.contains(n1)) continue;
                    
                    for (ASTNode<?> n2 : nodes2) {
                        if (anchorMatched2.contains(n2)) continue;

                        // Anchor-mapping conditions depend on parametrised matching strictness
                        if (n1.getFingerprint(useAbstraction).equals(n2.getFingerprint(useAbstraction))) {
                            // Add mapping for this subtree
                            addToRawResult(res, n1, n1.getWeight(), n2, n2.getWeight(), 1.0f);
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

            List<ASTNode<?>> postOrder1 = postOrder(tree1);
            List<ASTNode<?>> postOrder2 = postOrder(tree2);
            // PHASE 1.5: Pre-calculate the sum of anchor weights for every subtree
            // This allows O(1) retrieval of the "subtracted" weight during Phase 2
            Map<ASTNode<?>, Integer> anchorWeightSums1 = new HashMap<>();
            Map<ASTNode<?>, Integer> anchorWeightSums2 = new HashMap<>();
             
            for (ASTNode<?> n : postOrder1) {
                int sum = 0;
                if (anchorMatched1.contains(n)) {
                    sum = n.getWeight(); // Entire subtree is matched; its anchor weight is its total weight
                } else {
                    for (ASTNode<?> child : n.getChildren()) { // Not an anchor itself, so pull up the anchor weights from below
                        sum += anchorWeightSums1.getOrDefault(child, 0); 
                    }
                }
                anchorWeightSums1.put(n, sum); // each node key stores the total weight of anchor-mapped nodes in its subtree
            }
            for (ASTNode<?> n : postOrder2) {
                int sum = 0;
                if (anchorMatched2.contains(n)) {
                    sum = n.getWeight(); 
                } else {
                    for (ASTNode<?> child : n.getChildren()) { 
                        sum += anchorWeightSums2.getOrDefault(child, 0); 
                    }
                }
                anchorWeightSums2.put(n, sum); 
            }

            // PHASE 2: Bottom-up search for similar subtrees (containers) based on Dice similarity of their children
            for (ASTNode<?> n1 : postOrder1) {
                if (anchorMatched1.contains(n1)) continue; // If already anchor-mapped, skip
                if (n1.getChildren().isEmpty()) continue; // container-type mappings cannot be leaves

                // Check if this node has any anchor-mapped descendants
                boolean hasAnchorMatchedDescendants = anchorWeightSums1.getOrDefault(n1, 0) > 0;
                if (!hasAnchorMatchedDescendants) continue;

                // Find the candidate matches in tree2
                List<ASTNode<?>> candidates = findCandidates(n1, tree2);

                // Find the BEST container match for a node (for tree1 from tree2)
                ASTNode<?> bestMatch = null;
                float bestDice = 0;

                for (ASTNode<?> candidate : candidates) { // Compute Dice coefficient between n1 and candidate node from tree2
                    // dice(t1, t2) = 2 * |common_descendants| / (|desc(t1)| + |desc(t2)|)
                    Set<ASTNode<?>> desc1 = n1.getDescendants();
                    Set<ASTNode<?>> descCandidate = candidate.getDescendants();
                    int commonCount = 0;
                    for (ASTNode<?> d1 : desc1) {
                        ASTNode<?> partner = anchorMap.get(d1);
                        if (partner != null && descCandidate.contains(partner)) {
                            commonCount++;
                        }
                    }
                    float dice = (2f * commonCount) / (n1.getWeight() + candidate.getWeight());

                    if (dice > bestDice) {
                        bestDice = dice;
                        bestMatch = candidate;
                    }
                }

                if (bestMatch != null && bestDice >= MIN_DICE) {
                    // EFFECTIVE WEIGHT n_c - sum(n_a)
                    // (subtract weight of anchor-mapped descendants from container-mapping to prevent double-counting)
                    int effectiveWeight1 = n1.getWeight() - anchorWeightSums1.getOrDefault(n1, 0);
                    int effectiveWeight2 = bestMatch.getWeight() - anchorWeightSums2.getOrDefault(bestMatch, 0);
                    if (effectiveWeight1 > 0 && effectiveWeight2 > 0) {
                        addToRawResult(res, n1, effectiveWeight1, bestMatch, effectiveWeight2, bestDice); // add container mapping
    
                        // We "hide" the anchor weights from all ancestors so they aren't used again (only use first container mapping found for each anchor-mapped node, to prevent double-counting in multiple similar containers)
                        int weightToHide1 = anchorWeightSums1.getOrDefault(n1, 0);
                        int weightToHide2 = anchorWeightSums2.getOrDefault(bestMatch, 0);

                        // Subtract this container's anchor-mass from all its ancestors
                        ASTNode<?> p1 = n1.getParent();
                        while (p1 != null) {
                            int current = anchorWeightSums1.getOrDefault(p1, 0);
                            anchorWeightSums1.put(p1, Math.max(0, current - weightToHide1));
                            p1 = p1.getParent();
                        }
                        ASTNode<?> p2 = bestMatch.getParent();
                        while (p2 != null) {
                            int current = anchorWeightSums2.getOrDefault(p2, 0);
                            anchorWeightSums2.put(p2, Math.max(0, current - weightToHide2));
                            p2 = p2.getParent();
                        }
                        // 3. Clear the anchorMap for these descendants to kill the commonCount for parents
                        for (ASTNode<?> d1 : n1.getDescendants()) {
                            anchorMap.remove(d1); 
                        }

                        containerMatched1.add(n1);
                        containerMatched2.add(bestMatch);
                    }
                }
            }

            this.result = res;
        }
    }

}