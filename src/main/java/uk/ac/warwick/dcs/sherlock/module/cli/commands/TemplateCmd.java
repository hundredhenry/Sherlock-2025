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

    @CommandLine.Command(name="create", description="Create a new template", mixinStandardHelpOptions = true)
    static class add_template implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String template_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Language used in the template", required = true)
        String template_language;
        
        @Override
        public void run() {
            System.out.println("Creating new template with name: " + template_name + " and language: " + template_language);      
        
        }
        
    }

    @CommandLine.Command(name="set detectors", description="Set detectors for a template", mixinStandardHelpOptions = true)
    static class set_detectors implements Runnable {

        @Override
        public void run() {
            System.out.println("Setting detectors for a template...");
        }
        
    }

    @CommandLine.Command(name="set parameters", description="Set parameters for a template", mixinStandardHelpOptions = true)
    static class set_parameters implements Runnable {

        @Override
        public void run() {
            System.out.println("Setting parameters for a template...");
        }
        
    }

    @CommandLine.Command(name="update", description="Update name and language of template", mixinStandardHelpOptions = true)
    static class update_template implements Runnable {

        @Override
        public void run() {
            System.out.println("Updating a template...");
        }
        
    }

    @CommandLine.Command(name="delete", description="Delete a template", mixinStandardHelpOptions = true)
    static class delete_template implements Runnable {

        @Override
        public void run() {
            System.out.println("Deleting a template...");
        }
        
    }

    @CommandLine.Command(name="view", description="View template details", mixinStandardHelpOptions = true)
    static class view_template implements Runnable {

        @Override
        public void run() {
            System.out.println("Viewing a template...");
        }
        
    }
}
