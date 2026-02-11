package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import java.util.List;

public class StatisticsService {
    
    public int getWorkspacesCount() {
        return SherlockEngine.storage.getWorkspaces().size();
    }

    public int getSubmissionsCount() {
        List<IWorkspace> workspaces = SherlockEngine.storage.getWorkspaces();
        return workspaces.stream().mapToInt(w -> w.getSubmissions().size()).sum();
    }

}
