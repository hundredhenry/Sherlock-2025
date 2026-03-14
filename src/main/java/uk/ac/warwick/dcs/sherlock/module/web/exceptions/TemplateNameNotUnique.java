package uk.ac.warwick.dcs.sherlock.module.web.exceptions;

/**
 * Thrown if the user attempts to create a template with a name already in use
 * by another template they either own, or that is public
 */
public class TemplateNameNotUnique extends Exception {
    public TemplateNameNotUnique(String errorMessage) {
        super(errorMessage);
    }
}