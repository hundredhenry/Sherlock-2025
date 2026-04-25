package uk.ac.warwick.dcs.sherlock.module.model.base.CLI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TParameterRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import org.mockito.MockedStatic;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Template;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TDetector;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TParameter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameterObj;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.DetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.EngineDetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import picocli.CommandLine;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;
import static org.mockito.Mockito.*;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.TemplateCmd;
import uk.ac.warwick.dcs.sherlock.launch.SherlockCli;


/**
 * Tests for the Template related commands within the CLI
 */
class TemplateCmdTest {

    //All repositories and the account the "user" would have
    private TemplateRepository templateRepository;
    private TDetectorRepository detectorRepository;
    private TParameterRepository parameterRepository;
    private AccountWrapper account;

    //the actual command line application
    private CommandLine cmd;
    //the output that the command line would print to
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        //mock the repositories to track methods being called
        //mock the template repo
        templateRepository = mock(TemplateRepository.class);
        //initialise all methods to just return null or empty values
        when(templateRepository.findByIdAndPublic(anyLong(),any())).thenReturn(
                null
        );
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                new ArrayList<Template>()
        );
        when(templateRepository.findByAccountAndPublicAndLanguage(any(),anyString())).thenReturn(
                new ArrayList<Template>()
        );

        //mock the detector repo
        detectorRepository = mock(TDetectorRepository.class);
        when(detectorRepository.findByNameAndTemplate(anyString(), any())).thenReturn(
                null
        );

        //mock the parameter repo
        parameterRepository = mock(TParameterRepository.class);
        when(parameterRepository.findByTDetector(any())).thenReturn(new ArrayList<TParameter>());

        account = mock(AccountWrapper.class);
        //whenever the account is requested, we return a new account with test values
        when(account.getAccount()).thenReturn(new Account("test.com","test","test"));


        //create a new template command using the mocked repos
        TemplateCmd tCmd = new TemplateCmd(
                templateRepository,
                account,
                detectorRepository,
                parameterRepository);

        //make the full command line with the template being a sub command
        //use a mocked spring context, so spring does not have to be booted
        AnnotationConfigApplicationContext context = mock(AnnotationConfigApplicationContext.class);
        SherlockCli sCli = new SherlockCli(context);
        cmd = new CommandLine(sCli);
        cmd.addSubcommand("template", tCmd);

        //change the output stream to be out so we can track the output
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
    }


    /**
     * Tests if the create command works as expected when correct inputs are provided
     */
    @Test
    void TemplateCmdTestCreateValid() {

        //as Sherlock registry is used, need to mock it
        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            //just make it always return Java and Haskell as available languages
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));

            //and run the command
            cmd.execute("template","create", "-n=test", "-l=Haskell");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Creating new template with name: test and language: Haskell"));
        verify(templateRepository, times(1)).save(any());
        assertTrue(out.toString().contains("Template created."));
    }

    /**
     * Tests that the output is correct when user attempts to create a template with invalid language
     */
    @Test
    void TemplateCmdTestCreateInvalidLanguage(){
        try (MockedStatic<SherlockRegistry> mocked = mockStatic(SherlockRegistry.class)) {
            mocked.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));

            cmd.execute("template","create", "-n=test", "-l=Python");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Creating new template with name: test and language: Python"));
        verify(templateRepository, times(0)).save(any());
        assertTrue(out.toString().contains("Language not supported."));
        //since set, names could be outputted in either order, so check both
        assertTrue(
                out.toString().contains("Supported languages: Java, Haskell") ||
                        out.toString().contains("Supported languages: Haskell, Java")
                );
    }

    /**
     * Tests what happens when a user creates a template with a duplicate name
     */
    @Test
    void TemplateCmdTestCreateDuplicateName(){
        //Need to create the new template which uses the same name
        //method that returns the previously made templates returns a list, so initialise the list
        List<Template> templatesOfSameName = new ArrayList<>();
        //and make the new template
        Template templateOfSameName = new Template();
        templateOfSameName.setName("TestName");
        templateOfSameName.setAccount(account.getAccount());
        //then add it to the list
        templatesOfSameName.add(templateOfSameName);
        //and make it so whenever the template repository is called to find old templates, return this list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                templatesOfSameName
        );
        //as normal
        try (MockedStatic<SherlockRegistry> mocked = mockStatic(SherlockRegistry.class)) {
            mocked.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));

            cmd.execute("template","create", "-n=TestName", "-l=Java");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Creating new template with name: TestName and language: Java"));
        verify(templateRepository, times(0)).save(any());
        assertTrue(out.toString().contains("Name is already in use, please choose a different name."));
    }

    /**
     * Tests that the output for Viewing Detectors via the CLI is as expected
     */
    @Test
    void TemplateCmdTestViewDetectorsValid(){
        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class)
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
                    ));

            cmd.execute("template","viewDetectors");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Viewing detectors for all languages..."));
        assertTrue(out.toString().contains("Java: ASTDetector, NGramDetector, VariableNameDetector"));
        assertTrue(out.toString().contains("Haskell: ASTDetector, NGramDetector, VariableNameDetector"));
    }

    /**
     * Testing that the output for viewing detectors works as expected via the CLI when providing a template
     */
    @Test
    void TemplateCmdTestViewDetectorsValidWithTemplate(){
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //add it to the list
        testTemplateList.add(testTemplate);

        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class)
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            cmd.execute("template","viewDetectors", "-n=TestName");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Viewing detectors for template: TestName"));
        assertTrue(out.toString().contains("ASTDetector, NGramDetector, VariableNameDetector"));
    }

    /**
     * Tests that the CLI command to update detectors for a CLI works as intended
     */
    @Test
    void TemplateCmdTestUpdateDetectorsValid(){
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //add it to the list
        testTemplateList.add(testTemplate);

        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class)
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            //run the command
            cmd.execute("template","updateDetectors", "-n=TestName", "ASTDetector=TRUE");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Setting detectors for a template..."));
        //creating a captor to check that the values of the detector being added to the template are correct
        ArgumentCaptor<TDetector> tDetectorCapturer = ArgumentCaptor.forClass(TDetector.class);
        //check that save was called
        verify(detectorRepository).save(tDetectorCapturer.capture());
        //extract the values
        TDetector tDetector = tDetectorCapturer.getValue();
        assertTrue(tDetector.getName().equals("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector"));
        assertTrue(tDetector.getTemplate().getName().equals("TestName"));
        //and check final output
        assertTrue(out.toString().contains("Template Detectors updated."));
    }


    /**
     * Tests that the output when changing preprocessing params via the CLI is correct
     */
    @Test
    void TemplateCmdTestPreProcesingParametersValid(){
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //Then initialise the detector currently set in the template
        TDetector astDetector = new TDetector("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",testTemplate);
        Set<TDetector> setOfAstDetector = Set.of(astDetector);
        //and add it to the template
        testTemplate.setDetectors(setOfAstDetector);

        //add the template to the list
        testTemplateList.add(testTemplate);
        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //now need to create the parameter for the detector that we are updating
        //this will be mocked
        AdjustableParameterObj mockedAdjParamObj = mock(AdjustableParameterObj.class);
        //then set the values that will be returned when methods are called
        when(mockedAdjParamObj.getDefaultValue()).thenReturn(0.5f);
        when(mockedAdjParamObj.getDescription()).thenReturn("Height");
        when(mockedAdjParamObj.getDisplayName()).thenReturn("Height");
        when(mockedAdjParamObj.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObj.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObj.getName()).thenReturn("Height");
        when(mockedAdjParamObj.getReference()).thenReturn("Height");
        when(mockedAdjParamObj.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObj.isFixed()).thenReturn(false);
        when(mockedAdjParamObj.isInt()).thenReturn(false);


        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            //make it so when finding params for the detector, returns the fake params we made earlier
            sherlockRegistry.when(() -> SherlockRegistry.getDetectorAdjustableParameters(any())).thenReturn(
              List.of(mockedAdjParamObj)
            );
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            //run the command
            cmd.execute("template","setPreProcessingParameters", "-n=TestName", "-d=ASTDetector","Height=0.35");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Setting parameters for a template..."));
        //check that the parameters being saved are correct by using a captor
        ArgumentCaptor<TParameter> tParameterCapturer = ArgumentCaptor.forClass(TParameter.class);
        verify(parameterRepository).save(tParameterCapturer.capture());
        TParameter tParameter = tParameterCapturer.getValue();
        //check height and value
        assertTrue(tParameter.getName().equals("Height"));
        assertTrue(tParameter.getValue() == 0.35f);
        assertTrue(out.toString().contains("Valid parameters updated."));
    }

    /**
     * Tests that the output when changing preprocessing parameters via the CLI the step is accurate
     */
    @Test
    void TemplateCmdTestPreProcesingParametersCheckStep(){
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //Then initialise the detector currently set in the template
        TDetector astDetector = new TDetector("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",testTemplate);
        Set<TDetector> setOfAstDetector = Set.of(astDetector);
        //and add it to the template
        testTemplate.setDetectors(setOfAstDetector);

        //add the template to the list
        testTemplateList.add(testTemplate);
        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //now need to create the parameter for the detector that we are updating
        //this will be mocked
        AdjustableParameterObj mockedAdjParamObj = mock(AdjustableParameterObj.class);
        //then set the values that will be returned when methods are called
        when(mockedAdjParamObj.getDefaultValue()).thenReturn(0.5f);
        when(mockedAdjParamObj.getDescription()).thenReturn("Height");
        when(mockedAdjParamObj.getDisplayName()).thenReturn("Height");
        when(mockedAdjParamObj.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObj.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObj.getName()).thenReturn("Height");
        when(mockedAdjParamObj.getReference()).thenReturn("Height");
        when(mockedAdjParamObj.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObj.isFixed()).thenReturn(false);
        when(mockedAdjParamObj.isInt()).thenReturn(false);

        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            sherlockRegistry.when(() -> SherlockRegistry.getDetectorAdjustableParameters(any())).thenReturn(
                    List.of(mockedAdjParamObj)
            );
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            //Passing 0.34 with a step of 0.05 should round it up to 0.35
            cmd.execute("template","setPreProcessingParameters", "-n=TestName", "-d=ASTDetector","Height=0.34");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Setting parameters for a template..."));
        //check the values of the stored parameter using a captor
        ArgumentCaptor<TParameter> tParameterCapturer = ArgumentCaptor.forClass(TParameter.class);
        verify(parameterRepository).save(tParameterCapturer.capture());
        TParameter tParameter = tParameterCapturer.getValue();
        //check the name and value is as expected
        assertTrue(tParameter.getName().equals("Height"));
        assertTrue(tParameter.getValue() == 0.35f);
        assertTrue(out.toString().contains("Valid parameters updated."));
    }

    /**
     * Check that when updating post processing params via the CLI the output is as expected
     */
    @Test
    void TemplateCmdTestUpdatePostProcesingParameters(){
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //Then initialise the detector currently set in the template
        TDetector astDetector = new TDetector("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",testTemplate);
        Set<TDetector> setOfAstDetector = Set.of(astDetector);
        //and add it to the template
        testTemplate.setDetectors(setOfAstDetector);

        //add the template to the list
        testTemplateList.add(testTemplate);
        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //now need to create the parameter for the detector that we are updating
        //this will be mocked
        AdjustableParameterObj mockedAdjParamObj = mock(AdjustableParameterObj.class);
        //then set the values that will be returned when methods are called
        when(mockedAdjParamObj.getDefaultValue()).thenReturn(0.5f);
        when(mockedAdjParamObj.getDescription()).thenReturn("Height");
        when(mockedAdjParamObj.getDisplayName()).thenReturn("Height");
        when(mockedAdjParamObj.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObj.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObj.getName()).thenReturn("Height");
        when(mockedAdjParamObj.getReference()).thenReturn("Height");
        when(mockedAdjParamObj.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObj.isFixed()).thenReturn(false);
        when(mockedAdjParamObj.isInt()).thenReturn(false);


        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java","Haskell"));
            sherlockRegistry.when(() -> SherlockRegistry.getPostProcessorAdjustableParametersFromDetector(any())).thenReturn(
                    List.of(mockedAdjParamObj)
            );
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            cmd.execute("template","setPostProcessingParameters", "-n=TestName", "-d=ASTDetector","Height=0.35");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Setting parameters for a template..."));
        //check the values using a captor
        ArgumentCaptor<TParameter> tParameterCapturer = ArgumentCaptor.forClass(TParameter.class);
        verify(parameterRepository).save(tParameterCapturer.capture());
        TParameter tParameter = tParameterCapturer.getValue();
        assertTrue(tParameter.getName().equals("Height"));
        assertTrue(tParameter.getValue() == 0.35f);
        assertTrue(out.toString().contains("Valid parameters updated."));
    }

    /**
     * Check that when updating post parameters via a CLI, the values are rounded to step ranges correctly
     */
    @Test
    void TemplateCmdTestUpdatePostProcesingParametersCheckStep() {
        //Create a test template
        //template repository returns a list of all templates, so initialise our list
        List<Template> testTemplateList = new ArrayList<>();

        //and create the test template associated with this account
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //Then initialise the detector currently set in the template
        TDetector astDetector = new TDetector("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",testTemplate);
        Set<TDetector> setOfAstDetector = Set.of(astDetector);
        //and add it to the template
        testTemplate.setDetectors(setOfAstDetector);

        //add the template to the list
        testTemplateList.add(testTemplate);
        //set up the repo to return the list
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //now need to create the parameter for the detector that we are updating
        //this will be mocked
        AdjustableParameterObj mockedAdjParamObj = mock(AdjustableParameterObj.class);
        //then set the values that will be returned when methods are called
        when(mockedAdjParamObj.getDefaultValue()).thenReturn(0.5f);
        when(mockedAdjParamObj.getDescription()).thenReturn("Height");
        when(mockedAdjParamObj.getDisplayName()).thenReturn("Height");
        when(mockedAdjParamObj.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObj.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObj.getName()).thenReturn("Height");
        when(mockedAdjParamObj.getReference()).thenReturn("Height");
        when(mockedAdjParamObj.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObj.isFixed()).thenReturn(false);
        when(mockedAdjParamObj.isInt()).thenReturn(false);

        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));
            sherlockRegistry.when(() -> SherlockRegistry.getPostProcessorAdjustableParametersFromDetector(any())).thenReturn(
                    List.of(mockedAdjParamObj)
            );
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            //using 0.34 with a step size of 0.05 should round up to 0.35
            cmd.execute("template", "setPostProcessingParameters", "-n=TestName", "-d=ASTDetector", "Height=0.34");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Setting parameters for a template..."));
        //check the values using a captor
        ArgumentCaptor<TParameter> tParameterCapturer = ArgumentCaptor.forClass(TParameter.class);
        verify(parameterRepository).save(tParameterCapturer.capture());
        TParameter tParameter = tParameterCapturer.getValue();
        assertTrue(tParameter.getName().equals("Height"));
        assertTrue(tParameter.getValue() == 0.35f);
        assertTrue(out.toString().contains("Valid parameters updated."));
    }

    /**
     * tests that the update command via the CLI updates templates correctly
     */
    @Test
    void TemplateCmdTestUpdateValid() {
        //create the list of current templates
        List<Template> testTemplateList = new ArrayList<>();
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        testTemplateList.add(testTemplate);
        //and make sure it gets returned when finding current templates from the repo
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );
        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            cmd.execute("template", "update", "-c=TestName", "-n=NewTestName", "-l=Haskell");
        }

        //check that the old template's name and language has changed correctly
        assertTrue(testTemplateList.get(0).getName().equals("NewTestName"));
        assertTrue(testTemplateList.get(0).getLanguage().equals("Haskell"));

        //and that the output is correct
        assertTrue(out.toString().contains("Name updated to NewTestName"));
        assertTrue(out.toString().contains("Language updated to Haskell"));
        assertTrue(out.toString().contains("Successfully updated template."));
    }

    /**
     * Tests that the delete template command via the CLI works as intended
     */
    @Test
    void TemplateCmdTestDeleteValid() {

        //create the list of current templates
        List<Template> testTemplateList = new ArrayList<>();
        //and the template to delete
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        testTemplateList.add(testTemplate);
        //mock the repo so it returns the list above
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //run the delete command
        cmd.execute("template", "delete", "-n=TestName");

        //check that the output is correct
        assertTrue(out.toString().contains("Deleting a template..."));

        //need to check the right detector got deleted, so use a captor to check values
        ArgumentCaptor<TParameter> tParameterCapturer = ArgumentCaptor.forClass(TParameter.class);
        ArgumentCaptor<Template>  templateCapturer = ArgumentCaptor.forClass(Template.class);
        verify(templateRepository).delete(templateCapturer.capture());
        Template template = templateCapturer.getValue();

        //and check name and language is correct
        assertTrue(template.getName().equals("TestName"));
        assertTrue(template.getLanguage().equals("Java"));

        //then check final output
        assertTrue(out.toString().contains("TestName was deleted."));
    }

    //Tests that the list command for templates via the CLI is accurate
    @Test
    void TemplateCmdTestListValid() {
        //create the list for all current templates
        List<Template> testTemplateList = new ArrayList<>();
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        testTemplateList.add(testTemplate);
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        //and list the templates
        cmd.execute("template", "list");

        //check that the output is correct
        assertTrue(out.toString().contains("All Templates:"));
        assertTrue(out.toString().contains("TestName"));
    }

    //tests the view command for templates via the CLI is outputted correctly
    @Test
    void TemplateCmdTestViewValid() {
        //create the list of current templates in the system
        List<Template> testTemplateList = new ArrayList<>();
        Template testTemplate = new Template();
        testTemplate.setName("TestName");
        testTemplate.setAccount(account.getAccount());
        testTemplate.setLanguage("Java");

        //associate an ast detector to the template
        TDetector astDetector = new TDetector("uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector", testTemplate);
        Set<TDetector> setOfAstDetector = Set.of(astDetector);
        testTemplate.setDetectors(setOfAstDetector);

        //create an adjustable parameter for the detector, which will be the preprocessing param
        AdjustableParameterObj mockedAdjParamObjPre = mock(AdjustableParameterObj.class);
        //and associate values with it, in particular its value being 0.5
        when(mockedAdjParamObjPre.getDefaultValue()).thenReturn(0.5f);
        when(mockedAdjParamObjPre.getDescription()).thenReturn("Height");
        when(mockedAdjParamObjPre.getDisplayName()).thenReturn("Height");
        when(mockedAdjParamObjPre.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObjPre.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObjPre.getName()).thenReturn("Height");
        when(mockedAdjParamObjPre.getReference()).thenReturn("Height");
        when(mockedAdjParamObjPre.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObjPre.isFixed()).thenReturn(false);
        when(mockedAdjParamObjPre.isInt()).thenReturn(false);

        //then create a post processing parameter
        AdjustableParameterObj mockedAdjParamObjPost = mock(AdjustableParameterObj.class);
        //and associate values with it, in particular its value of 0.25
        when(mockedAdjParamObjPost.getDefaultValue()).thenReturn(0.25f);
        when(mockedAdjParamObjPost.getDescription()).thenReturn("HeightPost");
        when(mockedAdjParamObjPost.getDisplayName()).thenReturn("HeightPost");
        when(mockedAdjParamObjPost.getMaximumBound()).thenReturn(1.0f);
        when(mockedAdjParamObjPost.getMinimumBound()).thenReturn(0f);
        when(mockedAdjParamObjPost.getName()).thenReturn("HeightPost");
        when(mockedAdjParamObjPost.getReference()).thenReturn("HeightPost");
        when(mockedAdjParamObjPost.getStep()).thenReturn(0.05f);
        when(mockedAdjParamObjPost.isFixed()).thenReturn(false);
        when(mockedAdjParamObjPost.isInt()).thenReturn(false);

        //and mock the repo to return the list of templates
        testTemplateList.add(testTemplate);
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                testTemplateList
        );

        try (
                MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class);
                MockedStatic<EngineDetectorWrapper> engineDetectorWrapper = mockStatic(EngineDetectorWrapper.class);
        ) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));
            //when returning pre and post params for the detector, return the above mocked objects instead
            sherlockRegistry.when(() -> SherlockRegistry.getDetectorAdjustableParameters(any())).thenReturn(
                    List.of(mockedAdjParamObjPre)
            );
            sherlockRegistry.when(() -> SherlockRegistry.getPostProcessorAdjustableParametersFromDetector(any())).thenReturn(
                    List.of(mockedAdjParamObjPost)
            );
            engineDetectorWrapper.when(() -> EngineDetectorWrapper.getDetectorNames(anyString())).thenReturn(List.of(
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector",
                    "uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector"
            ));

            //and run the command
            cmd.execute("template", "view", "-n=TestName");
        }

        //check that the output is correct
        assertTrue(out.toString().contains("Viewing a template..."));
        assertTrue(out.toString().contains("|| Pre-Processing Parameters:"));
        assertTrue(out.toString().contains("||| " + "Height" + ": " + "0.5" + " (Range: " + "0.0" + " - " + "1.0" + ", Step: " + "0.05" + ")"));
        assertTrue(out.toString().contains("|| Post-Processing Parameters:"));
        assertTrue(out.toString().contains("||| " + "HeightPost" + ": " + "0.25" + " (Range: " + "0.0" + " - " + "1.0" + ", Step: " + "0.05" + ")"));
    }
}