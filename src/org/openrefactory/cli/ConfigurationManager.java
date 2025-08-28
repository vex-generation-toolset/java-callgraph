/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Contains the configuration for the iCR. Provides utility methods to load and access configuration data.
 *
 * @author Rifat Rubayatul Islam
 */
public class ConfigurationManager {

    /**
     * Holds all the information provided in the config.json file
     *
     * @author Rifat Rubayatul Islam
     */
    public static class Configuration {
        /** Path to directory that represents the source root. */
        public final String SOURCE;

        /** Path to directory that holds the resultant JSON files after refactorings are complete. */
        public final String RESULT;

        /** Path to the summaries directory */
        public final String SUMMARIES;

        /** Debug flag, enables additional debug information to be printed to console/logs. */
        public final boolean DEBUG;

        public Configuration(String source, String result, String summaries, boolean debug) {
            SOURCE = source;
            RESULT = result;
            SUMMARIES = summaries;
            DEBUG = debug;
        }

        /**
         * Utility method to print configuration information
         *
         * @param progressReporter the progress reporter where info will be dumped
         */
        public void print(IProgressReporter progressReporter) {
            progressReporter.showProgress("source : " + SOURCE);
            progressReporter.showProgress("result : " + RESULT);
            progressReporter.showProgress("summaries : " + SUMMARIES);
            progressReporter.showProgress("debug : " + DEBUG);
        }
    }

    public static Configuration config;

    // Differentiates a test run from a regular run on a project
    // During a normal project run, we will store the CG info on disk
    // During a test run, we do not want to store info on disk.
    public static boolean isTestRun = false;

    /**
     * Loads configuration info from config.json to the config
     *
     * @param configPath path to the config.json file
     * @param progressReporter the progress reporter
     * @return true if config successfully loaded, false otherwise.
     */
    public static boolean loadConfig(String configPath, IProgressReporter progressReporter) {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            progressReporter.showProgress("File does not exist: " + configFile.getAbsolutePath());
            return false;
        }

        progressReporter.showProgress("Updating config information...");
        try (Reader in = new BufferedReader(new FileReader(configFile))) {
            JSONObject configObj = new JSONObject(new JSONTokener(in));
            String source = configObj.getString("source");
            String result = configObj.getString("result");
            String summaries = configObj.getString("summaries");
            boolean debug = configObj.optBoolean("debug");

            config = new Configuration(source, result, summaries, debug);

            config.print(progressReporter);
            progressReporter.showProgress("Configuration info successfully updated.");
        } catch (IOException | JSONException e) {
            progressReporter.showProgress("Error occurred while loading configuration file.");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /** Method to load dummy configuration for test suite run. Only orHome and summaries are necessary to run tests. */
    public static void loadConfigForTest() {
        config = new Configuration("", "", "", false);
        isTestRun = true;
    }
}
