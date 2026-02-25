package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Workspace;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;


public class WorkspaceManagementService {

    public void createWorkspace(Account account, WorkspaceRepository workspaceRepository, String name, String language) {
        IWorkspace iWorkspace = SherlockEngine.storage.createWorkspace(name, language);
        Workspace workspace = new Workspace(account, iWorkspace.getPersistentId());
        workspaceRepository.save(workspace);
    }

}
