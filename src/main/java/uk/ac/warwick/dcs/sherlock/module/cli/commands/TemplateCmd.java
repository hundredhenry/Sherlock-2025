package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import org.h2.engine.Engine;
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
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.TemplateNameNotUnique;





import java.util.Set; 
import java.util.List;
import java.util.ArrayList;


@CommandLine.Command(name="template", description="Commands for template management", mixinStandardHelpOptions = true,
subcommands = {
    TemplateCmd.add_template.class,
    TemplateCmd.view_template.class,
    TemplateCmd.delete_template.class,
    TemplateCmd.view_detectors.class,
    TemplateCmd.set_detectors.class
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

    private String detectorNameToReadable(String detectorName) {
        String[] split = detectorName.split("\\.");
        return split.length >0 ? split[split.length - 1] : detectorName;
    }

    private String detectorNameFromReadable(String detectorName) {
        return "uk.ac.warwick.dcs.sherlock.module.model.base.detection." + detectorName;
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
            }catch(TemplateNameNotUnique e) {
                System.out.println("Name is already in use, please choose a different name.");
            }
        }
        
    }

    @CommandLine.Command(name="viewDetectors", description="View detectors for a language or template", mixinStandardHelpOptions = true)
    static class view_detectors implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Name of the language", required = false)
        String language_name;

        @CommandLine.Option(names = {"-t", "--template"}, description = "Name of the template", required = false)
        String template_name;

        @Override
        public void run() {
            //if language name is provided, then output all detectors for that language
            if (language_name != null) {
                //first check if the language is supported
                Set<String> languages = SherlockRegistry.getLanguages();
                if (!languages.contains(language_name)) {
                    System.out.println("Language not supported.");
                    System.out.println("Supported languages: " + String.join(", ", languages));
                }else{
                    System.out.println("Viewing detectors for language: " + language_name);
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(language_name);
                    //convert the detectors to readable names
                    List<String> readableDetectors = new ArrayList<String>();
                    for (String detector : detectors) {
                        readableDetectors.add(parent.detectorNameToReadable(detector));
                    }
                    //then output the detectors
                    System.out.println(String.join(", ", readableDetectors));
                }
            }
            //if template name is provided, then output all detectors for that template
            if (template_name != null) {
                System.out.println("Viewing detectors for template: " + template_name);
                List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
                for (TemplateWrapper wrapper : wrapperList) {
                    //check that the name is a string
                    if (wrapper.getTemplate().getName() == null) continue;
                    ///then find the detectors for that template's language
                    if (wrapper.getTemplate().getName().equals(template_name)) {
                        List<String> detectors = EngineDetectorWrapper.getDetectorNames(wrapper.getTemplate().getLanguage());
                        //convert the detectors to readable names
                        List<String> readableDetectors = new ArrayList<String>();
                        for (String detector : detectors) {
                            readableDetectors.add(parent.detectorNameToReadable(detector));
                        }
                        //then output the detectors
                        System.out.println(String.join(", ", readableDetectors));
                    }
                }
            }
            //if neither is provided, then output all detectors for all languages
            if (language_name == null && template_name == null) {
                System.out.println("Viewing detectors for all languages...");
                Set<String> languages = SherlockRegistry.getLanguages();
                for (String language : languages) {
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(language);
                    //convert the detectors to readable names
                    List<String> readableDetectors = new ArrayList<String>();
                    for (String detector : detectors) {
                        readableDetectors.add(parent.detectorNameToReadable(detector));
                    }
                    //then output the detectors
                    System.out.println(String.join(", ", readableDetectors));
                }            
            }       
        } 
    }

    @CommandLine.Command(name="setDetectors", description="Set detectors for a template", mixinStandardHelpOptions = true)
    static class set_detectors implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-t", "--template"}, description = "Name of the template", required = true)
        String template_name;

        @CommandLine.Option(names = {"-d", "--detectors"}, description = "Detectors to be set", required = true)
        List<String> detector_names;

        @Override
        public void run() {
            System.out.println("Setting detectors for a template...");
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (wrapper.getTemplate().getName().equals(template_name)) {
                    //initialise a new template form
                    TemplateForm templateForm = new TemplateForm(wrapper);
                    //then intialise the detector list
                    List<String> detector_list = new ArrayList<String>();
                    //find the supported detectors for the template's language
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(wrapper.getTemplate().getLanguage());
                    //then for each detector name
                    for (String detector_name : detector_names) {
                        String fullDetectorName = parent.detectorNameFromReadable(detector_name);
                        //check that the detector name is supported
                        if (detectors.contains(fullDetectorName)){
                            //then set the detector for the template
                            detector_list.add(fullDetectorName);
                        }else{
                            System.out.println(detector_name + " is not supported for this template's language. Use 'view detectors' to view supported detectors.");
                        }
                    }
                    //set the detectors in the form
                    templateForm.setDetectors(detector_list);
                    boolean success = false;
                    //then try and update the template
                    try{
                        //if the template is owned by the user, then set success to true
                        wrapper.update(templateForm, parent.templateRepository, parent.tDetectorRepository);
                        success = true;
                    }catch(NotTemplateOwner e){
                        System.out.println("Found template of same name, but it is not owned by you.");
                    }
                    //if template was not owned by the user, then continue to next template
                    if (success) {
                        System.out.println("Template Detectors updated.");
                        return;
                    }
                }
            }
            System.out.println("Template not found.");
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

        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String template_name;

        @Override
        public void run() {
            System.out.println("Deleting a template...");
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one to be deleted
                if (wrapper.getTemplate().getName().equals(template_name)) {
                    try {
                        wrapper.delete(parent.templateRepository);
                    }catch(NotTemplateOwner e){
                        System.out.println("Found template of same name, but it is not owned by you.");
                        //this should not occur, if it does then keep searching for another template
                        //with the same name which is owned by the user
                        continue;
                    }
                    System.out.println(template_name+" was deleted.");
                    return;
                }
            }
            System.out.println("Template not found.");
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
