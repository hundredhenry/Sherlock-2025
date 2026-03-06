package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService.Work;
import org.stringtemplate.v4.compiler.CodeGenerator.primary_return;

import groovyjarjarpicocli.CommandLine.ParentCommand;
import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
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
        }
    }

    @CommandLine.Command(name="view", description="View workspace details", mixinStandardHelpOptions = true)
    public static class view_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace")
        String workspace_name;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            // Display name, language, list submissions, list results
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspace_name);
            if (workspace == null) {
                System.out.println("Could not find a workspace with that name.");
            } else {
                String name = workspace.getName();
                String language = workspace.getLanguage();
                List<ISourceFile> allFiles = workspace.getFiles();
                int fileCount = allFiles.size();
                System.out.println("Workspace details:");
                System.out.println(String.format("-- Name: %s", name));
                System.out.println(String.format("-- Language: %s", language));
                System.out.println(String.format("-- Files (%d):", fileCount));

                if (fileCount == 0) {
                    System.out.println("---- There are no files in this workspace.");
                } else {
                    for (ISourceFile f : allFiles) {
                        System.out.println(String.format("---- %s (%sB)", f.getFileDisplayName(), f.getFileSize()));
                    }
                }

            }
        }
    }

    @CommandLine.Command(name="delete", description="Delete a workspace", mixinStandardHelpOptions = true)
    public static class delete_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspace_name;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            service.deleteWorkspace(accountWrapper, workspaceRepository, workspace_name);
        }
    }

}
