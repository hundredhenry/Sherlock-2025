package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Workspace;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import java.util.List;


public class WorkspaceManagementService {

    public void createWorkspace(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, WorkspaceForm workspaceForm) {
        WorkspaceWrapper workspaceWrapper = new WorkspaceWrapper(workspaceForm, accountWrapper.getAccount(), workspaceRepository);
    }

    public List<WorkspaceWrapper> getWorkspaces(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository) {
        return WorkspaceWrapper.findByAccount(accountWrapper.getAccount(), workspaceRepository);
    }

}
