/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.ExtendedCallGraph;
import org.openrefactory.analysis.callgraph.MultiThreadCallGraphProcessor;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.capslock.Site;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModel;
import org.openrefactory.util.manager.C2PManager;
import org.openrefactory.util.manager.C2SManager;
import org.openrefactory.util.manager.FNDSpecManager;
import org.openrefactory.util.manager.SpecialRootSpecManager;
import org.openrefactory.util.progressreporter.IProgressReporter;
import org.openrefactory.util.progressreporter.NullProgressReporter;
import org.openrefactory.util.test.GeneralTestSuiteFromMarkers;
import org.openrefactory.util.test.JUnitCommandLineTestCase;
import org.openrefactory.util.test.JavaTestUtility;
import org.openrefactory.util.test.MarkerUtil;
import org.openrefactory.util.test.TestUtility;

import junit.framework.Test;

/**
 * Issue 6
 * A test suite for testing the extended call graph output
 *
 * @author Munawar Hafiz
 */
public class ExtendedCallGraphTestSuite extends GeneralTestSuiteFromMarkers {

    private static final File DIRECTORY = new File("callgraph-tests" + File.separator);

    private String COMMA_PLACEHOLDER = "@@@";

    public static Test suite() throws Exception {
        return new ExtendedCallGraphTestSuite();
    }

    public ExtendedCallGraphTestSuite() throws Exception {
        super("Running extended call graph tests", "/*$$$$$", JavaTestUtility.MARKER_END, DIRECTORY,
                JavaTestUtility.JAVA_FILENAME_FILTER);
    }

    @Override
    protected Test createTestFor(File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        return new ExtendedCallGraphTestCase(fileContainingMarker, markerOffset, markerText);
    }

    public class ExtendedCallGraphTestCase extends JUnitCommandLineTestCase {
        private File file;

        private String markerText;

        public ExtendedCallGraphTestCase(File file, int markerOffset, String markerText) {
            super("test");
            this.file = file;
            this.markerText = markerText;
        }

        /*
         * Marker = caller, number of callsites, [callsite (line, column), no of callees, set of callees ]
         * Set of callers/callees are comma separated method signature strings.
         */
         public void test() throws Exception {
            try {
                JavaVPG javaVPG = JavaVPG.getInstance();
                // Initialize work space and clear all ASTs
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                Model.getInstance().deinitialize();
                root.delete(true, true, null);
                JavaVPG.getInstance().releaseAllASTs();
                // Copy all the files in the project directory into a temp directory
                // and conduct the calculations there
                String rootPath = root.getRawLocation().toOSString();
                File rootFile = new File(rootPath);
                String projectPath = rootFile.getAbsolutePath() + File.separator + "TestProject";
                ConfigurationManager.loadConfigForTest(projectPath);
                File absoluteFile = file.getAbsoluteFile();
                File temp = file.getAbsoluteFile();

                temp = TestUtility.getTestContainerDirectory(temp);
                File copiedFileContainingMarker = TestUtility.copyFolder(temp,
                    new File(projectPath + File.separator + absoluteFile.getName()), absoluteFile);
                // Create Eclipse File System Model
                try {
                    Model.useModel(new EclipseModel(
                           new File(projectPath + File.separator + copiedFileContainingMarker.getName())));
                } catch (CoreException e) {
                    e.printStackTrace();
                }
                javaVPG.releaseAllASTs();

                LinkedList<String> markers = MarkerUtil.parseMarker(markerText);
                String expectedCaller = markers.removeFirst().replaceAll(COMMA_PLACEHOLDER, ",");

                int expectedCallsCount = Integer.parseInt(markers.removeFirst());
                Map<Site, Set<String>> expectedCalls = new HashMap<>();
                String fileName = absoluteFile.getName();
                for (int i = 0; i < expectedCallsCount; i++) {
                    int offset = Integer.parseInt(markers.removeFirst());
                    int length = Integer.parseInt(markers.removeFirst());
                    Site site = new Site(fileName, fileName, offset, length);
                    int noOfCallees = Integer.parseInt(markers.removeFirst());
                    Set<String> expectedCallees = new HashSet<>(noOfCallees);
                    for (int j = 0; j < noOfCallees; j++) {
                    	expectedCallees.add(markers.removeFirst().replaceAll(COMMA_PLACEHOLDER, ","));
                    }
                    expectedCalls.put(site, expectedCallees);
                }
                
                IProgressReporter progressReporter = new NullProgressReporter();
                CallGraphDataStructures.initialize();
                C2PManager.loadC2PInfo(progressReporter);
                C2SManager.loadC2SInfo(progressReporter);
                FNDSpecManager.loadFNDSpecInfo(progressReporter);
                SpecialRootSpecManager.loadSpecsFromJson(progressReporter);

                MultiThreadCallGraphProcessor.BuildAndProcessCallGraph(true, progressReporter, projectPath);
                ExtendedCallGraph callGraph = CallGraphDataStructures.getExtendedCallGraph();
                // Null check
                assertNotNull("In " + file + ", Expected a call graph, but found null", callGraph);
                // Match the caller
                boolean matched = callGraph.getAllCallers().contains(expectedCaller);

				if (expectedCallsCount == 0) {
					assertFalse("In " + file + ", The following caller should not be present: " + expectedCaller
							+ ", but it was.", matched);
					return;
				} else {
					assertTrue("In " + file + ", Expected the following caller " + expectedCaller
							+ ", but did not find it.", matched);
				}
				// Get the callsite and callees from the only caller in the map
				Map<Site, Set<String>> actualCalls = callGraph.getCalleesWithCallsites(expectedCaller);
				// Match the number of callees for the caller
				assertEquals("In " + file + ", For caller " + expectedCaller + ", number of method calls do not match.",
						expectedCalls.size(), actualCalls.size());
				// Match each callee
				for (Entry<Site, Set<String>> expectedCall : expectedCalls.entrySet()) {
					assertTrue(
							"In " + file + ", For caller " + expectedCaller + ", expected callsite "
									+ expectedCall.getKey() + " is not found in the actual callgraph: " + actualCalls,
							actualCalls.containsKey(expectedCall.getKey()));

					assertEquals(
							"In " + file + ", For caller " + expectedCaller + ", for callsite: " + expectedCall.getKey()
									+ " callees do not match.",
							expectedCall.getValue(), actualCalls.get(expectedCall.getKey()));
				}
            } catch (Exception e) {
                throw new Exception("For file: " + file, e);
            }
        }
    }
}
