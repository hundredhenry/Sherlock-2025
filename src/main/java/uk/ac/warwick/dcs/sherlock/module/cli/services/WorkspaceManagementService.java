package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import java.util.List;

import uk.ac.warwick.dcs.sherlock.module.web.exceptions.WorkspaceNameNotUnique;

/**
 * A class to separate the CLI front-end from the back-end.
 * Services as an interface between the two sides.
 */
public class WorkspaceManagementService {

    /**
     * Creates a new workspace.
     * 
     * @param accountWrapper the current account
     * @param workspaceRepository the workspace repository
     * @param workspaceForm the form containing the new workspace details
     */
    public void createWorkspace(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, WorkspaceForm workspaceForm) {
        try {
            new WorkspaceWrapper(workspaceForm, accountWrapper.getAccount(), workspaceRepository);
            System.out.println(String.format("Workspace '%s' created successfully with language '%s'!", workspaceForm.getName(), workspaceForm.getLanguage()));
        } catch(WorkspaceNameNotUnique e){
            System.out.println("Name is already in use, please choose a different name.");
        }
    }

    /**
     * Fetch all existing workspaces associated with the current account.
     * 
     * @param accountWrapper the current account
     * @param workspaceRepository the workspace repository
     * @return a list of all of the workspaces in their wrappers
     */
    public List<WorkspaceWrapper> getWorkspaces(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository) {
        return WorkspaceWrapper.findByAccount(accountWrapper.getAccount(), workspaceRepository);
    }

    /**
     * Fetches a specific workspace by name associated with the current account.
     *  
     * @param accountWrapper the current account
     * @param workspaceRepository the workspace repository
     * @param workspace_name the workspace name to match
     * @return the matching workspace
     */
    public WorkspaceWrapper getWorkspaceByName(AccountWrapper accountWrapper, WorkspaceRepository workspaceRepository, String workspace_name) {
        return WorkspaceWrapper.findByName(accountWrapper.getAccount(), workspaceRepository, workspace_name);
    }

    /**
     * Deletes a workspace by its name.
     * 
     * @param accountWrapper the current account
     * @param workspaceRepository the workspace repository
     * @param workspace_name the workspace name
     */
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
