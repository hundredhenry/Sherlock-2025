package uk.ac.warwick.dcs.sherlock.module.core.data.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.warwick.dcs.sherlock.module.core.data.results.CodeMatchData;

public class MatchGroupData {

    private int matchId;
    private String groupId;
    private float score;
    private Map<String, List<CodeMatchData>> fileMatches;

    public MatchGroupData(int matchId, String groupId, float score) {
        this.matchId = matchId;
        this.groupId = groupId;
        this.score = score;
        this.fileMatches = new HashMap<>();
        
    }

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

    public float getScore() {
        return (float) (Math.round(this.score * 100.0) / 100.0);
    }

    public Map<String, List<CodeMatchData>> getAllFileMatches() {
        return fileMatches;
    }

    public List<CodeMatchData> getFileMatches(String filename) {
        return this.fileMatches.get(filename);
    }

    public void addFileMatches(String filename, List<CodeMatchData> codeBlockMatches) {
        this.fileMatches.put(filename, codeBlockMatches);
    }
}