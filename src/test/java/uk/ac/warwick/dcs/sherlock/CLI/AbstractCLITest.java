// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.WorkspaceRepository;
// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TDetectorRepository;
// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TParameterRepository;
// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.TemplateRepository;
// import uk.ac.warwick.dcs.sherlock.module.core.data.wrappers.AccountWrapper;

// public class AbstractCLITest {
//     // SherlockEngine engine = context.getBean(SherlockEngine.class);
//     private AccountRepository accountRepository;
//     private WorkspaceRepository workspaceRepository;
//     private AccountWrapper accountWrapper;
//     private TemplateRepository templateRepository;
//     private TDetectorRepository tDetectorRepository;
//     private TParameterRepository tParameterRepository;

//     public void initialise() {
//         this.accountRepository = context.getBean(AccountRepository.class);
//         this.workspaceRepository = context.getBean(WorkspaceRepository.class);
//         this.accountWrapper = new AccountWrapper(accountRepository.findByEmail(CoreSecurityConfig.getLocalEmail()));
//         this.templateRepository = context.getBean(TemplateRepository.class);
//         this.tDetectorRepository = context.getBean(TDetectorRepository.class);
//         this.tParameterRepository = context.getBean(TParameterRepository.class);
//     }

//     public void dropTables() {
//         this.accountRepository.deleteAll();
//         this.workspaceRepository.deleteAll();
//         this.templateRepository.deleteAll();
//         this.tDetectorRepository.deleteAll();
//         this.tParameterRepository.deleteAll();
//     }
// }
