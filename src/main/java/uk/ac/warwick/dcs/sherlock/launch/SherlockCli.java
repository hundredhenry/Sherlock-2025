package uk.ac.warwick.dcs.sherlock.launch;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.thymeleaf.TemplateEngine;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.*;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.configuration.CoreSecurityConfig;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

/** 
 * The base Sherlock CLI command.
 * Launches and manages the interactive shell structure.
 */
@CommandLine.Command(name = "sherlock", description = "Sherlock Command Line Interface", subcommands = {DashboardCmd.class}, mixinStandardHelpOptions = true)
public class SherlockCli {
    private final AnnotationConfigApplicationContext context;

    public SherlockCli(AnnotationConfigApplicationContext context) {
        this.context = context;
    }
    
    /** 
	 * The entry point for the interactive shell.
     * Fetches repositories and the account wrapper via the application context.
	 * Accepts specific commands, including "exit" to stop execution.
     * Explicitly sets the workspace and template commands due to the need for arguments.
	 */
    public void launchCli() {
        AccountRepository accountRepository = context.getBean(AccountRepository.class);
        WorkspaceRepository workspaceRepository = context.getBean(WorkspaceRepository.class);
        AccountWrapper accountWrapper = new AccountWrapper(accountRepository.findByEmail(CoreSecurityConfig.getLocalEmail()));
        TemplateRepository templateRepository = context.getBean(TemplateRepository.class);
        TDetectorRepository tDetectorRepository = context.getBean(TDetectorRepository.class);
        TParameterRepository tParameterRepository = context.getBean(TParameterRepository.class);
        TemplateEngine templateEngine = context.getBean(TemplateEngine.class);
        
        System.out.println("Welcome to the Sherlock CLI Interface!");

        WorkspaceCmd workspaceCmd = new WorkspaceCmd(workspaceRepository, templateRepository, accountWrapper, templateEngine, new WorkspaceManagementService());
        CommandLine workspaceCmdLine = new CommandLine(workspaceCmd);
        TemplateCmd templateCmd = new TemplateCmd(templateRepository, accountWrapper, tDetectorRepository, tParameterRepository);
        CommandLine templateCmdLine = new CommandLine(templateCmd);


        CommandLine cmd = new CommandLine(this);
        
        cmd.addSubcommand("workspace", workspaceCmdLine);
        cmd.addSubcommand("template", templateCmdLine);
        Scanner scanner = new Scanner(System.in);

        Pattern regex = Pattern.compile("([^\\s\"']|(\"[^\"]*\")|('[^']*'))+");

        while (true) {
            try {
                System.out.print("> ");
                String line = scanner.nextLine().trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }

                List<String> argsList = new ArrayList<>();
                Matcher m = regex.matcher(line);

                while (m.find()) {
                    argsList.add(m.group().replace("\"", "").replace("'", ""));
                }

                String[] args = argsList.toArray(new String[0]);

                try {
                    cmd.execute(args);

                } catch (Exception e) {
                    System.out.println("Error executing command: " + e.getMessage());
                }
            } catch (NoSuchElementException nsee) {
                break;
            }

        }
        System.out.println("Exiting Sherlock CLI. Goodbye!");
        scanner.close();
    }

}
