/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.openrefactory.analysis.callgraph.method.MethodHandler;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.callgraph.method.MethodInvocationCalculationScratchPad;
import org.openrefactory.analysis.callgraph.method.MethodInvocationType;
import org.openrefactory.analysis.callgraph.method.MethodMatchFinderUtil;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.analysis.type.typeinfo.ParameterizedTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.SymbolicTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.AnnotationUtility;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.NeoLRUCache;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Constructs Call Graph using JDT's Class Hierarchy Analysis (CHA) implementation.
 *
 * <p>The CallGraphProcessor is the core engine responsible for building call graphs
 * from Java source code. It processes compilation units in multiple phases to construct
 * a comprehensive representation of method call relationships throughout a Java program.</p>
 * 
 * <p>The processor operates in four distinct phases:</p>
 * <ul>
 *   <li><strong>Phase 1:</strong> Basic class information, inheritance relationships, and field analysis</li>
 *   <li><strong>Phase 2:</strong> Single-threaded inheritance hierarchy population and field inheritance</li>
 *   <li><strong>Phase 3:</strong> Method identity calculation and type information refinement</li>
 *   <li><strong>Phase 4:</strong> Method invocation analysis and call graph edge creation</li>
 * </ul>
 * 
 * <p>This class implements Runnable to support parallel processing of multiple files
 * during phases 1, 3, and 4, while phase 2 runs sequentially to ensure data consistency.</p>
 * 
 * <p>The processor handles various Java language constructs including:</p>
 * <ul>
 *   <li>Regular class, interface, and enum declarations</li>
 *   <li>Anonymous class declarations</li>
 *   <li>Method invocations and constructor calls</li>
 *   <li>Field initializations and static blocks</li>
 *   <li>Inheritance and interface implementation relationships</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class CallGraphProcessor implements Runnable {

    /** 
     * The file to process in this thread.
     * 
     * <p>This field holds the file path that this processor instance
     * is responsible for analyzing. Each processor instance handles
     * one file to enable parallel processing across multiple files.</p>
     */
    private String file = null;

    /** 
     * The progress reporter for tracking processing status.
     * 
     * <p>This interface is used to report progress updates during
     * call graph construction, allowing the calling application to
     * monitor the analysis progress.</p>
     */
    private IProgressReporter progressReporter;

    /** 
     * Enumeration defining the four phases of call graph construction.
     * 
     * <p>Each phase performs specific tasks in the call graph construction process:</p>
     * <ul>
     *   <li><strong>PHASE_1:</strong> Iterates over type declarations and stores basic info.
     *       Runs in parallel across multiple files.</li>
     *   <li><strong>PHASE_2:</strong> Iterates over type declarations and updates class inheritance
     *       and implementation hierarchy and inherited fields. Only call graph phase to run
     *       in a single-threaded manner to ensure data consistency.</li>
     *   <li><strong>PHASE_3:</strong> Iterates over type declarations and calculates and updates type info.
     *       Iterates over all method declarations and calculates their hash. Runs in parallel.</li>
     *   <li><strong>PHASE_4:</strong> Iterates over type declarations and then the method declarations
     *       and stores CHA call graph information. Runs in parallel.</li>
     * </ul>
     */
    public enum Phase {
        /** Phase 1: Basic class information and inheritance setup */
        PHASE_1,
        /** Phase 2: Single-threaded inheritance hierarchy population */
        PHASE_2,
        /** Phase 3: Method identity calculation and type refinement */
        PHASE_3,
        /** Phase 4: Method invocation analysis and call graph edges */
        PHASE_4
    }

    /** 
     * The current call graph phase being executed.
     * 
     * <p>This field determines which processing logic to apply
     * when analyzing the assigned file.</p>
     */
    private Phase cgPhase;

    public CallGraphProcessor(IProgressReporter progressReporter, Phase phase) {
        this.progressReporter = progressReporter;
        this.cgPhase = phase;
    }

    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Executes the call graph processing for the assigned file.
     *
     * <p>This method is the main entry point for processing a single file.
     * It loads the compilation unit from the file path and delegates the
     * actual call graph construction to {@link #buildCallGraph(CompilationUnit, String)}.</p>
     *
     * <p>If debug mode is enabled, progress information is reported including
     * the thread name and file being processed.</p>
     *
     * <p>This method implements the Runnable interface, allowing the processor
     * to be executed in separate threads for parallel processing.</p>
     */
    @Override
    public void run() {
        if (file != null) {
            if (ConfigurationManager.config.DEBUG) {
                progressReporter.showProgress(Thread.currentThread().getName() + ": Processing File: "
                        + file.substring(file.lastIndexOf(File.separatorChar) + 1));
            }
            CompilationUnit cu = ASTNodeUtility.getCompilationUnitFromFilePath(file);
            if (cu != null) {
                buildCallGraph(cu, file);
            }
        }
    }

    /**
     * Builds the call graph using Class Hierarchy Analysis (CHA).
     *
     * <p>This method is the core orchestrator that processes compilation units
     * according to the current call graph phase. It handles different processing
     * logic based on the phase:</p>
     *
     * <p><strong>Phase 1:</strong> In visit of TypeDeclaration, stores information about the class
     * and its immediate relationships. Does not calculate field information since it's not needed.
     * Does not follow all super and sub interfaces.</p>
     *
     * <p><strong>Phase 3:</strong> In visit of TypeDeclaration, stores information about inner classes
     * and calculates type information.</p>
     *
     * <p><strong>Phase 4:</strong> In visit of MethodDeclaration, populates the caller-callee map
     * of the global call graph container.</p>
     *
     * <p>The method processes both regular type declarations and anonymous class declarations,
     * handling test file detection and exclusion, import processing, and phase-specific logic.</p>
     *
     * @param cu the CompilationUnit to process
     * @param filePath the file path for the compilation unit
     */
    private void buildCallGraph(CompilationUnit cu, String filePath) {
        // This check about test file inclusion is redundantly done for all files in the first phase
        // During the first phase this test will always be true since nothing is included in the skip files set
        // It gets calculated inside and becomes relevant in phase 3 and 4
        if (!CallGraphDataStructures.isExcludedFile(filePath) && !CallGraphDataStructures.isAProtoFile(filePath)) {
            try {
                // Store all package names imported in a file
                // in CallGraphDataStructures
                Pair<Set<String>, Boolean> importsPair = getImports(cu);
                List<Class<?>> clazzes = new ArrayList<>(2);
                clazzes.add(AbstractTypeDeclaration.class);
                clazzes.add(AnonymousClassDeclaration.class);
                Map<Class<?>, List<ASTNode>> foundNodes = new HashMap<>(2);
                ASTNodeUtility.findAndPopulateAll(cu, clazzes, foundNodes);
                List<ASTNode> typeDeclarations = foundNodes.get(AbstractTypeDeclaration.class);
                if (cgPhase == Phase.PHASE_1
                        && (!ConfigurationManager.isTestRun
                                && (importsPair.snd || CallGraphUtility.isUnderTestOrExampleDirectory(filePath)))) {
                    // In CG phase 1, we will calculate the files that match test files
                    // In subsequent phases, we already operate with those files being skipped
                    CallGraphDataStructures.addFileToExcludedList(filePath);
                    // Inherited classes/interfaces of JUnit test files are also to be excluded.
                    // Need class signature from class hash of excluded files to resolve this.
                    if (typeDeclarations != null) {
                        for (ASTNode node : typeDeclarations) {
                            AbstractTypeDeclaration typeDec = (AbstractTypeDeclaration) node;
                            // AbstractTypeDeclaration is super type of TypeDeclaration,
                            // EnumDeclaration and AnnotationTypeDeclaration.
                            // Skip if the typeDec is an AnnotationTypeDeclaration,
                            // as we do not process it now.
                            if (typeDec instanceof AnnotationTypeDeclaration) {
                                continue;
                            }
                            ITypeBinding binding = typeDec.resolveBinding();
                            // Skip if the binding is null
                            if (binding == null) {
                                continue;
                            }
                            TokenRange declarationTokenRange = ASTNodeUtility.getTokenRangeOfBinding(binding);
                            Pair<String, String> pairClassHashAndSign =
                                    CallGraphUtility.getClassHashAndSignatureFromSourceTypeBinding(
                                            binding, declarationTokenRange);
                            CallGraphDataStructures.addToHashToClassSignature(
                                    pairClassHashAndSign.fst, pairClassHashAndSign.snd);

                            // We are not populating bitIndex for this class. Because we assumed
                            // that we can filter all test classes and there will be no method of this
                            // test class in framework and pointer analysis.
                            // If we fail to detect a test file, whose method calls this class method, then
                            // this class method will be added into caller-calee edge and it leads this class method
                            // to framework and pointer analysis. As we are not populating bit index for this class
                            // it can throws exception.
                        }
                    }
                } else {
                    CallGraphDataStructures.addToFilePathToImportsMap(
                            ASTNodeUtility.getFilePathFromCompilationUnit(cu), importsPair.fst);
                    if (typeDeclarations != null) {
                        for (ASTNode node : typeDeclarations) {
                            AbstractTypeDeclaration typeDec = (AbstractTypeDeclaration) node;
                            // AbstractTypeDeclaration is super type of TypeDeclaration,
                            // EnumDeclaration and AnnotationTypeDeclaration.
                            // Skip if the typeDec is an AnnotationTypeDeclaration,
                            // as we do not process it now.
                            if (typeDec instanceof AnnotationTypeDeclaration) {
                                continue;
                            }
                            ITypeBinding binding = typeDec.resolveBinding();
                            // Skip if the binding is null
                            if (binding == null) {
                                continue;
                            }
                            TokenRange declarationTokenRange = ASTNodeUtility.getTokenRangeOfBinding(binding);

                            if (cgPhase == Phase.PHASE_1) {
                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationForPhase1(typeDec, binding, declarationTokenRange);
                            } else if (cgPhase == Phase.PHASE_3) {
                                // We are guaranteed to find class hash from a token range here
                                String classHash =
                                        CallGraphDataStructures.getClassHashFromTokenRange(declarationTokenRange);

                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationsForPhase3(
                                        typeDec, binding, declarationTokenRange, classHash);
                            } else if (cgPhase == Phase.PHASE_4) {
                                // We are guaranteed to find class hash from a token range here
                                String classHash =
                                        CallGraphDataStructures.getClassHashFromTokenRange(declarationTokenRange);

                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationsForPhase4(
                                        typeDec, binding, declarationTokenRange, classHash);
                            }
                        }
                    }

                    // Handle the anonymous class declarations
                    List<ASTNode> anonClassDecs = foundNodes.get(AnonymousClassDeclaration.class);
                    if (anonClassDecs != null) {
                        for (ASTNode node : anonClassDecs) {
                            AnonymousClassDeclaration anonClassDec = (AnonymousClassDeclaration) node;
                            ITypeBinding binding = anonClassDec.resolveBinding();
                            // If the binding is null, just skip it and it's contents.
                            if (binding == null) {
                                continue;
                            }
                            // Directly calculating token range, since for an anonymous class,
                            // the token range comes from the anonymous class declaration
                            TokenRange declarationTokenRange =
                                    new TokenRange(anonClassDec.getStartPosition(), anonClassDec.getLength(), filePath);

                            if (cgPhase == Phase.PHASE_1) {
                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationForPhase1(anonClassDec, binding, declarationTokenRange);
                            } else if (cgPhase == Phase.PHASE_3) {
                                // We are guaranteed to find class hash from a token range here
                                String classHash =
                                        CallGraphDataStructures.getClassHashFromTokenRange(declarationTokenRange);

                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationsForPhase3(
                                        anonClassDec, binding, declarationTokenRange, classHash);
                            } else if (cgPhase == Phase.PHASE_4) {
                                // We are guaranteed to find class hash from a token range here
                                String classHash =
                                        CallGraphDataStructures.getClassHashFromTokenRange(declarationTokenRange);

                                // Actual processing happens inside this, algorithm inside the method
                                processContainerDeclarationsForPhase4(
                                        anonClassDec, binding, declarationTokenRange, classHash);
                            }
                        }
                    }
                }
            } catch (Exception | Error e) {
                e.printStackTrace();
            }

            // Change state when call graph processing for this part is completed
            // Moved the code inside try to handle exceptions locally
            // Moved the progress printing in the finally part so that we progress even during exceptions
            int phaseNo = cgPhase.ordinal() + 1;
            int completed = 0;
            if (cgPhase == Phase.PHASE_1) {
                completed = CallGraphDataStructures.completedFilesOne.incrementAndGet();
            } else if (cgPhase == Phase.PHASE_3) {
                completed = CallGraphDataStructures.completedFilesThree.incrementAndGet();
            } else if (cgPhase == Phase.PHASE_4) {
                completed = CallGraphDataStructures.completedFilesFour.incrementAndGet();
            } else {
                completed = CallGraphDataStructures.completedFilesTwo.incrementAndGet();
            }
            progressReporter.showProgress("CALL GRAPH STAGE-" + phaseNo + ": Completed " + completed + " out of "
                    + CallGraphDataStructures.totalFiles);
        }
    }

    /**
     * *************************************************************
     * *************************************************************
     *
     * Methods used during call graph phase 1
     *
     * *************************************************************
     * *************************************************************
     */

    /**
     * Retrieves file path from the compilation unit and stores all imported package names.
     *
     * <p>This method processes all import declarations in a compilation unit and stores
     * the imported package names in the relevant CallGraphDataStructures. It also detects
     * JUnit test imports to help identify test files for exclusion.</p>
     *
     * <p>The method handles both regular imports and on-demand imports (wildcard imports),
     * converting them to the appropriate format for storage.</p>
     *
     * @param cu the compilation unit for which file path and imports are to be stored
     * @return a Pair where the first element is the set of imported packages and the second
     *         element is true if the compilation unit contains JUnit test imports
     */
    private Pair<Set<String>, Boolean> getImports(CompilationUnit cu) {
        @SuppressWarnings("unchecked")
        List<ImportDeclaration> importsInFile = cu.imports();
        Set<String> importedPackages = new HashSet<>();
        boolean hasJUnitImport = false;
        for (ImportDeclaration singleImport : importsInFile) {
            StringBuilder importString = new StringBuilder();
            String importName = singleImport.getName().getFullyQualifiedName();
            importString.append(importName);
            if (singleImport.isOnDemand()) {
                importString.append(".*");
            }
            importedPackages.add(importString.toString());

            // To cover all type of junit packages, we added these 2 conditions.
            // Previously we used to maintain a list containing test packages.
            if (importString.toString().startsWith("org.junit")
                    || importString.toString().startsWith("junit")) {
                hasJUnitImport = true;
            }
        }
        return Pair.of(importedPackages, hasJUnitImport);
    }

    /**
     * Processes a type declaration or an anonymous class declaration for Phase 1.
     *
     * <p>This method handles the core Phase 1 processing for both regular type declarations
     * and anonymous class declarations. It performs several key steps:</p>
     *
     * <p>For anonymous class declarations, annotations are not checked. The class instance
     * creation expression is used to distinguish anonymous inner class declarations and to
     * choose the appropriate constructor in the superclass that will be called by the
     * anonymous inner class's default constructor.</p>
     *
     * @param declaration the declaration node (either a type declaration or an anonymous class declaration)
     * @param binding the type binding of the node
     * @param tokenRange the token range of the type or anonymous type declaration
     */
    private void processContainerDeclarationForPhase1(
            ASTNode declaration, ITypeBinding binding, TokenRange tokenRange) {
        // Steps in Phase 1 when we process a container declaration
        // (normal type declaration or an anonymous declaration):
        //     (1) Use the token range to calculate the class hash
        //     (2) Update info about static or nested classes
        //     (3) Update annotations map
        //     (4) Add fields for this class only
        //     (5) Update immediate super class and implemented interfaces information
        //     (6) Create default and static constructor hash and update map
        //     (7) Calculate and populate soft type info

        // Step (1)
        // Pair of this class's hash and signature
        // pair.fst = class hash and pair.snd = class signature
        Pair<String, String> pairClassHashAndSign =
                CallGraphUtility.getClassHashAndSignatureFromSourceTypeBinding(binding, tokenRange);
        // Store binding hash and the class hash in the map
        String bindingHash = CallGraphDataStructures.getBindingHash(binding, tokenRange);
        if (bindingHash != null) {
            CallGraphDataStructures.addToBindingHashToClassHashMap(bindingHash, pairClassHashAndSign.fst);
        }
        CallGraphDataStructures.addToTokenRangeToClassHashMap(tokenRange, pairClassHashAndSign.fst);
        CallGraphDataStructures.addToHashToClassSignature(pairClassHashAndSign.fst, pairClassHashAndSign.snd);
        CallGraphDataStructures.updateOrGetBitIndexFromClassHash(pairClassHashAndSign.fst);

        // Step (2)
        if (binding.isNested()) {
            CallGraphDataStructures.getNestedClassHashes().add(pairClassHashAndSign.fst);
        }
        if (Modifier.isStatic(binding.getModifiers())) {
            CallGraphDataStructures.getStaticClassHashes().add(pairClassHashAndSign.fst);
        }

        // Step (4)
        // Maps fields of this class to its hash
        boolean hasStaticFields = addFieldsFromThisContainer(binding, pairClassHashAndSign.fst);

        // Step (5)
        // Populates the superclass-subclass and interface relationships in the
        // global data-structures for the current type declaration
        populateImmediateSuperclassAndInterfaceInfo(binding, tokenRange, pairClassHashAndSign.fst);

        // Step (6)
        // Adding a default and static constructor and updating the map
        // We use the same function to create both default and static constructor,
        // for normal default constructor, we pass 'false' in last the parameter
        createAndPopulateDefaultConstructor(binding, tokenRange, pairClassHashAndSign, false);
        // Create static constructor only if any static field exist for this class,
        // for static constructor we pass 'true' in last the parameter
        if (hasStaticFields) {
            createAndPopulateDefaultConstructor(binding, tokenRange, pairClassHashAndSign, true);
        }
        // Step (7)
        // Calculate and populate soft type info
        TypeInfo softType =
                TypeCalculator.typeOf(binding, tokenRange.getFileName(), tokenRange, pairClassHashAndSign.fst, true);
        if (softType != null) {
            // We do not store parametric typeInfo,
            // but now we need it in matching overriding methods in CG 4
            // So, now we store this in a separate temporary map.
            if (softType instanceof ParameterizedTypeInfo) {
                CallGraphDataStructures.getParametricClassTypeInfoMap().putIfAbsent(pairClassHashAndSign.fst, softType);
            } else {
                CallGraphDataStructures.addToClassIdToSoftTypeInfoMap(pairClassHashAndSign.fst, softType);
            }
        }
    }

    /**
     * Creates the default or static constructor for a class and populates related data structures.
     *
     * <p>This method creates either a default constructor or a static constructor depending on
     * the isStatic parameter. It uses the same underlying logic for both types, with the
     * isStatic parameter determining the constructor type.</p>
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Creates the constructor hash and signature using CallGraphUtils</li>
     *   <li>Updates the method signature hash map</li>
     *   <li>Creates a MethodIdentity with appropriate bit flags</li>
     *   <li>Sets the static bit if creating a static constructor</li>
     *   <li>Populates the appropriate constructor map (default or static)</li>
     * </ul>
     *
     * @param binding the type binding of the class type declaration
     * @param tokenRange the token range of the type declaration
     * @param classHashAndSign the class hash and signature pair
     * @param isStatic true for static constructor, false for normal default constructor
     */
    void createAndPopulateDefaultConstructor(
            ITypeBinding binding, TokenRange tokenRange, Pair<String, String> classHashAndSign, boolean isStatic) {
        // We are using the same method to create default and static default constructors.
        // the last parameter indicates the type of default constructor (normal or static)
        Pair<String, String> defaultConstructor =
                CallGraphUtility.createDefaultConstructor(binding, tokenRange, isStatic);
        if (defaultConstructor == null) {
            return;
        }
        // Update the method signature hash map
        int methodHashIndex = CallGraphDataStructures.getMethodHashIndexAndPotentiallyUpdateOtherInitialStructures(
                defaultConstructor.fst, defaultConstructor.snd);
        // The method identity for a default constructor will have the name and some set bits only
        String className = CallGraphUtility.getClassNameFromClassSignature(classHashAndSign.snd);
        MethodIdentity identity = new MethodIdentity(className, new ScalarTypeInfo("void"), new ArrayList<>(0));
        identity.setBodylessBit();
        identity.setConstructorBit();
        if (isStatic) {
            // Populate static constructor hash to classToStaticConstructorMap,
            // also set the static bit in the related method identity
            identity.setStaticBit();
            CallGraphDataStructures.addToClassToStaticConstructorMap(classHashAndSign.fst, defaultConstructor.fst);
        } else {
            // Populate default constructor hash to classToDefaultConstructorMap,
            CallGraphDataStructures.addToClassToDefaultConstructorMap(classHashAndSign.fst, defaultConstructor.fst);
        }
        CallGraphDataStructures.addMethodIdentity(methodHashIndex, identity);
    }

    /**
     * Phase 1, Step 4: Associates all fields of this container to the container hash.
     *
     * <p>This method processes all declared fields of a container (class, interface, or enum)
     * and associates them with the container's hash in the global data structures.</p>
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Retrieves all declared fields from the container binding</li>
     *   <li>Creates FieldInfo objects with appropriate access modifiers</li>
     *   <li>Handles special cases for enum constants and their initializers</li>
     *   <li>Stores field information in the global data structures</li>
     *   <li>Tracks whether any static fields exist in the container</li>
     * </ul>
     *
     * <p>Note: This method only processes fields declared directly in the container,
     * not inherited fields, which are handled in later phases.</p>
     *
     * @param containerBinding the binding for the container
     * @param containerHash the hash of the container to which the fields are being mapped
     * @return true if any static fields exist, false otherwise
     */
    private boolean addFieldsFromThisContainer(ITypeBinding containerBinding, String containerHash) {
        boolean hasStaticFields = false;
        // Ensure that we are only doing it once
        if (!CallGraphDataStructures.containsFieldsForContainer(containerHash)) {
            IVariableBinding[] fields = containerBinding.getDeclaredFields();
            String containerFilePath = CallGraphUtility.getFileNameFromHash(containerHash);
            if (fields != null && containerFilePath != null) {
                for (IVariableBinding field : fields) {
                    TokenRange fieldRange = ASTNodeUtility.getTokenRangeOfBinding(field);
                    if (fieldRange != null) {
                        byte modifiers = CallGraphUtility.getFieldAccessModifier(field, containerBinding);
                        FieldInfo fieldInfo = new FieldInfo(field.getName(), containerHash, modifiers);
                        // Check whether the field is static
                        if (fieldInfo.isStatic()) {
                            hasStaticFields = true;
                        }
                        // EnumConstantDeclaration implicitly invokes the enum constructor.
                        // If we do not get VariableDeclarationFragment for the field,
                        // search for EnumConstantDeclaration and use itself as field initializer.
                        ASTNode initializer = null;
                        VariableDeclarationFragment fieldDeclaration = ASTNodeUtility.getASTNodeFromTokenRange(
                                fieldRange, VariableDeclarationFragment.class);
                        if (fieldDeclaration != null) {
                            initializer = fieldDeclaration.getInitializer();
                        } else if (containerBinding.isEnum()) {
                            initializer = ASTNodeUtility.getASTNodeFromTokenRange(
                                    fieldRange, EnumConstantDeclaration.class);
                        }
                        if (initializer != null) {
                            TokenRange initializerRange =
                                    ASTNodeUtility.getTokenRangeFromNode(initializer, fieldRange.getFileName());
                            if (initializerRange != null) {
                                fieldInfo.addToInitializerTokenRanges(containerHash, initializerRange);
                            }
                        }
                        CallGraphDataStructures.addToFieldTokenRangeToFieldInfoMap(fieldRange, fieldInfo);
                        CallGraphDataStructures.addToContainerHashToFieldsList(containerHash, fieldRange);
                    }
                }
            }
        }
        return hasStaticFields;
    }

    /**
     * Phase 1, Step 5: Populates information about immediate superclass and implemented interfaces.
     *
     * <p>This method processes the inheritance hierarchy for a container by:</p>
     * <ul>
     *   <li>Identifying all directly implemented interfaces</li>
     *   <li>Detecting protobuf-generated files for exclusion</li>
     *   <li>Resolving interface bindings to class hashes</li>
     *   <li>Identifying the immediate superclass</li>
     *   <li>Storing inheritance relationships in global data structures</li>
     * </ul>
     *
     * <p>The method handles both source code types and library types, creating appropriate
     * hashes for each. It also performs special handling for protobuf-generated classes
     * by detecting specific interface implementations and adding them to the exclusion list.</p>
     *
     * <p>Note: This method only processes immediate relationships (direct superclass and
     * directly implemented interfaces). The complete inheritance hierarchy is populated
     * in later phases.</p>
     *
     * @param containerBinding the type binding of the container
     * @param containerTokenRange the token range of the container
     * @param containerHash the hash of the container
     */
    private void populateImmediateSuperclassAndInterfaceInfo(
            ITypeBinding containerBinding, TokenRange containerTokenRange, String containerHash) {
        // Get the implemented interfaces and store information
        ITypeBinding[] interfaces = containerBinding.getInterfaces();
        for (ITypeBinding interfaceBinding : interfaces) {
            // Exclude auto generated files by protobuf.
            // Builder interfaces of generated by protobuf implements
            // "com.google.protobuf.MessageOrBuilder" interface.
            if (interfaceBinding.getQualifiedName().equals("com.google.protobuf.MessageOrBuilder")) {
                CallGraphDataStructures.addToProtobufFilesToExcludeList(containerTokenRange.getFileName());
            }
            String interfaceHash = null;
            if (CallGraphUtility.isResolvableType(interfaceBinding)) {
                // The interface is from source code
                // The interface that is implemented is supposed to be a regular interface
                // So, we use the regular method for getting token range
                TokenRange interfaceTokenRange = ASTNodeUtility.getTokenRangeOfBinding(interfaceBinding);
                interfaceHash = CallGraphDataStructures.getClassHashFromTokenRange(interfaceTokenRange);
                if (interfaceHash == null) {
                    interfaceHash = CallGraphUtility.getClassHashFromTypeBinding(
                            interfaceBinding, interfaceTokenRange, interfaceTokenRange.getFileName());
                }
            } else {
                // If a type is coming from library, get a special type name
                if (interfaceBinding != null) {
                    interfaceHash =
                            CallGraphUtility.getLibraryTypeHash(interfaceBinding, containerTokenRange.getFileName());

                    // Even though we do not assign bit index to source classes until we see them
                    // If the library type is not covered yet in the bit index, add it
                    // since we will not be seeing them in AST node iteration anyway
                    CallGraphDataStructures.updateOrGetBitIndexFromClassHash(interfaceHash);
                }
            }
            if (interfaceHash != null) {
                CallGraphDataStructures.addToDirectlyImplementedInterfaces(containerHash, interfaceHash);
            }
        }

        // Get the immediate super class information
        ITypeBinding superClassBinding = containerBinding.getSuperclass();
        // Exclude auto generated files by protobuf.
        // Message classes generated by protobuf extends from
        // "com.google.protobuf.GeneratedMessageV3" class.
        if (superClassBinding != null
                && superClassBinding.getQualifiedName().equals("com.google.protobuf.GeneratedMessageV3")) {
            CallGraphDataStructures.addToProtobufFilesToExcludeList(containerTokenRange.getFileName());
        }
        String superClassHash = null;
        if (CallGraphUtility.isResolvableType(superClassBinding)) {
            // The super class is from source code
            // The super class is supposed to be a regular class
            // So, we use the regular method for getting token range
            TokenRange superClassTokenRange = ASTNodeUtility.getTokenRangeOfBinding(superClassBinding);
            superClassHash = CallGraphDataStructures.getClassHashFromTokenRange(superClassTokenRange);
            if (superClassHash == null) {
                superClassHash = CallGraphUtility.getClassHashFromTypeBinding(
                        superClassBinding, superClassTokenRange, superClassTokenRange.getFileName());
            }
        } else {
            // If a type is coming from library, get a special type name
            // We do not want java.lang.Object to have all objects as subclass
            // Yet we allow it to the the super class of all objects here
            // Later when we populate all super classes and sub classes, we filter the
            // Object classes to not have sub classes
            if (superClassBinding != null) {
                superClassHash =
                        CallGraphUtility.getLibraryTypeHash(superClassBinding, containerTokenRange.getFileName());

                // Even though we do not assign bit index to source classes until we see them
                // If the library type is not covered yet in the bit index, add it
                // since we will not be seeing them in AST node iteration anyway
                CallGraphDataStructures.updateOrGetBitIndexFromClassHash(superClassHash);
            }
        }
        if (superClassHash != null) {
            CallGraphDataStructures.addToImmediateSuperClassMap(containerHash, superClassHash);
        }
    }

    /**
     * *************************************************************
     * *************************************************************
     *
     * Methods used during call graph phase 3
     *
     * *************************************************************
     * *************************************************************
     */

    /**
     * Processes container declarations for Phase 3.
     *
     * <p>Phase 3 focuses on method identity calculation and type information refinement.
     * This method performs several key tasks:</p>
     *
     * <ol>
     *   <li><strong>Inner Class Mapping:</strong> Maps inner classes to their outer containers,
     *       handling both class-local and method-local inner classes. This is staged in Phase 3
     *       to enable parallel processing, as it couldn't be done in Phase 1 due to anonymous
     *       class ordering issues.</li>
     *   <li><strong>Method Processing:</strong> Visits method declarations and populates method
     *       information including method hashes, method-to-class relationships, and method identities.</li>
     *   <li><strong>Type Information:</strong> Calculates proper type information for fields,
     *       which will be used during proper type and points-to calculation to reduce AST loading.</li>
     * </ol>
     *
     * <p>The method handles different declaration types (TypeDeclaration, EnumDeclaration,
     * AnonymousClassDeclaration) in a generalized way to avoid code duplication.</p>
     *
     * @param declaration the type declaration to process
     * @param binding the type binding of the declaration
     * @param tokenRange the token range of the declaration
     * @param classHash the class hash of the declaration
     */
    private void processContainerDeclarationsForPhase3(
            ASTNode declaration, ITypeBinding binding, TokenRange tokenRange, String classHash) {
        // Steps in Phase 3 when we process a container declaration
        // (normal type declaration or an anonymous declaration):
        //     (1) Update a map that contains info of the inner classes to the hash of the
        //         immediately enclosing class/anonymous class/method declaration of that.
        //         Also update a map that maps an outer class to all its inner classes.
        //         For this map, even for a method local inner class, we will go to the
        //         outer class declaration for that method and map that outer class to the inner class.
        //         The second step was previously calculated here but the first step was calculated
        //         in phase 4. 
        //     (2) Visit method declarations and populate method info
        //         i.e., method hash, method to class relationship, and
        //         method identity
        //     (3) Calculate proper type-info for the fields.

        // Step (1)
        // Map an inner class to its outer container class
        // This is staged in phase 3 so that it can be done in parallel
        // We could not do it in phase 1, because for an inner class inside
        // an anonymous inner class, we will not be able to find the outer
        // anonymous inner class because the anonymous classes are visited after the
        // regular classes. And we avoided doing it in phase 2 since that is single threaded.
        // There is also the reverse mapping task that was previously done in phase 4.
        if (binding.isNested()) {
            // Iterate to the outer class and find appropriate parents and handle the cases
            ASTNode temp = declaration;
            boolean foundMethodLocalInnerClass = false;
            while (temp != null) {
                temp = temp.getParent();
                if (temp instanceof MethodDeclaration) {
                    // Store both the enclosing class and the enclosing method
                    // Storing the enclosing method here. Not bailing out now, continue until we hit a class
                    // This will happen for a method local inner class. However, we will
                    // continue to explore outside until we hit an enclosing class too.
                    // Mark here that we have found a method local inner class so that
                    // when we find the enclosing class, we do not attempt to venture outside any more.
                    foundMethodLocalInnerClass = true;
                    Pair<String, String> enclosingMethodHashAndSignature =
                            CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(
                                    (MethodDeclaration) temp, tokenRange.getFileName());
                    if (enclosingMethodHashAndSignature != null && enclosingMethodHashAndSignature.fst != null) {
                        CallGraphDataStructures.addToImmediateEnclosingMethodMap(
                                classHash, enclosingMethodHashAndSignature.fst);
                    }
                    // No break here, since we continue traversal
                } else if (temp instanceof TypeDeclaration) {
                    String enclosingClassHash =
                            CallGraphUtility.getClassHashFromContainerDeclaration(temp, tokenRange.getFileName());
                    if (enclosingClassHash != null) {
                        CallGraphDataStructures.addToImmediateEnclosingClassMap(classHash, enclosingClassHash);
                        CallGraphDataStructures.addToInnerClassesMap(enclosingClassHash, classHash);
                    }
                    if (!foundMethodLocalInnerClass) {
                        // Multiple nesting may happen for a method local inner class
                        // So, if we have
                        //    void foo() {
                        //       class Inner {
                        //            class Inner2 {
                        //                   ...
                        //            }
                        //            ...
                        //        }
                        //    }
                        // we still need to look for the case if the class is method local inner or not.
                        // This should only happen if we have not established that the class is a method local inner
                        // class before. We only look for a method declaration at this point.
                        while (temp != null) {
                            temp = temp.getParent();
                            if (temp instanceof MethodDeclaration) {
                                Pair<String, String> enclosingMethodHashAndSignature =
                                        CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(
                                                (MethodDeclaration) temp, tokenRange.getFileName());
                                if (enclosingMethodHashAndSignature != null
                                        && enclosingMethodHashAndSignature.fst != null) {
                                    CallGraphDataStructures.addToImmediateEnclosingMethodMap(
                                            classHash, enclosingMethodHashAndSignature.fst);
                                }
                                // Already found that we have an inner class, now established method local inner class.
                                // We are done at that point.
                                break;
                            }
                        }
                    }
                    break;
                } else if (temp instanceof AnonymousClassDeclaration || temp instanceof EnumDeclaration) {
                    String enclosingClassHash =
                            CallGraphUtility.getClassHashFromContainerDeclaration(temp, tokenRange.getFileName());
                    if (enclosingClassHash != null) {
                        CallGraphDataStructures.addToImmediateEnclosingClassMap(classHash, enclosingClassHash);
                        CallGraphDataStructures.addToInnerClassesMap(enclosingClassHash, classHash);
                    }
                    // Multiple nesting may happen for a method local inner class.
                    // See the explanation in the above comment.
                    while (temp != null) {
                        temp = temp.getParent();
                        if (temp instanceof MethodDeclaration) {
                            Pair<String, String> enclosingMethodHashAndSignature =
                                    CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(
                                            (MethodDeclaration) temp, tokenRange.getFileName());
                            if (enclosingMethodHashAndSignature != null
                                    && enclosingMethodHashAndSignature.fst != null) {
                                CallGraphDataStructures.addToImmediateEnclosingMethodMap(
                                        classHash, enclosingMethodHashAndSignature.fst);
                            }
                            // Already found that we have an inner class, now established method local inner class.
                            // We are done at that point.
                            break;
                        }
                    }
                    break;
                }
            }
        }
        // To avoid repetitive code snippet, we extract the fields and methods
        // of TypeDeclaration, EnumDeclaration and AnonymousClassDeclaration
        // in list and process them in a generalized way.
        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();
        // fieldNames contains the simeple names from
        // both FieldDeclaration and EnumConstantDeclaration
        List<SimpleName> fieldNames = new ArrayList<>();

        if (declaration instanceof TypeDeclaration) {
            fields = Arrays.asList(((TypeDeclaration) declaration).getFields());
            methods = Arrays.asList(((TypeDeclaration) declaration).getMethods());
        } else if (declaration instanceof EnumDeclaration) {
            EnumDeclaration enumDeclaration = (EnumDeclaration) declaration;
            for (Object bodyDecl : enumDeclaration.bodyDeclarations()) {
                if (bodyDecl instanceof MethodDeclaration) {
                    methods.add((MethodDeclaration) bodyDecl);
                } else if (bodyDecl instanceof FieldDeclaration) {
                    fields.add((FieldDeclaration) bodyDecl);
                }
            }
            for (Object enumConst : enumDeclaration.enumConstants()) {
                fieldNames.add(((EnumConstantDeclaration) enumConst).getName());
            }
        } else if (declaration instanceof AnonymousClassDeclaration) {
            for (Object bodyDecl : ((AnonymousClassDeclaration) declaration).bodyDeclarations()) {
                if (bodyDecl instanceof MethodDeclaration) {
                    methods.add((MethodDeclaration) bodyDecl);
                } else if (bodyDecl instanceof FieldDeclaration) {
                    fields.add((FieldDeclaration) bodyDecl);
                }
            }
        }
        // Step (2)
        // Calculate method hashes and method identities from method declarations
        for (MethodDeclaration method : methods) {
            populateMethodHashAndMethodIdentity(classHash, method);
        }
        // Step (3)
        // Calculate proper type-info for the fields.
        // Save field information in the data structure
        // Will be used during proper type and points to calculation.
        // It helps reducing ast loading
        for (FieldDeclaration field : fields) {
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fieldFragments = field.fragments();
            for (VariableDeclarationFragment vdf : fieldFragments) {
                fieldNames.add(vdf.getName());
            }
        }
        for (SimpleName field : fieldNames) {
            TypeInfo fieldTypeInfo = TypeCalculator.typeOf(field, false);
            TokenRange fieldRange = ASTNodeUtility.getTokenRangeFromNode(field, tokenRange.getFileName());
            FieldInfo existingFieldInfo = CallGraphDataStructures.getFieldInfoFromTokenRange(fieldRange);
            // Must have field info already
            if (existingFieldInfo != null) {
                existingFieldInfo.setTypeInfo(fieldTypeInfo);
            } else {
                // Previously the entry was not created because of bad binding
                // i.e., a binding for a container misses one of its fields.
                // Now we are at actual AST node and we will indeed find the field
                // So add the info here.
                IBinding varBinding = field.resolveBinding();
                byte modifiers;
                if (varBinding instanceof IVariableBinding) {
                    modifiers = CallGraphUtility.getFieldAccessModifier(
                            varBinding, ((IVariableBinding) varBinding).getDeclaringClass());
                } else {
                    // Default is regular public class
                    modifiers = AccessModifiers.PUBLIC;
                }
                FieldInfo info = new FieldInfo(field.toString(), classHash, modifiers);
                info.setTypeInfo(fieldTypeInfo);
                CallGraphDataStructures.addToFieldTokenRangeToFieldInfoMap(fieldRange, info);
            }
        }
    }

    /**
     * Calculates and stores method hash and method identity from a method declaration.
     *
     * <p>This method processes method declarations to create the core method information
     * needed for call graph construction. It performs several validation checks before
     * proceeding with the calculation:</p>
     *
     * <p>The method only creates method associations if all of the following conditions hold:</p>
     * <ul>
     *   <li>Binding is not null (defensive check)</li>
     *   <li>Binding associates with a method (defensive check)</li>
     *   <li>Method is not a lambda (defensive check)</li>
     *   <li>Container class is not null (defensive check)</li>
     *   <li>Container class is either a class or an interface (not an enum)</li>
     *   <li>Method has a body or is abstract/interface method</li>
     * </ul>
     *
     * <p>The method also filters out methods annotated with @VisibleForTesting as they
     * are only used in test cases and should not be included in the main call graph.</p>
     *
     * <p>For valid methods, it:</p>
     * <ul>
     *   <li>Calculates the method hash and signature</li>
     *   <li>Creates a MethodIdentity using MethodHandler.process()</li>
     *   <li>Updates the global method data structures</li>
     *   <li>Associates the method with its container class</li>
     * </ul>
     *
     * @param containerHash the hash of the container (class or anonymous class)
     * @param methodDeclaration the method declaration for which the information is created
     */
    private void populateMethodHashAndMethodIdentity(String containerHash, MethodDeclaration methodDeclaration) {
        // Suppress @VisibleForTesting methods as they are only used in test cases
        Annotation visibleForTestingAnnoation =
                AnnotationUtility.findFirstMatchingAnnotation(methodDeclaration, "VisibleForTesting");
        if (visibleForTestingAnnoation != null) {
            return;
        }
        // This is when the method hashes are calculated the first time
        // So, no need to check in cache, since it is not available
        Pair<String, String> thisMethodHashAndSign =
                CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(methodDeclaration, null);
        if (thisMethodHashAndSign != null && thisMethodHashAndSign.fst != null) {
            // Calculate the method identity
            MethodIdentity identity = MethodHandler.process(methodDeclaration);

            // For all methods (methods with bodies and abstract/interface methods with bodies)
            // we create method identity and populate information
            // For abstract/interface methods, we additionally add the entry in a set
            int index = CallGraphDataStructures.getMethodHashIndexAndPotentiallyUpdateOtherInitialStructures(
                    thisMethodHashAndSign.fst, thisMethodHashAndSign.snd);
            CallGraphDataStructures.addToClassToMethodsMap(
                    containerHash, methodDeclaration.getName().getIdentifier(), index);
            CallGraphDataStructures.addMethodIdentity(index, identity);
        }
    }

    /**
     * *************************************************************
     * *************************************************************
     *
     * <p>Methods used during call graph phase 4
     *
     * <p>*************************************************************
     * *************************************************************
     */

    /**
     * Processes container declarations for Phase 4.
     *
     * <p>Phase 4 is the final phase that focuses on method invocation analysis and
     * call graph edge creation. This method orchestrates several key tasks:</p>
     *
     * <ol>
     *   <li><strong>Constructor Linking:</strong> Links default constructors with superclass
     *       constructors as appropriate, handling both regular classes and anonymous classes.</li>
     *   <li><strong>Field Initialization:</strong> Links default constructor method hashes as
     *       callers with method invocations inside field initializations as callees.</li>
     *   <li><strong>Type Information:</strong> Calculates and populates proper type information
     *       for the container, which is needed for accurate method resolution.</li>
     *   <li><strong>Method Body Processing:</strong> Processes method bodies to link method
     *       declarations as callers with various method invocations as callees, using the
     *       method identities calculated in Phase 3.</li>
     *   <li><strong>Initializer Block Processing:</strong> Checks for class instance creations
     *       and method invocations inside initializer blocks and creates call graph links
     *       from default methods to servicing methods in those invocations.</li>
     * </ol>
     *
     * <p>This phase uses the soft types calculated in Phase 1 for method matching,
     * ensuring that the method resolution is based on the most accurate type information
     * available at this stage.</p>
     *
     * @param declaration the declaration (regular or anonymous) to process
     * @param binding the type binding of the declaration
     * @param tokenRange the token range of the declaration
     * @param containerHash the container hash of the declaration
     */
    @SuppressWarnings("unchecked")
	private void processContainerDeclarationsForPhase4(
            ASTNode declaration, ITypeBinding binding, TokenRange tokenRange, String containerHash) {
        // Steps in Phase 4 when we process a container declaration
        // and also all the methods in it. For the method bodies
        // we look for various kinds of method invocations and
        // populate caller callee information for them.
        //     (1) Link a default constructor with the constructors from super class
        //         as appropriate
        //     (2) Link a default constructor method hash as the caller with the
        //         method invocations inside field initializations as callees.
        //         Note initializer blocks are handled separately
        //     (3) Calculate and populate the data structure for proper type info.
        //     (4) Process the method bodies and link the method declaration as
        //         callers with various method invocations as callees. Use the
        //         method identities calculates in Phase 3 for matching methods.
        //         This is why phase 3 is separate as that is where we calculate the
        //         method identities for all methods. Only soft types, calculated in
        //         phase 1, are used for matching methods.
        //     (5) Check for all class instance creations and method invocations inside
        //         initializer blocks and create links in call graphs from the default
        //         methods to the servicing methods in those invocations.

        // Connect default constructor from this class to the default constructor of the superclass
        // if permitted
        String staticConstructorHash = CallGraphDataStructures.getStaticConstructorFor(containerHash);
        String defaultConstructorHash = CallGraphDataStructures.getDefaultConstructorFor(containerHash);
        if (defaultConstructorHash != null) {
            // Step (1)
            // Link default constructor with super class constructors
            if (declaration instanceof TypeDeclaration) {
                addCGEdgeFromThisClassToSuperClassConstructor(defaultConstructorHash, binding, null, null);
            } else if (declaration instanceof AnonymousClassDeclaration) {
                addCGEdgeFromThisClassToSuperClassConstructor(
                        defaultConstructorHash, binding, null, ((AnonymousClassDeclaration) declaration).getParent());
            }
            // Step (2)
            // Link default constructor with field init method invocations
            addFieldInitsToConstructors(binding, containerHash, defaultConstructorHash, staticConstructorHash);
        }

        // Step (3)
        // Calculate and populate proper type info
        TypeInfo containerTypeInfo = TypeCalculator.typeOf(binding, containerHash, null, null, false);
        if (containerTypeInfo != null && !(containerTypeInfo instanceof ParameterizedTypeInfo)) {
            CallGraphDataStructures.addToClassIdToProperTypeInfoMap(containerHash, containerTypeInfo);
        }
        // To avoid repetitive code snippets, extract the body declarations
        // from the TypeDeclaration, EnumDeclaration or AnonymousClassDeclaration
        // in a list and process them in a generalized way.
        List<BodyDeclaration> bodyDeclarations = null;
        if (declaration instanceof TypeDeclaration) {
            bodyDeclarations = ((TypeDeclaration) declaration).bodyDeclarations();
        } else if (declaration instanceof AnonymousClassDeclaration) {
            bodyDeclarations = ((AnonymousClassDeclaration) declaration).bodyDeclarations();
        } else if (declaration instanceof EnumDeclaration) {
            bodyDeclarations = ((EnumDeclaration) declaration).bodyDeclarations();
        }
        if (bodyDeclarations != null) {
            for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    // Step (5)
                    // Process method bodies to create caller callee
                    // relationships for method invocations of various types
                    MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
                    // Suppress @VisibleForTesting methods as they are only used in test cases
                    Annotation visibleForTestingAnnoation =
                            AnnotationUtility.findFirstMatchingAnnotation(methodDeclaration, "VisibleForTesting");
                    if (visibleForTestingAnnoation != null) {
                        continue;
                    }
                    processMethodDeclaration(methodDeclaration, binding, containerHash, tokenRange.getFileName());
                } else if (bodyDeclaration instanceof Initializer) {
                    // Step (6)
                    // Process initializer blocks
                    Initializer initializer = (Initializer) bodyDeclaration;
                    String constructorHash = Modifier.isStatic(initializer.getModifiers())
                            ? staticConstructorHash
                            : defaultConstructorHash;
                    processInitializerBlocks(initializer, constructorHash, tokenRange.getFileName());
                }
            }
        }
    }

    /**
     * Processes an initializer block inside a class declaration.
     *
     * <p>This method analyzes initializer blocks (both static and instance) to create
     * call graph edges between the constructor (caller) and the method invocations and
     * class instance creations (callees) found within the block.</p>
     *
     * <p>The method handles different types of initializer blocks:</p>
     * <ul>
     *   <li><strong>Instance Initializer:</strong> Links to the default constructor</li>
     *   <li><strong>Static Initializer:</strong> Links to the static constructor</li>
     * </ul>
     *
     * <p>For each initializer block, it:</p>
     * <ul>
     *   <li>Finds all method invocations, class instance creations, and super method invocations</li>
     *   <li>Processes static field references to create appropriate constructor links</li>
     *   <li>Creates call graph edges from the constructor to the servicing methods</li>
     *   <li>Handles both qualified and unqualified static references</li>
     * </ul>
     *
     * <p>Note: Classes without static fields may still have static initializer blocks
     * for resource initialization, but these are not processed if no static constructor exists.</p>
     *
     * @param initBlock an initializer block inside a class
     * @param defaultOrStaticConstructorHash the hash of the default or static constructor which
     *        is the caller of the class instance creation and method invocations for fields and initializer blocks
     * @param filePath the file path
     */
    private void processInitializerBlocks(
            Initializer initBlock, String defaultOrStaticConstructorHash, String filePath) {
        // Some classes does not have any static field, so static constructor hash is null,
        // but they can have static initializer block for some other resource initialization,
        // we do not process this type of static initializer blocks
        if (defaultOrStaticConstructorHash == null) {
            return;
        }
        // A static reference is most of the time @QualifiedName but also can be
        // referenced without qualifier within an instance of the same class or sub-class,
        // so we need to process the simple names too.
        // add @Name.class to the list to extract all references
        List<Class<?>> classes =
                List.of(MethodInvocation.class, ClassInstanceCreation.class, SuperMethodInvocation.class, Name.class);
        Map<Class<?>, List<ASTNode>> foundNodes = new HashMap<>(2);
        ASTNodeUtility.findAndPopulateAllInThisScope(initBlock, classes, foundNodes);

        processAllMethodInvocations(
                foundNodes.get(MethodInvocation.class), "", defaultOrStaticConstructorHash, null, filePath);
        // Some constructor calls such as `new A()` are getting
        // missing in the big projects. So, we are taking all the class instance
        // creation inside this initializer to add them as callees of this method
        processAllClassIntanceCreations(
                foundNodes.get(ClassInstanceCreation.class), defaultOrStaticConstructorHash, filePath);
        processAllSuperMethodInvocation(
                foundNodes.get(SuperMethodInvocation.class), "", defaultOrStaticConstructorHash, filePath);

        processAllStaticFieldReferences(foundNodes.get(Name.class), defaultOrStaticConstructorHash);
    }

    /**
     * Processes each method declaration and populates the call graph.
     *
     * <p>This method is the core of Phase 4 processing, analyzing method bodies to create
     * comprehensive call graph edges. It handles various types of method calls and
     * constructor invocations found within method bodies.</p>
     *
     * <p>The method performs several key tasks:</p>
     * <ul>
     *   <li><strong>Constructor Analysis:</strong> For constructors, creates links to default
     *       constructors and superclass constructors based on specific conditions</li>
     *   <li><strong>Exception Analysis:</strong> Collects and analyzes thrown exceptions from
     *       both throws clauses and throw statements within the method body</li>
     *   <li><strong>Method Invocation Processing:</strong> Finds and processes all method
     *       invocations, class instance creations, and constructor calls</li>
     *   <li><strong>Static Reference Handling:</strong> Processes static field references
     *       to create appropriate constructor links</li>
     *   <li><strong>Call Graph Edge Creation:</strong> Creates caller-callee relationships
     *       between the method and all its callees</li>
     * </ul>
     *
     * <p>The method filters out @VisibleForTesting methods and handles various edge cases
     * including anonymous class instantiation, super calls, and this() constructor calls.</p>
     *
     * @param node the method declaration node to process
     * @param containerBinding the type binding of the container class
     * @param containerHash the hash of the container class
     * @param filePath the file path containing the method
     */
    private void processMethodDeclaration(
            MethodDeclaration node, ITypeBinding containerBinding, String containerHash, String filePath) {
        try {
            String methodName = node.getName().getIdentifier();
            progressReporter.showProgress("Processing Method " + methodName);
            // Gets hash and signature pair of this method
            Pair<String, String> thisMethodHashAndSign =
                    CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(node, filePath);
            String methodQname = containerBinding.getQualifiedName() + "." + methodName;

            // If we got the hash of this method, then add it to the global
            // data-structures
            if (thisMethodHashAndSign != null && thisMethodHashAndSign.fst != null) {
                IMethodBinding methodBinding = node.resolveBinding();
                // Collect thrown exceptions of this method
                @SuppressWarnings("unchecked")
                List<Type> thrownExceptions = node.thrownExceptionTypes();
                Set<String> exceptionSet = new HashSet<>();
                for (Type exception : thrownExceptions) {
                    exceptionSet.add(exception.toString());
                }

                // Add the default constructor we created as callee if this method
                // is a constructor. So, all constructors will call the default constructor
                if (methodBinding != null && methodBinding.isConstructor()) {
                    // We will add a link to default constructor of this class
                    // only if this() is not present in the first position
                    // Case 8 and 9
                    boolean allowLinkToDefaultConsInThisClass = true;
                    if (!node.getBody().statements().isEmpty()) {
                        Object firstStatementInsideConstructor =
                                node.getBody().statements().get(0);
                        if (firstStatementInsideConstructor instanceof ConstructorInvocation) {
                            allowLinkToDefaultConsInThisClass = false;
                        }
                    }
                    // Case 8 and 9
                    if (allowLinkToDefaultConsInThisClass) {
                        String defaultConsHash = CallGraphDataStructures.getDefaultConstructorFor(containerHash);
                        // Defensive checking, there should be default cons hash for all constructors
                        if (defaultConsHash != null) {
                            CallGraphDataStructures.getCallGraph().addEdge(thisMethodHashAndSign.fst, defaultConsHash);
                            int defaultConsHashIndex = CallGraphDataStructures.getMethodIndexFromHash(defaultConsHash);
                            // Since the default constructor is a virtual method, we do not
                            // create a mapping from a method call. But we create a data type for
                            // this.
                            MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                    methodName, defaultConsHashIndex, defaultConsHash, false);
                        }
                    }
                    // Link to a super class constructor if certain conditions are met
                    // Case 3 and 4 (Filter Case 5, 6, 7, 8)
                    addCGEdgeFromThisClassToSuperClassConstructor(
                            thisMethodHashAndSign.fst, containerBinding, node.getBody(), null);
                }
                List<Class<?>> clazzes = new ArrayList<>(2);
                clazzes.add(MethodInvocation.class);
                clazzes.add(ClassInstanceCreation.class);
                clazzes.add(ConstructorInvocation.class);
                clazzes.add(SuperConstructorInvocation.class);
                clazzes.add(SuperMethodInvocation.class);
                clazzes.add(EnhancedForStatement.class);
                // A static reference is most of the time @QualifiedName but also can be
                // referenced without qualifier within an instance of the same class or sub-class,
                // so we need to process the simple names too.
                // add @Name.class to the list to extract all references
                clazzes.add(Name.class);

                // Collect all the throw statements inside this method
                clazzes.add(ThrowStatement.class);
                clazzes.add(SynchronizedStatement.class);
                Map<Class<?>, List<ASTNode>> foundNodes = new HashMap<>(2);
                ASTNodeUtility.findAndPopulateAllInThisScope(node, clazzes, foundNodes);

                List<ASTNode> throwStatements = foundNodes.get(ThrowStatement.class);
                if (throwStatements != null) {
                    // For each throw statement find the type of its expression
                    for (ASTNode throwStatement : throwStatements) {
                        Expression expression = ((ThrowStatement) throwStatement).getExpression();
                        if (expression instanceof ClassInstanceCreation) {
                            // The expression is a new Exception creation
                            // Get the name of that exception
                            exceptionSet.add(((ClassInstanceCreation) expression)
                                    .getType()
                                    .toString());
                        } else {
                            // The expression is not a new Exception creation
                            // So, we need to find its type info
                            TypeInfo expressionType = TypeCalculator.typeOf(expression, true);
                            if (expressionType == null) {
                                // Could not find the type info
                                // Nothing to do.
                                continue;
                            }

                            // We have found a type info for the expression
                            // Try to get its exception name
                            String name = CallGraphUtility.getClassNameFromClassHash(expressionType.getName());
                            if (name == null) {
                                // Could not find the name of the exception
                                // Nothing to do
                                continue;
                            }
                            // We got the name
                            // But sometimes the name is a qualified one
                            // So, we will sanitize it to keep only the simple name of the exception
                            int lastDot = name.lastIndexOf(".");
                            exceptionSet.add(lastDot < 0 ? name : name.substring(lastDot + 1));
                        }
                    }
                }

                // There were some callee missing in the big projects. So, we are now
                // taking all the method invocation inside of a method declaration body
                // to add the caller-callee edges found to the call graph if it doesn't
                // already exists.
                processAllMethodInvocations(
                        foundNodes.get(MethodInvocation.class),
                        methodName,
                        thisMethodHashAndSign.fst,
                        methodQname,
                        filePath);
                // Some constructor calls such as - <code>new A()</code> are getting
                // missing in the big projects. So, we are taking all the class instance
                // creation inside this method's body to add them as callees of this method
                processAllClassIntanceCreations(
                        foundNodes.get(ClassInstanceCreation.class), thisMethodHashAndSign.fst, filePath);
                processAllConstructorInvocations(
                        foundNodes.get(ConstructorInvocation.class), thisMethodHashAndSign.fst, filePath);
                processAllSuperConstructorInvocation(
                        foundNodes.get(SuperConstructorInvocation.class),
                        thisMethodHashAndSign.fst,
                        containerHash,
                        filePath);
                processAllSuperMethodInvocation(
                        foundNodes.get(SuperMethodInvocation.class), methodName, thisMethodHashAndSign.fst, filePath);
                processAllStaticFieldReferences(foundNodes.get(Name.class), thisMethodHashAndSign.fst);

                foundNodes.clear();
                progressReporter.showProgress("Ending Method " + node.getName().toString());
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes all static field references and creates caller-callee edges.
     *
     * <p>This method analyzes static field references within a method body to create
     * appropriate call graph edges between the method and the static constructors of
     * the classes containing those static fields.</p>
     *
     * <p>Static references can appear in two forms:</p>
     * <ul>
     *   <li><strong>Qualified Names:</strong> Most common form (e.g., ClassName.fieldName)</li>
     *   <li><strong>Simple Names:</strong> Can be referenced without qualifier within an instance
     *       of the same class or subclass</li>
     * </ul>
     *
     * <p>The method:</p>
     * <ul>
     *   <li>Resolves each reference to its binding</li>
     *   <li>Identifies static fields and their declaring classes</li>
     *   <li>Finds the static constructor for each class containing static fields</li>
     *   <li>Creates call graph edges from the method to the static constructors</li>
     *   <li>Excludes self-references to avoid circular edges</li>
     * </ul>
     *
     * <p>This ensures that static field access triggers the appropriate static initialization
     * in the call graph, maintaining the correct execution order.</p>
     *
     * @param references the list of qualified names and simple names inside the method body
     * @param methodHash the hash of the method containing the static field references
     */
    private void processAllStaticFieldReferences(List<ASTNode> references, String methodHash) {
        if (references != null) {
            for (ASTNode reference : references) {
                IBinding binding = ((Name) reference).resolveBinding();
                if (binding != null && Modifier.isStatic(binding.getModifiers())) {
                    ASTNode fieldDecl = ASTNodeUtility.getDeclaringNode(binding.getJavaElement());
                    if (fieldDecl instanceof FieldDeclaration || fieldDecl instanceof EnumConstantDeclaration) {
                        ITypeBinding containerBinding = CallGraphUtility.getContainerTypeBinding(fieldDecl);
                        if (containerBinding != null && containerBinding.isFromSource()) {
                            Pair<String, String> pair = CallGraphUtility.getClassHashAndSignatureFromSourceTypeBinding(
                                    containerBinding, null);
                            if (pair != null) {
                                String staticConstructor = CallGraphDataStructures.getStaticConstructorFor(pair.fst);
                                // Static constructor hash can be null, for classes
                                // that we do not process like enum, test files .etc.
                                if (staticConstructor != null && !methodHash.equals(staticConstructor)) {
                                    CallGraphDataStructures.getCallGraph().addEdge(methodHash, staticConstructor);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Iterates through all MethodInvocation nodes and adds them as callees to the caller method.
     *
     * <p>This method processes method invocations found within method bodies or initializer blocks
     * to create comprehensive call graph edges. It handles both the extended call graph (for
     * qualified method names) and the main call graph (for method hash-based relationships).</p>
     *
     * <p>The method performs two main processing passes:</p>
     * <ol>
     *   <li><strong>Extended Call Graph:</strong> Creates edges using qualified method names
     *       for high-level method relationship tracking</li>
     *   <li><strong>Main Call Graph:</strong> Creates edges using method hashes for detailed
     *       call graph analysis and method resolution</li>
     * </ol>
     *
     * <p>For each method invocation, it:</p>
     * <ul>
     *   <li>Resolves the method binding to find the servicing method</li>
     *   <li>Handles static vs. non-static method resolution</li>
     *   <li>Calculates method invocation types for polymorphic methods</li>
     *   <li>Creates appropriate call graph edges</li>
     *   <li>Handles library class method resolution through subclass analysis</li>
     * </ul>
     *
     * <p>The method uses a calling context cache to improve performance and handle
     * local variable type resolution accurately.</p>
     *
     * @param methodInvocations the list of method invocations to process
     * @param callerMethodName the name of the caller method
     * @param callerMethodHash the caller method's hash (default constructor hash in case of initializer block)
     * @param callerMethodQname the qualified name of the caller method for extended call graph
     * @param filePath the file path of the compilation unit containing the method
     */
    private void processAllMethodInvocations(
            List<ASTNode> methodInvocations,
            String callerMethodName,
            String callerMethodHash,
            String callerMethodQname,
            String filePath) {
        if (methodInvocations != null && callerMethodQname != null) {
            for (ASTNode methodInvoc : methodInvocations) {
                // For all the method invocations, generate the qualified name
                // of callee methods.
                if (methodInvoc instanceof MethodInvocation invocation) {
                    IMethodBinding mb = invocation.resolveMethodBinding();
                    String qname;

                    if (mb != null) {
                        ITypeBinding typeBinding = mb.getDeclaringClass();
                        String name = invocation.getName().toString();
                        qname = typeBinding.getQualifiedName() + "." + name;
                    } else {
                        qname = TypeCalculator.qualifiedNameOf(invocation, filePath, false);
                    }

                    if (qname != null) {
                        CallGraphDataStructures.getExtendedCallGraph().addEdge(callerMethodQname, qname);
                    }
                }
            }
        }
        if (methodInvocations != null) {
            // From a method method's calling context, store the pair of type info and
            // token range of scope if it is a local variable or field of local variable.
            // Different branch can have local variables with same name and as we are caching
            // against string key, it may give wrong type from cache. So we need this scope.
            // We are not keeping a method identity cache because there may be overloaded methods
            NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache = new NeoLRUCache<>(100);
            for (ASTNode methodInvoc : methodInvocations) {
                try {
                    MethodInvocation invocation = (MethodInvocation) methodInvoc;
                    TokenRange invocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(invocation, filePath);
                    if (CallGraphDataStructures.hasInfoAboutMethodInvocation(invocationTokenRange)) {
                        continue;
                    }
                    Expression expression = invocation.getExpression();

                    // Calculating for the first time
                    // No need to pass the path since the servicing method hash index will not be found
                    int servicingCalleeMethodIndex = CallGraphUtility.getServicingMethodHashIndex(
                            invocation, callerMethodHash, null, methodCallingContextCache);
                    if (servicingCalleeMethodIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                        // For a static method, no need to store the method invocation type since it is
                        // calculated differently. In that case, just add an edge to the call graph and continue
                        if (CallGraphUtility.isStaticMethod(servicingCalleeMethodIndex)) {
                            CallGraphDataStructures.getCallGraph()
                                    .addEdge(
                                            callerMethodHash,
                                            CallGraphDataStructures.getMethodHashFromIndex(servicingCalleeMethodIndex));
                            // No override for a static method, but create a method invocation type
                            Set<Integer> calleeContenderMethodInvocationTypes =
                                    MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                            null,
                                            servicingCalleeMethodIndex,
                                            CallGraphDataStructures.getMethodHashFromIndex(servicingCalleeMethodIndex),
                                            false);
                            if (calleeContenderMethodInvocationTypes != null
                                    && !calleeContenderMethodInvocationTypes.isEmpty()) {
                                CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                        invocationTokenRange,
                                        servicingCalleeMethodIndex,
                                        calleeContenderMethodInvocationTypes);
                            }
                        } else {
                            Set<Integer> calleeContenderMethodInvocationTypes =
                                    CallGraphDataStructures.getMethodInvocationTypes(servicingCalleeMethodIndex);
                            MethodIdentity identityToMatch = null;
                            if (calleeContenderMethodInvocationTypes == null) {
                                // If the container type is parametric type and the calling context
                                // type has more specific information for elementTypes, we can filter out
                                // the proper overriding methods. For example:
                                //      class Container<T> {
                                //          T foo(String s) {}
                                //      }
                                //      class StringContainer extends Container<String> {
                                //          String foo(String s) {}
                                //      }
                                //      class NumberContainer extends Container<Number> {
                                //          Number foo(String s) {}
                                //      }
                                // if the calling-context type is Container<String> the return type is String
                                //      Container<String> c1 = new Container<String>(); // generic one
                                //      Container<String> c2 = new StringContainer();   // matched one
                                // We can drop out method foo of NumberContainer
                                if (expression != null) {
                                    TypeInfo actualContainerType =
                                            TypeCalculator.getCallingContextType(expression, methodCallingContextCache);
                                    if (actualContainerType != null
                                            && actualContainerType instanceof ParameterizedTypeInfo) {
                                        TypeInfo declaredContainerType =
                                                CallGraphDataStructures.getParametricClassTypeInfoMap()
                                                        .get(actualContainerType.getName());
                                        if (declaredContainerType != null) {
                                            Map<TypeInfo, TypeInfo> capturedSymbolicTypes = new HashMap<>(3);
                                            declaredContainerType.parseAndMapSymbols(
                                                    actualContainerType, null, null, capturedSymbolicTypes, null);
                                            if (!capturedSymbolicTypes.isEmpty()) {
                                                MethodIdentity identity =
                                                        CallGraphDataStructures.getMatchingMethodIdentity(
                                                                servicingCalleeMethodIndex);
                                                TypeInfo newRetType = identity.getReturnTypeInfo();
                                                if (newRetType instanceof SymbolicTypeInfo) {
                                                    // get replacement for this symbolic type
                                                    newRetType = capturedSymbolicTypes.get(newRetType);
                                                }
                                                List<TypeInfo> newArgTypeInfos =
                                                        new ArrayList<>(identity.getArgParamTypeInfos()
                                                                .size());
                                                for (TypeInfo argType : identity.getArgParamTypeInfos()) {
                                                    if (argType instanceof SymbolicTypeInfo) {
                                                        // get replacement for this symbolic type
                                                        argType = capturedSymbolicTypes.get(argType);
                                                    }
                                                    newArgTypeInfos.add(argType);
                                                }
                                                identityToMatch = new MethodIdentity(
                                                        identity.getMethodName(), newRetType, newArgTypeInfos);
                                            }
                                        }
                                    }
                                }
                                // The method invocation types have not been calculated before,
                                // So calculate them here
                                Map<Integer, MethodInvocationCalculationScratchPad> scratchPadMap =
                                        MethodMatchFinderUtil.calculateMethodInvocationTypesFromSubtypes(
                                                servicingCalleeMethodIndex);
                                if (scratchPadMap != null) {
                                    Iterator<Map.Entry<Integer, MethodInvocationCalculationScratchPad>> iterator =
                                            scratchPadMap.entrySet().iterator();
                                    while (iterator.hasNext()) {
                                        MethodInvocationCalculationScratchPad scratchPad =
                                                iterator.next().getValue();
                                        MethodInvocationType invocationAtClass = scratchPad.getInvocationType();
                                        if (invocationAtClass != null) {
                                            CallGraphDataStructures.addMethodInvocationType(
                                                    scratchPad.getRelevantMethodIndex(), invocationAtClass);
                                            CallGraphDataStructures.addMethodInvocationTypesFromSubclasses(
                                                    scratchPad.getRelevantMethodIndex(),
                                                    scratchPad.getMethodInvocationTypesFromSubclasses());
                                            if (servicingCalleeMethodIndex == scratchPad.getRelevantMethodIndex()) {
                                                calleeContenderMethodInvocationTypes =
                                                        scratchPad.getMethodInvocationTypesFromSubclasses();
                                            }
                                        }
                                    }
                                }
                            }
                            if (calleeContenderMethodInvocationTypes != null) {
                                // If some methods are abstract/interface methods but they were matched
                                // they should be filtered here
                                Iterator<Integer> iterator = calleeContenderMethodInvocationTypes.iterator();
                                while (iterator.hasNext()) {
                                    int methodHashIndex = iterator.next();
                                    MethodIdentity identity =
                                            CallGraphDataStructures.getMatchingMethodIdentity(methodHashIndex);
                                    // Check for polymorphic methods and interface methods as triggers.
                                    if (identity.isAnAbstactOrInterfaceMethodWithNoBody()) {
                                        iterator.remove();
                                    } else if (methodHashIndex != servicingCalleeMethodIndex
                                            && identityToMatch != null
                                            && !identity.equals(identityToMatch)) {
                                        // We have an specific identity to match which is found from resolving
                                        // the symbolic types from the calling-context,
                                        // drop out the methods that do not match except the top generic method.
                                        iterator.remove();
                                    } else {
                                        CallGraphDataStructures.getCallGraph()
                                                .addEdge(
                                                        callerMethodHash,
                                                        CallGraphDataStructures.getMethodHashFromIndex(
                                                                methodHashIndex));
                                    }
                                }
                                if (!calleeContenderMethodInvocationTypes.isEmpty()) {
                                    CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                            invocationTokenRange,
                                            servicingCalleeMethodIndex,
                                            calleeContenderMethodInvocationTypes);
                                    if (calleeContenderMethodInvocationTypes.size() > 1) {
                                        iterator = calleeContenderMethodInvocationTypes.iterator();
                                        while (iterator.hasNext()) {
                                            int methodHashIndex = iterator.next();
                                            MethodIdentity identity =
                                                    CallGraphDataStructures.getMatchingMethodIdentity(methodHashIndex);
                                            if (identity != null) {
                                                identity.setPossiblyPolymorphicBit();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // If we get an invocation of a library class/interface method
                        // we need to match with all of its implementations/subclasses
                        // to match the method. Otherwise we might lose method in caller-callee
                        // map and those methods would end up being root methods.
                        // As the invoked method is a library class method, we wont find any matching hash
                        // index for the actual method. So, we will use the first matching methods index
                        // and all the methods would be added with that method hash index.
                        String callingContextDeclaredTypeHash =
                                CallGraphUtility.getCallingContextDeclaredTypeHashOfMethodInvocation(
                                        invocation, callerMethodHash, methodCallingContextCache);
                        if (callingContextDeclaredTypeHash != null
                                && callingContextDeclaredTypeHash.startsWith(Constants.LIB_TYPE)) {
                            Set<String> subTypes =
                                    CallGraphDataStructures.getAllSubClass(callingContextDeclaredTypeHash);
                            if (subTypes != null) {
                                Set<Integer> calleeContenderMethodInvocationTypes = new HashSet<>();
                                int matchingMethodHashIndex = Constants.INVALID_METHOD_HASH_INDEX;
                                for (String subType : subTypes) {
                                    if (subType.startsWith(Constants.LIB_TYPE)) {
                                        // Subclass is also a library.
                                        // Ignore it.
                                        continue;
                                    }
                                    // Find the matching method in the subclass
                                    // No need to look recursively in super, as super type
                                    // is from library and we already come from there
                                    MethodIdentity identityFromInvocation = MethodHandler.process(invocation);
                                    int matchingMethodIndex =
                                            MethodMatchFinderUtil.getBestMatchedMethodServicingInvocation(
                                                    subType, identityFromInvocation);
                                    if (matchingMethodIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                                        // No match found.
                                        // So, nothing to do.
                                        continue;
                                    } else if (matchingMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                                        // If it is our first match, save its hash index
                                        matchingMethodHashIndex = matchingMethodIndex;
                                    }
                                    // Add edge to the call graph
                                    CallGraphDataStructures.getCallGraph()
                                            .addEdge(
                                                    callerMethodHash,
                                                    CallGraphDataStructures.getMethodHashFromIndex(
                                                            matchingMethodIndex));
                                    Set<Integer> calleeContenderMethodInvocations =
                                            MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                                    null,
                                                    matchingMethodIndex,
                                                    CallGraphDataStructures.getMethodHashFromIndex(matchingMethodIndex),
                                                    false);
                                    if (calleeContenderMethodInvocations != null
                                            && !calleeContenderMethodInvocations.isEmpty()) {
                                        // If we find a callee contender
                                        // add it to the list
                                        calleeContenderMethodInvocationTypes.addAll(calleeContenderMethodInvocations);
                                    }
                                }
                                if (matchingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX
                                        && !calleeContenderMethodInvocationTypes.isEmpty()) {
                                    // We have found some matching method in the sub class
                                    // Add it to the callee candidates map.
                                    CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                            invocationTokenRange,
                                            matchingMethodHashIndex,
                                            calleeContenderMethodInvocationTypes);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                } catch (Error e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                }
            }
            methodCallingContextCache.clear();
            methodInvocations.clear();
        }
    }

    /**
     * Iterates through all ClassInstanceCreation nodes and adds caller-callee edges.
     *
     * <p>This method processes class instance creation expressions found within method bodies
     * or initializer blocks to create call graph edges between the caller method and the
     * constructors being invoked.</p>
     *
     * <p>The method handles two main scenarios:</p>
     * <ul>
     *   <li><strong>Regular Class Instantiation:</strong> Creates direct edges from the caller
     *       to the constructor of the instantiated class</li>
     *   <li><strong>Anonymous Class Instantiation:</strong> Creates a two-hop path: caller →
     *       anonymous class default constructor → superclass constructor</li>
     * </ul>
     *
     * <p>For each class instance creation, it:</p>
     * <ul>
     *   <li>Resolves the constructor binding to find the servicing method</li>
     *   <li>Handles anonymous class declarations with special processing</li>
     *   <li>Creates appropriate call graph edges</li>
     *   <li>Updates method invocation type information for method resolution</li>
     *   <li>Handles cases where no matching constructor is found</li>
     * </ul>
     *
     * <p>The method ensures that all constructor calls are properly represented in the
     * call graph, maintaining the complete execution flow.</p>
     *
     * @param classInstanceCreations the list of class instance creations to process
     * @param callerMethodHash the caller method's hash (default constructor hash in case of initializer block)
     * @param filePath the file path of the compilation unit containing the class instance creation calls
     */
    private void processAllClassIntanceCreations(
            List<ASTNode> classInstanceCreations, String callerMethodHash, String filePath) {
        if (classInstanceCreations != null) {
            for (ASTNode classCreate : classInstanceCreations) {
                try {
                    TokenRange invocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(classCreate, filePath);
                    if (CallGraphDataStructures.hasInfoAboutMethodInvocation(invocationTokenRange)) {
                        continue;
                    }
                    ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) classCreate;
                    Set<Integer> calleeContenders = null;
                    // Calculating for the first time
                    // No need to pass the path since the servicing method hash index will not be found
                    int consHashIndex =
                            CallGraphUtility.getServicingMethodHashIndex(classInstanceCreation, null, null, null);
                    if (classInstanceCreation.getAnonymousClassDeclaration() != null) {
                        // A class instance creation for an anonymous class.
                        // If we have gotten a matching method, that method will be called from the
                        // default constructor of the anonymous class.
                        AnonymousClassDeclaration anonClass = classInstanceCreation.getAnonymousClassDeclaration();
                        // The file path is the same file since the anon declaration happens in the same file
                        String anonClassHash = CallGraphUtility.getClassHashFromContainerDeclaration(anonClass, filePath);
                        String defaultConsOfAnonClass = null;
                        if (anonClassHash != null) {
                            defaultConsOfAnonClass = CallGraphDataStructures.getDefaultConstructorFor(anonClassHash);
                            if (defaultConsOfAnonClass != null) {
                                int defaultConsHashIndex =
                                        CallGraphDataStructures.getMethodIndexFromHash(defaultConsOfAnonClass);
                                if (defaultConsHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                                    if (consHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                                        // Calling the default constructor of the anon class which calls the servicing
                                        // constructor
                                        // in the super class
                                        CallGraphDataStructures.getCallGraph()
                                                .addEdge(callerMethodHash, defaultConsOfAnonClass);
                                        CallGraphDataStructures.getCallGraph()
                                                .addEdge(
                                                        defaultConsOfAnonClass,
                                                        CallGraphDataStructures.getMethodHashFromIndex(consHashIndex));
                                        // Create information about the servicing method for this class instance
                                        // creation
                                        calleeContenders =
                                                MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                                        null,
                                                        consHashIndex,
                                                        CallGraphDataStructures.getMethodHashFromIndex(consHashIndex),
                                                        false);
                                        if (calleeContenders != null && !calleeContenders.isEmpty()) {
                                            CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                                    invocationTokenRange, consHashIndex, calleeContenders);
                                        }
                                    } else {
                                        // Only calling the default constructor of the anonymous declaration
                                        CallGraphDataStructures.getCallGraph()
                                                .addEdge(callerMethodHash, defaultConsOfAnonClass);
                                        // Create information about this class instance creation
                                        // No overriding here. So only update method invocation type in CG
                                        calleeContenders =
                                                MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                                        null, defaultConsHashIndex, defaultConsOfAnonClass, false);
                                        if (calleeContenders != null
                                                && !calleeContenders.isEmpty()
                                                && consHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                                            CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                                    invocationTokenRange, defaultConsHashIndex, calleeContenders);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // A normal class instance creation.
                        // The method returned is the actual method that is called.
                        if (consHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                            String consHash = CallGraphDataStructures.getMethodHashFromIndex(consHashIndex);
                            CallGraphDataStructures.getCallGraph().addEdge(callerMethodHash, consHash);
                            // Create information about this class instance creation
                            // No overriding here. So only update method invocation type in CG
                            calleeContenders = MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                    null, consHashIndex, consHash, false);
                            if (calleeContenders != null
                                    && !calleeContenders.isEmpty()
                                    && consHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                                CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                        invocationTokenRange, consHashIndex, calleeContenders);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                } catch (Error e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                }
            }
            classInstanceCreations.clear();
        }
    }

    /**
     * Iterates through all SuperMethodInvocation nodes and adds caller-callee edges.
     *
     * <p>This method processes super method invocation expressions found within method bodies
     * to create call graph edges between the caller method and the superclass methods being invoked.</p>
     *
     * <p>Super method invocations are direct calls to methods in the superclass and represent
     * a specific form of method resolution that bypasses the normal polymorphic dispatch.</p>
     *
     * <p>For each super method invocation, it:</p>
     * <ul>
     *   <li>Resolves the super method binding to find the servicing method</li>
     *   <li>Creates call graph edges from the caller to the super method</li>
     *   <li>Updates method invocation type information</li>
     *   <li>Filters out abstract or interface methods without bodies</li>
     *   <li>Handles cases where no matching super method is found</li>
     * </ul>
     *
     * <p>Note: This method does not use the method calling context cache since super method
     * invocations have a fixed resolution path through the inheritance hierarchy.</p>
     *
     * @param superMethodCalls the list of super method calls to process
     * @param callerMethodName the name of the caller method
     * @param callerHash the caller method's hash
     * @param filePath the file path of the compilation unit containing the method invocation
     */
    private void processAllSuperMethodInvocation(
            List<ASTNode> superMethodCalls, String callerMethodName, String callerHash, String filePath) {
        if (superMethodCalls != null) {
            for (ASTNode superMethod : superMethodCalls) {
                try {
                    SuperMethodInvocation node = (SuperMethodInvocation) superMethod;
                    TokenRange invocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(node, filePath);
                    if (CallGraphDataStructures.hasInfoAboutMethodInvocation(invocationTokenRange)) {
                        continue;
                    }
                    // We are not using the method calling context cache for super method invocation.
                    // So the last param is null.
                    // Also, Calculating for the first time
                    // No need to pass the path since the servicing method hash index will not be found
                    int servicingCalleeMethodIndex =
                            CallGraphUtility.getServicingMethodHashIndex(node, callerHash, null, null);
                    if (servicingCalleeMethodIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                        // This is a single method call, no sub classes
                        // So, just point to it
                        String servicingMethodHash =
                                CallGraphDataStructures.getMethodHashFromIndex(servicingCalleeMethodIndex);
                        if (servicingMethodHash != null) {
                            Set<Integer> calleeContenderMethodInvocationTypes =
                                    MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                            node.getName().getIdentifier(),
                                            servicingCalleeMethodIndex,
                                            servicingMethodHash,
                                            true);
                            if (calleeContenderMethodInvocationTypes != null
                                    && !calleeContenderMethodInvocationTypes.isEmpty()) {
                                // If some methods are abstract/interface methods but they were matched
                                // they should be filtered here
                                Iterator<Integer> iterator = calleeContenderMethodInvocationTypes.iterator();
                                while (iterator.hasNext()) {
                                    int methodHashIndex = iterator.next();
                                    MethodIdentity identity =
                                            CallGraphDataStructures.getMatchingMethodIdentity(methodHashIndex);
                                    if (identity.isAnAbstactOrInterfaceMethodWithNoBody()) {
                                        iterator.remove();
                                    } else {
                                        CallGraphDataStructures.getCallGraph()
                                                .addEdge(
                                                        callerHash,
                                                        CallGraphDataStructures.getMethodHashFromIndex(
                                                                methodHashIndex));
                                    }
                                }
                                if (!calleeContenderMethodInvocationTypes.isEmpty()) {
                                    CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                            invocationTokenRange,
                                            servicingCalleeMethodIndex,
                                            calleeContenderMethodInvocationTypes);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                } catch (Error e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                }
            }
            superMethodCalls.clear();
        }
    }

    /**
     * Processes all this() constructor calls inside a method declaration.
     *
     * <p>This method handles constructor invocations that use the this() syntax to call
     * other constructors within the same class. These calls represent constructor chaining
     * where one constructor delegates to another constructor in the same class.</p>
     *
     * <p>For each this() call, the method:</p>
     * <ul>
     *   <li>Identifies the container class of the caller method</li>
     *   <li>Finds the matching constructor based on parameter count and types</li>
     *   <li>Creates call graph edges from the caller to the target constructor</li>
     *   <li>Updates method invocation type information for method resolution</li>
     * </ul>
     *
     * <p>This method is essential for capturing the complete constructor call flow within
     * a class, ensuring that all constructor delegation patterns are properly represented
     * in the call graph.</p>
     *
     * @param thisConsCalls the list of this() constructor calls in a method
     * @param callerHash the hash of the caller method
     * @param filePath the file path for token range calculation
     */
    private void processAllConstructorInvocations(List<ASTNode> thisConsCalls, String callerHash, String filePath) {
        if (thisConsCalls != null) {
            String callerContainerHash = CallGraphUtility.getClassHashFromMethodHash(callerHash);
            if (callerContainerHash != null) {
                for (ASTNode thisCons : thisConsCalls) {
                    try {
                        ConstructorInvocation node = (ConstructorInvocation) thisCons;
                        TokenRange consInvocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(node, filePath);
                        if (CallGraphDataStructures.hasInfoAboutMethodInvocation(consInvocationTokenRange)) {
                            continue;
                        }
                        int consHashIndex = MethodMatchFinderUtil.getHashOfServicingMethod(callerContainerHash, node);
                        if (consHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                            String calleeConstructorHash =
                                    CallGraphDataStructures.getMethodHashFromIndex(consHashIndex);
                            CallGraphDataStructures.getCallGraph().addEdge(callerHash, calleeConstructorHash);
                            // The constructors are not overridden
                            // Create information about this class instance creation
                            // No overriding here. So only update method invocation type in CG.
                            Set<Integer> calleeContenders =
                                    MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                            null, consHashIndex, calleeConstructorHash, false);
                            if (calleeContenders != null && !calleeContenders.isEmpty()) {
                                CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                        consInvocationTokenRange, consHashIndex, calleeContenders);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore and continue processing the next
                        e.printStackTrace();
                    } catch (Error e) {
                        // Ignore and continue processing the next
                        e.printStackTrace();
                    }
                }
            }
            thisConsCalls.clear();
        }
    }

    /**
     * Processes all super() constructor calls inside a method declaration.
     *
     * <p>This method handles constructor invocations that use the super() syntax to call
     * constructors in the superclass. These calls represent the initialization chain
     * where a subclass constructor must initialize its superclass before proceeding.</p>
     *
     * <p>For each super() call, the method:</p>
     * <ul>
     *   <li>Identifies the superclass of the caller's container class</li>
     *   <li>Handles both parameterized and parameterless super() calls</li>
     *   <li>Finds the appropriate superclass constructor based on parameters</li>
     *   <li>Creates call graph edges from the caller to the superclass constructor</li>
     *   <li>Updates method invocation type information for method resolution</li>
     * </ul>
     *
     * <p>The method handles special cases including:</p>
     * <ul>
     *   <li>Library superclasses (which are excluded from processing)</li>
     *   <li>Anonymous type superclasses (which are excluded)</li>
     *   <li>Parameterless super() calls that may link to default constructors</li>
     * </ul>
     *
     * @param superConsCalls the list of super() constructor calls in a method
     * @param callerHash the method hash of the caller
     * @param callerContainerHash the hash of the container class of the caller
     * @param filePath the file path for token range calculation
     */
    private void processAllSuperConstructorInvocation(
            List<ASTNode> superConsCalls, String callerHash, String callerContainerHash, String filePath) {
        if (superConsCalls != null) {
            for (ASTNode superCons : superConsCalls) {
                try {
                    SuperConstructorInvocation node = (SuperConstructorInvocation) superCons;
                    TokenRange superConsInvocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(node, filePath);
                    if (CallGraphDataStructures.hasInfoAboutMethodInvocation(superConsInvocationTokenRange)) {
                        continue;
                    }
                    // The super() call only goes to a class (abstract or concrete).
                    // An interface has no constructors.
                    String superclassHash = CallGraphDataStructures.getSuperClassOf(callerContainerHash);
                    if (superclassHash == null
                            || superclassHash.startsWith(Constants.LIB_TYPE)
                            || superclassHash.contains(CallGraphUtility.CG_ANONYMOUS_TYPE)) {
                        // There must be a super class in order to have a
                        // super constructor. Library class also does not count.
                        // So skipping.
                        // Not sure why the anonymous type is needed, but kept to be
                        // compatible with previous code.
                        continue;
                    }
                    // The super constructor may be a regular or a default constructor
                    // Case 5 and 6
                    if (((SuperConstructorInvocation) superCons).arguments().isEmpty()) {
                        // Case 5 and 6
                        // Since an anonymous inner class does not have any implemented constructors,
                        // there is no way to have a super constructor there. So, the last actual param is null
                        addCGEdgeToAppropriateSuperClassConstructor(
                                superclassHash, callerHash, null, superConsInvocationTokenRange);
                    } else {
                        // Find the actual constructor
                        int superConsHashIndex = MethodMatchFinderUtil.getHashOfServicingMethod(superclassHash, node);
                        if (superConsHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                            String superConsHash = CallGraphDataStructures.getMethodHashFromIndex(superConsHashIndex);
                            CallGraphDataStructures.getCallGraph().addEdge(callerHash, superConsHash);
                            // The constructors are not overridden
                            // Create information about this class instance creation
                            // No overriding here. So only update method invocation type in CG.
                            Set<Integer> calleeContenders =
                                    MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                                            null, superConsHashIndex, superConsHash, false);
                            if (calleeContenders != null && !calleeContenders.isEmpty()) {
                                CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                                        superConsInvocationTokenRange, superConsHashIndex, calleeContenders);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                } catch (Error e) {
                    // Ignore and continue processing the next
                    e.printStackTrace();
                }
            }
            superConsCalls.clear();
        }
    }

    /**
     * Creates a Call Graph edge between a class's constructor and its superclass's constructor.
     *
     * <p>This method establishes the inheritance relationship in the call graph by linking
     * constructors to their appropriate superclass constructors. The linking behavior varies
     * based on the constructor type and context.</p>
     *
     * <p>The method handles several scenarios:</p>
     * <ul>
     *   <li><strong>Default Constructor:</strong> Links to superclass's default or zero-param
     *       constructor if no implemented constructors exist in the current class</li>
     *   <li><strong>Regular Constructor:</strong> Links to superclass only if there are no
     *       this() or super() calls as the first statement inside the constructor</li>
     *   <li><strong>Anonymous Inner Class:</strong> Links the default constructor to the
     *       parent class's constructor based on the instance creation expression</li>
     * </ul>
     *
     * <p>The method performs several validation checks:</p>
     * <ul>
     *   <li>Verifies the superclass exists and is not a library class</li>
     *   <li>Checks if the superclass file is excluded (e.g., test files)</li>
     *   <li>Validates constructor chaining rules (this()/super() calls)</li>
     *   <li>Ensures proper inheritance hierarchy navigation</li>
     * </ul>
     *
     * @param constructorHash the hash value of the constructor (regular or default)
     * @param classBinding the type binding of the class for which the constructor is passed
     * @param methodBodyOfAnImplementedConstructor null for a default constructor, or the method body
     *        for a regular implemented constructor
     * @param anonymousInnerInstanceCreation if not null, the anonymous inner class instance creation
     *        expression that determines the parent constructor choice
     */
    private void addCGEdgeFromThisClassToSuperClassConstructor(
            String constructorHash,
            ITypeBinding classBinding,
            Block methodBodyOfAnImplementedConstructor,
            ASTNode anonymousInnerInstanceCreation) {
        String hashOfConstructorContainer = CallGraphUtility.getClassHashFromMethodHash(constructorHash);
        if (hashOfConstructorContainer != null) {
            String superClassHash = CallGraphDataStructures.getSuperClassOf(hashOfConstructorContainer);
            if (superClassHash != null && !superClassHash.startsWith(Constants.LIB_TYPE)) {
                boolean allowedToLinkToSuperClassConstructors = true;
                String superClassFilePath = CallGraphUtility.getFileNameFromHash(superClassHash);
                if (superClassFilePath == null || CallGraphDataStructures.isExcludedFile(superClassFilePath)) {
                    // Since we are navigating to super classes, we may end up being in a class
                    // that has not been processed because it is a test file. In such a case, the
                    // class may not have a class signature (hence, the file path will not be found)
                    // or we may get the file path and it is amongst the set of test files.
                    allowedToLinkToSuperClassConstructors = false;
                } else {
                    if (anonymousInnerInstanceCreation != null) {
                        // For an anonymous class there are no implemented constructors,
                        // so we link the default constructor with a default
                        // implemented constructor in the parent.
                        // In case of implemented constructors in parent, which constructor to
                        // choose depends on the anonymous instance creation expression
                    } else {
                        if (methodBodyOfAnImplementedConstructor == null) {
                            // Only if the current class has only default constructor,
                            // then we link the current class's default constructor to
                            // a callee in the super class. If the current class
                            // has any implemented constructor, then no link to above
                            // Case 3, 4, (Cases 5, 6, 7, 8 are filtered)
                            //
                            // Note, this check could be added at the top, But added it here
                            // Since the filtering at the top level (no need to link at all
                            // because no super class) appears to be less expensive
                            IMethodBinding[] thisClassMethods = classBinding.getDeclaredMethods();
                            for (IMethodBinding method : thisClassMethods) {
                                if (method.isConstructor() && !method.isDefaultConstructor()) {
                                    allowedToLinkToSuperClassConstructors = false;
                                    break;
                                }
                            }
                        } else {
                            // Get the first statement inside method body
                            if (!methodBodyOfAnImplementedConstructor
                                    .statements()
                                    .isEmpty()) {
                                Object firstStatementInsideConstructor = methodBodyOfAnImplementedConstructor
                                        .statements()
                                        .get(0);
                                // Filtering cases 5, 6, 7, 8
                                if ((firstStatementInsideConstructor instanceof SuperConstructorInvocation)
                                        || (firstStatementInsideConstructor instanceof ConstructorInvocation)) {
                                    allowedToLinkToSuperClassConstructors = false;
                                }
                            }
                        }
                    }
                }
                if (allowedToLinkToSuperClassConstructors) {
                    addCGEdgeToAppropriateSuperClassConstructor(
                            superClassHash, constructorHash, anonymousInnerInstanceCreation, null);
                }
            }
        }
    }

    /**
     * Adds an edge to the appropriate superclass constructor.
     *
     * <p>This method finds the appropriate constructor in the superclass that will serve
     * as the callee node for the call graph edge. The choice of constructor depends on
     * the context and type of the calling constructor.</p>
     *
     * <p>The method handles two main scenarios:</p>
     * <ul>
     *   <li><strong>Regular Classes:</strong> Implements cases 1, 2, 3, and 4. If the
     *       superclass has an actual zero-param constructor, that is used; otherwise,
     *       the virtual default constructor of the superclass is used.</li>
     *   <li><strong>Anonymous Inner Classes:</strong> The constructor choice is dictated
     *       by the anonymous class instance creation expression, which determines the
     *       appropriate parent constructor to call.</li>
     * </ul>
     *
     * <p>For anonymous classes, the method:</p>
     * <ul>
     *   <li>Finds the matching constructor based on the instance creation expression</li>
     *   <li>Falls back to the default constructor if no match is found</li>
     *   <li>Calculates the appropriate token range for the call graph edge</li>
     *   <li>Creates the edge and updates method invocation type information</li>
     * </ul>
     *
     * @param superClassHash the hash of the superclass
     * @param callerHash the hash of the caller method
     * @param anonymousInnerInstanceCreation if not null, the anonymous inner class instance creation
     *        expression that determines the parent constructor choice
     * @param superConsInvocationTokenRange the token range for the super constructor invocation
     */
    private void addCGEdgeToAppropriateSuperClassConstructor(
            String superClassHash,
            String callerHash,
            ASTNode anonymousInnerInstanceCreation,
            TokenRange superConsInvocationTokenRange) {
        Set<Integer> calleeContenders = null;
        String superClassConstHash = null;
        int superClassConstIndex = Constants.INVALID_METHOD_HASH_INDEX;
        if (anonymousInnerInstanceCreation == null) {
            // Implements case 1 (and 3) and case 2 (and 4)
            //
            // The subclass has a default constructor that is the caller (param 1)
            // If the superclass has an actual 0-param constructor, then that is the
            // callee, otherwise the virtual default constructor of the superclass
            // is the callee method
            String implementedZeroParamConsHash =
                    MethodMatchFinderUtil.getMatchingConstructorBasedOnParamCount(superClassHash, 0);
            int implementedZeroParamConstructor = Constants.INVALID_METHOD_HASH_INDEX;
            if (implementedZeroParamConsHash != null) {
                implementedZeroParamConstructor =
                        CallGraphDataStructures.getMethodIndexFromHash(implementedZeroParamConsHash);
            }
            if (implementedZeroParamConstructor != Constants.INVALID_METHOD_HASH_INDEX) {
                superClassConstHash = CallGraphDataStructures.getMethodHashFromIndex(implementedZeroParamConstructor);
                superClassConstIndex = implementedZeroParamConstructor;
            } else {
                // Case 1 (and 3)
                superClassConstHash = CallGraphDataStructures.getDefaultConstructorFor(superClassHash);
                superClassConstIndex = CallGraphDataStructures.getMethodIndexFromHash(superClassConstHash);
            }
        } else {
            // When we have an anonymous constructor instantiation expression passed, then we have to match the
            // constructor that matches the constructor called in the expression
            int servicingConsIndex =
                    MethodMatchFinderUtil.getHashOfServicingMethod(superClassHash, anonymousInnerInstanceCreation);
            if (servicingConsIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                superClassConstHash = CallGraphDataStructures.getMethodHashFromIndex(servicingConsIndex);
                superClassConstIndex = servicingConsIndex;
            } else {
                // Case 1 (and 3)
                superClassConstHash = CallGraphDataStructures.getDefaultConstructorFor(superClassHash);
                superClassConstIndex = CallGraphDataStructures.getMethodIndexFromHash(superClassConstHash);
            }
            // For anonymous, we passed the token range as null.
            // This is an edge from an anonymous class's default constructor
            // to its actual super class's implemented constructor.
            // In this context, the token range was not passed.
            // So, we calculate the token range. The token range will
            // be stored against the anonymous class declaration body.
            // The class instance creation call (new ..) associated will
            // be used to get to the class body. The new call had its own information
            // stored regarding the link from the new call to the default cons.
            // That is why we cannot use that for the token range of the default constructor.
            // Note, a default cons is not a method call, but we still store the info here.
            String containerHash = CallGraphUtility.getClassHashFromMethodHash(callerHash);
            if (containerHash != null) {
                String filePath = CallGraphUtility.getFileNameFromHash(containerHash);
                if (filePath != null) {
                    AnonymousClassDeclaration anonymousClass = null;
                    if (anonymousInnerInstanceCreation instanceof EnumConstantDeclaration) {
                        anonymousClass = ((EnumConstantDeclaration) anonymousInnerInstanceCreation)
                                .getAnonymousClassDeclaration();
                    } else if (anonymousInnerInstanceCreation instanceof ClassInstanceCreation) {
                        anonymousClass =
                                ((ClassInstanceCreation) anonymousInnerInstanceCreation).getAnonymousClassDeclaration();
                    }
                    if (anonymousClass != null) {
                        superConsInvocationTokenRange =
                                ASTNodeUtility.getTokenRangeFromNode(anonymousClass, filePath);
                    }
                }
            }
        }
        if (superClassConstIndex != Constants.INVALID_METHOD_HASH_INDEX) {
            CallGraphDataStructures.getCallGraph().addEdge(callerHash, superClassConstHash);
            // The constructors are not overridden
            // Create information about this class instance creation
            // No overriding here. So only update method invocation type in CG
            calleeContenders = MethodMatchFinderUtil.updateCGAndGetSingleServicingMethodInvocation(
                    null, superClassConstIndex, superClassConstHash, false);
            if (calleeContenders != null && !calleeContenders.isEmpty()) {
                if (superConsInvocationTokenRange != null) {
                    CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                            superConsInvocationTokenRange, superClassConstIndex, calleeContenders);
                }
            }
        }
    }

    /**
     * Adds field initialization method invocations and class creations to constructors.
     *
     * <p>This method processes field initializers to create call graph edges between
     * constructors and the method invocations or class creations that occur during
     * field initialization.</p>
     *
     * <p>The method handles two types of field initializers:</p>
     * <ul>
     *   <li><strong>Instance Field Initializers:</strong> Linked to the default constructor</li>
     *   <li><strong>Static Field Initializers:</strong> Linked to the static constructor</li>
     * </ul>
     *
     * <p>For each field with initializers, the method:</p>
     * <ul>
     *   <li>Finds all method invocations, class instance creations, and super method invocations</li>
     *   <li>Processes enum constant declarations and static field references</li>
     *   <li>Creates call graph edges from the appropriate constructor to the servicing methods</li>
     *   <li>Handles both instance and static field initialization patterns</li>
     * </ul>
     *
     * <p>This ensures that field initialization logic is properly represented in the call graph,
     * maintaining the correct execution order and dependencies.</p>
     *
     * @param declaringContainerBinding the container (class or anonymous class) binding
     * @param declaringContainerHash the container class hash
     * @param defaultConstructorHash the hash of the default constructor used as caller for instance initializers
     * @param staticConstructorHash the hash of the static constructor used as caller for static initializers
     */
    private void addFieldInitsToConstructors(
            ITypeBinding declaringContainerBinding,
            String declaringContainerHash,
            String defaultConstructorHash,
            String staticConstructorHash) {
        if (CallGraphUtility.isResolvableType(declaringContainerBinding)) {
            String filePath = CallGraphUtility.getFileNameFromHash(declaringContainerHash);
            if (filePath != null) {
                String constructorName =
                        declaringContainerBinding.getQualifiedName() + "." + declaringContainerBinding.getName();
                List<TokenRange> instanceFields =
                        CallGraphDataStructures.getFieldsFromClassHash(declaringContainerHash);
                if (instanceFields != null) {
                    final List<Class<?>> classes = new ArrayList<>(2);
                    classes.add(MethodInvocation.class);
                    classes.add(ClassInstanceCreation.class);
                    classes.add(SuperMethodInvocation.class);
                    classes.add(EnumConstantDeclaration.class);
                    // A static reference is most of the time @QualifiedName but also can be
                    // referenced without qualifier within an instance of the same class or sub-class,
                    // so we need to process the simple names too.
                    // add @Name.class to the list to extract all references
                    classes.add(Name.class);

                    Map<Class<?>, List<ASTNode>> foundNodes = new HashMap<>(2);

                    for (TokenRange tokenRange : instanceFields) {
                        FieldInfo existingFieldInfo = CallGraphDataStructures.getFieldInfoFromTokenRange(tokenRange);
                        if (existingFieldInfo != null && !existingFieldInfo.isEmptyOrNullInitializerMap()) {
                            Set<TokenRange> initializerTokenRanges =
                                    existingFieldInfo.getInitializerTokenRanges(declaringContainerHash);
                            if (initializerTokenRanges != null) {
                                // All of the initializers will be being called from the
                                // default constructor. So, we collect them in the same
                                // place then process them together.
                                for (TokenRange range : initializerTokenRanges) {
                                    ASTNode initializer =
                                            ASTNodeUtility.getASTNodeFromTokenRange(range, ASTNode.class);
                                    // For EnumConstantDeclaration which have only the name part,
                                    // it has same token range as the simple name,
                                    // get the parent node for this case.
                                    if (initializer instanceof SimpleName
                                            && initializer.getParent() instanceof EnumConstantDeclaration) {
                                        initializer = initializer.getParent();
                                    }
                                    if (initializer != null) {
                                        ASTNodeUtility.findAndPopulateAllInThisScope(
                                                initializer, classes, foundNodes);
                                    }
                                }
                                String methodHash =
                                        existingFieldInfo.isStatic() ? staticConstructorHash : defaultConstructorHash;
                                // The method invocations, etc., in the initializers
                                // are processed here
                                processAllMethodInvocations(
                                        foundNodes.get(MethodInvocation.class),
                                        "",
                                        methodHash,
                                        constructorName,
                                        filePath);
                                processAllClassIntanceCreations(
                                        foundNodes.get(ClassInstanceCreation.class), methodHash, filePath);
                                processAllSuperMethodInvocation(
                                        foundNodes.get(SuperMethodInvocation.class), "", methodHash, filePath);
                                processAllEnumConstantsDeclarations(
                                        foundNodes.get(EnumConstantDeclaration.class),
                                        methodHash,
                                        declaringContainerHash,
                                        filePath);
                                processAllStaticFieldReferences(foundNodes.get(Name.class), methodHash);
                                foundNodes.clear();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes EnumConstantDeclaration nodes inside an EnumDeclaration.
     *
     * <p>Enum constants are declared with the constant name and arguments for the enum
     * constructor, which is invoked implicitly when the enum constant is accessed. This
     * method creates call graph edges between the static constructor of the enum and
     * the appropriate constructor that matches the enum constant declaration.</p>
     *
     * <p>For each enum constant declaration, the method:</p>
     * <ul>
     *   <li>Creates a MethodIdentity representing the enum constant's constructor call</li>
     *   <li>Finds the best matching constructor in the enum class based on the arguments</li>
     *   <li>Falls back to the default constructor if no matching constructor is found</li>
     *   <li>Creates call graph edges from the static constructor to the enum constructor</li>
     *   <li>Updates method invocation type information for method resolution</li>
     * </ul>
     *
     * <p>This ensures that enum constant initialization is properly represented in the
     * call graph, maintaining the correct execution order when enum constants are accessed.</p>
     *
     * @param enumConstantDeclarations the list of enum constant declaration nodes to process
     * @param callerMethodHash the hash of the caller static constructor
     * @param containerEnumHash the hash of the container enum declaration
     * @param filePath the path of the container file
     */
    private void processAllEnumConstantsDeclarations(
            List<ASTNode> enumConstantDeclarations,
            String callerMethodHash,
            String containerEnumHash,
            String filePath) {
        if (enumConstantDeclarations == null) {
            return;
        }
        for (ASTNode enumConstantDeclaration : enumConstantDeclarations) {
            TokenRange invocationTokenRange = ASTNodeUtility.getTokenRangeFromNode(enumConstantDeclaration, filePath);
            // Find the matching constructor of enum
            String constructorHash;
            MethodIdentity identity = MethodHandler.process(enumConstantDeclaration);
            int methodIndex =
                    MethodMatchFinderUtil.getBestMatchedMethodServicingInvocation(containerEnumHash, identity);
            if (methodIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                constructorHash = CallGraphDataStructures.getMethodHashFromIndex(methodIndex);
            } else {
                // Matching constructor not found, use default constructor.
                constructorHash = CallGraphDataStructures.getDefaultConstructorFor(containerEnumHash);
                methodIndex = CallGraphDataStructures.getMethodIndexFromHash(constructorHash);
            }
            CallGraphDataStructures.getCallGraph().addEdge(callerMethodHash, constructorHash);
            // No override for a enum constructor, but create a method invocation type
            Set<Integer> invocationTypes = new HashSet<>();
            invocationTypes.add(methodIndex);
            CallGraphDataStructures.addToMethodInvocationToCalleeCandidatesMap(
                    invocationTokenRange, methodIndex, invocationTypes);
        }
    }
}
