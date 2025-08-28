/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModel;
import org.openrefactory.util.ClassPathUtility;
import org.openrefactory.util.logger.ORDaemonLogger;
import org.openrefactory.util.progressreporter.IProgressReporter;
import org.openrefactory.util.progressreporter.PrefixLoggerProgressReporter;

/**
 * OpenRefactory daemon that runs as an Eclipse application.
 * 
 * <p>This daemon is responsible for:</p>
 * <ul>
 *   <li>Loading configuration from a config.json file</li>
 *   <li>Setting up the Eclipse project environment</li>
 *   <li>Executing call graph analysis</li>
 * </ul>
 * 
 * @author Munawar Hafiz
 */
public class ORDaemon implements IApplication {
    
    /** Progress reporter for daemon operations */
    private static IProgressReporter daemonProgressReporter;
        
    /** Synchronization lock for daemon operations */
    public static Object lock = new Object();
    
    /**
     * Constructs a new ORDaemon instance.
     * 
     * <p>Initializes the daemon progress reporter if it hasn't been initialized yet.
     * The progress reporter is configured with a "[SERVER]" prefix and uses the
     * ORDaemonLogger for output.</p>
     * 
     * @throws IOException if there's an error initializing the progress reporter
     */
    public ORDaemon() throws IOException {
        if (daemonProgressReporter == null) {
            daemonProgressReporter = new PrefixLoggerProgressReporter("[SERVER]", new ORDaemonLogger());
        }
    }
    
    /**
     * Entry method based on the extension point of
     * org.eclipse.core.runtime.applications
     * 
     * See manifest file and plugin.xml
     * 
     * @param context the Eclipse application context containing command line arguments
     * @return always returns {@code null} as per Eclipse application contract
     * @throws Exception if there's an error during daemon startup or execution
     */
    @Override
	public Object start(IApplicationContext context) throws Exception {
		String configPath = getConfigPath(context);
		if (!Path.of(configPath).getFileName().toString().equals("config.json")) {
			daemonProgressReporter.showProgress("Error: Invalid config file path: " + configPath);
			terminate(true);
		}

		if (!ConfigurationManager.loadConfig(configPath, daemonProgressReporter)) {
			daemonProgressReporter.showProgress("Error: Unable to load config file.");
			terminate(true);
		}

		File currentProjectDirectory = new File(ConfigurationManager.config.SOURCE);
		if (!currentProjectDirectory.isDirectory()) {
			daemonProgressReporter.showProgress("ERROR: Project path is not a directory");
			terminate(true);
		}
		// Create the class paths needed for eclipse.
		if (ClassPathUtility.createClassPath(ConfigurationManager.config.SOURCE)) {
			daemonProgressReporter.showProgress("Class path created...");
		} else {
			daemonProgressReporter.showProgress("Error: Unable to create class path.");
			terminate(true);
		}
		// Create Eclipse File System Model
		try {
			Model.useModel(new EclipseModel(currentProjectDirectory));
		} catch (CoreException e) {
			e.printStackTrace();
		}

		try {
			new CallGraphPassCommand().run(daemonProgressReporter);
			synchronized (lock) {
				lock.wait();
			}

			terminate(false);
		} catch (Exception | Error e) {
			e.printStackTrace();
			terminate(true);
		}
		return null;
	}

    /**
     * Stops the Eclipse application gracefully.
     * 
     * <p>This method is called by Eclipse when the application is requested to stop.
     * It initiates a clean shutdown of the daemon without indicating an error condition.</p>
     * 
     * @see #terminate(boolean)
     */
    @Override
    public void stop() {
        terminate(false);
    }
    
    /**
     * Parses command line arguments to find the configuration file path.
     * 
     * <p>This method searches for the <code>-config="&lt;path&gt;"</code> argument in the
     * command line arguments. If found, it returns the specified path. If no such argument
     * is provided, it defaults to <code>./config.json</code>.</p>
     * 
     * @param context the Eclipse application context containing command line arguments
     * @return the path to the configuration file, never {@code null}
     * @see #start(IApplicationContext)
     */
	private String getConfigPath(IApplicationContext context) {
		String configPath = "config.json";

		@SuppressWarnings("unchecked")
		Map<Object, Object> m = context.getArguments();
		if (m.isEmpty()) {
			return configPath;
		}

		String[] args = null;
		if (m.containsKey("application.args")) {
			args = (String[]) m.get("application.args");
		}

		if (args == null) {
			return configPath;
		}

		for (String arg : args) {
			if (arg.startsWith("-config=")) {
				String conf = arg.split("=")[1];
				if (!conf.isBlank()) {
					return conf;
				}
			}
		}
		return configPath;
	}
        
    /**
     * Terminates the daemon with appropriate shutdown behavior.
     * 
     * <p>This method displays a shutdown message and exits the system with an appropriate
     * exit code. If an error occurred during execution, it exits with code 1 (error).
     * Otherwise, it exits with code 0 (success).</p>
     * 
     * <p>This method is called in several scenarios:</p>
     * <ul>
     *   <li>When the daemon completes successfully</li>
     *   <li>When an error occurs during startup or execution</li>
     *   <li>When the Eclipse application is stopped</li>
     * </ul>
     * 
     * @param hasError {@code true} if an error occurred during execution, {@code false} otherwise
     */
	private void terminate(boolean hasError) {
		daemonProgressReporter.showProgress("Shutting down daemon...");
		if (hasError) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
}
