package uk.ac.warwick.dcs.sherlock.module.core.data.results;

import java.util.List;

public class CodeMatchData {

    private int codeBlockId;
    private List<String> code;
    private int startLine;
    private int endLine;
    private int contextStartLine;
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
