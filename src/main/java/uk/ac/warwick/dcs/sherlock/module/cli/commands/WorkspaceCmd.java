package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import org.stringtemplate.v4.compiler.CodeGenerator.primary_return;

import groovyjarjarpicocli.CommandLine.ParentCommand;
import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import java.util.List;

@CommandLine.Command(name="workspace", description="Commands for workspace management", mixinStandardHelpOptions = true,
    subcommands = {
        WorkspaceCmd.list_workspaces.class,
        WorkspaceCmd.add_workspace.class,
        WorkspaceCmd.view_workspace.class,
        WorkspaceCmd.delete_workspace.class
    }
)
public class WorkspaceCmd implements Runnable {

    private final AccountRepository accountRepository;
    private final AccountWrapper accountWrapper;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceManagementService wms;

    public WorkspaceCmd(AccountRepository accountRepository, WorkspaceRepository workspaceRepository, AccountWrapper accountWrapper) {
        this.accountRepository = accountRepository;
        this.workspaceRepository = workspaceRepository;
        this.accountWrapper = accountWrapper;
        this.wms = new WorkspaceManagementService();
    }


    @Override
    public void run() {

    }

    @CommandLine.Command(name="list", description="List all existing workspaces", mixinStandardHelpOptions = true)
    public static class list_workspaces implements Runnable {
        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            List<WorkspaceWrapper> user_workspaces = service.getWorkspaces(accountWrapper, workspaceRepository);
            System.out.println("Your workspaces:");
            for (WorkspaceWrapper w : user_workspaces) {
                System.out.println(String.format("- %s", w.getName()));
            }
        }
    }

    @CommandLine.Command(name="add", description="Add a new workspace", mixinStandardHelpOptions = true)
    public static class add_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspace_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Programming language of the workspace", required = true)
        String workspace_language;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;
        
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceForm wForm = new WorkspaceForm();
            wForm.setName(workspace_name);
            wForm.setLanguage(workspace_language);
            service.createWorkspace(accountWrapper, workspaceRepository, wForm);
            System.out.println(String.format("Workspace '%s' created successfully with language '%s'!", wForm.getName(), wForm.getLanguage()));
        }
    }

    @CommandLine.Command(name="view", description="View workspace details", mixinStandardHelpOptions = true)
    public static class view_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace")
        String workspace_name;
        @Override
        public void run() {
            System.out.println("Viewing a workspace...");
        }
    }

    @CommandLine.Command(name="delete", description="Delete a workspace", mixinStandardHelpOptions = true)
    public static class delete_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspace_name;
        @Override
        public void run() {
            System.out.println("Deleting a workspace...");
        }
    }

}
