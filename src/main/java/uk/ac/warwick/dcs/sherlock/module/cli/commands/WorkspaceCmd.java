package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService.Work;
import org.springframework.security.access.method.P;
import org.springframework.web.multipart.MultipartFile;
import org.stringtemplate.v4.compiler.CodeGenerator.primary_return;

import groovyjarjarpicocli.CommandLine.ParentCommand;
import org.springframework.web.multipart.MultipartFile;
import picocli.CommandLine;
import uk.ac.warwick.dcs.sherlock.api.component.IJob;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.results.JobResultsData;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.util.ZipMultipartFile;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.FileUploadFailed;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.NoFilesUploaded;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.ParameterNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.TemplateContainsNoDetectors;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.DetectorNotFound;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.SubmissionsForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.List;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.ArrayList;

@CommandLine.Command(name="workspace", description="Commands for workspace management", mixinStandardHelpOptions = true,
    subcommands = {
        WorkspaceCmd.list_workspaces.class,
        WorkspaceCmd.create_workspace.class,
        WorkspaceCmd.files_workspace.class,
        WorkspaceCmd.view_workspace.class,
        WorkspaceCmd.delete_workspace.class,
        WorkspaceCmd.update_workspace.class,
        WorkspaceCmd.run_workspace.class,
        WorkspaceCmd.jobs_workspace.class
    }
)
public class WorkspaceCmd implements Runnable {

    private final AccountRepository accountRepository;
    private final AccountWrapper accountWrapper;
    private final WorkspaceRepository workspaceRepository;
    private final TemplateRepository templateRepository;
    private final WorkspaceManagementService wms;

    public WorkspaceCmd(AccountRepository accountRepository, WorkspaceRepository workspaceRepository, TemplateRepository templateRepository, AccountWrapper accountWrapper) {
        this.accountRepository = accountRepository;
        this.workspaceRepository = workspaceRepository;
        this.templateRepository = templateRepository;
        this.accountWrapper = accountWrapper;
        this.wms = new WorkspaceManagementService();
    }


    @Override
    public void run() {

    }

    private static String validate_language(String lang) {
        Set<String> languages = SherlockRegistry.getLanguages();
        Optional<String> real_lang = languages.stream().filter(l -> l.equalsIgnoreCase(lang)).findFirst();
        if (real_lang.isEmpty()) {
            System.out.println("Language not supported.");
            System.out.println("Supported languages: " + String.join(", ", languages));
            return null;
        }
        return real_lang.get();
    }

    // List all of the existing workspaces
    @CommandLine.Command(name="list", description="List all existing workspaces", mixinStandardHelpOptions = true)
    public static class list_workspaces implements Runnable {
        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            List<WorkspaceWrapper> user_workspaces = service.getWorkspaces(accountWrapper, workspaceRepository);
            System.out.println("Your workspaces:");
            for (WorkspaceWrapper w : user_workspaces) {
                System.out.println(String.format("- %s", w.getName()));
            }
        }
    }

    // Create a new workspace given a name and language
    @CommandLine.Command(name="create", description="Create a new workspace", mixinStandardHelpOptions = true)
    public static class create_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspace_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Programming language of the workspace", required = true)
        String workspace_language;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;
        
        @Override
        public void run() {
            String match_lang = validate_language(workspace_language);
            if (match_lang == null) return;

            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceForm wForm = new WorkspaceForm();
            wForm.setName(workspace_name);
            wForm.setLanguage(match_lang);
            service.createWorkspace(accountWrapper, workspaceRepository, wForm);
        }
    }

    // Manage a workspace's files given workspace name
    // Actions: add files, clear all files
    // Should try to add: delete individual file
    @CommandLine.Command(name="files", description="Manage the workspace files", mixinStandardHelpOptions = true)
    public static class files_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspace_name;

        @CommandLine.Option(names = {"-a", "--add"}, description = "Path to file/zip/directory to add")
        List<Path> input_paths;

        @CommandLine.Option(names = {"-c", "--clear"}, description = "Delete all submissions")
        boolean clear;

        @CommandLine.Option(names = {"-d", "--delete"}, description = "Delete a specific file")
        String del_file;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;
        
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspace_name);
            
            if (clear) {
                workspace.deleteAll();
                System.out.println("Deleted all submissions successfully.");
                return;
            } else if (del_file != null) {
                List<ISourceFile> all_files = workspace.getFiles();
                ISourceFile file = null;

                for (ISourceFile f : all_files) {
                    if (f.getFileDisplayName().equals(del_file)) {
                        file = f;
                        break;
                    }
                }

                if (file != null) {
                    file.remove();
                    System.out.println("File deleted successfully.");
                } else {
                    System.out.println("File not found.");
                }

            } else if (input_paths.size() > 0) {
                SubmissionsForm submissionForm = new SubmissionsForm();
                List<MultipartFile> all_files = new ArrayList<>();

                for (Path p : input_paths) {
                    if (!Files.exists(p)) {
                        System.out.println("File path does not exist.");
                        continue;
                    }
                    if (p.toString().endsWith(".zip")) {
                        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(p))) {
                            ZipEntry entry;
                            while ((entry = zip.getNextEntry()) != null) {
                                if (!entry.isDirectory()) {
                                    byte[] bytes = zip.readAllBytes();
                                    String fname = Paths.get(entry.getName()).getFileName().toString();
                                    MultipartFile mf = new ZipMultipartFile(fname, fname, "application/octet-stream", bytes);
                                    all_files.add(mf);
                                }
                                zip.closeEntry();
                            }
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }
                    } else if (Files.isDirectory(p)) {
                        try (Stream<Path> paths = Files.walk(p)) {
                            paths.filter(Files::isRegularFile).forEach(fp -> {
                                try {
                                    byte[] bytes = Files.readAllBytes(fp);
                                    MultipartFile mf = new ZipMultipartFile(fp.getFileName().toString(), fp.getFileName().toString(), "application/octet-stream", bytes);
                                    all_files.add(mf);
                                } catch (IOException e) {
                                    System.out.println(String.format("Failed to read directory %s.", fp));
                                }
                            });
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }
                    } else if (Files.isRegularFile(p)) {
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            MultipartFile mf = new ZipMultipartFile(p.getFileName().toString(), p.getFileName().toString(), "application/octet-stream", bytes);
                            all_files.add(mf);
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }
                    }
                }
                submissionForm.setFiles(all_files.toArray(new MultipartFile[0]));
                submissionForm.setSingle(true);
                try {
                    workspace.addSubmissions(submissionForm);
                } catch (NoFilesUploaded nfu) {
                    System.out.println("No files uploaded.");
                } catch (FileUploadFailed fuf) {
                    System.out.println("File upload failed.");
                }
            }
        }
    }

    // View a workspace's details; name, language, files, jobs
    @CommandLine.Command(name="view", description="View workspace details", mixinStandardHelpOptions = true)
    public static class view_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace")
        String workspace_name;

        @CommandLine.Option(names = {"-f", "--files"}, description = "List the workspace files")
        boolean list_files = false;

        @CommandLine.Option(names = {"-r", "--results"}, description = "List the workspace results")
        boolean list_results = false;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspace_name);
            if (workspace == null) {
                System.out.println("Could not find a workspace with that name.");
                return;
            }
            String name = workspace.getName();
            String language = workspace.getLanguage();
            
            List<ISourceFile> allFiles = workspace.getFiles();
            int fileCount = allFiles.size();

            List<IJob> wJobs = workspace.getJobs();
            int jobCount = wJobs.size();

            System.out.println("Workspace details:");
            System.out.println(String.format("-- Name: %s", name));
            System.out.println(String.format("-- Language: %s", language));
            if (list_files) {
                System.out.println(String.format("-- Files (%d):", fileCount));

                if (fileCount == 0) {
                    System.out.println("---- There are no files in this workspace.");
                } else {
                    for (ISourceFile f : allFiles) {
                        System.out.println(String.format("---- %s (%sB)", f.getFileDisplayName(), f.getFileSize()));
                    }
                }
            }
            if (list_results) {
                System.out.println(String.format("-- Results (%d)", jobCount));
                
                if (jobCount == 0) {
                    System.out.println("---- There are no jobs in this workspace.");
                } else {
                    for (IJob j : wJobs) {
                        System.out.println(String.format("---- %s (%s)", j.getPersistentId(), j.getStatus().toString()));
                    }
                }
            }
        }
    }

    // Delete a workspace given its name
    @CommandLine.Command(name="delete", description="Delete a workspace", mixinStandardHelpOptions = true)
    public static class delete_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspace_name;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            service.deleteWorkspace(accountWrapper, workspaceRepository, workspace_name);
        }
    }

    // Update a workspace's name and/or language
    @CommandLine.Command(name="update", description="Update a workspace", mixinStandardHelpOptions = true)
    public static class update_workspace implements Runnable {

        @CommandLine.Option(names = {"-c", "--current"}, description = "Current name of the workspace", required=true)
        String cur_name;

        @CommandLine.Option(names = {"-n", "--new"}, description = "New name of the workspace", required=false)
        String new_name;

        @CommandLine.Option(names = {"-l", "--language"}, description = "New language for the workspace", required=false)
        String new_lang;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, cur_name);

            if (workspace == null) {
                System.out.println("Could not find a workspace with that name.");
                return;
            }

            if (new_name == null && new_lang == null) {
                System.out.println("No updates proposed.");
                return;
            }

            if (new_name != null) {
                workspace.setName(new_name);
                System.out.println(String.format("Updated workspace name from %s to %s.", cur_name, new_name));
            }

            if (new_lang != null) {
                String match_lang = validate_language(new_lang);
                if (match_lang == null) return;

                workspace.setLanguage(match_lang);
                System.out.println(String.format("Successfully updated language to %s", match_lang));
            }
            
        }

    }

    // Run a workspace given a template and provided there are files
    @CommandLine.Command(name="run", description="Run a new analysis job", mixinStandardHelpOptions=true)
    public static class run_workspace implements Runnable {
        
        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspace_name;

        @CommandLine.Option(names = {"-t", "--template"}, description = "Name of the template to use", required=true)
        String template_name;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspace_name);
            TemplateWrapper template = null;
            List<TemplateWrapper> templateList = TemplateWrapper.findByAccountAndPublic(accountWrapper.getAccount(), parent.templateRepository);
            for (TemplateWrapper t : templateList) {
                
                if (t.getTemplate().getName() == null) continue;
                if (t.getTemplate().getName().equals(template_name)) {
                    template = t;
                    break;
                }
            }

            if (template != null) {
                try {
                    System.out.println("Running analysis...");
                    long jobid = workspace.runTemplate(template);
                    System.out.println(String.format("Anaysis complete. Saved with job ID %s", jobid));
                } catch (TemplateContainsNoDetectors tcnd) {
                    System.out.println("Template does not have any associated detectors.");
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("Detector no longer exists.");
                } catch (ParameterNotFound pnf) {
                    System.out.println("Template detector parameters not found.");
                } catch (DetectorNotFound dnf) {
                    System.out.println("Detector not found.");
                } catch (NoFilesUploaded nfu) {
                    System.out.println("No files found.");
                } catch (Exception e) {
                    System.out.println("Error whilst trying to run this workspace.");
                }
            } else {
                System.out.println("Could not find template.");
            }
        }
    }

    // Manage a workspace's jobs; delete, view results
    @CommandLine.Command(name="jobs", description="Manage workspace jobs", mixinStandardHelpOptions = true)
    public static class jobs_workspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description="Name of the workspace", required=true)
        String workspace_name;

        @CommandLine.Option(names = {"-j", "--job"}, description="ID of the job", required=true)
        long job_id;

        @CommandLine.Option(names = {"-d", "--delete"}, description="Delete a specific job")
        boolean del_job;

        @CommandLine.Option(names = {"-r", "--results"}, description="View the results of a job")
        boolean view_job;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspace_name);
            IJob job = null;
            List<IJob> all_jobs = workspace.getJobs();
            for (IJob j : all_jobs) {
                if (j.getPersistentId() == job_id) {
                    job = j;
                    break;
                }
            }

            if (job == null) {
                System.out.println(String.format("No job found with ID %s in workspace %s.", job_id, workspace.getName()));
                return;
            }

            if (del_job) {
                job.remove();
                System.out.println("Job deleted successfully.");
            }

            if (view_job) {
                System.out.println("Viewing job results (incomplete)");
                // Not sure how to do this yet
                // Download the report?
            }
        }
    }
}
