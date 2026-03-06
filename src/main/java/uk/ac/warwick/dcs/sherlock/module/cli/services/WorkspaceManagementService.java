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

import uk.ac.warwick.dcs.sherlock.module.web.exceptions.WorkspaceNameNotUnique;


public class WorkspaceManagementService {

    public void createWorkspace(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, WorkspaceForm workspaceForm) {
        try {
            WorkspaceWrapper workspaceWrapper = new WorkspaceWrapper(workspaceForm, accountWrapper.getAccount(), workspaceRepository);
            System.out.println(String.format("Workspace '%s' created successfully with language '%s'!", workspaceForm.getName(), workspaceForm.getLanguage()));
        } catch(WorkspaceNameNotUnique e){
            System.out.println("Name is already in use, please choose a different name.");
        }
    }

    public List<WorkspaceWrapper> getWorkspaces(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository) {
        return WorkspaceWrapper.findByAccount(accountWrapper.getAccount(), workspaceRepository);
    }

    public WorkspaceWrapper getWorkspaceByName(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, String workspace_name) {
        return WorkspaceWrapper.findByName(accountWrapper.getAccount(), workspaceRepository, workspace_name);
    }

    public void deleteWorkspace(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, String workspace_name) {
        WorkspaceWrapper workspace_to_delete = WorkspaceWrapper.findByName(accountWrapper.getAccount(), workspaceRepository, workspace_name);
        if (workspace_to_delete == null) {
            System.out.println(String.format("No workspace found with name '%s.'", workspace_name));
        } else {
            workspace_to_delete.delete(workspaceRepository);
            System.out.println("Workspace deleted successfully.");
        }
    }
}
