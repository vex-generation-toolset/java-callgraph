/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.test;

import java.io.File;
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
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModel;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.datastructure.TokenRange;
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
         * Marker = caller, number of callees, [set of callees]
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
                String expectedCaller = markers.removeFirst();
	            expectedCaller = expectedCaller.replaceAll(COMMA_PLACEHOLDER, ",");
                int calleeCount = Integer.parseInt(markers.removeFirst());
                Set<String> expectedCallees = new HashSet<String>(calleeCount);
                for (int i = 0; i < calleeCount; i++) {
                    String expectedCallee = markers.removeFirst();
                    expectedCallee = expectedCallee.replaceAll(COMMA_PLACEHOLDER, ",");
                    expectedCallees.add(expectedCallee);
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
                String matchedCallerHash = null;
                for (String callerHash : callGraph.getCallerToCalleeMap().keySet()) {
                    String callerName = CallGraphUtility.getMethodNameInCanonicalizedFormat(callerHash);
                    if (callerName.equals(expectedCaller)) {
                        matchedCallerHash = callerHash;
                        break;
                    }
                }
                if (calleeCount == 0) {
                    assertNull("In " + file + ", The following caller should not be present: " + expectedCaller
                            + ", but it was.", matchedCallerHash);
                    return;
                } else {
                    assertNotNull("In " + file + ", Expected the following caller " + expectedCaller
                            + ", but did not find it.", matchedCallerHash);
                }
                // Get the callsite and callees from the only caller in the map
                Map<TokenRange, Set<String>> foundCallsitesAndCallees = callGraph.getCallerToCalleeMap()
                        .get(matchedCallerHash);
                // First collect the callees
                Set<String> foundCallees = new HashSet<String>(2);
                for (Entry<TokenRange, Set<String>> entry : foundCallsitesAndCallees.entrySet()) {
                    for (String calleeHash : entry.getValue()) {
                        foundCallees.add(CallGraphUtility.getMethodNameInCanonicalizedFormat(calleeHash));
                    }
                }
                // Match the number of callees for the caller
				assertTrue(
						"In " + file + ", For the following caller " + expectedCaller + ", expected "
								+ expectedCallees.size() + " callees, but found " + foundCallees.size(),
						foundCallees.size() == expectedCallees.size());
                // Match each callee
                for (String expectedCallee : expectedCallees) {
					assertTrue(
							"In " + file + ", For the following caller " + expectedCaller
									+ ", expected the following callee: " + expectedCallee
									+ ", but found the following " + foundCallees,
							foundCallees.contains(expectedCallee));
                }
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
