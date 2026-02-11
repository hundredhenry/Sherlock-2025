package uk.ac.warwick.dcs.sherlock.launch;

import java.util.Scanner;

import org.eclipse.persistence.sessions.coordination.Command;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.*;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;

@CommandLine.Command(
    name = "sherlock",
    description = "Sherlock Command Line Interface",
    subcommands = {
        DisplayCmd.class,
        DashboardCmd.class,
        TemplateCmd.class
    },
    mixinStandardHelpOptions = true
)
public class SherlockCli {
    
    private final CommandLine cmd = new CommandLine(this);

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    public void launch_cli() {
        SherlockEngine engine = new SherlockEngine(Side.CLIENT);
        if (!engine.isValidInstance()) {
            System.err.println("Sherlock is already running, closing...");
            System.exit(1);
        }

        engine.initialise();

        System.out.println("Welcome to the Sherlock CLI Interface!");

        CommandLine commandLine = spec.commandLine();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("exit")) {
                break;
            } else if (line.equalsIgnoreCase("help")) {
                for (String name : commandLine.getSubcommands().keySet()) {
                    System.out.println(" - " + name);
                }
                continue;
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
        System.exit(0);
    }

}
