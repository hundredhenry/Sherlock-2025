package uk.ac.warwick.dcs.sherlock.module.core.data.results;

import java.util.List;

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

    public CodeMatchData(int codeBlockId, List<String> code, int startLine, int endLine, int contextStartLine, int contextEndLine) {
        this.codeBlockId = codeBlockId;
        this.code = code;
        this.startLine = startLine;
        this.endLine = endLine;
        this.contextStartLine = contextStartLine;
        this.contextEndLine = contextEndLine;
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
}
