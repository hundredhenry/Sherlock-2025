package uk.ac.warwick.dcs.sherlock.module.cli;

import picocli.CommandLine;

@CommandLine.Command(name="help", description="Display help information")
public class HelpCmd implements Runnable {
    
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        CommandLine commandLine = spec.commandLine();
        commandLine.usage(System.out);
    }
}
