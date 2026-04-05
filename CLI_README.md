# Sherlock CLI Guide

This README serves as an explanation of the usage of the command-line interface commands available.
The commands are typically direct equivalents to, or a combination of, functionalities in Sherlock's web interface.

## Dashboard

Command: `dashboard`

This command displays the user's dashboard statistics, stating the number of workspaces they own and the total number of submissions.

## Workspaces

The commands available under the `workspace` category are used to view and manage a user's workspaces.

### List

Command: `workspace list`

This command lists the names of all available workspaces owned by the user

### Create

Command: `workspace create -n=<name> -l=<language>`

This command creates a new (empty) workspace under the provided name and with the specified language.
The language must be recognised by Sherlock.

### Update

Command: `workspace update -c=<current name>`
Optional flags:
-> -l, --lang=<new language>        Update the language of the workspace
-> -n, --new=<new name>             Rename the workspace

This command is used to update the identifying details of a workspace

### View

Command: `workspace view -n=<name>`
Optional flags:
-> -f, --files      List workspace files
-> -r, --results    List workspace jobs

This command displays information about a specific workspace, such as its name, language, the files submitted, and the jobs that have been ran.
Workspace jobs are named with their unique ID.

### Delete

Command: `workspace delete -n=<name>`

This command deletes a specified workspace and all the jobs/results associated with it.

### Files

Command: `workspace files -n=<name>`
Optional flags:
-> -a, --add=<input path>       Add a file or directory to the workspace. 
-> -c, --clear                  Clear all files in the workspace
-> -d, --delete=<filename>      Delete a specific file from the workspace

This command manipulates the files under a specific workspace.

### Run

Command: `workspace run -n=<name> -t=<template name>`

This command is used to run a workspace with a valid template. The template must exist before running a workspace.
The ID of the job is displayed after completion and can be accessed via the `workspace view` command.

## Templates 

The commands grouped under the `template` category are used to view and manage a user's detector templates.

### List

Command: `template list`

This command lists all of the available templates for the user

### Create

Command: `template create -n=<template name> -l=<language>`

This command creates a new template with the given name and language.

### Update

Command: `template update -t=<template name>`
Optional flags:
-> -l, --lang=<new language>        The new language of the template
-> -n, --new=<new name>             The new name of the template

This command is used to update the name and language of a template.

### View

Command: `template view -t=<template name>`

This commmand displays information about a template's detectors i.e. its parameters.

### Delete

Command: `template delete -n=<template name>`

This command deletes the specified template.

### View Detectors

Command: `template viewDetectors`
Optional flags:
-> -t, --template=<template name>       Name of the template; list detectors associated with this template
-> -l, --language=<language>            List all detectors associated with a language

This command lists the available detectors. No additional flags will display all detectors.

### Update Detectors

Command: `template updateDetectors -t=<template name> <detector name>=TRUE/FALSE`

This template enables/disables a detector in a template. Set to false to disable it, or true to enable.

### Set Pre-Processing Parameters

Command: `template setPreProcessingParameters -d=<detector name> -t=<template name> <parameter>=<value>

This command allows the pre-processing parameters for a template's detector to be set. Initial values are defaults.

### Set Post-Processing Parameters

Command: `template setPostProcessingParameters -d=<detector name> -t=<template name> <parameter>=<value>

This command alows the post-processing parameters for a template's detector to be set. Initial values are defaults