package uk.ac.warwick.dcs.sherlock.launch;

import java.util.Scanner;

import org.eclipse.persistence.sessions.coordination.Command;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.*;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.core.configuration.CoreSecurityConfig;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TParameterRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;



@CommandLine.Command(
    name = "sherlock",
    description = "Sherlock Command Line Interface",
    subcommands = {
        DisplayCmd.class,
        DashboardCmd.class,
    },
    mixinStandardHelpOptions = true
)
public class SherlockCli {
    private final AnnotationConfigApplicationContext context;

    private final CommandLine cmd = new CommandLine(this);

    public SherlockCli(AnnotationConfigApplicationContext context) {
        this.context = context;
    }

    public void launch_cli() {
        // SherlockEngine engine = context.getBean(SherlockEngine.class);
        AccountRepository accountRepository = context.getBean(AccountRepository.class);
        WorkspaceRepository workspaceRepository = context.getBean(WorkspaceRepository.class);
        AccountWrapper accountWrapper = new AccountWrapper(accountRepository.findByEmail(CoreSecurityConfig.getLocalEmail()));
        TemplateRepository templateRepository = context.getBean(TemplateRepository.class);
        TDetectorRepository tDetectorRepository = context.getBean(TDetectorRepository.class);
        TParameterRepository tParameterRepository = context.getBean(TParameterRepository.class);
        System.out.println("Authenticated as: " + accountWrapper.getAccount().getEmail() + " (" + accountWrapper.getAccount().getUsername() + ")");
        
        System.out.println("Welcome to the Sherlock CLI Interface!");

        WorkspaceCmd workspaceCmd = new WorkspaceCmd(accountRepository, workspaceRepository, templateRepository, accountWrapper);
        CommandLine workspaceCmdLine = new CommandLine(workspaceCmd);
        TemplateCmd templateCmd = new TemplateCmd(accountRepository, templateRepository, accountWrapper, tDetectorRepository, tParameterRepository);
        CommandLine templateCmdLine = new CommandLine(templateCmd);

        // cmd = new CommandLine(this);

        // cmd.addSubcommand(workspaceCmd);
        CommandLine cmd = new CommandLine(this);
        
        cmd.addSubcommand("workspace", workspaceCmdLine);
        cmd.addSubcommand("template", templateCmdLine);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("exit")) {
                break;
            }

            String[] args = line.split("\\s+");

            try {
                cmd.execute(args);
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }

        }
        System.out.println("Exiting Sherlock CLI. Goodbye!");
        scanner.close();
    }

}
