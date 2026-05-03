package uk.ac.warwick.dcs.sherlock.module.model.base.CLI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.api.storage.IStorageWrapper;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.TemplateCmd;
import uk.ac.warwick.dcs.sherlock.module.cli.commands.WorkspaceCmd;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TParameter;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Workspace;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.SubmissionsForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Template;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.TDetector;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TParameterRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.DetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.EngineDetectorWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.FileUploadFailed;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.IWorkspaceNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.NoFilesUploaded;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.WorkspaceNameNotUnique;
import uk.ac.warwick.dcs.sherlock.api.component.IJob;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.component.ISubmission;
import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import uk.ac.warwick.dcs.sherlock.api.exception.WorkspaceUnsupportedException;
import uk.ac.warwick.dcs.sherlock.api.executor.IExecutor;
import uk.ac.warwick.dcs.sherlock.launch.SherlockCli;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.thymeleaf.TemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import picocli.CommandLine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;

public class WorkspaceCmdTest {
    
    private WorkspaceRepository workspaceRepository;
    private WorkspaceManagementService wms;
    private TemplateRepository templateRepository;
    private TDetectorRepository detectorRepository;
    private TParameterRepository parameterRepository;
    private AccountWrapper account;

    private CommandLine cmd;
    private ByteArrayOutputStream out;

    private IStorageWrapper mockStorage;
    private IWorkspace mockIWorkspace;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        
        when(workspaceRepository.findByAccount(any())).thenReturn(new ArrayList<Workspace>());
        when(workspaceRepository.findByIdAndAccount(anyLong(), any())).thenReturn(null);

        wms = new WorkspaceManagementService();

        templateRepository = mock(TemplateRepository.class);
        
        when(templateRepository.findByIdAndPublic(anyLong(),any())).thenReturn(
                null
        );
        when(templateRepository.findByAccountAndPublic(any())).thenReturn(
                new ArrayList<uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Template>()
        );
        when(templateRepository.findByAccountAndPublicAndLanguage(any(),anyString())).thenReturn(
                new ArrayList<Template>()
        );

        detectorRepository = mock(TDetectorRepository.class);
        when(detectorRepository.findByNameAndTemplate(anyString(), any())).thenReturn(
                null
        );

        parameterRepository = mock(TParameterRepository.class);
        when(parameterRepository.findByTDetector(any())).thenReturn(new ArrayList<TParameter>());
        
        account = mock(AccountWrapper.class);
        when(account.getAccount()).thenReturn(new Account("test.com","test","test"));

        mockStorage = mock(IStorageWrapper.class);
        mockIWorkspace = mock(IWorkspace.class);
        
        when(mockStorage.createWorkspace(anyString(), anyString())).thenReturn(mockIWorkspace);
        when(mockIWorkspace.getPersistentId()).thenReturn(1L);

        // Assign to the static field so WorkspaceWrapper can find it
        SherlockEngine.storage = mockStorage;

        TemplateCmd tCmd = new TemplateCmd(templateRepository, account, detectorRepository, parameterRepository);

        WorkspaceCmd wCmd = new WorkspaceCmd(workspaceRepository, templateRepository, account, null, wms);

        AnnotationConfigApplicationContext context = mock(AnnotationConfigApplicationContext.class);
        SherlockCli sCli = new SherlockCli(context);
        cmd = new CommandLine(sCli);
        cmd.addSubcommand("template", tCmd);
        cmd.addSubcommand("workspace", wCmd);

        //change the output stream to be out so we can track the output
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

    }

    @AfterEach
    void tearDown() {
        SherlockEngine.storage = null;
    }

    // Tests that workspaces can be listed
    @Test
    void listWorkspacesTest() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");
        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        cmd.execute("workspace", "list");
        assertTrue(out.toString().contains("Your workspaces:"));
        assertTrue(out.toString().contains("- test"));
    }

    // Tests that workspaces can be correctly created
    @Test
    void createWorkspaceTestValid() {

        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));

            cmd.execute("workspace", "create", "-n=test", "-l=Java");
        }

        assertTrue(out.toString().contains("Creating workspace with name 'test' and language 'Java'"));
        verify(workspaceRepository, times(1)).save(any());
        assertTrue(out.toString().contains("Workspace created successfully"));
    }

    // Tests that workspaces cannot be created if an unsupported language is provided
    @Test
    void createWorkspaceTestInvalidLanguage() {
        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));

            cmd.execute("workspace", "create", "-n=test", "-l=Python");
        }

        assertTrue(out.toString().contains("Language not supported."));
        assertTrue(out.toString().contains("Supported languages: Java, Haskell") || out.toString().contains("Supported languages: Haskell, Java"));
        verify(workspaceRepository, times(0)).save(any());
        
    }

    // tests that an existing workspace can be deleted by name
    @Test
    void deleteWorkspaceTestValid() {

        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");
        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        cmd.execute("workspace", "delete", "-n=test");
        assertTrue(out.toString().contains("Deleting workspace..."));
        verify(workspaceRepository, times(1)).delete(any(Workspace.class));
        assertTrue(out.toString().contains("Workspace 'test' deleted successfully."));
    }

    // Tests that a workspace that does not exist, cannot be deleted
    @Test
    void deleteWorkspacetestInvalid() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");
        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        cmd.execute("workspace", "delete", "-n=testfail");
        assertTrue(out.toString().contains("Deleting workspace..."));
        verify(workspaceRepository, times(0)).delete(any(Workspace.class));
        assertTrue(out.toString().contains("No workspace found with name 'testfail'."));
    }

    // Tests that an existing workspace can be viewed
    @Test
    void viewWorkspaceTest() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        when(iws.getName()).thenReturn("test");
        when(iws.getLanguage()).thenReturn("Java");
        when(iws.getSubmissions()).thenReturn(new ArrayList<ISubmission>());
        when(iws.getFiles()).thenReturn(new ArrayList<ISourceFile>());
        when(iws.getJobs()).thenReturn(new ArrayList<IJob>());

        cmd.execute("workspace", "view", "-n=test", "-rfs");
        assertTrue(out.toString().contains("Workspace details:"));
        assertTrue(out.toString().contains("-- Name: test"));
        assertTrue(out.toString().contains("-- Language: Java"));
        assertTrue(out.toString().contains("-- Submissions (0):"));
        assertTrue(out.toString().contains("---- There are no submissions in this workspace."));
        assertTrue(out.toString().contains("-- Files (0):"));
        assertTrue(out.toString().contains("---- There are no files in this workspace."));
        assertTrue(out.toString().contains("-- Results (0):"));
        assertTrue(out.toString().contains("---- There are no jobs in this workspace."));

    }

    // Tests that a workspace that does not exist cannot be viewed
    @Test
    void viewWorkspaceTestInvalid() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));

        cmd.execute("workspace", "view", "-n=testfail");
        assertTrue(out.toString().contains("Could not find a workspace with name 'testfail'."));
    }

    // Tests that a workspace's language and name can be updated
    @Test
    void updateWorkspaceTest() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));

        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));

            cmd.execute("workspace", "update", "-c=test", "-n=updatedTest", "-l=Haskell");
        }

        assertTrue(out.toString().contains("Updated workspace name from test to updatedTest."));
        assertTrue(out.toString().contains("Successfully updated language to Haskell"));

    }

    // Tests that a non-existent workspace cannot be updated
    @Test
    void updateWorkspaceTestInvalidNoWorkspace() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());


        when(iws.getName()).thenReturn("test");

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));

        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));

            cmd.execute("workspace", "update", "-c=testfail", "-n=newname");
        }

        assertTrue(out.toString().contains("Could not find a workspace with that name."));

    }

    // Tests that a workspace isn't updated if no desired updates are provided
    @Test
    void updateWorkspaceTestInvalidNoUpdates() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));

        try (MockedStatic<SherlockRegistry> sherlockRegistry = mockStatic(SherlockRegistry.class)) {
            sherlockRegistry.when(SherlockRegistry::getLanguages).thenReturn(Set.of("Java", "Haskell"));

            cmd.execute("workspace", "update", "-c=test");
        }

        assertTrue(out.toString().contains("No updates proposed."));
    }

    // Tests that all of the files in a workspace can be cleared
    @Test
    void filesClearTest() {

        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");

        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        cmd.execute("workspace", "files", "-n=test", "-c");

        assertTrue(out.toString().contains("Deleting submissions..."));
        assertTrue(out.toString().contains("Deleted all submissions successfully."));
    }

    // Tests that a specific file in a workspace can be deleted
    @Test
    void filesDeleteSpecificTest() {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");
        
        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        ISourceFile mockFile = mock(ISourceFile.class);
        when(mockFile.getFileDisplayName()).thenReturn("example.java");
        when(iws.getFiles()).thenReturn(List.of(mockFile));

        cmd.execute("workspace", "files", "-n=test", "-d=example.java");
    
        assertTrue(out.toString().contains("Deleting file example.java"));
        assertTrue(out.toString().contains("File deleted successfully."));
        verify(mockFile, times(1)).remove();
    }

    // Tests that an individual file can be uploaded to a workspace
    @Test
    void filesUploadSingleFileTest() throws IOException {
        IWorkspace iws = mockStorage.createWorkspace("test", "Java");
        Workspace ws = new Workspace(account.getAccount(), iws.getPersistentId());

        when(iws.getName()).thenReturn("test");
        
        when(mockStorage.getWorkspaces(List.of(1L))).thenReturn(List.of(iws));
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(ws));
        
        Path fileUpload = tempDir.resolve("example.java");
        Files.writeString(fileUpload, "public class example {}");

        cmd.execute("workspace", "files", "-n=test", "-a=" + fileUpload.toString());

        assertFalse(out.toString().contains("File path does not exist."));
        assertFalse(out.toString().contains("Failed to read file"));
        assertFalse(out.toString().contains("No files uploaded."));
        assertFalse(out.toString().contains("File upload failed."));
    }
    
    // Tests that a workspace can be used to analyse files, given a template
    //  and at least two submissions
    @Test
    void runWorkspaceTestValid() {
        SherlockEngine.executor = mock(IExecutor.class);

        IWorkspace mockIws = mock(IWorkspace.class);
        IJob mockJob = mock(IJob.class);
        when(mockIws.getName()).thenReturn("wspace");
        when(mockIws.createJob()).thenReturn(mockJob);
        when(mockJob.getPersistentId()).thenReturn(483L);
        when(mockJob.getTasks()).thenReturn(new ArrayList<>());
        
        when(mockIws.getSubmissions()).thenReturn(List.of(mock(ISubmission.class), mock(ISubmission.class)));
        when(mockIws.getFiles()).thenReturn(List.of(mock(ISourceFile.class)));
        
        when(SherlockEngine.storage.getWorkspaces()).thenReturn(List.of(mockIws));
        when(SherlockEngine.storage.getWorkspaces(List.of(1L))).thenReturn(List.of(mockIws));

        TDetector detector = new TDetector();
        detector.setName("java.lang.Object");
        
        Template dbTemplate = new Template();
        dbTemplate.setName("temp");
        dbTemplate.setDetectors(Set.of(detector));
        dbTemplate.setAccount(account.getAccount());
        dbTemplate.setLanguage("Java");

        when(templateRepository.findByAccountAndPublic(any())).thenReturn(List.of(dbTemplate));

        Workspace dbWs = new Workspace(account.getAccount(), 1L);
        when(workspaceRepository.findByAccount(any())).thenReturn(List.of(dbWs));

        cmd.execute("workspace", "run", "-n=wspace", "-t=temp");

        assertTrue(out.toString().contains("Analysis complete. Saved with job ID 483"));
    }

}
