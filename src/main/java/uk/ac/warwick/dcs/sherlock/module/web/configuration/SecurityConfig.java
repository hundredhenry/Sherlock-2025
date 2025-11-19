package uk.ac.warwick.dcs.sherlock.module.web.configuration;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.warwick.dcs.sherlock.module.web.data.models.db.Account;
import uk.ac.warwick.dcs.sherlock.module.web.data.models.db.Role;
import uk.ac.warwick.dcs.sherlock.module.web.configuration.properties.SecurityProperties;
import uk.ac.warwick.dcs.sherlock.module.web.configuration.properties.SetupProperties;
import uk.ac.warwick.dcs.sherlock.module.web.data.repositories.AccountRepository;
import uk.ac.warwick.dcs.sherlock.module.web.data.repositories.RoleRepository;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * Sets up both the web and http security to prevent unauthorised access to account/admin pages
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	//All @Autowired variables are automatically loaded by Spring
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

	/**
	 * Configures the authentication provider to use the custom user details service
	 *
	 * @return the authentication provider
	 */
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		//Check if running as a client
		if (Arrays.asList(environment.getActiveProfiles()).contains("client")) {
			//Try to find the "local user"
			Account account = accountRepository.findByEmail(SecurityConfig.getLocalEmail());

			//Add the "local user" if not found
			if (account == null) {
				account = new Account(
						SecurityConfig.getLocalEmail(),
						passwordEncoder.encode(SecurityConfig.getLocalPassword()),
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
	 * Configures the http security to prevent unauthorised requests to the
	 * account/admin pages as well as enable the login/logout pages
	 *
	 * @param http Spring's http security object that allows configuring web based security for specific http requests
	 *
	 * @return the security filter chain
	 * @throws Exception if there was an issue with http security
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		//Check if running in development mode
		boolean isDevMode = Arrays.asList(environment.getActiveProfiles()).contains("dev");
		
		//Check if running as a client
		if (Arrays.asList(environment.getActiveProfiles()).contains("client")) {
			//Set the required role to "ADMIN" to prevent the local user seeing the account page
			final String requiredRole = "ADMIN";

			/*
				If running locally, make all pages require authentication to ensure that the
				user is automatically redirected to the login page no matter what page they
				start on
			*/
			http
					.authorizeHttpRequests(auth -> {
						if (isDevMode) {
							//Development mode - fixes access to h2 database console
							auth.requestMatchers("/h2-console/**").permitAll();
						}
						auth.requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
							.requestMatchers("/", "/terms", "/privacy", "/help/**").hasAuthority("USER")
							.requestMatchers("/dashboard/**").hasAuthority("USER")
							.requestMatchers("/account/**").hasAuthority(requiredRole)
							.requestMatchers("/admin/**").hasAuthority("ADMIN")
							.anyRequest().authenticated();
					});
		} else {
			//If running as a server, allow access to the home/help pages
			final String requiredRole = "USER";
			
			http
					.authorizeHttpRequests(auth -> {
						if (isDevMode) {
							auth.requestMatchers("/h2-console/**").permitAll();
						}
						auth.requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
							.requestMatchers("/", "/terms", "/privacy", "/help/**").permitAll()
							.requestMatchers("/dashboard/**").hasAuthority("USER")
							.requestMatchers("/account/**").hasAuthority(requiredRole)
							.requestMatchers("/admin/**").hasAuthority("ADMIN")
							.anyRequest().authenticated();
					});
		}

		//Check if running in development mode - fixes for h2 database console
		if (isDevMode){
			http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
			http.csrf(csrf -> csrf.disable());
		}

		//Set the login page
		http
				.formLogin(form -> form
						.defaultSuccessUrl("/dashboard/index")
						.loginPage("/login")
						.usernameParameter("username")
						.passwordParameter("password")
						.permitAll()
				);

		//Delete the cookies on logout
		http
				.logout(logout -> logout
						.deleteCookies("JSESSIONID")
				);

		//Enable "remember me" support
		http
				.rememberMe(remember -> remember
						.key(securityProperties.getKey())
				);

		return http.build();
	}

	/**
	 * Generates a random password for new accounts or when an admin changes
	 * the password on an account
	 *
	 * @return the random password
	 */
	public static String generateRandomPassword() {
		final Random r = new SecureRandom();
		byte[] b = new byte[12];
		r.nextBytes(b);
		return Base64.encodeBase64String(b);
	}
}
