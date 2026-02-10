package uk.ac.warwick.dcs.sherlock.module.cli;

import picocli.CommandLine;

@CommandLine.Command(name="display", description="Print out a statement")
public class DisplayCmd implements Runnable {

    @Override
    public void run() {
        System.out.println("You ran the display module! uwu");
    }

}
