package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import picocli.CommandLine;

@CommandLine.Command(name="template", description="Commands for template management", mixinStandardHelpOptions = true,
subcommands = {
    TemplateCmd.add_template.class,
    TemplateCmd.view_template.class,
    TemplateCmd.delete_template.class
})
public class TemplateCmd implements Runnable {
    
    @Override
    public void run() {
    }

    @CommandLine.Command(name="add", description="Add a new template", mixinStandardHelpOptions = true)
    static class add_template implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true, defaultValue="NEW_TEMP")
        String template_name;

        @Override
        public void run() {
            System.out.println("Creating new template with name: " + template_name);
        }
        
    }

    @CommandLine.Command(name="view", description="View template details")
    static class view_template implements Runnable {

        @Override
        public void run() {
            System.out.println("Viewing a template...");
        }
        
    }

    @CommandLine.Command(name="delete", description="Delete a template")
    static class delete_template implements Runnable {

        @Override
        public void run() {
            System.out.println("Deleting a template...");
        }
        
    }
}
