/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.test;

import java.io.File;
import java.text.Collator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.MultiThreadCallGraphProcessor;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModel;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.datastructure.IntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.progressreporter.NullProgressReporter;
import org.openrefactory.util.test.GeneralTestSuiteFromMarkers;
import org.openrefactory.util.test.JUnitCommandLineTestCase;
import org.openrefactory.util.test.JavaTestUtility;
import org.openrefactory.util.test.MarkerUtil;
import org.openrefactory.util.test.TestUtility;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Issue 91 A test suite for testing the Call Graph
 *
 * @author Mohammad Rafid Ul Islam
 */
public class CallGraphTestSuite extends GeneralTestSuiteFromMarkers {

    private static final File DIRECTORY = new File("callgraph-tests" + File.separator);

    public static Test suite() throws Exception {
        return new CallGraphTestSuite();
    }

    public CallGraphTestSuite() throws Exception {
        super(
                "Running call graph tests",
                JavaTestUtility.MARKER,
                JavaTestUtility.MARKER_END,
                DIRECTORY,
                JavaTestUtility.JAVA_FILENAME_FILTER);
    }

    @Override
    protected Test createTestFor(File fileContainingMarker, int markerOffset, String markerText) throws Exception {
        boolean shouldInclude = true;
        boolean failingTest = false;

        if (shouldInclude) {
            return new CallGraphTestCase(fileContainingMarker, markerOffset, markerText);
        } else {
            if (failingTest) {
                return new TestSuite("Skipping " + fileContainingMarker.getName()
                        + " intentionally since the feature has not been implemented");
            } else {
                return new TestSuite(
                        "Skipping " + fileContainingMarker.getName() + " since Multi config support disabled");
            }
        }
    }

    public class CallGraphTestCase extends JUnitCommandLineTestCase {
        private File file;

        private String markerText;

        public CallGraphTestCase(File file, int markerOffset, String markerText) {
            super("test");
            this.file = file;
            this.markerText = markerText;
        }

        /**
         * Gets method representation from JDT's method signature for test suite
         *
         * @param signature JDT's method signature
         * @return string representation of a method
         */
        private String getMethodRepFromMethodSignature(String signature) {
            String[] splits = signature.split(CallGraphUtility.CG_SEPARATOR);
            String methodRep = splits[6] + ":" + splits[1];
            return methodRep;
        }

        /**
         * Gets a sorted Set of callers' method signatures for a invocation of this method. The set is sorted in the
         * alphabetically in the ascending order.
         *
         * @param calleeHash hash of the callee
         * @return a set of hashes of the caller methods for that method
         */
        public Set<String> getCallers(String calleeHash) {
            List<String> callerHashes = CallGraphDataStructures.getCallGraph().getCallerListOf(calleeHash, false);
            if (callerHashes != null) {
                Set<String> callers = new TreeSet<String>(Collator.getInstance());
                for (String hash : callerHashes) {
                    if (CallGraphUtility.isDefaultConstructor(hash)) {
                        String methodName = CallGraphUtility.getMethodNameFromSignature(
                                CallGraphDataStructures.getMethodSignatureFromHash(hash));
                        String[] splits = methodName.split(CallGraphUtility.CG_SEPARATOR);
                        callers.add(splits[0] + ":" + splits[1] + "()V");
                    } else if (CallGraphUtility.isStaticConstructor(hash)) {
                        // Issue 1403
                        // For virtual static constructors we use suffix VSC
                        String methodName = CallGraphUtility.getMethodNameFromSignature(
                                CallGraphDataStructures.getMethodSignatureFromHash(hash));
                        String[] splits = methodName.split(CallGraphUtility.CG_SEPARATOR);
                        callers.add(splits[0] + ":" + splits[1] + "()VSC");
                    } else {
                        String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(hash);
                        MethodDeclaration caller = CallGraphUtility.getMethodFromMethodSignature(methodSignature);
                        IBinding callerBinding = caller.resolveBinding();
                        IMethod callerIMethod = CallGraphUtility.getIMethodFromBinding(callerBinding);
                        try {
                            String sign = callerIMethod.getSignature();
                            callers.add(getMethodRepFromMethodSignature(methodSignature) + sign);
                        } catch (JavaModelException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return callers;
            }
            return null;
        }

        /**
         * Gets a sorted Set of callees' method signatures for this method. The set is sorted in the alphabetically in
         * the ascending order.
         *
         * @param callerHash hash of the caller
         * @return a set of hashes of the callee methods from that method
         */
        public Set<String> getCallees(String callerHash) {
            // Issue 1228
            // Test case, we do not need the virtual connector method
            List<String> calleeHashes = CallGraphDataStructures.getCallGraph().getCalleeListOf(callerHash, false);
            Set<String> callees = new TreeSet<String>(Collator.getInstance());
            for (String hash : calleeHashes) {
                if (CallGraphUtility.isDefaultConstructor(hash)) {
                    String methodName = CallGraphUtility.getMethodNameFromSignature(
                            CallGraphDataStructures.getMethodSignatureFromHash(hash));
                    String[] splits = methodName.split(CallGraphUtility.CG_SEPARATOR);
                    callees.add(splits[0] + ":" + splits[1] + "()V");
                } else if (CallGraphUtility.isStaticConstructor(hash)) {
                    // Issue 1403
                    // For virtual static constructors we use suffix VSC
                    String methodName = CallGraphUtility.getMethodNameFromSignature(
                            CallGraphDataStructures.getMethodSignatureFromHash(hash));
                    String[] splits = methodName.split(CallGraphUtility.CG_SEPARATOR);
                    callees.add(splits[0] + ":" + splits[1] + "()VSC");
                } else {
                    String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(hash);
                    MethodDeclaration callee = CallGraphUtility.getMethodFromMethodSignature(methodSignature);
                    IBinding calleeBinding = callee.resolveBinding();
                    IMethod calleeIMethod = CallGraphUtility.getIMethodFromBinding(calleeBinding);
                    try {
                        String sign = calleeIMethod.getSignature();
                        callees.add(getMethodRepFromMethodSignature(methodSignature) + sign);
                    } catch (JavaModelException e) {
                        e.printStackTrace();
                    }
                }
            }
            return callees;
        }

        /*
         * Marker = startLine,startOffset,endLine,endOffset,choice {caller or callee), [list of callers/callees]
         * In place of list a number 0 is present if no caller or callee (introduced in Issue 1374)
         * Here, choice is what should be returned callers or callees of the selected
         * method.
         * List of callers/callees are comma separated method signature strings.
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
                File copiedFileContainingMarker = TestUtility.copyFolder(
                        temp, new File(projectPath + File.separator + absoluteFile.getName()), absoluteFile);
				// Create Eclipse File System Model
				try {
					Model.useModel(new EclipseModel(
							new File(projectPath + File.separator + copiedFileContainingMarker.getName())));
				} catch (CoreException e) {
					e.printStackTrace();
				}
                javaVPG.releaseAllASTs();

                CompilationUnit unit = javaVPG.acquireAST(projectPath
                        + File.separator
                        + copiedFileContainingMarker.getName()
                        + File.separator
                        + copiedFileContainingMarker.getName());

                LinkedList<String> markers = MarkerUtil.parseMarker(markerText);

                int startLine = Integer.parseInt(markers.removeFirst());
                int startOffset = Integer.parseInt(markers.removeFirst());
                int endLine = Integer.parseInt(markers.removeFirst());
                int endOffset = Integer.parseInt(markers.removeFirst());
                String choice = markers.removeFirst();
                IntPair positionPair = ASTNodeUtility.getPosition(unit, startLine, startOffset, endLine, endOffset);

                assertTrue("For file " + file.getName() + ", selection not correct", positionPair.fst >= 0);
                assertTrue("For file " + file.getName() + ", selection not correct", positionPair.snd >= 0);

                ASTNode selectedNode = findNode(unit, ASTNode.class, positionPair.fst, positionPair.snd);
                assertNotNull("For file " + file.getName() + ", the selection returned a null object", selectedNode);

                CallGraphDataStructures.initialize();
                MultiThreadCallGraphProcessor.BuildAndProcessCallGraph(new NullProgressReporter(), projectPath);

                Set<String> methods = null;
                if (selectedNode instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) selectedNode;
                    Pair<String, String> methodHashAndSig =
                            CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(method, null);
                    if (methodHashAndSig != null && methodHashAndSig.fst != null) {
                        if (choice.equals("caller")) {
                            methods = getCallers(methodHashAndSig.fst);
                        } else {
                            methods = getCallees(methodHashAndSig.fst);
                        }
                    }
                } else {
                    TokenRange tr = ASTNodeUtility.getTokenRangeFromNode(
                            selectedNode, copiedFileContainingMarker.getAbsolutePath());
                    Set<Integer> ss = CallGraphDataStructures.getCalleeCandidatesFor(tr);
                    if (ss != null && !ss.isEmpty()) {
                        methods = new TreeSet<String>(Collator.getInstance());
                        Iterator<Integer> iter = ss.iterator();
                        while (iter.hasNext()) {
                            int index = iter.next();
                            String hash = CallGraphDataStructures.getMethodHashFromIndex(index);
                            if (choice.equals("caller")) {
                                Set<String> callers = getCallers(hash);
                                if (callers != null) {
                                    methods.addAll(callers);
                                }
                            } else {
                                Set<String> callees = getCallees(hash);
                                if (callees != null) {
                                    methods.addAll(callees);
                                }
                            }
                        }
                    }
                }
                if (methods == null) {
                    int entries = Integer.parseInt(markers.removeFirst());
                    assertEquals(entries, 0);
                } else {
                    String actual = "";
                    for (String method : methods) {
                        actual += method + "##";
                    }
                    Set<String> expectedMethods = new TreeSet<String>(Collator.getInstance());
                    while (!markers.isEmpty()) {
                        String expectedMethod = markers.removeFirst();
                        expectedMethods.add(expectedMethod);
                    }
                    String expected = "";
                    for (String method : expectedMethods) {
                        expected += method + "##";
                    }
                    CallGraphDataStructures.deinitialize();
                    assertEquals(
                            "In " + file + " Failure",
                            expected.substring(0, expected.length() - 2).trim(),
                            actual.substring(0, actual.length() - 2).trim());
                }
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
