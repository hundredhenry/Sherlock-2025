// package uk.ac.warwick.dcs.sherlock.module.cli.services;

// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
// import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;

// public class AccountsService {
    
//     public AccountsService() {
        
//     }

//     public Account getLocalAccount() {
//         Authentication auth = SecurityContextHolder.getContext().getAuthentication();

//         String email = auth.getName();

//         Account account = accountRepository.findByEmail(email);
//         return account;
//     }
// }
