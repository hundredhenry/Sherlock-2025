package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import org.hibernate.sql.Template;

import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.EngineDetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.TemplateForm;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.TemplateNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.NotTemplateOwner;



import java.util.Set; 
import java.util.List;

@CommandLine.Command(name="template", description="Commands for template management", mixinStandardHelpOptions = true,
subcommands = {
    TemplateCmd.add_template.class,
    TemplateCmd.view_template.class,
    TemplateCmd.delete_template.class
})
public class TemplateCmd implements Runnable {
    
    private final AccountRepository accountRepository;
    private final AccountWrapper account;
    private final TemplateRepository templateRepository;
    private final TDetectorRepository tDetectorRepository;

    public TemplateCmd(AccountRepository accountRepository, TemplateRepository templateRepository, AccountWrapper account, TDetectorRepository tDetectorRepository) {
        this.accountRepository = accountRepository;
        this.templateRepository = templateRepository;
        this.account = account;
        this.tDetectorRepository = tDetectorRepository;
    }

    @Override
    public void run() {
    }

    @CommandLine.Command(name="create", description="Create a new template", mixinStandardHelpOptions = true)
    public static class add_template implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String template_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Language used in the template", required = true)
        String template_language;
        
        @Override
        public void run() {
            System.out.println("Creating new template with name: " + template_name + " and language: " + template_language);      
        
            Set<String> languages = SherlockRegistry.getLanguages();
            if (!languages.contains(template_language)) {
                System.out.println("Language not supported.");
                System.out.println("Supported languages: " + String.join(", ", languages));
                return;
            }
            TemplateForm templateForm = new TemplateForm(template_language);
            templateForm.setName(template_name);
            List<EngineDetectorWrapper> detectors = EngineDetectorWrapper.getDetectors(template_language);

            try {
                TemplateWrapper templateWrapper = new TemplateWrapper(templateForm, parent.account.getAccount(), parent.templateRepository, parent.tDetectorRepository);
            }catch (NotTemplateOwner e) {
                System.out.println("Error making template.");
            }
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
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @Override
        public void run() {
            System.out.println("Viewing a template...");
            //initially just view all templates available to the user:
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                System.out.println(wrapper.getTemplate().getName());
            }
        }
        
    }
}
