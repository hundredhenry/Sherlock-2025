package uk.ac.warwick.dcs.sherlock.launch;

import org.springframework.boot.builder.SpringApplicationBuilder;
import uk.ac.warwick.dcs.sherlock.api.annotation.EventHandler;
import uk.ac.warwick.dcs.sherlock.api.annotation.SherlockModule;
import uk.ac.warwick.dcs.sherlock.api.annotation.SherlockModule.Instance;
import uk.ac.warwick.dcs.sherlock.api.event.EventInitialisation;
import uk.ac.warwick.dcs.sherlock.api.event.EventPostInitialisation;
import uk.ac.warwick.dcs.sherlock.api.event.EventPreInitialisation;
import uk.ac.warwick.dcs.sherlock.api.util.Side;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.module.client.Splash;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.net.URI;
import picocli.CommandLine;
import java.util.Arrays;

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

	@Override
	public void run() {
		String[] java_args = spec.commandLine().getParseResult().originalArgs().toArray(new String[0]);
		launchMode = Arrays.asList(java_args).contains("--cli") ? LaunchMode.CLI : LaunchMode.WEB;
		if (cli_state) {
			System.out.println("Starting Sherlock client in CLI mode...");
			cli_launcher(java_args);
		} else {
			System.out.println("Starting Sherlock client in WEB mode...");
			web_launcher(java_args);
		}
	}

	public static void main(String[] args) {
		new CommandLine(new SherlockClient()).execute(args);
	}

	public void cli_launcher(String[] args) {
		SherlockCli cli = new SherlockCli();
		cli.launch_cli();
	}

	public void web_launcher(String[] args) {
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
