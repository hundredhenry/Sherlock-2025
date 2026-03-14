package uk.ac.warwick.dcs.sherlock.module.web.exceptions;

/**
 * Thrown if the user attempts to create a workspace with a name already in use
 * by another template they either own, or that is public
 */
public class WorkspaceNameNotUnique extends Exception {
    public WorkspaceNameNotUnique(String errorMessage) {
        super(errorMessage);
    }
}
