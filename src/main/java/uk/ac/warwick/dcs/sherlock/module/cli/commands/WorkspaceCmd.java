package uk.ac.warwick.dcs.sherlock.module.cli.commands;

import picocli.CommandLine;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import uk.ac.warwick.dcs.sherlock.api.component.IJob;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.component.ISubmission;
import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.module.cli.services.WorkspaceManagementService;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.SubmissionsForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.forms.WorkspaceForm;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.TemplateWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.WorkspaceWrapper;
import uk.ac.warwick.dcs.sherlock.module.core.util.ZipMultipartFile;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.DetectorNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.FileUploadFailed;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.NoFilesUploaded;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.ParameterNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.SubmissionNotFound;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.TemplateContainsNoDetectors;
import uk.ac.warwick.dcs.sherlock.module.web.exceptions.MapperException;
import uk.ac.warwick.dcs.sherlock.module.core.data.results.JobResultsData;
import uk.ac.warwick.dcs.sherlock.module.core.data.results.ResultsHelper;
import uk.ac.warwick.dcs.sherlock.module.core.data.results.SubmissionResultsData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The Workspace command that centralises workspace managemet
 * Commands include: list, create, view, files, delete, update, run, jobs
 * Command structure: workspace [cmd]
 */
@CommandLine.Command(name="workspace", description="Commands for workspace management", mixinStandardHelpOptions = true,
    subcommands = {
        WorkspaceCmd.listWorkspaces.class,
        WorkspaceCmd.createWorkspace.class,
        WorkspaceCmd.filesWorkspace.class,
        WorkspaceCmd.viewWorkspace.class,
        WorkspaceCmd.deleteWorkspace.class,
        WorkspaceCmd.updateWorkspace.class,
        WorkspaceCmd.runWorkspace.class,
        WorkspaceCmd.jobsWorkspace.class
    }
)
public class WorkspaceCmd implements Runnable {

    private final AccountWrapper accountWrapper;
    private final WorkspaceRepository workspaceRepository;
    private final TemplateRepository templateRepository;
    private final WorkspaceManagementService wms;
    private final TemplateEngine templateEngine;

    /**
     * Constructor for the Workspace command. Creates an object for the 
     *  Workspace Management Service.
     * 
     * @param workspaceRepository the workspace repository
     * @param templateRepository the template repository
     * @param accountWrapper the wrapper for the active user account
     */
    public WorkspaceCmd(WorkspaceRepository workspaceRepository, TemplateRepository templateRepository, AccountWrapper accountWrapper, TemplateEngine templateEngine) {
        this.workspaceRepository = workspaceRepository;
        this.templateRepository = templateRepository;
        this.accountWrapper = accountWrapper;
        this.templateEngine = templateEngine;
        this.wms = new WorkspaceManagementService();
    }


    @Override
    public void run() {

    }

    /**
     * Validates a language's existence in Sherlock's registry.
     * Tries to match the string regardless of case
     * @param lang the language to search for
     * @return the matched language in the registry if one is found,
     *  otherwise return null
     */
    private static String validateLanguage(String lang) {
        Set<String> languages = SherlockRegistry.getLanguages();
        Optional<String> realLang = languages.stream().filter(l -> l.equalsIgnoreCase(lang)).findFirst();
        if (realLang.isEmpty()) {
            System.out.println("Language not supported.");
            System.out.println("Supported languages: " + String.join(", ", languages));
            return null;
        }
        return realLang.get();
    }

    /**
     * The List command that outputs the name of all existing workspace.
     * Command: workspace list
     */
    @CommandLine.Command(name="list", description="List all existing workspaces", mixinStandardHelpOptions = true)
    public static class listWorkspaces implements Runnable {
        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * Fetches all of the workspaces associated with the user 
         *  and outputs their names.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            List<WorkspaceWrapper> userWorkspaces = service.getWorkspaces(accountWrapper, workspaceRepository);
            System.out.println("Your workspaces:");
            for (WorkspaceWrapper w : userWorkspaces) {
                System.out.println(String.format("- %s", w.getName()));
            }
        }
    }

    /**
     * The Create command that allows for new workspaces to be created.
     * Command: workspace create -n=[name] -l=[language]
     */
    @CommandLine.Command(name="create", description="Create a new workspace", mixinStandardHelpOptions = true)
    public static class createWorkspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspaceName;

        @CommandLine.Option(names = {"-l", "--language"}, description = "Programming language of the workspace", required = true)
        String workspaceLanguage;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;
        
        /**
         * Takes the provided name and language and submits them to
         *  the workspace repository as part of a workspace form.
         */
        @Override
        public void run() {
            String matchLang = validateLanguage(workspaceLanguage);
            if (matchLang == null) return;

            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceForm wForm = new WorkspaceForm();
            wForm.setName(workspaceName);
            wForm.setLanguage(matchLang);
            service.createWorkspace(accountWrapper, workspaceRepository, wForm);
        }
    }

    /**
     * The Files command that manages a workspace's files / submissions
     * Command: workspace files -n=[name] [OPTIONS]
     */
    @CommandLine.Command(name="files", description="Manage the workspace files", mixinStandardHelpOptions = true)
    public static class filesWorkspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspaceName;

        @CommandLine.Option(names = {"-a", "--add"}, description = "Path to file/zip/directory to add")
        List<Path> inputPaths;

        @CommandLine.Option(names = {"-c", "--clear"}, description = "Delete all submissions")
        boolean clear;

        @CommandLine.Option(names = {"-d", "--delete"}, description = "Delete a specific file")
        String delFile;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;
        
        /**
         * The entry point for the Files command.
         * Determines whether to delete all files, a specific file, 
         *  or add files.
         * Can add either an individual file, a set of files under a directory,
         *  or a zip file containing a directory or directories of files.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspaceName);
            
            if (clear) {
                // Delete all files
                workspace.deleteAll();
                System.out.println("Deleted all submissions successfully.");
                return;

            } else if (delFile != null) {
                // Delete a specific file
                List<ISourceFile> allFiles = workspace.getFiles();
                ISourceFile file = null;

                for (ISourceFile f : allFiles) {
                    if (f.getFileDisplayName().equals(delFile)) {
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
                return;

            } else if (inputPaths.size() > 0) {
                // Add files
                SubmissionsForm submissionForm = new SubmissionsForm();
                List<MultipartFile> allFiles = new ArrayList<>();

                for (Path p : inputPaths) {
                    if (!Files.exists(p)) {
                        System.out.println("File path does not exist.");
                        continue;
                    }
                    if (p.toString().endsWith(".zip")) {
                        // Add a zip file containing a directory of files
                        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(p))) {
                            ZipEntry entry;
                            while ((entry = zip.getNextEntry()) != null) {
                                if (!entry.isDirectory()) {
                                    byte[] bytes = zip.readAllBytes();
                                    String fname = Paths.get(entry.getName()).getFileName().toString();
                                    MultipartFile mf = new ZipMultipartFile(fname, fname, "application/octet-stream", bytes);
                                    allFiles.add(mf);
                                }
                                zip.closeEntry();
                            }
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }

                    } else if (Files.isDirectory(p)) {
                        // Add a directory of files
                        try (Stream<Path> paths = Files.walk(p)) {
                            paths.filter(Files::isRegularFile).forEach(fp -> {
                                try {
                                    byte[] bytes = Files.readAllBytes(fp);
                                    MultipartFile mf = new ZipMultipartFile(fp.getFileName().toString(), fp.getFileName().toString(), "application/octet-stream", bytes);
                                    allFiles.add(mf);
                                } catch (IOException e) {
                                    System.out.println(String.format("Failed to read directory %s.", fp));
                                }
                            });
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }

                    } else if (Files.isRegularFile(p)) {
                        // Add a single file
                        try {
                            byte[] bytes = Files.readAllBytes(p);
                            MultipartFile mf = new ZipMultipartFile(p.getFileName().toString(), p.getFileName().toString(), "application/octet-stream", bytes);
                            allFiles.add(mf);
                        } catch (IOException e) {
                            System.out.println(String.format("Failed to read directory %s.", p));
                        }
                    }
                }
                submissionForm.setFiles(allFiles.toArray(new MultipartFile[0]));
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

    /**
     * The View command for the workspace.
     * Outputs details regarding a specific workspace, including
     *  the name, language, and, optionally, the files, and jobs.
     * Command: workspace view -n=[name] [OPTIONS]
     */
    @CommandLine.Command(name="view", description="View workspace details", mixinStandardHelpOptions = true)
    public static class viewWorkspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required = true)
        String workspaceName;

        @CommandLine.Option(names = {"-f", "--files"}, description = "List the workspace files")
        boolean listFiles = false;

        @CommandLine.Option(names = {"-r", "--results"}, description = "List the workspace results")
        boolean listResults = false;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * Fetches all of the workspace's details (name, language, files, jobs)
         *  and displays them accordingly.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspaceName);
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

            if (listFiles) {
                System.out.println(String.format("-- Files (%d):", fileCount));

                if (fileCount == 0) {
                    System.out.println("---- There are no files in this workspace.");
                } else {
                    for (ISourceFile f : allFiles) {
                        System.out.println(String.format("---- %s (%sB)", f.getFileDisplayName(), f.getFileSize()));
                    }
                }
            }

            if (listResults) {
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

    /**
     * The Delete command for workspaces.
     * Command: workspace delete -n=[name]
     */
    @CommandLine.Command(name="delete", description="Delete a workspace", mixinStandardHelpOptions = true)
    public static class deleteWorkspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspaceName;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * Deletes a workspace given its name.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            service.deleteWorkspace(accountWrapper, workspaceRepository, workspaceName);
        }
    }

    /**
     * The Update command to update a workspace's name or language.
     * Command: workspace update -c=[current name] [OPTIONS]
     */
    @CommandLine.Command(name="update", description="Update a workspace", mixinStandardHelpOptions = true)
    public static class updateWorkspace implements Runnable {

        @CommandLine.Option(names = {"-c", "--current"}, description = "Current name of the workspace", required=true)
        String curName;

        @CommandLine.Option(names = {"-n", "--new"}, description = "New name of the workspace", required=false)
        String newName;

        @CommandLine.Option(names = {"-l", "--language"}, description = "New language for the workspace", required=false)
        String newLang;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * The entry point for the update command.
         * Checks if a new name or (valid) language has been supplied.
         * Updates the workspace name or language.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, curName);

            if (workspace == null) {
                System.out.println("Could not find a workspace with that name.");
                return;
            }

            if (newName == null && newLang == null) {
                System.out.println("No updates proposed.");
                return;
            }

            if (newName != null) {
                workspace.setName(newName);
                System.out.println(String.format("Updated workspace name from %s to %s.", curName, newName));
            }

            if (newLang != null) {
                String matchLang = validateLanguage(newLang);
                if (matchLang == null) return;

                workspace.setLanguage(matchLang);
                System.out.println(String.format("Successfully updated language to %s", matchLang));
            }
            
        }

    }

    /**
     * The Run command for the workspace.
     * Executes the analysis on the workspace files, given a template.
     * Command: workspace run -n=[name] -t=[template name]
     */
    @CommandLine.Command(name="run", description="Run a new analysis job", mixinStandardHelpOptions=true)
    public static class runWorkspace implements Runnable {
        
        @CommandLine.Option(names = {"-n", "--name"}, description = "Name of the workspace", required=true)
        String workspaceName;

        @CommandLine.Option(names = {"-t", "--template"}, description = "Name of the template to use", required=true)
        String templateName;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * The entry point for the Run command.
         * Matches the template if it exists and executes the job.
         * Catches various potential exceptions.
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspaceName);
            TemplateWrapper template = null;
            List<TemplateWrapper> templateList = TemplateWrapper.findByAccountAndPublic(accountWrapper.getAccount(), parent.templateRepository);
            for (TemplateWrapper t : templateList) {
                
                if (t.getTemplate().getName() == null) continue;
                if (t.getTemplate().getName().equals(templateName)) {
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

    /**
     * The Jobs command for the workspace.
     * Allows management of the workspace jobs; deleting and viewing.
     * (Viewing is not yet implemented)
     * Command: workspace jobs -n=[name] -j=[job id] [OPTIONS]
     */
    @CommandLine.Command(name="jobs", description="Manage workspace jobs", mixinStandardHelpOptions = true)
    public static class jobsWorkspace implements Runnable {

        @CommandLine.Option(names = {"-n", "--name"}, description="Name of the workspace", required=true)
        String workspaceName;

        @CommandLine.Option(names = {"-j", "--job"}, description="ID of the job", required=true)
        long jobId;

        @CommandLine.Option(names = {"-d", "--delete"}, description="Delete a specific job")
        boolean delJob;

        @CommandLine.Option(names = {"-s", "--scores"}, description="View all submission overall match scores")
        boolean matchScores;

        @CommandLine.Option(names = {"-r", "--report"}, description="ID of a submission to view")
        String viewReport;

        @CommandLine.ParentCommand
        WorkspaceCmd parent;

        /**
         * Matches the workspace and job if they exist.
         * Deletes the job, or provides the means to view it (not yet implemented).
         */
        @Override
        public void run() {
            AccountWrapper accountWrapper = parent.accountWrapper;
            WorkspaceRepository workspaceRepository = parent.workspaceRepository;
            WorkspaceManagementService service = parent.wms;
            WorkspaceWrapper workspace = service.getWorkspaceByName(accountWrapper, workspaceRepository, workspaceName);
            IJob job = null;
            List<IJob> allJobs = workspace.getJobs();
            for (IJob j : allJobs) {
                if (j.getPersistentId() == jobId) {
                    job = j;
                    break;
                }
            }

            if (job == null) {
                System.out.println(String.format("No job found with ID %s in workspace %s.", jobId, workspace.getName()));
                return;
            }

            if (delJob) {
                job.remove();
                System.out.println("Job deleted successfully.");
            }

            if (matchScores) {
                System.out.println("Submission match scores:");

                for (ISubmission submission : workspace.getSubmissions()) {
                    try {
                        SubmissionResultsData resultsWrapper = new SubmissionResultsData(job, submission);
                        System.out.println(String.format("(ID: %s) %s: %s", submission.getId(), submission.getName(), resultsWrapper.getScore()));
                    } catch (MapperException me) {
                        System.out.println("Error with initialising the FileMapper.");
                    }
                }
            }

            if (viewReport != null) {
                TemplateEngine templateEngine = parent.templateEngine;
                try {
                    ISubmission submission = ResultsHelper.getSubmission(workspace, Long.parseLong(viewReport));
                    SubmissionResultsData resultsWrapper = new SubmissionResultsData(job, submission);
                    if (resultsWrapper == null) return;
    
                    JobResultsData jobData = new JobResultsData(job);
                    String mapJSON = resultsWrapper.getMapJSON();
                    String matchesJSON = resultsWrapper.getMatchesJSON();
                    System.out.println(mapJSON);
                    System.out.println("\n\n\n");
                    System.out.println(matchesJSON);
    
                    Context context = new Context();
                    context.setVariable("workspace", workspace);
                    context.setVariable("results", jobData);
                    context.setVariable("submission", submission);
                    context.setVariable("wrapper", resultsWrapper);
                    context.setVariable("printing", true);
                    
                    String htmlStr = templateEngine.process("dashboard/workspaces/results/reportPDF", context);
    
                    try (OutputStream os = new FileOutputStream("ROROBEAR.pdf")) {
                        PdfRendererBuilder builder = new PdfRendererBuilder();
                        builder.withHtmlContent(htmlStr, null);
                        builder.toStream(os);
                        builder.run();
                        System.out.println("PDF generated and saved successfully.");
                    } catch (FileNotFoundException fnfe) {
                        System.out.println("File not found.");
                    } catch (IOException ioe) {
                        System.out.println("Failed to generate PDF.");
                    }
                } catch (SubmissionNotFound snf) {
                    System.out.println("Submission not found.");
                } catch (MapperException me) {
                    System.out.println("FileMapper was not initialised correctly.");
                }
                

            }
        }
    }
}
