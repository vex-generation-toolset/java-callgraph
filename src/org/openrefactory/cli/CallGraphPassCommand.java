/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.MultiThreadCallGraphProcessor;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.model.IModel;
import org.openrefactory.model.IModelFileElement;
import org.openrefactory.model.Model;
import org.openrefactory.util.manager.C2PManager;
import org.openrefactory.util.manager.C2SManager;
import org.openrefactory.util.manager.FNDSpecManager;
import org.openrefactory.util.manager.SpecialRootSpecManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Command class responsible for executing the call graph analysis pass.
 * 
 * <p>This class orchestrates the complete call graph generation process by:</p>
 * <ul>
 *   <li>Parsing Java source files using JDT (Java Development Tools)</li>
 *   <li>Loading various configuration and specification managers</li>
 *   <li>Building and processing the call graph using multi-threaded processing</li>
 *   <li>Storing the results</li>
 * </ul>
 * 
 * @author Munawar Hafiz
 */
public class CallGraphPassCommand {

    /** Executor for managing call graph analysis jobs */
    private static final Executor jobManager = Executors.newSingleThreadExecutor();

    public CallGraphPassCommand() {
    }
    
    /**
     * Initiates the call graph analysis process asynchronously.
     * 
     * <p>This method schedules the call graph analysis to run in a separate thread
     * using the job manager executor. The analysis includes source parsing, configuration
     * loading, call graph building, and result storage.</p>
     * 
     * @param progressReporter the progress reporter to track and display analysis progress
     */
    public void run(IProgressReporter progressReporter) {
        // Run refactoring pass in separate thread
        jobManager.execute(new CallGraphPass(progressReporter));
    }

    /**
     * Inner class that implements the actual call graph analysis process.
     * 
     * <p>This class encapsulates the complete workflow for generating call graphs,
     * including source parsing, configuration management, call graph computation,
     * and result persistence.</p>
     */
    private class CallGraphPass implements Runnable {
        
        /** Progress reporter for tracking analysis progress */
        IProgressReporter progressReporter;

        /**
         * Constructs a new CallGraphPass instance.
         * 
         * @param progressReporter the progress reporter to use for tracking progress
         */
		private CallGraphPass(IProgressReporter progressReporter) {
			this.progressReporter = progressReporter;
		}

        /**
         * Executes the complete call graph analysis workflow.
         * 
         * <p>This method performs the following steps in sequence:</p>
         * <ol>
         *   <li>Updates source files using JDT parsing</li>
         *   <li>Loads configuration information from various managers</li>
         *   <li>Builds and processes the call graph</li>
         *   <li>Notifies the daemon that processing is complete</li>
         * </ol>
         * 
         * <p>If any error occurs during processing, the application will exit with
         * error code 1.</p>
         */
        @Override
		public void run() {
			// Pull source using JDT
			sourceUpdate();
			// Load configs
			C2PManager.loadC2PInfo(progressReporter);
			C2SManager.loadC2SInfo(progressReporter);
			FNDSpecManager.loadFNDSpecInfo(progressReporter);
			SpecialRootSpecManager.loadSpecsFromJson(progressReporter);
			try {
				// Calculate call graph
				buildAndProcessCallGraph();

				synchronized (ORDaemon.lock) {
					ORDaemon.lock.notify();
				}
			} catch (Exception | Error e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

        /**
         * Parses and updates the Java source files using JDT.
         * 
         * <p>This method ensures that all Java source files in the configured source
         * directory are parsed and their Abstract Syntax Trees (ASTs) are available
         * for call graph analysis.</p>
         * 
         * <p>The working directory is displayed in the progress reporter for user
         * visibility.</p>
         */
		private void sourceUpdate() {
			File sourceDirectory = new File(ConfigurationManager.config.SOURCE);

			progressReporter.showProgress("Working directory: " + sourceDirectory.getPath());

			JavaVPG.getInstance().ensureASTParsing(progressReporter);
		}

		/**
		 * Builds and processes the call graph using multi-threaded processing.
		 * 
		 * <p>This method initializes the call graph data structures and then
		 * processes the call graph using the multi-threaded processor. After
		 * completion, it stores the results to persistent storage.</p>
		 * 
		 * <p>The call graph is built from the Java source files in source directory
		 * specified in the configuration.</p>
		 */
		private void buildAndProcessCallGraph() {
			CallGraphDataStructures.initialize();
			File sourceDirectory = new File(ConfigurationManager.config.SOURCE);
			MultiThreadCallGraphProcessor.BuildAndProcessCallGraph(true, progressReporter,
					sourceDirectory.getAbsolutePath());
			storeCallGraph();
		}

		/**
		 * Writes the call graph information to a JSON file.
		 * 
		 * <p>This method performs the following operations:</p>
		 * <ul>
		 *   <li>Filters out excluded files (test files, proto files, symbolic links)</li>
		 *   <li>Creates a timestamped result directory and file</li>
		 *   <li>Extracts the caller-to-callee mapping from the call graph</li>
		 *   <li>Serializes the data to JSON format with proper formatting</li>
		 * </ul>
		 * 
		 * <p>The output file is named with the pattern: <code>yyyyMMddHHmmss_cg.json</code></p>
		 * 
		 * <p>If no valid Java files are found, the application will exit with error code 1.</p>
		 */
		private void storeCallGraph() {
			Iterable<IModelFileElement> allFiles = Model.getInstance().getAllSourceFiles(IModel.JAVA_LANGUAGE);
			int totalFiles = 0;
			for (IModelFileElement file : allFiles) {
				// Determine when to skip files
				String fullPath = file.getFullPath();
				// Stop processing test files, example files and symbolic links
				// Stop processing auto generated protobuf files.
				if (CallGraphDataStructures.isExcludedFile(fullPath) || CallGraphDataStructures.isAProtoFile(fullPath)
						|| isUnderSymbolicLink(fullPath)) {
					continue;
				}
				totalFiles++;
			}
			if (totalFiles == 0) {
				progressReporter.showProgress("No java file found at SOURCE. Exiting...");
				System.exit(1);
			}
			// Create result directory and result file before running fixer for each file
			DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
			Date date = new Date();
			String timeStamp = df.format(date);
			File resultDir = new File(ConfigurationManager.config.RESULT);
			if (!resultDir.exists()) {
				resultDir.mkdirs();
			}

			String cgFileName = timeStamp + "_cg.json";
			File cgFile = Path.of(ConfigurationManager.config.RESULT, cgFileName).toFile();
			try (FileWriter fOut = new FileWriter(cgFile); BufferedWriter bw = new BufferedWriter(fOut)) {
				Map<String, Set<String>> callerToCalleeMap = CallGraphDataStructures.getExtendedCallGraph()
						.getCallerToCalleeMap();
				bw.write(new JSONObject(callerToCalleeMap).toString(4));
			} catch (Exception | Error e) {
				progressReporter.showProgress("Failed to generate cg file: " + e.getMessage());
			}

			progressReporter.showProgress("Call graph generation complete");
		}
        
        /**
         * Checks whether the file path is under a symbolic link.
         * 
         * <p>This method traverses up the directory hierarchy from the given file path
         * to the root directory, checking each parent directory for symbolic links.
         * If any symbolic link is encountered during traversal, the method returns true.</p>
         * 
         * <p>Symbolic links are excluded from call graph analysis to avoid processing
         * files that may point to external or dynamically changing locations.</p>
         * 
         * @param filePathStr the file path to check for symbolic link ancestry
         * @return {@code true} if the file path is under a symbolic link, {@code false} otherwise
         */
		private boolean isUnderSymbolicLink(String filePathStr) {
			try {
				Path filePath = Paths.get(filePathStr);
				// Loop until reaching the root directory or encountering a symbolic link
				while (filePath != null) {
					if (Files.isSymbolicLink(filePath)) {
						return true;
					}
					filePath = filePath.getParent();
				}
				// If reached here, all parent directories
				// have been checked without finding any symbolic link
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
        
    }
}
