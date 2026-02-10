package uk.ac.warwick.dcs.sherlock.launch;

import java.util.Scanner;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.DisplayCmd;
import uk.ac.warwick.dcs.sherlock.module.cli.HelpCmd;

@CommandLine.Command(
    subcommands = {
        DisplayCmd.class,
        HelpCmd.class,
    }
)
public class SherlockCli {
    
    private final CommandLine cmd = new CommandLine(this);

    public void launch_cli() {
        System.out.println("Welcome to the Sherlock CLI Interface!");

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
    }

}
