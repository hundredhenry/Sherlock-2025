package uk.ac.warwick.dcs.sherlock.module.core.data.results;

import java.util.HashSet;
import java.util.List;

import uk.ac.warwick.dcs.sherlock.api.util.ITuple;

/**
 * Stores the data of a code block from a match
 */
public class CodeMatchData {

    /* ID of the code block */
    private int codeBlockId;

    /* List of the lines of code */
    private List<String> code;

    /* Starting line number of the match */
    private int startLine;

    /* Ending line number of the match */
    private int endLine;

    /* Starting line number of the added context */
    private int contextStartLine;

    /* Ending line number of the added context */
    private int contextEndLine;

    /* The internal skeleton code for this block */
    private HashSet<ITuple<Integer, Integer>> internalSkeletonCode;

    public CodeMatchData(int codeBlockId, List<String> code, int startLine, int endLine, int contextStartLine, int contextEndLine, HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
        this.codeBlockId = codeBlockId;
        this.code = code;
        this.startLine = startLine;
        this.endLine = endLine;
        this.contextStartLine = contextStartLine;
        this.contextEndLine = contextEndLine;
        this.internalSkeletonCode = (internalSkeletonCode != null) ? internalSkeletonCode : new HashSet<>();;
    }
    
    public int getCodeBlockId() {
        return codeBlockId;
    }

    public List<String> getCode() {
        return code;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getContextStartLine() {
        return contextStartLine;
    }

    public int getContextEndLine() {
        return contextEndLine;
    }

    public HashSet<ITuple<Integer, Integer>> getInternalSkeletonCode() {
        return internalSkeletonCode;
    }
}
