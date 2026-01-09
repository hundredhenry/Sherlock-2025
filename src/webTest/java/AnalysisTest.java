
import Utils.AccountUtils;
import Utils.Sleeper;
import Utils.TemplateUtils;
import Utils.WorkspaceUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysisTest extends AbstractWebTest {
    String nameOfTemplate = "Analysis Template";
    String nameOfWorkspace = "Analysis Workspace";

    @BeforeAll
    public static void setupClass() {

    }

    @BeforeEach
    public void setupTest() {
        setDefaultTestSettings();
        setDirectory("AnalysisTests");
        //return to home page
        browser.get(baseURL);
        AccountUtils.loginWithAdmin(getSettings());
        setSubDirectory("");
        TemplateUtils.addTemplate(getSettings(), nameOfTemplate);
        WorkspaceUtils.addWorkspace(getSettings(), nameOfWorkspace);
    }

    @AfterEach
    public void tearDown() {
        WorkspaceUtils.deleteWorkspace(getSettings(), nameOfWorkspace);
        Sleeper.sleep();
        TemplateUtils.deleteTemplate(getSettings(), nameOfTemplate);
        browser.close();
    }

    //Test to show that it is possible to run an analysis on uploaded files.
    @Test
    public void uploadAndRunAnalysis() {
        setSubDirectory("UploadAndRunAnalysis");

        String resource1 = System.getProperty("user.dir") + "\\src\\webTest\\resources\\Original.zip";
        String resource2 = System.getProperty("user.dir") + "\\src\\webTest\\resources\\Copy.zip";
        WorkspaceUtils.selectWorkspace(getSettings(), nameOfWorkspace);

        takeScreenshot("01_Workspace.jpg");
        WebElement uploadButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#submissions-parent .upload-new-submission")));
        uploadButton.click();
        // Upload first zip of files
        WebElement modal = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#modal")));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input#num_sub_multi"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input#sub_org_folder"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#sub_org_folder_div .form-row input"))).sendKeys(resource1);
        takeScreenshot("02_CompleteUploadForm.jpg");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#sub_org_folder_div .form-group .btn-primary"))).click();

        // Confirm successful upload of first zip of files
        // String alertMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.alert"))).getText();
        Sleeper.sleep();
        
        String alertMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.alert.alert-info"))).getText();
        takeScreenshot("03_UploadSuccessful.jpg");
        String expectedMessage = messageProperties.getProperty("workspaces.submissions.uploaded.no_dups");
        assertEquals(expectedMessage, alertMessage);
        modal.findElement(By.cssSelector(".btn-secondary")).click();
        Sleeper.sleep();

        // Upload second zip of files
        WorkspaceUtils.uploadZipToWorkspace(getSettings(), resource2);
        takeScreenshot("04_FilesUploaded.jpg");
        Sleeper.sleep();

        // Confirm successful upload of second zip of files
        alertMessage = modal.findElement(By.cssSelector("div.alert.alert-info")).getText();
        expectedMessage = messageProperties.getProperty("workspaces.submissions.uploaded.no_dups");
        assertEquals(expectedMessage, alertMessage);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#modal .btn-secondary"))).click();
        
        // Run analysis on uploaded files
        browser.findElement(By.cssSelector("div#run-parent .btn-primary")).click();
        takeScreenshot("05_Run.jpg");
        String completeStatus = messageProperties.getProperty("COMPLETE");
        wait.until(ExpectedConditions.textToBe(By.cssSelector("span.badge"), completeStatus));
        takeScreenshot("06_Complete.jpg");
    }


}
