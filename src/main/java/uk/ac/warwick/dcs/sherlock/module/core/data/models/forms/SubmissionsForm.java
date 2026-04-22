package uk.ac.warwick.dcs.sherlock.module.core.data.models.forms;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * The form to upload submission(s)
 */
public class SubmissionsForm {
    @NotEmpty(message = "{error.file.empty}")
    public MultipartFile[] files;

    @NotNull(message = "{error.single.empty}")
    public boolean single;

    @NotNull(message = "{error.duplicate}")
    @Min(value=0, message = "{error.duplicate}")
    @Max(value=2, message = "{error.duplicate}")
    public int duplicate;

    public boolean multiFolder = false;

    //denotes whether this submission is skeleton code or not
    public boolean skeleton = false;

    //denote whether the uploaded submission is in a zipped file or not
    public boolean zipped = false;

    public SubmissionsForm() { }

    public SubmissionsForm(MultipartFile[] files, boolean single) {
        this.files = files;
        this.single = single;
    }

    public MultipartFile[] getFiles() {
        return this.files;
    }

    public void setFiles(MultipartFile[] files) {
        this.files = files;
    }

    public boolean isSingle() {
        return single;
    }

    public boolean getSingle() {
        return single;
    }

    public void setSingle(boolean single) {
        this.single = single;
    }

    public int getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(int duplicate) {
        this.duplicate = duplicate;
    }

    public boolean isMultiFolder() {
        return multiFolder;
    }

    public boolean getMultiFolder() {
        return multiFolder;
    }

    public void setMultiFolder(boolean multiFolder) {
        this.multiFolder = multiFolder;
    }

    public boolean isSkeleton() {
        return skeleton;
    }

    public boolean getSkeleton() {
        return skeleton;
    }

    public void setSkeleton(boolean skeleton) {
        this.skeleton = skeleton;
    }

    public boolean isZipped() {
        return zipped;
    }

    public boolean getZipped() {
        return zipped;
    }

    public void setZipped(boolean zipped) {
        this.zipped = zipped;
    }
}
