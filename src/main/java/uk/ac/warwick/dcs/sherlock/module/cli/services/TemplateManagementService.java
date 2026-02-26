package uk.ac.warwick.dcs.sherlock.module.cli.services;

import uk.ac.warwick.dcs.sherlock.api.component.IWorkspace;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Template;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;

//This code is invalid in its current state, will be fixed upon finishing Template CLI command
public class TemplateManagementService {

    public void createTemplate(Account account, TemplateRepository templateRepository, String name, String language) {
        // ITemplate iWorkspace = SherlockEngine.storage.createWorkspace(name, language);
        // Template workspace = new Template(account, iWorkspace.getPersistentId());
        // workspaceRepository.save(workspace);
    }

}
