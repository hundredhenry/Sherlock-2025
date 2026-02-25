package uk.ac.warwick.dcs.sherlock.module.core.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.core.data.models.db.Role;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.core.data.repositories.RoleRepository;
import uk.ac.warwick.dcs.sherlock.module.web.configuration.properties.SecurityProperties;
import uk.ac.warwick.dcs.sherlock.module.web.configuration.properties.SetupProperties;
import java.util.Arrays;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.codec.binary.Base64;

@Configuration
public class CoreSecurityConfig {
    @Autowired
	private AccountRepository accountRepository;
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	@Autowired
	private Environment environment;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	private SecurityProperties securityProperties;
	@Autowired
	private SetupProperties setupProperties;
	@Autowired
	private UserDetailsService userDetailsService;

    /**
	 * Get the email of the local user
	 *
	 * @return the email
	 */
	public static String getLocalEmail() {
		return "local@dcs-sherlock.github.io";
	}

	/**
	 * Get the password of the local user
	 *
	 * @return the password
	 */
	public static String getLocalPassword() {
		return "local_password";
	}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


	/**
	 * Configures the authentication provider to use the custom user details service
	 *
	 * @return the authentication provider
	 */
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		//Check if running as a client
		if (Arrays.asList(environment.getActiveProfiles()).contains("client") || Arrays.asList(environment.getActiveProfiles()).contains("cli")) {
			//Try to find the "local user"
			Account account = accountRepository.findByEmail(CoreSecurityConfig.getLocalEmail());

			//Add the "local user" if not found
			if (account == null) {
				account = new Account(
						CoreSecurityConfig.getLocalEmail(),
						passwordEncoder.encode(CoreSecurityConfig.getLocalPassword()),
						"Local User");
				accountRepository.save(account);
				roleRepository.save(new Role("USER", account));
				roleRepository.save(new Role("LOCAL_USER", account));
			}
		} else {
			//Running as a server, so check if there are no accounts
			if (accountRepository.count() == 0) {
				//No accounts so create the default one using the settings from the application.properties file
				Account account = new Account(
						setupProperties.getEmail(),
						passwordEncoder.encode(setupProperties.getPassword()),
						setupProperties.getName()
				);
				accountRepository.save(account);
				roleRepository.save(new Role("USER", account));
				roleRepository.save(new Role("ADMIN", account));
			}
		}

		//Make the authentication provider use the custom user details service
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder);
		return authProvider;
	}

    /**
	 * Generates a random password for new accounts or when an admin changes
	 * the password on an account
	 *
	 * @return the random password
	 */
    @Bean
	public static String generateRandomPassword() {
		final Random r = new SecureRandom();
		byte[] b = new byte[12];
		r.nextBytes(b);
		return Base64.encodeBase64String(b);
	}
}
