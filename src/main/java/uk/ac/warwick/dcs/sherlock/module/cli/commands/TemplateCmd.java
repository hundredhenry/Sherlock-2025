package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import picocli.CommandLine;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameterObj;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TDetector;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TParameter;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.ParameterForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.TemplateForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TParameterRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.DetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.EngineDetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.DetectorNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.NotTemplateOwner;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.TemplateNameNotUnique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is the main CLI command for the Template module.
 * Commands include: list, create, viewDetectors, updateDetectors, viewParameters, setPreProcessingParameters,
 *  setPostProcessingParameters, update, delete, listTemplates
 */
@CommandLine.Command(name="template", description="Commands for template management", mixinStandardHelpOptions = true,
subcommands = {
    TemplateCmd.addTemplate.class,
    TemplateCmd.viewTemplateDetails.class,
    TemplateCmd.deleteTemplate.class,
    TemplateCmd.viewDetectors.class,
    TemplateCmd.setDetectors.class,
    TemplateCmd.setPreProcessingParameters.class,
    TemplateCmd.setPostProcessingParameters.class,
    TemplateCmd.listTemplates.class,
    TemplateCmd.updateTemplates.class
})
public class TemplateCmd implements Runnable {
    
    private final AccountWrapper account;
    private final TemplateRepository templateRepository;
    private final TDetectorRepository tDetectorRepository;
    private final TParameterRepository tParameterRepository;

    /**
     * Constructor for the Template command. Takes in account, and database repositories
     * @param templateRepository The H2 template repository for all Templates
     * @param account The account of the current user
     * @param tDetectorRepository The H2 Detector repository for all Detectors
     * @param tParameterRepository The H2 Parameter repository for all Parameters
     */
    public TemplateCmd(TemplateRepository templateRepository, AccountWrapper account, 
        TDetectorRepository tDetectorRepository, TParameterRepository tParameterRepository) 
        {
        this.templateRepository = templateRepository;
        this.account = account;
        this.tDetectorRepository = tDetectorRepository;
        this.tParameterRepository = tParameterRepository;
    }

    /**
     * Helper method to convert a backend detector name to a user friendly name
     * @param detectorName The backend detector name
     * @return String - The user friendly version of the detector name
     */
    private String detectorNameToReadable(String detectorName) {
        String[] split = detectorName.split("\\.");
        return split.length >0 ? split[split.length - 1] : detectorName;
    }

    /**
     * Helper method to convert a user friendly detector name to a backend name
     * @param detectorName The user friendly detector name
     * @return String - The backend version of the detector name
     */
    private String detectorNameFromReadable(String detectorName) {
        return "uk.ac.warwick.dcs.sherlock.module.model.base.detection." + detectorName;
    }

    /**
     * The main command for the Template command. Does nothing.
     */
    @Override
    public void run() {
    }

    /**
     * The Create command which creates a new Empty Template, given a name and language
     * Command: template create -n=[template name] -l=[language]
     */
    @CommandLine.Command(name="create", description="Create a new template", mixinStandardHelpOptions = true)
    public static class addTemplate implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Language used in the template", required = true)
        String templateLanguage;
        
        /**
         * Creates a new template with the name and language provided
         */
        @Override
        public void run() {
            System.out.println("Creating new template with name: " + templateName + " and language: " + templateLanguage);      
        
            //Fetch all languages supported by Sherlock
            Set<String> languages = SherlockRegistry.getLanguages();
            //if the language is not supported, then print error and let user know valid languages
            if (!languages.contains(templateLanguage)) {
                System.out.println("Language not supported.");
                System.out.println("Supported languages: " + String.join(", ", languages));
                return;
            }
            //if language is supported, then create a new TemplateForm using the language
            TemplateForm templateForm = new TemplateForm(templateLanguage);
            //and set the name of the template to the provided name
            templateForm.setName(templateName);

            //then try and make a new templateWrapper using the form, outputting appropriate error messages 
            // if needed
            try {
                new TemplateWrapper(templateForm, parent.account.getAccount(), parent.templateRepository, parent.tDetectorRepository);
                System.out.println("Template created.");
            }catch (NotTemplateOwner e) {
                System.out.println("Error making template.");
            }catch(TemplateNameNotUnique e) {
                System.out.println("Name is already in use, please choose a different name.");
            }
        }
        
    }

    /**
     * The View Detectors command for the Template command. Outputs the detectors
     *  available in Sherlock for a language, or template. If no language or template
     *  is provided, then output all detectors available in Sherlock.
     * Command: template viewDetectors -l=[language] -n=[template name]
     */
    @CommandLine.Command(name="viewDetectors", description="View detectors for a language or template", mixinStandardHelpOptions = true)
    static class viewDetectors implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Name of the language", required = false)
        String languageName;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = false)
        String templateName;

        /**
         * Outputs all detectors for a language, or template's language. If neither
         *  is provided, then output all detectors for all languages.
         */
        @Override
        public void run() {
            //if language name is provided, then output all detectors for that language
            if (languageName != null) {
                //first check if the language is supported
                Set<String> languages = SherlockRegistry.getLanguages();
                if (!languages.contains(languageName)) {
                    System.out.println("Language not supported.");
                    System.out.println("Supported languages: " + String.join(", ", languages));
                }else{
                    System.out.println("Viewing detectors for language: " + languageName);
                    //get detectors for the language
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(languageName);
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
            if (templateName != null) {
                System.out.println("Viewing detectors for template: " + templateName);
                //find all owned templates
                List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
                for (TemplateWrapper wrapper : wrapperList) {
                    //check that the name is a string
                    if (wrapper.getTemplate().getName() == null) continue;
                    ///then find the detectors for that template's language
                    if (wrapper.getTemplate().getName().equals(templateName)) {
                        //get all detectors for that template's language
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
            if (languageName == null && templateName == null) {
                System.out.println("Viewing detectors for all languages...");
                //fetch all languages supported by Sherlock
                Set<String> languages = SherlockRegistry.getLanguages();
                for (String language : languages) {
                    //get the detector name for detectors supported by language
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(language);
                    //convert the detectors to readable names
                    List<String> readableDetectors = new ArrayList<String>();
                    for (String detector : detectors) {
                        readableDetectors.add(parent.detectorNameToReadable(detector));
                    }
                    //then output the detectors
                    System.out.println(language + ": " + String.join(", ", readableDetectors));
                }            
            }       
        } 
    }

    /**
     * The Update Detectors command for the Template command. Updates the detectors
     *  for a template. Does not change detectors not provided - only adds or removes 
     *  detectors that are provided.
     * Command: template updateDetectors -n=[template name] [detectorName=TRUE/FALSE]
     */
    @CommandLine.Command(name="updateDetectors", description="Change detectors for a template", mixinStandardHelpOptions = true)
    static class setDetectors implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        @CommandLine.Parameters(arity = "1..*", paramLabel = "detectorName=TRUE/FALSE", description = "Detectors to update, in the form of detectorName=TRUE/FALSE")
        Map<String, Boolean> detectorNames;


        /**
         * Updates the detectors for a template. Does not change detectors not provided - only adds or removes 
         *  detectors that are provided.
         */
        @Override
        public void run() {
            System.out.println("Setting detectors for a template...");
            //find all owned templates
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (wrapper.getTemplate().getName().equals(templateName)) {
                    //initialise a new template form
                    TemplateForm templateForm = new TemplateForm(wrapper);
                    //then intialise the detector list
                    Set<String> currentDetectors = wrapper.getDetectors().stream().map(d -> d.getDetector().getName()).collect(Collectors.toSet());
                    //find the supported detectors for the template's language
                    List<String> detectors = EngineDetectorWrapper.getDetectorNames(wrapper.getTemplate().getLanguage());
                    //then for each detector name
                    for (String detectorName : detectorNames.keySet()) {
                        String fullDetectorName = parent.detectorNameFromReadable(detectorName);
                        //check that the detector name is supported
                        if (detectors.contains(fullDetectorName)){
                            //check if user wanted to remove 
                            if (!detectorNames.get(detectorName)) {
                                //then remove from current detectors if its there
                                currentDetectors.remove(fullDetectorName);
                            }else{
                                //then add to current detectors if its not there (handled by Set)
                                currentDetectors.add(fullDetectorName);
                            }
                        }else{
                            System.out.println(detectorName + " is not supported for this template's language. Use 'view detectors' to view supported detectors.");
                        }
                    }
                    //set the detectors in the form
                    List<String> detectorList = new ArrayList<String>(currentDetectors);
                    templateForm.setDetectors(detectorList);
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

    /**
     * The Set Pre-Processing Parameters command for the Template command. Sets the pre-processing
     *  parameters for a template's detector. Does not change parameters not provided - only adds or removes 
     *  parameters that are provided.
     * Command: template setPreProcessingParameters -n=[template name] -d=[detector] PARAMETER_NAME=VALUE
     */
    @CommandLine.Command(name="setPreProcessingParameters", description="Set pre-processing parameters for a template's detector", mixinStandardHelpOptions = true)
    static class setPreProcessingParameters implements Runnable {

        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        @CommandLine.Option(names = {"-d", "--detector"}, description = "Name of the detector", required = true)
        String detectorName;

        @CommandLine.Parameters(arity = "1..*", paramLabel = "PARAM=VALUE", description = "Pre-Processing Parameters to set, in the form of NAME=VALUE")
        Map<String, Float> parameters;

        /**
         * Sets the pre-processing parameters for a template's detector.
         */
        @Override
        public void run() {
            System.out.println("Setting parameters for a template...");
            //find all owned templates
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (wrapper.getTemplate().getName().equals(templateName)) {
                    //check owned by user
                    if (!wrapper.isOwner()) {
                        System.out.println("Found template of same name, but it is not owned by you.");
                        continue;
                    }
                    //then get the detector
                    for (DetectorWrapper detectorWrapper : wrapper.getDetectors()) {
                        //then check if the detector name is the same as one chosen
                        if (detectorWrapper.getDetector().getName().equals(parent.detectorNameFromReadable(detectorName))){
                            //then need to get paramters available for the detector
                            List<AdjustableParameterObj> parameterList;
                            ParameterForm parameterForm;
                            try{
                                parameterList = detectorWrapper.getEngineParameters();
                                parameterForm = new ParameterForm(detectorWrapper);
                            }catch(DetectorNotFound e){
                                System.out.println("Detector not found.");
                                return;
                            }

                            //for each of the paramters given by the user, validate them
                            for (Map.Entry<String, Float> entry : parameters.entrySet()) {
                                //first check valid parameter
                                int index = -1;
                                for (AdjustableParameterObj parameter : parameterList) {
                                    if (parameter.getName().equals(entry.getKey())) {
                                        index = parameterList.indexOf(parameter);
                                        break;
                                    }
                                }
                                //if not valid paramter
                                if (index == -1) {
                                    System.out.println("Parameter not found: " + entry.getKey());
                                    //and then remove it from the map
                                    parameters.remove(entry.getKey());
                                    continue;
                                }

                                //then check if value lies within valid range
                                if (entry.getValue() < parameterList.get(index).getMinimumBound()) {
                                    System.out.println("Value of " + entry.getKey() + " is below minimum bound.");
                                    parameters.remove(entry.getKey());
                                    continue;
                                }
                                if (entry.getValue() > parameterList.get(index).getMaximumBound()) {
                                    System.out.println("Value of " + entry.getKey() + " is above maximum bound.");
                                    parameters.remove(entry.getKey());
                                    continue;
                                }
                                //then round the value to the nearest step
                                parameters.put(entry.getKey(),Math.round(entry.getValue() / parameterList.get(index).getStep()) * parameterList.get(index).getStep());
                            }
                            //now we know all parameters are valid, we can update them
                            //first we need to get postprocessing parameters, so they dont get changed
                            TDetector detector = detectorWrapper.getDetector();
                            Set<TParameter> currentParameters = detector.getParameters();
                            Map<String, Float> postProcessingParameterValues = new HashMap<String, Float>();
                            //for each parameter that is currently set in the detector
                            for (TParameter parameter : currentParameters) {
                                //if its postprocessing parameter then add it to the "new" post processing parameters
                                if (parameter.getPostprocessing()) {
                                    postProcessingParameterValues.put(parameter.getName(), parameter.getValue());
                                }
                            }
                            //then set up our form
                            parameterForm.setParameters(parameters);
                            parameterForm.setPostprocessing(postProcessingParameterValues);
                            //and submit the form
                            try{
                                detectorWrapper.updateParameters(parameterForm, parent.tParameterRepository);
                            }catch(NotTemplateOwner e){
                                System.out.println("Template owner mismatch, please recreate template.");
                                //this shouldnt happen as we check template ownership earlier, if it does then since we changed the parameters,
                                // we can't continue through to find another template, so just returning
                                return;
                            }
                            System.out.println("Valid parameters updated.");
                            return;
                        }
                        
                    }
                    System.out.println("Detector not found.");
                    return;
                }
                
            }
            System.out.println("Template not found.");

        }
        
    }

    /**
     * The Set Post-Processing Parameters command for the Template command. Sets the post-processing
     *  parameters for a template's detector. Does not change parameters not provided - only adds or removes 
     *  parameters that are provided.
     * Command: template setPostProcessingParameters -n=[template name] -d=[detector] PARAMETER_NAME=VALUE
     */
    @CommandLine.Command(name="setPostProcessingParameters", description="Set post-processing parameters for a template's detector", mixinStandardHelpOptions = true)
    static class setPostProcessingParameters implements Runnable {

        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        @CommandLine.Option(names = {"-d", "--detector"}, description = "Name of the detector", required = true)
        String detectorName;

        @CommandLine.Parameters(arity = "1..*", paramLabel = "PARAM=VALUE", description = "Post-Processing Parameters to set, in the form of NAME=VALUE")
        Map<String, Float> parameters;

        /**
         * Sets the post-processing parameters for a template's detector.
         */
        @Override
        public void run() {
            System.out.println("Setting parameters for a template...");
            //find all owned templates
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (wrapper.getTemplate().getName().equals(templateName)) {
                    //check owned by user
                    if (!wrapper.isOwner()) {
                        System.out.println("Found template of same name, but it is not owned by you.");
                        continue;
                    }
                    //then get the detector
                    for (DetectorWrapper detectorWrapper : wrapper.getDetectors()) {
                        //then check if the detector name is the same as one chosen
                        if (detectorWrapper.getDetector().getName().equals(parent.detectorNameFromReadable(detectorName))){
                            //then we need to get the parameters available for the detector
                            List<AdjustableParameterObj> parameterList;
                            ParameterForm parameterForm;
                            try{
                                parameterList = detectorWrapper.getEnginePostProcessingParameters();
                                parameterForm = new ParameterForm(detectorWrapper);
                            }catch(DetectorNotFound e){
                                System.out.println("Detector not found.");
                                return;
                            }

                            //for each of the paramters given by the user, validate them
                            for (Map.Entry<String, Float> entry : parameters.entrySet()) {
                                //first check valid parameter
                                int index = -1;
                                for (AdjustableParameterObj parameter : parameterList) {
                                    if (parameter.getName().equals(entry.getKey())) {
                                        index = parameterList.indexOf(parameter);
                                        break;
                                    }
                                }
                                //if not valid paramter
                                if (index == -1) {
                                    System.out.println("Parameter not found: " + entry.getKey());
                                    //and then remove it from the map
                                    parameters.remove(entry.getKey());
                                    continue;
                                }

                                //then check if value lies within range
                                if (entry.getValue() < parameterList.get(index).getMinimumBound()) {
                                    System.out.println("Value of " + entry.getKey() + " is below minimum bound.");
                                    parameters.remove(entry.getKey());
                                    continue;
                                }
                                if (entry.getValue() > parameterList.get(index).getMaximumBound()) {
                                    System.out.println("Value of " + entry.getKey() + " is above maximum bound.");
                                    parameters.remove(entry.getKey());
                                    continue;
                                }
                                //then round the value to the nearest step
                                parameters.put(entry.getKey(),Math.round(entry.getValue() / parameterList.get(index).getStep()) * parameterList.get(index).getStep());
                            }
                            //now we know all parameters are valid, we can update them
                            //first we need to get preprocessing parameters, so they dont get changed
                            TDetector detector = detectorWrapper.getDetector();
                            Set<TParameter> currentParameters = detector.getParameters();
                            Map<String, Float> preProcessingParameterValues = new HashMap<String, Float>();
                            //for each parameter that is currently set in the detector
                            for (TParameter parameter : currentParameters) {
                                //if its NOT postprocessing (ie preprocessing), then
                                //add it to the "new" pre-processing parameters
                                if (!parameter.getPostprocessing()) {
                                    preProcessingParameterValues.put(parameter.getName(), parameter.getValue());
                                }
                            }
                            //then set up our form
                            //setParameters is the preprocessing parameter method
                            parameterForm.setParameters(preProcessingParameterValues);
                            parameterForm.setPostprocessing(parameters);
                            //and submit the form
                            try{
                                detectorWrapper.updateParameters(parameterForm, parent.tParameterRepository);
                            }catch(NotTemplateOwner e){
                                System.out.println("Template owner mismatch, please recreate template.");
                                //this shouldnt happen as we check template ownership earlier, if it does then since we changed the parameters,
                                // we can't continue through to find another template, so just returning
                                return;
                            }
                            System.out.println("Valid parameters updated.");
                            return;
                        }
                    }
                    System.out.println("Detector not found.");
                    return;
                }
                

            }
            System.out.println("Template not found.");
        }
        
    }

    /**
     * The Update command for the Template command. Updates the name and/or language of a template.
     * Command: template update -c=[current template name] -n=[newName] -l=[newLanguage]
     */
    @CommandLine.Command(name="update", description="Update name and/or language of template", mixinStandardHelpOptions = true)
    static class updateTemplates implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-c", "--current"}, description = "Current name of the template", required = true)
        String currentName;

        @CommandLine.Option(names = {"-n", "--new"}, description = "New name of the template", required = false)
        String newName;

        @CommandLine.Option(names = {"-l", "--language"}, description = "New language for the template", required = false)
        String newLang;

        /**
         * Updates the name and/or language of a template. Automatically updates detectors to fit new language
         */
        @Override
        public void run() {
            //find all templates owned by the user
            List<TemplateWrapper> templateList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper template : templateList) {
                //check that the name is a string
                if (template.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (template.getTemplate().getName().equals(currentName)) {
                    //check owned by user
                    if (!template.isOwner()) continue;
                    TemplateForm templateForm = new TemplateForm(template);

                    //if a new name is provided, update this name
                    if (newName != null) {
                        templateForm.setName(newName);
                    }

                    //first we check new languages
                    Boolean success = false;
                    //if a new language is provided, then change the language
                    if (newLang != null) {
                        //first check if its a valid language
                        Set<String> languages = SherlockRegistry.getLanguages();
                        if (!languages.contains(newLang)) {
                            System.out.println("Language not supported.");
                            System.out.println("Supported languages: " + String.join(", ", languages));
                            System.out.println("Language will stay as" + templateForm.getLanguage());
                        }else{
                            //if its valid, then update the language
                            templateForm.setLanguage(newLang);
                            //now need to update the detectors that dont work with new language
                            List<String> currentDetectors = templateForm.getDetectors();
                            List<String> validDetectors = EngineDetectorWrapper.getDetectorNames(newLang);
                            List<String> finalDetectors = new ArrayList<String>();
                            //for each current detector in the template
                            for (String detector : currentDetectors) {
                                //if its valid then add it to the "new" detector list
                                if (validDetectors.contains(detector)) {
                                    finalDetectors.add(detector);
                                }else{
                                    //let the user know that we have removed it
                                    System.out.println("Detector " + parent.detectorNameToReadable(detector) + " is not supported for this language. It will be removed.");
                                }
                            }
                            //now actually set the detectors
                            templateForm.setDetectors(finalDetectors);
                        }
                    }

                    //now try and update the template
                    try{
                        //if the template is owned by the user, then set success to true
                        template.update(templateForm, parent.templateRepository, parent.tDetectorRepository);
                        success = true;
                    }catch(NotTemplateOwner e){
                        System.out.println("Found template of same name, but it is not owned by you.");
                    }
                    //if template was owned, then everything went ok
                    if (success) {
                        if (newName != null) {
                            System.out.println("Name updated to " + newName);
                        }
                        if (newLang != null) {
                            System.out.println("Language updated to " + newLang);
                        }
                        System.out.println("Successfully updated template.");
                        return;
                    }else{
                        //this is just for the edge case where somehow, despite the user owning the template,
                        // an error occurs when updating the template due to the user not being the owner
                        // This should never happen, but if it does, then do some weird skip to the next template
                        continue;
                    }
                    
                }
            }
            //if never returned, then template was not found
            System.out.println("Template not found.");
            return;
        }
    }

    /**
     * The Delete command for the Template command. Deletes a template.
     * Command: template delete -n=[templateName]
     */
    @CommandLine.Command(name="delete", description="Delete a template", mixinStandardHelpOptions = true)
    static class deleteTemplate implements Runnable {

        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        /**
         * Deletes a template.
         */
        @Override
        public void run() {
            System.out.println("Deleting a template...");
            //find all owned templates
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one to be deleted
                if (wrapper.getTemplate().getName().equals(templateName)) {
                    try {
                        wrapper.delete(parent.templateRepository);
                    }catch(NotTemplateOwner e){
                        System.out.println("Found template of same name, but it is not owned by you.");
                        //this should not occur, if it does then keep searching for another template
                        //with the same name which is owned by the user
                        continue;
                    }
                    System.out.println(templateName+" was deleted.");
                    return;
                }
            }
            System.out.println("Template not found.");
        }
        
    }

    /**
     * The List command for the Template command. Outputs the name of all templates owned by the user, or public
     *  templates
     * Command: template list
     */
    @CommandLine.Command(name="list", description="View template details", mixinStandardHelpOptions = true)
    static class listTemplates implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        /**
         * Outputs the name of all templates owned by the user, or public templates
         */
        @Override
        public void run() {
            System.out.println("All Templates:");
            //find all the templates owned by the user
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            //and output them all
            for (TemplateWrapper wrapper : wrapperList) {
                System.out.println(wrapper.getTemplate().getName());
            }
        }
        
    }

    /**
     * The View command for the Template command. Outputs the details of a template. Does the same
     *  as the viewParameters command, just named differently.
     * Command: template view -n=[templateName]
     */
    @CommandLine.Command(name="view", description="View template details", mixinStandardHelpOptions = true)
    static class viewTemplateDetails implements Runnable {
        @CommandLine.ParentCommand
        TemplateCmd parent;

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the template", required = true)
        String templateName;

        @Override
        public void run() {
            System.out.println("Viewing a template...");
            //initially just view all templates available to the user:
            List<TemplateWrapper> wrapperList = TemplateWrapper.findByAccountAndPublic(parent.account.getAccount(), parent.templateRepository);
            for (TemplateWrapper wrapper : wrapperList) {
                //check that the name is a string
                if (wrapper.getTemplate().getName() == null) continue;
                //then check if the name is the same as one chosen
                if (wrapper.getTemplate().getName().equals(templateName)) {
                    //we need to output all active detectors, and all parameters for the detectors
                    List<DetectorWrapper> templateDetectors = wrapper.getDetectors();
                    for (DetectorWrapper detectorWrapper : templateDetectors) {
                        //then find all set parameters for that detector
                        TDetector detector = detectorWrapper.getDetector();
                        Set<TParameter> parameters = detector.getParameters();
                        System.out.println("| Detector: " + parent.detectorNameToReadable(detector.getName()));
                        //we need to find out if the parameter is pre or post processing
                        Map<String, Float> preProcessingParameterValues = new HashMap<String, Float>();
                        Map<String, Float> postProcessingParameterValues = new HashMap<String, Float>();
                        Map<String, AdjustableParameterObj> preProcessingParameters;
                        Map<String, AdjustableParameterObj> postProcessingParameters;
                        //so for each set parameter
                        for (TParameter parameter : parameters) {
                            //assign it to the correct map
                            if (parameter.isPostprocessing()) {
                                postProcessingParameterValues.put(parameter.getName(), parameter.getValue());
                            }else{
                                preProcessingParameterValues.put(parameter.getName(), parameter.getValue());
                            }
                        }

                        //now try and get all of the possible parameters
                        try{
                            preProcessingParameters = detectorWrapper.getEngineParametersMap();
                            postProcessingParameters = detectorWrapper.getEnginePostProcessingParametersMap();
                        }catch(DetectorNotFound e){
                            System.out.println("Detector not found.");
                            return;
                        }

                        System.out.println("|| Pre-Processing Parameters:");
                        //then for each parameter
                        for (Map.Entry<String, AdjustableParameterObj> entry : preProcessingParameters.entrySet()) {
                            //then print the parameter if we have a value for it, otherwise print the default value
                            if (preProcessingParameterValues.containsKey(entry.getKey())) {
                                System.out.println("||| " + entry.getKey() + ": " + preProcessingParameterValues.get(entry.getKey()) + " (Range: " + entry.getValue().getMinimumBound() + " - " + entry.getValue().getMaximumBound() + ", Step: " + entry.getValue().getStep() + ")");
                            }else{
                                System.out.println("||| " + entry.getKey() + ": " + entry.getValue().getDefaultValue() + " (Range: " + entry.getValue().getMinimumBound() + " - " + entry.getValue().getMaximumBound() + ", Step: " + entry.getValue().getStep() + ")");
                            }
                        }
                        System.out.println("|| Post-Processing Parameters:");
                        for (Map.Entry<String, AdjustableParameterObj> entry : postProcessingParameters.entrySet()) {
                            //then print the parameter if we have a value for it, otherwise print the default value
                            if (postProcessingParameterValues.containsKey(entry.getKey())) {
                                System.out.println("||| " + entry.getKey() + ": " + postProcessingParameterValues.get(entry.getKey()) + " (Range: " + entry.getValue().getMinimumBound() + " - " + entry.getValue().getMaximumBound() + ", Step: " + entry.getValue().getStep() + ")");
                            }else{
                                System.out.println("||| " + entry.getKey() + ": " + entry.getValue().getDefaultValue() + " (Range: " + entry.getValue().getMinimumBound() + " - " + entry.getValue().getMaximumBound() + ", Step: " + entry.getValue().getStep() + ")");
                            }
                        }
                    }
                    return;
                }
            }
            System.out.println("Template not found.");
        }
        
    }
}
