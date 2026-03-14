package uk.ac.warwick.dcs.sherlock.module.web.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.warwick.dcs.sherlock.module.web.configuration.properties.SecurityProperties;

import java.util.Arrays;

/**
 * Sets up both the web and http security to prevent unauthorised access to account/admin pages
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	//All @Autowired variables are automatically loaded by Spring
	@Autowired
	private Environment environment;
	@Autowired
	private SecurityProperties securityProperties;

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
}
