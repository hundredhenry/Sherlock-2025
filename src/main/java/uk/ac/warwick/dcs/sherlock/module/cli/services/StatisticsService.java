package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import java.util.List;

/**
 * A class to separate the CLI front-end from the back-end
 * Used by the CLI dashboard
 */
public class StatisticsService {
    
    /**
     * Determines the number of existing workspaces
     * 
     * @return the number of workspaces 
     */
    public int getWorkspacesCount() {
        return SherlockEngine.storage.getWorkspaces().size();
    }

    /**
     * Gets the number of submissions across all workspaces
     * 
     * @return the number of submissions
     */
    public int getSubmissionsCount() {
        List<IWorkspace> workspaces = SherlockEngine.storage.getWorkspaces();
        return workspaces.stream().mapToInt(w -> w.getSubmissions().size()).sum();
    }

}
