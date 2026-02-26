package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Workspace;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import java.util.List;


public class WorkspaceManagementService {

    public void createWorkspace(Account account, WorkspaceRepository workspaceRepository, String name, String language) {
        WorkspaceWrapper workspaceWrapper = new WorkspaceWrapper(account, workspaceRepository, name, language);
    }

    public List<WorkspaceWrapper> getWorkspaces(Account account, WorkspaceRepository workspaceRepository) {
        return WorkspaceWrapper.findByAccount(account, workspaceRepository);
    }

}
