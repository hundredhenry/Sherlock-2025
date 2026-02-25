// package uk.ac.warwick.dcs.sherlock.module.cli.configuration;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import uk.ac.warwick.dcs.sherlock.module.core.configuration.CoreSecurityConfig;

// @Component
// public class CliConfiguration implements CommandLineRunner{
    
//     private final AuthenticationManager authenticationManager;

//     public CliConfiguration(AuthenticationManager authenticationManager) {
//         this.authenticationManager = authenticationManager;
//     }

//     @Override
//     public void run(String... args) throws Exception {
//         // No CLI commands to run, but we need to ensure the authentication manager is created at startup
//         UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(CoreSecurityConfig.getLocalEmail(), CoreSecurityConfig.getLocalPassword());
//         Authentication authentication = authenticationManager.authenticate(authRequest);
//         SecurityContextHolder.getContext().setAuthentication(authentication);
//         System.out.println("CLI configuration complete, authentication manager initialized with local user");
//     }

// }
