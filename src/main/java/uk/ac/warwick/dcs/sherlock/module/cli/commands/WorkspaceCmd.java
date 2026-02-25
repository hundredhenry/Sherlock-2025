package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import groovyjarjarpicocli.CommandLine.ParentCommand;
import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
// import uk.ac.warwick.dcs.sherlock.module.cli.services.AccountsService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;

@CommandLine.Command(name="workspace", description="Commands for workspace management", mixinStandardHelpOptions = true,
    subcommands = {
        WorkspaceCmd.add_workspace.class,
        WorkspaceCmd.view_workspace.class,
        WorkspaceCmd.delete_workspace.class
    }
)
public class WorkspaceCmd implements Runnable {

    private final AccountRepository accountRepository;
    private final Account account;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceCmd(AccountRepository accountRepository, WorkspaceRepository workspaceRepository, Account account) {
        this.accountRepository = accountRepository;
        this.workspaceRepository = workspaceRepository;
        this.account = account;
    }


    @Override
    public void run() {

    }

    @CommandLine.Command(name="add", description="Add a new workspace", mixinStandardHelpOptions = true)
    static class add_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspace_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Programming language of the workspace", required = true)
        String workspace_language;

        @ParentCommand
        WorkspaceCmd parent;
        
        @Override
        public void run() {
            System.out.println("Adding a new workspace...");
            Account account = parent.account;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = new WorkspaceManagementService();
            service.createWorkspace(account, workspaceRepository, workspace_name, workspace_language);
        }
    }

    @CommandLine.Command(name="view", description="View workspace details", mixinStandardHelpOptions = true)
    static class view_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace")
        String workspace_name;
        @Override
        public void run() {
            System.out.println("Viewing a workspace...");
        }
    }

    @CommandLine.Command(name="delete", description="Delete a workspace", mixinStandardHelpOptions = true)
    static class delete_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspace_name;
        @Override
        public void run() {
            System.out.println("Deleting a workspace...");
        }
    }

}
