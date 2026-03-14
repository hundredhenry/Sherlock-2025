package uk.ac.warwick.dcs.sherlock.launch;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import uk.ac.warwick.dcs.sherlock.api.annotation.EventHandler;
import uk.ac.warwick.dcs.sherlock.api.annotation.SherlockModule;
import uk.ac.warwick.dcs.sherlock.api.annotation.SherlockModule.Instance;
import uk.ac.warwick.dcs.sherlock.api.event.EventInitialisation;
import uk.ac.warwick.dcs.sherlock.api.event.EventPostInitialisation;
import uk.ac.warwick.dcs.sherlock.api.event.EventPreInitialisation;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.client.Splash;
import uk.ac.warwick.dcs.sherlock.module.core.configuration.CoreSecurityConfig;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.net.URI;

import picocli.CommandLine;
import java.util.Arrays;

/**
 * Entry point for the Sherlock client application.
 * Launches the Sherlock client in CLI or Web mode.
 * Registered as a Picocli command.
 */
@CommandLine.Command(name = "SherlockClient", description = "Launch the Sherlock client application", mixinStandardHelpOptions = true)
@SherlockModule(side = Side.CLIENT)
public class SherlockClient implements Runnable {

	public enum LaunchMode {
		CLI,
		WEB
	}

	@Instance
	public static SherlockClient instance;

	private static Splash splash;

	private static LaunchMode launchMode = LaunchMode.WEB;

	@CommandLine.Option(names = {"--cli"}, description = "Launch as command-line interface", defaultValue = "false")
	boolean cli_state;

	@CommandLine.Spec
	private CommandLine.Model.CommandSpec spec;

	/** 
	 * Entry point for Sherlock execution.
	 * Determines the launch mode (web/CLI) based on the command-line arguments.
	 * If --cli is provided, Sherlock will start in CLI mode.
	 */
	@Override
	public void run() {
		String[] javaArgs = spec.commandLine().getParseResult().originalArgs().toArray(new String[0]);
		launchMode = Arrays.asList(javaArgs).contains("--cli") ? LaunchMode.CLI : LaunchMode.WEB;
		if (cli_state) {
			System.out.println("Starting Sherlock client in CLI mode...");
			cliLauncher(javaArgs);
		} else {
			System.out.println("Starting Sherlock client in WEB mode...");
			webLauncher(javaArgs);
		}
	}

	public static void main(String[] args) {
		new CommandLine(new SherlockClient()).execute(args);
	}

	/** 
	 * Launches the Sherlock Command Line Interface.
	 * Creates and validates a client-side Sherlock Engine instance.
	 * Creates the application context.
	 * Authenticates the local user.
	 * 
	 * @param args command-line arguments passed to Sherlock
	 */
	public void cliLauncher(String[] args) {
		SherlockEngine engine = new SherlockEngine(Side.CLIENT);
		if (!engine.isValidInstance()) {
			System.err.println("Sherlock is already running, closing...");
			System.exit(1);
		}
		engine.initialise();	
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().setActiveProfiles("cli");
		context.register(SherlockCliSpring.class);
		context.refresh();
		AuthenticationManager authenticationManager = context.getBean(AuthenticationManager.class);

		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(CoreSecurityConfig.getLocalEmail(), CoreSecurityConfig.getLocalPassword());
        Authentication authentication = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("CLI configuration complete, authentication manager initialized with local user");

		SherlockCli cli = new SherlockCli(context);
		cli.launchCli();

		context.close();
		System.exit(0);
	}

	/** 
	 * Launches the Sherlock Web Interface.
	 * Creates and validates a client-side Sherlock Engine instance.
	 * Creates the splash (loading screen).
	 * Builds the web application.
	 * 
	 * @param args command-line arguments passed to Sherlock
	 */
	public void webLauncher(String[] args) {
		System.setProperty("spring.devtools.restart.enabled", "false"); // fix stupid double instance bug

		SherlockClient.splash = new Splash();
		SherlockServer.engine = new SherlockEngine(Side.CLIENT);

		if (!SherlockServer.engine.isValidInstance()) {
			JFrame jf = new JFrame();
			jf.setAlwaysOnTop(true);
			JOptionPane.showMessageDialog(jf, "Sherlock is already running", "Sherlock error", JOptionPane.ERROR_MESSAGE);
			splash.close();
			System.exit(1);
		}
		else {

			//If "-Dmodules" is in the JVM arguments, set the path to provided
			String modulesPath = System.getProperty("modules");
			if (modulesPath != null && !modulesPath.isEmpty()) {
				SherlockEngine.setOverrideModulesPath(modulesPath);
			}

			//If "-Doverride=True" is in the JVM arguments, make Spring think it is running as a server
			String override = System.getProperty("override");
			if (override != null && override.equals("True")) {
				new SpringApplicationBuilder(SherlockServer.class).headless(false).profiles("server").run(args);
			}
			else {
				new SpringApplicationBuilder(SherlockServer.class).headless(false).profiles("client").run(args);
			}
		}
	}

	@EventHandler
	public void initialisation(EventInitialisation event) {

	}

	/** 
	 * Handles the post-initialisation of Sherlock.
	 * Closes the splash.
	 * Opens the website.
	 * 
	 * @param event command-line arguments passed to Sherlock
	 */
	@EventHandler
	public void postInitialisation(EventPostInitialisation event) {
		if (SherlockClient.splash != null) {
			SherlockClient.splash.close();
		}
		try {
			if (Desktop.isDesktopSupported() && launchMode == LaunchMode.WEB) {
				System.out.println("Opening web interface in default browser...");
				Desktop.getDesktop().browse(new URI("http://localhost:2218"));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void preInitialisation(EventPreInitialisation event) {
	}
}
