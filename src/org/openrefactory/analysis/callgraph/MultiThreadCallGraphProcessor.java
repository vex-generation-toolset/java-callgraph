/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openrefactory.analysis.callgraph.CallGraphProcessor.Phase;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.model.IModel;
import org.openrefactory.model.IModelFileElement;
import org.openrefactory.model.Model;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.manager.SpecialRootSpecManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * A multi-threaded processor engine for Call Graph construction.
 *
 * <p>The MultiThreadCallGraphProcessor is the main orchestrator that coordinates
 * the construction of call graphs using a multi-threaded approach. It implements
 * a sophisticated four-phase algorithm that processes Java source files in parallel
 * to build comprehensive call graphs efficiently.</p>
 * 
 * <p>The processor operates in four distinct phases:</p>
 * <ul>
 *   <li><strong>Phase 1:</strong> Parallel processing of all classes to populate
 *       class and field data structures</li>
 *   <li><strong>Phase 2:</strong> Single-threaded creation of class relationships
 *       and inheritance hierarchy population</li>
 *   <li><strong>Phase 3:</strong> Parallel processing for inner class relationships
 *       and type information calculation</li>
 *   <li><strong>Phase 4:</strong> Parallel processing for method analysis and
 *       caller-callee relationship creation</li>
 * </ul>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Thread Management:</strong> Automatic thread pool sizing based on
 *       available processors and file count</li>
 *   <li><strong>Progress Tracking:</strong> Comprehensive progress reporting throughout
 *       the construction process</li>
 *   <li><strong>File Caching:</strong> Intelligent handling of cached vs. uncached files
 *       for optimal performance</li>
 *   <li><strong>Error Handling:</strong> Robust error handling with graceful degradation</li>
 *   <li><strong>Root Method Detection:</strong> Automatic identification of entry points
 *       in the call graph</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class MultiThreadCallGraphProcessor {

        /** 
     * Default number of logical threads available to the JVM.
     * 
     * <p>This field is set to one less than the total available processors
     * to ensure the main thread has a processor to run on. This prevents
     * resource contention and ensures smooth operation of the main application.</p>
     */
    private static int DEFAULT_NUM_OF_THREADS = Runtime.getRuntime().availableProcessors() - 1;

    /** 
     * Sets the default number of threads for processing.
     * 
     * <p>This method ensures that the default thread count never becomes zero,
     * even on single-threaded systems. It's a safety measure to prevent
     * the system from attempting to create a thread pool with zero threads.</p>
     */
    private static void setDefaultThreads() {
        // Make sure DEFAULT_NUM_OF_THREADS don't become 0
        // in a single threaded system
        if (DEFAULT_NUM_OF_THREADS < 1) {
            DEFAULT_NUM_OF_THREADS = 1;
        }
    }

    /** 
     * Determines the optimal number of threads for processing a given number of files.
     * 
     * <p>This method implements a simple heuristic: if the number of files is less
     * than the default thread count, use only one thread to avoid overhead. Otherwise,
     * use the full complement of available threads for maximum parallelism.</p>
     *
     * @param numOfFiles the number of files to be processed
     * @return the optimal number of threads to use for processing
     */
    private static int getNumOfThreadsForFiles(int numOfFiles) {
        return numOfFiles < DEFAULT_NUM_OF_THREADS ? 1 : DEFAULT_NUM_OF_THREADS;
    }

    /** 
     * Constructs the Class Hierarchy Analysis (CHA) Call Graph using a four-phase approach.
     * 
     * <p>This method orchestrates the complete call graph construction process by
     * executing four distinct phases in sequence. Each phase builds upon the previous
     * one to create a comprehensive representation of method call relationships.</p>
     * 
     * <p>The four phases are:</p>
     * <ol>
     *   <li><strong>Phase 1:</strong> Parallel processing of all classes to populate
     *       basic class and field data structures</li>
     *   <li><strong>Phase 2:</strong> Single-threaded creation of class inheritance
     *       relationships and population of inherited fields</li>
     *   <li><strong>Phase 3:</strong> Parallel processing for inner class relationships
     *       and type information calculation</li>
     *   <li><strong>Phase 4:</strong> Parallel processing for method analysis and
     *       actual caller-callee relationship creation</li>
     * </ol>
     * 
     * <p>The method handles test file and protobuf file exclusion, processes cached
     * and uncached files separately for optimal performance, and provides comprehensive
     * progress reporting throughout the process.</p>
     *
     * @param progressReporter the progress reporter for tracking construction progress
     */
    private static void constructCHACallGraph(IProgressReporter progressReporter) {
        Iterable<IModelFileElement> files = Model.getInstance().getAllSourceFiles(IModel.JAVA_LANGUAGE);
        List<IModelFileElement> fileList = new ArrayList<>();
        // Adds all the elements of files to filelist
        files.forEach(fileList::add);
        CallGraphDataStructures.totalFiles = fileList.size();

        // Phase 1
        // Process all classes to populate the class and field data-structures
        progressReporter.showProgress("CALL GRAPH STAGE-1 STARTS");
        processFiles(fileList, progressReporter, Phase.PHASE_1);
        progressReporter.showProgress("CALL GRAPH STAGE-1 ENDS");

        // Remove the Java files for test cases from being considered
        int testFileCount = CallGraphDataStructures.sizeOfFilesToExcludeSet();
        CallGraphDataStructures.totalFiles -= testFileCount;
        // Remove the protobuf files too.
        CallGraphDataStructures.totalFiles -= CallGraphDataStructures.sizeOfProtobufFilesToExclude();
        progressReporter.showProgress("Excluding " + testFileCount + " test files. Now processing "
                + CallGraphDataStructures.totalFiles + " files.");

        // Phase 2
        // A single threaded phase
        // Create relationship between classes and populate inherited fields
        progressReporter.showProgress("CALL GRAPH STAGE-2 STARTS");
        createClassHierarchyAndPopulateFields(progressReporter);
        progressReporter.showProgress("CALL GRAPH STAGE-2 ENDS");

        // Phase 3
        // Process all classes to calculate inner class relationship
        // and also calculate type info for classes
        progressReporter.showProgress("CALL GRAPH STAGE-3 STARTS");

        // Process the cached files first
        // Then the uncached ones
        Pair<List<IModelFileElement>, List<IModelFileElement>> cachedAndUncachedFiles =
                JavaVPG.getInstance().separateCachedAndUncachedFiles(fileList);
        processFiles(cachedAndUncachedFiles.fst, progressReporter, Phase.PHASE_3);
        processFiles(cachedAndUncachedFiles.snd, progressReporter, Phase.PHASE_3);
        cachedAndUncachedFiles.fst.clear();
        cachedAndUncachedFiles.snd.clear();
        progressReporter.showProgress("CALL GRAPH STAGE-3 ENDS");

        // Phase 4
        // Process all methods to create actual caller-callee relationships
        progressReporter.showProgress("CALL GRAPH STAGE-4 STARTS");
        // Process the cached files first
        // Then the uncached ones
        cachedAndUncachedFiles = JavaVPG.getInstance().separateCachedAndUncachedFiles(fileList);
        processFiles(cachedAndUncachedFiles.fst, progressReporter, Phase.PHASE_4);
        processFiles(cachedAndUncachedFiles.snd, progressReporter, Phase.PHASE_4);
        cachedAndUncachedFiles.fst.clear();
        cachedAndUncachedFiles.snd.clear();
        progressReporter.showProgress("CALL GRAPH STAGE-4 ENDS");
    }

    /**
     * Phase 2 of call graph processing: Creates relationships between classes and populates inherited fields.
     *
     * <p>This phase is single-threaded to ensure data consistency when building the inheritance
     * hierarchy. It processes both class inheritance and interface implementation relationships
     * in a recursive manner to establish the complete class hierarchy.</p>
     * 
     * <p>The method performs several key tasks:</p>
     * <ul>
     *   <li><strong>Class Hierarchy:</strong> Recursively follows extended superclasses to build
     *       the complete inheritance chain</li>
     *   <li><strong>Interface Hierarchy:</strong> Recursively follows implemented interfaces to
     *       establish interface inheritance relationships</li>
     *   <li><strong>Field Inheritance:</strong> Populates inherited fields from superclasses
     *       and interfaces into subclasses</li>
     *   <li><strong>Test File Exclusion:</strong> Automatically excludes subclasses of test files
     *       to maintain consistency</li>
     *   <li><strong>Progress Tracking:</strong> Provides detailed progress updates during processing</li>
     * </ul>
     * 
     * <p>This phase is critical for establishing the foundation upon which the later phases
     * build the actual call graph edges.</p>
     *
     * @param progressReporter the progress reporter for tracking processing status
     */
    private static void createClassHierarchyAndPopulateFields(IProgressReporter progressReporter) {
        Set<String> visitedClasses = new HashSet<>();
        Set<String> visitedInterfaces = new HashSet<>();
        Set<String> subClassSet = new HashSet<>();

        final int totalWork = CallGraphDataStructures.getSuperClassMap().size()
                + CallGraphDataStructures.getDirectlyImplementedInterfaces().size();
        int completed = 0;

        // From a class, recursively follow the extended super classes
        for (String currentClass : CallGraphDataStructures.getSuperClassMap().keySet()) {
            subClassSet.add(currentClass);
            try {
                processSuperClass(currentClass, visitedClasses, visitedInterfaces, subClassSet);
            } catch (Exception | Error e) {
                // Move on to process next class
                e.printStackTrace();
            }
            subClassSet.clear();
            progressReporter.showProgress("CALL GRAPH STAGE-2: Completed " + (++completed) + " out of " + totalWork);
        }

        // From a class, recursively follow the implemented interfaces
        for (String currentClass :
                CallGraphDataStructures.getDirectlyImplementedInterfaces().keySet()) {
            subClassSet.add(currentClass);
            try {
                processSuperInterfaces(currentClass, visitedInterfaces, subClassSet);
            } catch (Exception | Error e) {
                // Move on to process next class
                e.printStackTrace();
            }
            subClassSet.clear();
            progressReporter.showProgress("CALL GRAPH STAGE-2: Completed " + (++completed) + " out of " + totalWork);
        }
    }

    /**
     * Recursively follows the superclass hierarchy starting from the immediately extended superclass.
     *
     * <p>This method implements a depth-first traversal of the class inheritance hierarchy.
     * It recursively processes each superclass to build the complete inheritance chain and
     * establish relationships between classes and their subclasses.</p>
     * 
     * <p>The method handles several important scenarios:</p>
     * <ul>
     *   <li><strong>Recursive Processing:</strong> Follows the inheritance chain until reaching
     *       a class with no superclass (root of hierarchy)</li>
     *   <li><strong>Test File Propagation:</strong> Automatically excludes subclasses of test files
     *       to maintain consistency in the analysis</li>
     *   <li><strong>Interface Processing:</strong> Processes interfaces implemented by superclasses
     *       to establish complete interface hierarchies</li>
     *   <li><strong>Field Inheritance:</strong> Populates inherited fields from superclasses
     *       into subclasses</li>
     *   <li><strong>Subclass Tracking:</strong> Maintains a set of all subclasses encountered
     *       during traversal for relationship establishment</li>
     * </ul>
     * 
     * <p>The method uses visited sets to prevent infinite recursion in case of circular
     * inheritance relationships.</p>
     *
     * @param currentClass the hash for the current class being processed
     * @param visitedClasses the set of already visited classes to prevent recursion
     * @param visitedInterfaces the set of already visited interfaces to prevent recursion
     * @param subClassesSet the set of subclasses encountered during traversal
     */
    private static void processSuperClass(
            String currentClass, Set<String> visitedClasses, Set<String> visitedInterfaces, Set<String> subClassesSet) {
        if (!visitedClasses.contains(currentClass)) {
            visitedClasses.add(currentClass);
            String superClass = CallGraphDataStructures.getSuperClassMap().get(currentClass);
            // If superclass is a JUnit test file, then subclass will also be a test file.
            // Exclude the subclass too.
            String superClassFilePath = CallGraphUtility.getFileNameFromHash(superClass);
            if (superClassFilePath != null) {
                if (CallGraphDataStructures.isExcludedFile(superClassFilePath)) {
                    String currentClassFilePath = CallGraphUtility.getFileNameFromHash(currentClass);
                    if (currentClassFilePath != null) {
                        CallGraphDataStructures.addFileToExcludedList(currentClassFilePath);
                    }
                }
            }
            if (CallGraphDataStructures.getSuperClassMap().containsKey(superClass)) {
                // Immediate super class extends other class(s)
                // process that first
                subClassesSet.add(superClass);
                processSuperClass(superClass, visitedClasses, visitedInterfaces, subClassesSet);
                if (CallGraphDataStructures.getDirectlyImplementedInterfaces().containsKey(superClass)) {
                    // Super class may also implement other interface(s)
                    // Process them too.
                    // This is a bit odd that we are asking for the implemented interface from the super class
                    // but not the current class. This is because the current class's interfaces will be processed
                    // by the driver method of this recursive method
                    processSuperInterfaces(superClass, visitedInterfaces, subClassesSet);
                }
                subClassesSet.remove(superClass);
                // Get the class hierarchy of immediate super class,
                // Add immediate super class
                // Populate info for current class
                Pair<Set<Integer>, Set<Integer>> superClassesAndInterfaces =
                        CallGraphDataStructures.getClassHashToSuperClassesAndReachableInterfaces()
                                .get(superClass);
                if (superClassesAndInterfaces != null) {
                    Set<Integer> superClasses = new HashSet<>();
                    superClasses.add(CallGraphDataStructures.updateOrGetBitIndexFromClassHash(superClass));
                    superClasses.addAll(superClassesAndInterfaces.fst);
                    CallGraphDataStructures.addToClassHashToReachableSuper(currentClass, superClasses, false);
                    CallGraphDataStructures.addToClassHashToReachableSuper(
                            currentClass, superClassesAndInterfaces.snd, true);
                    Iterator<Integer> iterator = superClassesAndInterfaces.fst.iterator();
                    while (iterator.hasNext()) {
                        String classHash = CallGraphDataStructures.getClassHashFromBitIndex(iterator.next());
                        for (String subClass : subClassesSet) {
                            CallGraphDataStructures.addToAllReachableSubClassesMap(
                                    classHash, CallGraphDataStructures.updateOrGetBitIndexFromClassHash(subClass));
                        }
                    }

                    iterator = superClassesAndInterfaces.snd.iterator();
                    while (iterator.hasNext()) {
                        String classHash = CallGraphDataStructures.getClassHashFromBitIndex(iterator.next());
                        for (String subClass : subClassesSet) {
                            CallGraphDataStructures.addToAllReachableSubClassesMap(
                                    classHash, CallGraphDataStructures.updateOrGetBitIndexFromClassHash(subClass));
                        }
                    }
                }
            } else {
                // Super class doesn't extend any other class
                // Found the root, populate info
                Set<Integer> superClasses = new HashSet<>();
                superClasses.add(CallGraphDataStructures.updateOrGetBitIndexFromClassHash(superClass));
                CallGraphDataStructures.addToClassHashToReachableSuper(currentClass, superClasses, false);
            }
            // Add sub class information
            for (String subClass : subClassesSet) {
                // All Java classes implicitly extend the java.lang.Object class, but we do not
                // want to add all class's to its sub class.
                // So, we are excluding the subClasses entry for the java.lang.Object class.
                if (!superClass.equals(Constants.JAVA_LANG_OBJECT)) {
                    CallGraphDataStructures.addToAllReachableSubClassesMap(
                            superClass, CallGraphDataStructures.updateOrGetBitIndexFromClassHash(subClass));
                }
            }
            populateFields(currentClass, superClass);
        }
    }

    /**
     * Recursively follows the interface hierarchy starting from immediately implemented interfaces.
     *
     * <p>This method implements a depth-first traversal of the interface inheritance hierarchy.
     * It recursively processes each super-interface to build the complete interface hierarchy
     * and establish relationships between interfaces and their implementing classes.</p>
     * 
     * <p>The method handles several important scenarios:</p>
     * <ul>
     *   <li><strong>Recursive Processing:</strong> Follows the interface inheritance chain until
     *       reaching an interface with no super-interfaces (root of hierarchy)</li>
     *   <li><strong>Test File Propagation:</strong> Automatically excludes sub-interfaces and
     *       implementing classes of test file interfaces</li>
     *   <li><strong>Multiple Interface Support:</strong> Processes all interfaces directly
     *       implemented by a class</li>
     *   <li><strong>Field Inheritance:</strong> Populates inherited fields from interfaces
     *       into implementing classes</li>
     *   <li><strong>Subclass Tracking:</strong> Maintains a set of all subclasses encountered
     *       during traversal for relationship establishment</li>
     * </ul>
     * 
     * <p>The method uses visited sets to prevent infinite recursion in case of circular
     * interface inheritance relationships.</p>
     *
     * @param currentClassOrInterface the hash for the current class or interface being processed
     * @param visitedInterfaces the set of already visited interfaces to prevent recursion
     * @param subClassesSet the set of subclasses encountered during traversal
     */
    private static void processSuperInterfaces(
            String currentClassOrInterface, Set<String> visitedInterfaces, Set<String> subClassesSet) {
        if (!visitedInterfaces.contains(currentClassOrInterface)) {
            visitedInterfaces.add(currentClassOrInterface);
            Set<String> directlyImplementedSuperInterfaces =
                    CallGraphDataStructures.getDirectlyImplementedInterfaces().get(currentClassOrInterface);
            // One class can implement multiple interfaces
            // Need to visit them all.
            for (String superInterface : directlyImplementedSuperInterfaces) {
                // If super-interface is a JUnit test file, then sub-interface and subclass will also be a test file.
                // Exclude those sub-interfaces and subclasses too.
                String superIntefaceFilePath = CallGraphUtility.getFileNameFromHash(superInterface);
                if (superIntefaceFilePath != null) {
                    if (CallGraphDataStructures.isExcludedFile(superIntefaceFilePath)) {
                        String currentClassOrInterfaceFilePath =
                                CallGraphUtility.getFileNameFromHash(currentClassOrInterface);
                        if (currentClassOrInterfaceFilePath != null) {
                            CallGraphDataStructures.addFileToExcludedList(currentClassOrInterfaceFilePath);
                        }
                    }
                }
                if (CallGraphDataStructures.getDirectlyImplementedInterfaces().containsKey(superInterface)) {
                    // Directly implemented super interface extends other interface(s)
                    // process that first
                    subClassesSet.add(superInterface);
                    processSuperInterfaces(superInterface, visitedInterfaces, subClassesSet);
                    subClassesSet.remove(superInterface);
                    // Get the interface hierarchy of directly implemented super interface,
                    // Add the directly implemented super interface
                    // Populate info for current class/ interface
                    Pair<Set<Integer>, Set<Integer>> superClassesAndInterfaces =
                            CallGraphDataStructures.getClassHashToSuperClassesAndReachableInterfaces()
                                    .get(superInterface);
                    Set<Integer> superInterfaces = new HashSet<>();
                    superInterfaces.add(CallGraphDataStructures.updateOrGetBitIndexFromClassHash(superInterface));
                    superInterfaces.addAll(superClassesAndInterfaces.snd);

                    CallGraphDataStructures.addToClassHashToReachableSuper(
                            currentClassOrInterface, superInterfaces, true);
                    Iterator<Integer> iterator = superClassesAndInterfaces.snd.iterator();
                    while (iterator.hasNext()) {
                        String classHash = CallGraphDataStructures.getClassHashFromBitIndex(iterator.next());
                        for (String subClass : subClassesSet) {
                            CallGraphDataStructures.addToAllReachableSubClassesMap(
                                    classHash, CallGraphDataStructures.updateOrGetBitIndexFromClassHash(subClass));
                        }
                    }
                } else {
                    // Super interface doesn't extend any other interface
                    // Found the root, populate info
                    Set<Integer> superInterfaces = new HashSet<>();
                    superInterfaces.add(CallGraphDataStructures.updateOrGetBitIndexFromClassHash(superInterface));
                    CallGraphDataStructures.addToClassHashToReachableSuper(
                            currentClassOrInterface, superInterfaces, true);
                }
                // Add sub class information
                for (String subClass : subClassesSet) {
                    // All Java classes implicitly extend the java.lang.Object class, but we do not
                    // want to add all class's to its sub class.
                    // So, we are excluding the subClasses entry for the java.lang.Object class.
                    if (!superInterface.equals(Constants.JAVA_LANG_OBJECT)) {
                        CallGraphDataStructures.addToAllReachableSubClassesMap(
                                superInterface, CallGraphDataStructures.updateOrGetBitIndexFromClassHash(subClass));
                    }
                }
                populateFields(currentClassOrInterface, superInterface);
            }
        }
    }

    /**
     * Adds inherited fields from a superclass or interface into the current class.
     *
     * <p>This method implements field inheritance by copying non-private fields from
     * superclasses and interfaces into their subclasses. The order of fields is
     * determined by the inheritance hierarchy, with superclass fields appearing
     * before subclass fields.</p>
     * 
     * <p>The method handles several important aspects:</p>
     * <ul>
     *   <li><strong>Access Modifier Filtering:</strong> Only non-private fields are
     *       inherited, maintaining proper encapsulation</li>
     *   <li><strong>Field Ordering:</strong> Fields are added in the correct order
     *       based on the inheritance hierarchy</li>
     *   <li><strong>Null Safety:</strong> Handles cases where superclasses may not
     *       have any fields</li>
     *   <li><strong>Token Range Preservation:</strong> Maintains the original token
     *       ranges for accurate source code mapping</li>
     * </ul>
     * 
     * <p>This method is essential for establishing the complete field set that
     * each class can access, which is crucial for accurate method resolution
     * and call graph construction.</p>
     *
     * @param currentClass the hash of the current class receiving inherited fields
     * @param superClass the hash of the superclass providing the inherited fields
     */
    private static void populateFields(String currentClass, String superClass) {
        List<TokenRange> fieldRange = new ArrayList<>();
        List<TokenRange> superClassFields = CallGraphDataStructures.getFieldsFromContainerHash(superClass);
        if (superClassFields != null) {
            for (TokenRange superFieldTokenRange : superClassFields) {
                FieldInfo fieldInfo = CallGraphDataStructures.getFieldInfoFromTokenRange(superFieldTokenRange);
                if (!fieldInfo.isPrivate()) {
                    // Add the non private fields of the super class
                    fieldRange.add(superFieldTokenRange);
                }
            }
        }
        CallGraphDataStructures.addSuperClassFieldsInContainerHashToFieldList(currentClass, fieldRange);
    }

    /**
     * Initiates the CallGraphProcessor to process files in parallel based on the specified phase.
     *
     * <p>This method creates a thread pool and distributes file processing across multiple
     * threads to maximize parallelism. Each file is processed by a separate CallGraphProcessor
     * instance configured for the specified phase.</p>
     * 
     * <p>The method implements several optimization strategies:</p>
     * <ul>
     *   <li><strong>Dynamic Thread Pool Sizing:</strong> Automatically determines the optimal
     *       number of threads based on file count and available processors</li>
     *   <li><strong>File-Level Parallelism:</strong> Each file is processed independently
     *       in its own thread to maximize throughput</li>
     *   <li><strong>Phase-Specific Processing:</strong> Each processor is configured for
     *       the specific call graph construction phase</li>
     *   <li><strong>Resource Management:</strong> Proper thread pool shutdown and cleanup
     *       to prevent resource leaks</li>
     *   <li><strong>Error Handling:</strong> Graceful handling of execution exceptions
     *       with detailed error reporting</li>
     * </ul>
     * 
     * <p>The method waits for all threads to complete before returning, ensuring
     * that all files are processed before proceeding to the next phase.</p>
     *
     * @param fileList the complete list of Java files in the project to process
     * @param progressReporter the progress reporter for tracking processing status
     * @param phase the call graph construction phase to execute
     */
    private static void processFiles(
            List<IModelFileElement> fileList, IProgressReporter progressReporter, Phase phase) {

        int numOfFiles = fileList.size();
        int threadCount = getNumOfThreadsForFiles(numOfFiles);
        // Separate thread will be created to process each file,and will
        // pushed into this threadPool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (IModelFileElement file : fileList) {
            CallGraphProcessor callGraphProcessor = new CallGraphProcessor(progressReporter, phase);
            callGraphProcessor.setFile(file.getFullPath());
            Thread callGraphThread = new Thread(callGraphProcessor);
            try {
                executor.execute(callGraphThread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            progressReporter.showProgress("Time out for call-graph calculation");
            e.printStackTrace();
        }
    }

    /**
     * Builds and processes a static call graph using a four-phase algorithm.
     *
     * <p>This is the main entry point for call graph construction that delegates
     * to the full implementation with default settings. It provides a simplified
     * interface for call graph construction without requiring explicit configuration.</p>
     * 
     * <p>The method implements the complete four-phase call graph construction process:</p>
     * <ol>
     *   <li><strong>Phase 1:</strong> Process all classes to populate class and field data structures</li>
     *   <li><strong>Phase 2:</strong> Create relationships between classes and populate inherited fields</li>
     *   <li><strong>Phase 3:</strong> Process all classes to calculate inner class relationships and type info</li>
     *   <li><strong>Phase 4:</strong> Process all methods to create actual caller-callee relationships</li>
     * </ol>
     * 
     * <p>This method always attempts to load existing call graph data first, falling
     * back to fresh construction only if loading fails.</p>
     *
     * @param progressReporter the progress reporter for tracking construction progress
     * @param filePath the file path for the project being analyzed
     * @return true if the call graph was freshly calculated, false if it was loaded from disk
     */
    public static boolean BuildAndProcessCallGraph(IProgressReporter progressReporter, String filePath) {
        return BuildAndProcessCallGraph(false, progressReporter, filePath);
    }

    /**
     * Builds and processes a static call graph using a four-phase algorithm with configurable options.
     *
     * <p>This is the full implementation of call graph construction that provides complete
     * control over the construction process. It can either load existing call graph data
     * or build from scratch based on the buildFromScratch parameter.</p>
     * 
     * <p>The method implements the complete four-phase call graph construction process:</p>
     * <ol>
     *   <li><strong>Phase 1:</strong> Process all classes to populate class and field data structures</li>
     *   <li><strong>Phase 2:</strong> Create relationships between classes and populate inherited fields</li>
     *   <li><strong>Phase 3:</strong> Process all classes to calculate inner class relationships and type info</li>
     *   <li><strong>Phase 4:</strong> Process all methods to create actual caller-callee relationships</li>
     * </ol>
     * 
     * <p>Key features include:</p>
     * <ul>
     *   <li><strong>Conditional Construction:</strong> Can skip loading and force fresh construction</li>
     *   <li><strong>Library Integration:</strong> Automatically loads library type information</li>
     *   <li><strong>Root Method Calculation:</strong> Identifies entry points in the call graph</li>
     *   <li><strong>Performance Optimization:</strong> Clears temporary data structures after use</li>
     *   <li><strong>Comprehensive Reporting:</strong> Provides detailed progress and timing information</li>
     * </ul>
     *
     * @param buildFromScratch if true, builds the call graph from scratch without using old data
     * @param progressReporter the progress reporter for tracking construction progress
     * @param filePath the file path for the project being analyzed
     * @return true if the call graph was freshly calculated, false if it was loaded from disk
     */
    public static boolean BuildAndProcessCallGraph(
            boolean buildFromScratch, IProgressReporter progressReporter, String filePath) {
        boolean isFreshlyCalculated = false;
        setDefaultThreads();
        long startTime = System.currentTimeMillis();
        // Constructs JDT's call graph
        progressReporter.showProgress("-------- Initializing Call Graph --------");
        if (!buildFromScratch) {
            if (ConfigurationManager.config.DEBUG) {
                progressReporter.showProgress("Call Graph found on disk" + System.lineSeparator());
                progressReporter.showProgress("Loading Call Graph from disk" + System.lineSeparator());
                progressReporter.showProgress("Call Graph Loaded" + System.lineSeparator());
            }
            // Not freshly calculated
        } else {
            // We are reinitializing the Call Graph Data structures here
            // Because if data loading fails for some reason, some data structures
            // may be null, which will cause null pointer exception later
            CallGraphDataStructures.initialize();

            // Load the library method info after initialization
            // Otherwise these data will be cleared.
            progressReporter.showProgress("--------Loading Library Class Info--------");
            CallGraphDataStructures.addLibraryTypesToSubClassesAndReachableSuperMap();
            progressReporter.showProgress("--------Library Class Info Loaded--------");
            constructCHACallGraph(progressReporter);
            // Also clear the special root specs as root method
            // already calculated and will no longer be used.
            SpecialRootSpecManager.clearSpecialRootSpecs();
            // Clear the parametric class to typeInfo map, they are only used for CG phase 4
            CallGraphDataStructures.getParametricClassTypeInfoMap().clear();

            progressReporter.showProgress("Call Graph Construction Complete");
            // Previously we used to store call graph related data from here.
            // Currently, we are storing call graph data after framework analysis.
            isFreshlyCalculated = true;
        }

        if (ConfigurationManager.config.DEBUG) {
            CallGraphDataStructures.getCallGraph().printCallGraph();
            progressReporter.showProgress("_______________Root Methods______________" + System.lineSeparator());
            Iterator<String> rootMethodsIterator = CallGraphDataStructures.getIteratorForRootMethods();
            while (rootMethodsIterator.hasNext()) {
                String root = rootMethodsIterator.next();
                System.out.println(CallGraphDataStructures.getMethodSignatureFromHash(root));
            }
            CallGraphDataStructures.printObjectSizes(progressReporter);
            progressReporter.showProgress("root methods size: " + CallGraphDataStructures.getRootCount());
            long endTime = System.currentTimeMillis();
            progressReporter.showProgress(
                    "call graph size: " + CallGraphDataStructures.getCallGraph().getSize());
            progressReporter.showProgress("execution time: " + Double.toString((endTime - startTime) / 1000) + "s");
        }
        return isFreshlyCalculated;
    }
}
