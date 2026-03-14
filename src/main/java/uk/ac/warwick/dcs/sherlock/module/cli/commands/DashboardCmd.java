package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.services.StatisticsService;

/**
 * The Dashboard command to output the number of workspaces
 *  and submissions.
 */
@CommandLine.Command(name="dashboard", description="Display dashboard statistics", mixinStandardHelpOptions = true)
public class DashboardCmd implements Runnable {
    
    private StatisticsService statisticsService;

    /**
     * The entry point for the dashboard command.
     */
    @Override
    public void run() {
        statisticsService = new StatisticsService();
        int workspaceCount = statisticsService.getWorkspacesCount();
        int submissionCount = statisticsService.getSubmissionsCount();
        System.out.println("Dashboard Statistics:");
        System.out.println("Total Workspaces: " + workspaceCount);
        System.out.println("Total Submissions: " + submissionCount);
    }

}
