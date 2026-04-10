package uk.ac.warwick.dcs.sherlock.module.core.data.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.warwick.dcs.sherlock.module.core.data.results.CodeMatchData;

/**
 * Stores data about the match group
 */
public class MatchGroupData {

    /* ID of the match */
    private int matchId;

    /* Name (ID) of the overall group */
    private String groupId;

    /* Match score */
    private float score;

    /* Associates a file with its code blocks that have been matched*/
    private Map<String, List<CodeMatchData>> fileMatches;

    /**
     * Constructor without the code block matches data; sets a default value
     * 
     * @param matchId ID of the match
     * @param groupId ID of the group
     * @param score the raw match score
     */
    public MatchGroupData(int matchId, String groupId, float score) {
        this.matchId = matchId;
        this.groupId = groupId;
        this.score = score;
        this.fileMatches = new HashMap<>();
        
    }

    /**
     * Constructor with the code block matches data provided
     * 
     * @param matchId ID of the match
     * @param groupId ID of the group
     * @param score the raw match score
     * @param fileMatches the code block data
     */
    public MatchGroupData(int matchId, String groupId, float score, Map<String, List<CodeMatchData>> fileMatches) {
        this.matchId = matchId;
        this.groupId = groupId;
        this.score = score;
        this.fileMatches = fileMatches;
    }

    public int getMatchId() {
        return matchId;
    }

    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the match score, rounded to two decimal places
     * 
     * @return the rounded score
     */
    public float getScore() {
        return (float) (Math.round(this.score * 100.0) / 100.0);
    }

    public Map<String, List<CodeMatchData>> getAllFileMatches() {
        return fileMatches;
    }

    /**
     * Get all of a file's code block matches
     * @param filename the name of the file
     * @return the list
     */
    public List<CodeMatchData> getFileMatches(String filename) {
        return this.fileMatches.get(filename);
    }

    /**
     * Adds a list of code bock matches to the fileMatches map
     * 
     * @param filename the name of the file
     * @param codeBlockMatches the list of code block matches
     */
    public void addFileMatches(String filename, List<CodeMatchData> codeBlockMatches) {
        this.fileMatches.put(filename, codeBlockMatches);
    }
}