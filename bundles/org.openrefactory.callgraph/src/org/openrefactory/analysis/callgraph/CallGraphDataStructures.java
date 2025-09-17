/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.callgraph.method.MethodInfoBundle;
import org.openrefactory.analysis.callgraph.method.MethodInvocationType;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.HashUtility;
import org.openrefactory.util.LinkedListUtility;
import org.openrefactory.util.datastructure.IntObjectPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.datastructure.TokenRangeComparator;
import org.openrefactory.util.manager.C2SManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Global static data structures for various Call Graph operations.
 *
 * <p>The CallGraphDataStructures class serves as the central repository for all
 * data structures used throughout the call graph analysis process. It maintains
 * comprehensive information about classes, methods, fields, and their relationships
 * in a Java program.</p>
 * 
 * <p>This class is organized into several logical sections:</p>
 * <ul>
 *   <li><strong>Core Call Graph:</strong> Main call graph and root methods</li>
 *   <li><strong>Class Identity:</strong> Class hashes, signatures, and type information</li>
 *   <li><strong>Class Relationships:</strong> Inheritance, interfaces, and inner classes</li>
 *   <li><strong>Fields:</strong> Field information and container mappings</li>
 *   <li><strong>Methods:</strong> Method hashes, identities, and invocation types</li>
 *   <li><strong>Housekeeping:</strong> Progress tracking and file exclusion</li>
 * </ul>
 * 
 * <p>The class uses thread-safe collections and provides synchronized access to
 * critical data structures to support concurrent call graph construction and analysis.</p>
 * 
 * <p>All data structures are initialized in the {@link #initialize()} method and
 * can be cleaned up using the {@link #deinitialize()} method for reuse or cleanup.</p>
 *
 * @author Mohammad Rafid Ul Islam, Munawar Hafiz
 */
public class CallGraphDataStructures {

    /**
     * ************************************************
     *
     * Core data structures about the call graph
     *
     * ************************************************
     */

    /** 
     * The global object which contains the call graph.
     * 
     * <p>This field holds the main CallGraph instance that represents all
     * method call relationships in the analyzed program.</p>
     */
    private static CallGraph callGraph;

    /** 
     * A set of root methods that do not have any callers.
     * 
     * <p>Root methods represent entry points in the call graph - methods
     * that are called but don't call any other methods. These are typically
     * main methods, test methods, or methods called from external sources.</p>
     * 
     * <p>The set is thread-safe using ConcurrentHashMap.newKeySet() to support
     * concurrent access during call graph construction.</p>
     */
    private static Set<String> rootMethods;

    /** 
     * The call graph to dump in JSON format.
     * 
     * <p>This ExtendedCallGraph instance provides additional functionality
     * for serializing the call graph data to JSON format for external
     * analysis or persistence.</p>
     */
    private static ExtendedCallGraph extendedCallGraph;

    /**
     * ************************************************
     *
     * Core data structures about identity of a class
     *
     * ************************************************
     */

    /** 
     * Maps from a token range to corresponding class hash.
     * 
     * <p>This is the fundamental identity mapping that connects AST nodes and
     * type bindings to class hashes. We can get a token range from an AST node
     * or a type binding, and from that we can find the hash to delve into more
     * detailed class information.</p>
     */
    private static Map<TokenRange, String> tokenRangeToClassHashMap;

    /** 
     * Mapping from binding information to class hash.
     * 
     * <p>This mapping is used very frequently as we ask for class hash info
     * from bindings all the time. The binding is converted to a SHA-1 hash
     * for easy comparison and storage.</p>
     * 
     * <p>We store the class hash from two sources:</p>
     * <ul>
     *   <li>At the start of phase 1 when we get it directly</li>
     *   <li>When we find alternative bindings that map to the same class hash
     *       (e.g., when some fields may not be resolved in some bindings)</li>
     * </ul>
     * 
     * <p>We consciously do not save bindings from anonymous class declarations
     * as their bindings may be problematic (token range mismatches, etc.).</p>
     */
    private static Map<String, String> bindingHashToClassHashMap;

    /** 
     * A map containing the hash codes of class signatures as keys
     * and the class signatures as values.
     * 
     * <p>This provides a reverse lookup from class hashes to their
     * original signature strings.</p>
     */
    private static Map<String, String> hashToClassSignatureMap;

    /** 
     * A class hash to BitSet bit index mapping.
     * 
     * <p>This map assigns a unique integer bit index to each class hash,
     * enabling efficient bit-based operations for class relationships.</p>
     */
    private static Map<String, Integer> classHashToBitMap;

    /** 
     * A BitSet bit index to class hash reverse mapping.
     * 
     * <p>This provides the reverse lookup from bit indices back to
     * class hashes for efficient bidirectional access.</p>
     */
    private static Map<Integer, String> bitToClassHashMap;

    /** 
     * Map for a class hash and the corresponding soft type.
     * 
     * <p>During call graph phase 1, we create soft types because we don't
     * store field information yet. This type info is used for method matching.</p>
     * 
     * <p>For source code classes, the key is the class hash. For library classes,
     * the key is a string with a special prefix followed by a special separator (:).
     * We use class ID instead of class hash to accommodate these special library keys.</p>
     */
    private static Map<String, TypeInfo> classIdToSoftTypeInfoMap;

    /** 
     * Map for a class hash and the corresponding proper type.
     * 
     * <p>During call graph phase 3, we create proper type information after
     * phase 2 has populated the fields properly. This map is kept and reused
     * during points-to calculation and is used for creating locations where
     * field information is needed.</p>
     * 
     * <p>For source code classes, the key is the class hash. For library classes,
     * the key is a string with a special prefix followed by a special separator (:).</p>
     */
    private static Map<String, TypeInfo> classIdToProperTypeInfoMap;

    /** 
     * A map from class hash to soft TypeInfo for parametric type classes.
     * 
     * <p>This is similar to classIdToSoftTypeInfoMap but only stores parametric
     * type classes from source code. This is a temporary map needed in CG phase 4
     * and will be cleared after use.</p>
     */
    private static Map<String, TypeInfo> parametricClassToTypeInfoMap;

    /** 
     * Store nested class hashes for reducing ITypeBinding calculations.
     * 
     * <p>This set caches nested class information to avoid repeated
     * ITypeBinding calculations in later stages of analysis.</p>
     */
    private static Set<String> nestedClassHashes;
    
    /** 
     * Store static class hashes for reducing ITypeBinding calculations.
     * 
     * <p>This set caches static class information to avoid repeated
     * ITypeBinding calculations in later stages of analysis.</p>
     */
    private static Set<String> staticClassHashes;

    /**
     * *********************************************************
     *
     * Core data structures about relationships between classes
     *
     * *********************************************************
     */

    /** 
     * A class hash mapped with a pair of its super classes and super interfaces sets.
     * 
     * <p>This is used as a temporary record of all the super classes and interfaces
     * so we can populate the subclasses and fields data structures properly.</p>
     * 
     * <p>We use Set&lt;Integer&gt; instead of SparseBitSet because SparseBitSet
     * was becoming inconsistent and throwing exceptions.</p>
     */
    private static Map<String, Pair<Set<Integer>, Set<Integer>>> classHashToSuperClassesAndReachableInterfaces;

    /** 
     * A map of super class to all of its reachable subclasses.
     * 
     * <p>This map tracks the complete inheritance hierarchy, allowing efficient
     * lookup of all subclasses that inherit from a given superclass.</p>
     * 
     * <p>We use Set&lt;Integer&gt; instead of SparseBitSet because SparseBitSet
     * was becoming inconsistent and throwing exceptions.</p>
     */
    private static Map<String, Set<Integer>> allReachableSubClassesMap;

    /** 
     * A map of a class to its immediate super class.
     * 
     * <p>This map tracks the direct parent-child relationship in the
     * inheritance hierarchy, providing quick access to immediate superclasses.</p>
     */
    private static Map<String, String> immediateSuperClassMap;

    /** 
     * A map of a class/interface and its directly implemented interfaces.
     * 
     * <p>This map tracks which interfaces a class directly implements,
     * excluding inherited interface implementations.</p>
     */
    private static Map<String, Set<String>> directlyImplementedInterfaces;

    /** 
     * A map of a class to all of its inner classes.
     * 
     * <p>This map tracks the containment relationship between outer classes
     * and their nested inner classes.</p>
     */
    private static Map<String, Set<String>> innerClassMap;

    /** 
     * A map of an inner class to its immediate enclosing class.
     * 
     * <p>Note that an inner class can be in a method declaration or in another class.
     * In this data structure, we store only the enclosing class. The enclosing method
     * for a method-local inner class is stored in the other data structure.</p>
     * 
     * <p>There are some invariants: the size of the immediate enclosing method map
     * must be smaller than or equal to the size of the immediate enclosing class map.</p>
     */
    private static Map<String, String> immediateEnclosingClassMap;
    
    /** 
     * A map of an inner class to its immediate enclosing method.
     * 
     * <p>This tracks method-local inner classes that are declared within
     * method bodies rather than directly within a class.</p>
     */
    private static Map<String, String> immediateEnclosingMethodMap;

    /** 
     * Mapping from a file path to the imports in that file.
     * 
     * <p>For imports like:</p>
     * <pre>
     *   import a.b.c;
     *   import p.q.*;
     * </pre>
     * <p>We keep "a.b.c" and "p.q.*" under the file name.</p>
     */
    private static Map<String, Set<String>> filePathToImportsMap;

    /** 
     * All imported packages in the project.
     * 
     * <p>This set maintains a global view of all package imports
     * across the entire project for efficient lookup and analysis.</p>
     */
    public static Set<String> allImportedPackages;

    /**
     * ************************************************
     *
     * Core data structures about fields of a class
     *
     * ************************************************
     */

    /** 
     * A map containing the class hash to its (inherited fields included) fields token ranges.
     * 
     * <p>This map tracks all fields associated with a class, including fields
     * inherited from superclasses. The token ranges provide precise location
     * information for each field in the source code.</p>
     */
    private static Map<String, List<TokenRange>> containerHashToFieldsList;

    /** 
     * A map containing field token range to field information.
     * 
     * <p>This map provides detailed information about each field based on its
     * token range, enabling efficient lookup of field metadata during analysis.</p>
     */
    private static Map<TokenRange, FieldInfo> fieldTokenRangeToFieldInfoMap;

    /**
     * ************************************************
     *
     * Core data structures about methods inside a class
     *
     * ************************************************
     */

    /** 
     * A map from a method hash to an integer index.
     * 
     * <p>This map creates a total order of methods by assigning a unique
     * integer index to each method hash, enabling efficient indexing and
     * array-based access to method information.</p>
     */
    private static Map<String, Integer> methodHashToIndexMap;

    /** 
     * The reverse list of the total order of the method hashes.
     * 
     * <p>This list provides the reverse mapping from method indices back
     * to method hashes, enabling bidirectional access to method information.</p>
     */
    private static List<String> indexToMethodHashList;

    /** 
     * Map from a class's hash to the methods in the class.
     * 
     * <p>This is a nested map structure where the outer map keys on class hash,
     * and the inner map keys on method name and maps to a set of method hash indices.
     * The set is used because of method overloading - multiple methods can have
     * the same name but different signatures.</p>
     */
    private static Map<String, Map<String, Set<Integer>>> classToMethodsMap;

    /** 
     * A list that links a method hash index (list position) to a method identity.
     * 
     * <p>This list maintains the complete method information bundle for each
     * method, indexed by the method's hash index for efficient access.</p>
     */
    private static List<MethodInfoBundle> hashToMethodInfoBundleList;

    /** 
     * A map of a class to its default constructor hash.
     * 
     * <p>This map tracks the default (no-argument) constructor for each class,
     * which is automatically generated by the Java compiler if no explicit
     * constructor is provided.</p>
     */
    private static Map<String, String> classToDefaultConstructorMap;

    /** 
     * A map of a class to its static constructor hash.
     * 
     * <p>This map tracks the static initialization block for each class,
     * which is executed when the class is first loaded.</p>
     */
    private static Map<String, String> classToStaticConstructorMap;

    /** 
     * A mapping from a method declaration's token range to the method's hash.
     * 
     * <p>We store the method hash here as opposed to the hash index because
     * the hash is the common information sought by clients. The token range
     * provides precise location information for the method declaration.</p>
     */
    private static Map<TokenRange, String> methodDeclarationTokenRangeToMethodHash;

    /** 
     * Mapping from a method invocation to an index corresponding to a set of callee methods.
     * 
     * <p>The value is an IntObjectPair where the first integer denotes the original
     * servicing method hash that has been matched, and the integer set denotes the
     * polymorphic methods that are also possible servicing methods for this invocation.</p>
     */
    private static Map<TokenRange, IntObjectPair<Set<Integer>>> methodInvocationToCalleeCandidatesMap;

    /**
     * ************************************************
     *
     * Housekeeping data structures and caches etc.,
     *
     * ************************************************
     */
    private static Map<String, Integer> cgMethodNameToIndex;

    public static int totalFiles;
    public static int totalconflictedRoots;

    // Status updates for callgraph processing part 1, 2, 3 and 4 respectively
    public static AtomicInteger completedFilesOne = new AtomicInteger(0);
    public static AtomicInteger completedFilesTwo = new AtomicInteger(0);
    public static AtomicInteger completedFilesThree = new AtomicInteger(0);
    public static AtomicInteger completedFilesFour = new AtomicInteger(0);
    public static AtomicInteger completedConflictedRoots = new AtomicInteger(0);

    // Set of files that will not be processed further
    // like JUnit test files or files under example directory.
    private static Set<String> filesToExclude;

    // Auto generated protocol buffer files to exclude from
    // further processing.
    private static Set<String> protobufFilesToExclude;

    public static void initialize() {
        callGraph = new CallGraph();
        rootMethods = ConcurrentHashMap.newKeySet();
        extendedCallGraph = new ExtendedCallGraph();

        // General class structure
        tokenRangeToClassHashMap = new ConcurrentHashMap<>();
        bindingHashToClassHashMap = new ConcurrentHashMap<>();
        hashToClassSignatureMap = new ConcurrentHashMap<>();
        classIdToSoftTypeInfoMap = new ConcurrentHashMap<>();
        classIdToProperTypeInfoMap = new ConcurrentHashMap<>();
        parametricClassToTypeInfoMap = new ConcurrentHashMap<>();
        nestedClassHashes = new HashSet<>();
        staticClassHashes = new HashSet<>();
        classHashToBitMap = new ConcurrentHashMap<>();
        bitToClassHashMap = new ConcurrentHashMap<>();

        // Class relationships
        classHashToSuperClassesAndReachableInterfaces = new ConcurrentHashMap<>();
        allReachableSubClassesMap = new ConcurrentHashMap<>();
        immediateSuperClassMap = new ConcurrentHashMap<>();
        directlyImplementedInterfaces = new ConcurrentHashMap<>();
        innerClassMap = new ConcurrentHashMap<>();
        immediateEnclosingClassMap = new ConcurrentHashMap<>();
        immediateEnclosingMethodMap = new ConcurrentHashMap<>();
        filePathToImportsMap = new ConcurrentHashMap<>();
        allImportedPackages = new HashSet<>();

        // Fields in class
        containerHashToFieldsList = new ConcurrentHashMap<>();
        fieldTokenRangeToFieldInfoMap = new ConcurrentHashMap<>();

        // Methods in class
        methodHashToIndexMap = new ConcurrentHashMap<>();
        indexToMethodHashList = new ArrayList<>();
        classToMethodsMap = new ConcurrentHashMap<>();
        hashToMethodInfoBundleList = new ArrayList<>();
        classToDefaultConstructorMap = new ConcurrentHashMap<>();
        classToStaticConstructorMap = new ConcurrentHashMap<>();
        methodDeclarationTokenRangeToMethodHash = new ConcurrentHashMap<>();
        methodInvocationToCalleeCandidatesMap = new ConcurrentHashMap<>();

        // Other
        cgMethodNameToIndex = new ConcurrentHashMap<>();
        filesToExclude = new HashSet<>();
        protobufFilesToExclude = ConcurrentHashMap.newKeySet();
    }

    public static void deinitialize() {
        callGraph.deinitialize();
        rootMethods.clear();

        // General class structure
        tokenRangeToClassHashMap.clear();
        bindingHashToClassHashMap.clear();
        hashToClassSignatureMap.clear();
        classIdToSoftTypeInfoMap.clear();
        classIdToProperTypeInfoMap.clear();
        nestedClassHashes.clear();
        staticClassHashes.clear();
        classHashToBitMap.clear();
        bitToClassHashMap.clear();

        // Class relationships
        classHashToSuperClassesAndReachableInterfaces.clear();
        allReachableSubClassesMap.clear();
        immediateSuperClassMap.clear();
        directlyImplementedInterfaces.clear();
        innerClassMap.clear();
        immediateEnclosingClassMap.clear();
        immediateEnclosingMethodMap.clear();
        filePathToImportsMap.clear();
        allImportedPackages.clear();

        // Fields in class
        containerHashToFieldsList.clear();
        fieldTokenRangeToFieldInfoMap.clear();

        // Methods in class
        methodHashToIndexMap.clear();
        indexToMethodHashList.clear();
        classToMethodsMap.clear();
        hashToMethodInfoBundleList.clear();
        classToDefaultConstructorMap.clear();
        classToStaticConstructorMap.clear();
        methodDeclarationTokenRangeToMethodHash.clear();
        methodInvocationToCalleeCandidatesMap.clear();

        // Others
        cgMethodNameToIndex.clear();
        filesToExclude.clear();
        protobufFilesToExclude.clear();
    }

    public static CallGraph getCallGraph() {
        return callGraph;
    }

    public static void setCallGraph(CallGraph callGraph) {
        CallGraphDataStructures.callGraph = callGraph;
    }

    public static ExtendedCallGraph getExtendedCallGraph() {
        return extendedCallGraph;
    }

    public static void addToRootMethods(String methodHash) {
        // Don't add a method to root methods
        // if it is from a test file as test files are excluded
        String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(methodHash);
        if (methodSignature != null) {
            // For dummy methods, method signature is null
            // So, add a check
            String fileName = CallGraphUtility.getFileNameFromSignature(methodSignature);
            if (CallGraphDataStructures.isExcludedFile(fileName) || CallGraphDataStructures.isAProtoFile(fileName)) {
                return;
            }
        }
        rootMethods.add(methodHash);
    }

    public static void setRootMethods(Set<String> roots) {
        CallGraphDataStructures.rootMethods = roots;
    }

    public static int getRootCount() {
        return rootMethods.size();
    }

    public static Iterator<String> getIteratorForRootMethods() {
        return rootMethods.iterator();
    }

    @SuppressWarnings("rawtypes")
    public static Class<? extends Set> getRootMethodsClassForDeserialization() {
        return rootMethods.getClass();
    }

    public static boolean isRootMethod(String methodHash) {
        return rootMethods.contains(methodHash);
    }

    public static String getClassHashFromTokenRange(TokenRange range) {
        return tokenRangeToClassHashMap.get(range);
    }

    public static void addToTokenRangeToClassHashMap(TokenRange range, String classHash) {
        tokenRangeToClassHashMap.put(range, classHash);
    }

    /**
     * Get the binding hash
     *
     * @param binding the binding which will be used. A hash is generated only if the binding is null
     * @param tokenRange the token range is used in the binding calculation for anonymous class declarations.
     * @return the binding hash, if calculated, or null
     */
    public static String getBindingHash(ITypeBinding binding, TokenRange tokenRange) {
        if (binding != null) {
            if (binding.isAnonymous()) {
                // For an anonymous class binding, generate the binding hash with the extra info from
                // the token range. This is because different anonymous class bindings will be the same.
                // The only way to differentiate is by the actual instantiation of the class, which is
                // done by using token range.
                // If the token range is null, then we cannot risk creating binding hash because
                // a collision may occur in future.
                if (tokenRange != null) {
                    return HashUtility.generateSHA1(binding.toString() + tokenRange.toString());
                }
            } else {
                return HashUtility.generateSHA1(binding.toString());
            }
        }
        return null;
    }

    public static String getClassHashFromBindingHash(String bindingHash) {
        if (bindingHash != null) {
            return bindingHashToClassHashMap.get(bindingHash);
        } else {
            return null;
        }
    }

    public static void addToBindingHashToClassHashMap(String bindingHash, String classHash) {
        bindingHashToClassHashMap.putIfAbsent(bindingHash, classHash);
    }

    public static Map<String, String> getHashToClassSignatureMap() {
        return hashToClassSignatureMap;
    }

    public static String getClassSignatureFromHash(String hash) {
        return hashToClassSignatureMap.get(hash);
    }

    public static void addToHashToClassSignature(String key, String value) {
        hashToClassSignatureMap.putIfAbsent(key, value);
    }

    public static void setHashToClassSignatureMap(Map<String, String> hashToClassSignatureMap) {
        CallGraphDataStructures.hashToClassSignatureMap = hashToClassSignatureMap;
    }

    public static Map<String, Integer> getClassHashToBitMap() {
        return classHashToBitMap;
    }

    public static void addToClassHashToBit(String classHash, Integer bitIndex) {
        classHashToBitMap.put(classHash, bitIndex);
    }

    public static int getBitIndexFromClassHash(String classHash) {
        Integer index = classHashToBitMap.get(classHash);
        if (index == null) {
            index = Constants.INVALID_CLASS_HASH_INDEX;
        }
        return index;
    }

    public static void setClassHashToBitMap(Map<String, Integer> classHashToBitMap) {
        CallGraphDataStructures.classHashToBitMap = classHashToBitMap;
    }

    public static Map<Integer, String> getBitToClassHashMap() {
        return bitToClassHashMap;
    }

    public static void addToBitToClassHash(Integer bitIndex, String classHash) {
        bitToClassHashMap.put(bitIndex, classHash);
    }

    public static String getClassHashFromBitIndex(int bitIndex) {
        return bitToClassHashMap.get(bitIndex);
    }

    public static void setBitToClassHashMap(Map<Integer, String> bitToClassHashMap) {
        CallGraphDataStructures.bitToClassHashMap = bitToClassHashMap;
    }

    public static TypeInfo getSoftTypeInfoFromClassId(String classId) {
        return classIdToSoftTypeInfoMap.get(classId);
    }

    public static void addToClassIdToSoftTypeInfoMap(String classHash, TypeInfo typeInfo) {
        classIdToSoftTypeInfoMap.putIfAbsent(classHash, typeInfo);
        // Store type info in global type to index map.
        TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
    }

    public static TypeInfo getProperTypeInfoFromClassId(String classId) {
        return classIdToProperTypeInfoMap.get(classId);
    }

    public static void addToClassIdToProperTypeInfoMap(String classHash, TypeInfo typeInfo) {
        classIdToProperTypeInfoMap.putIfAbsent(classHash, typeInfo);
        // Store type info in global type to index map.
        TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
    }

    /**
     * Add a set of entries to the classHashToSuperClassesAndReachableInterfaces map
     *
     * @param classHash the class hash against which an entry will be added
     * @param superClassOrInterfaces the super class or interface set that is added
     * @param isInterface T if the passed information is an interface, F otherwise
     */
    public static void addToClassHashToReachableSuper(
            String classHash, Set<Integer> superClassOrInterfaces, boolean isInterface) {
        Pair<Set<Integer>, Set<Integer>> pair = classHashToSuperClassesAndReachableInterfaces.getOrDefault(
                classHash, Pair.of(new HashSet<>(), new HashSet<>()));
        if (isInterface) {
            pair.snd.addAll(superClassOrInterfaces);
        } else {
            pair.fst.addAll(superClassOrInterfaces);
        }
        classHashToSuperClassesAndReachableInterfaces.put(classHash, pair);
    }

    /**
     * Add an entry to the classHashToSuperClassesAndReachableInterfaces map
     *
     * @param classHash the class hash against which an entry will be added
     * @param superClassOrInterface the super class or interface entry that is added
     * @param isInterface T if the passed information is an interface, F otherwise
     */
    public static void addToClassHashToReachableSuper(
            String classHash, int superClassOrInterface, boolean isInterface) {
        Pair<Set<Integer>, Set<Integer>> pair = classHashToSuperClassesAndReachableInterfaces.getOrDefault(
                classHash, Pair.of(new HashSet<Integer>(), new HashSet<Integer>()));

        if (isInterface) {
            pair.snd.add(superClassOrInterface);
        } else {
            pair.fst.add(superClassOrInterface);
        }
        classHashToSuperClassesAndReachableInterfaces.put(classHash, pair);
    }

    public static Map<String, Pair<Set<Integer>, Set<Integer>>> getClassHashToSuperClassesAndReachableInterfaces() {
        return classHashToSuperClassesAndReachableInterfaces;
    }

    /**
     * Adds a subclass to its superclas's subclass list
     *
     * @param superClass
     * @param subClassBitIndex
     */
    public static void addToAllReachableSubClassesMap(String superClass, int subClassBitIndex) {
        Set<Integer> subClasses = allReachableSubClassesMap.getOrDefault(superClass, new HashSet<Integer>());
        subClasses.add(subClassBitIndex);
        allReachableSubClassesMap.put(superClass, subClasses);
    }

    /**
     * <p>Populate subClassesMap and reachableSuper map. Treating interfaces as classes also
     */
    public static void addLibraryTypesToSubClassesAndReachableSuperMap() {
        // Primary data structure loaded from spec json
        HashMap<String, Set<String>> superInterfaceToSubClassesC2SMap = C2SManager.getSuperInterfacesMap();
        HashMap<String, Set<String>> superClassToSubClassesC2SMap = C2SManager.getSuperClassesMap();
        addLibraryTypesToSubClassesAndReachableSuperMapHelper(superInterfaceToSubClassesC2SMap, true);
        addLibraryTypesToSubClassesAndReachableSuperMapHelper(superClassToSubClassesC2SMap, false);

        // The other Library types have been assigned bit indices when their subClasses
        // entry was made in the initial Call Graph building process. But, we filtered
        // out the java.lang.Object class from that. It may cause some adverse effects
        // in the type calculation operations in our SummaryAnalysis phase. So, to avoid
        // any unforeseen problems, we add an explicit entry for java.lang.Object in the
        // bit indices data-structures.
        updateOrGetBitIndexFromClassHash(Constants.JAVA_LANG_OBJECT);
    }

    public static void addLibraryTypesToSubClassesAndReachableSuperMapHelper(
            HashMap<String, Set<String>> map, boolean isInterface) {
        if (map != null) {
            for (String key : map.keySet()) {
                for (String val : map.get(key)) {
                    String superClass = Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + key;
                    String subClass = Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + val;
                    int subclassKey = updateOrGetBitIndexFromClassHash(subClass);
                    int superClassKey = updateOrGetBitIndexFromClassHash(superClass);
                    addToAllReachableSubClassesMap(superClass, subclassKey);
                    addToClassHashToReachableSuper(subClass, superClassKey, isInterface);
                }
            }
        }
    }

    /**
     * Return the bit index of the class hash after optionally creating the class hash
     *
     * @param classHash the class hash for which the bit index is to be calculated
     * @return the bit index for the class hash
     */
    public static synchronized int updateOrGetBitIndexFromClassHash(String classHash) {
        if (classHashToBitMap.containsKey(classHash)) {
            return classHashToBitMap.get(classHash);
        } else {
            int index = CallGraphDataStructures.getBitToClassHashMap().size();
            CallGraphDataStructures.addToBitToClassHash(index, classHash);
            CallGraphDataStructures.addToClassHashToBit(classHash, index);
            return index;
        }
    }

    public static Map<String, Set<Integer>> getAllReachableSubClassesMap() {
        return allReachableSubClassesMap;
    }

    public static void setAllReachableSubClassesMap(Map<String, Set<Integer>> allReachableSubClassesMap) {
        CallGraphDataStructures.allReachableSubClassesMap = allReachableSubClassesMap;
    }

    public static Map<String, String> getSuperClassMap() {
        return immediateSuperClassMap;
    }

    public static void addToImmediateSuperClassMap(String subClass, String superClass) {
        immediateSuperClassMap.putIfAbsent(subClass, superClass);
    }

    public static void setImmediateSuperClassMap(Map<String, String> superClassMap) {
        CallGraphDataStructures.immediateSuperClassMap = superClassMap;
    }

    public static Map<String, Set<String>> getDirectlyImplementedInterfaces() {
        return directlyImplementedInterfaces;
    }

    public static Map<String, Set<String>> getImplementedInterfaces() {
        return directlyImplementedInterfaces;
    }

    public static Set<String> getAllSubClass(String superClassHash) {
        Set<Integer> subClassBits = allReachableSubClassesMap.get(superClassHash);
        if (subClassBits == null) {
            return null;
        }
        Set<String> subClasses = new HashSet<>();
        Iterator<Integer> iterator = subClassBits.iterator();
        while (iterator.hasNext()) {
            String classHash = getClassHashFromBitIndex(iterator.next());
            if (classHash != null) {
                subClasses.add(classHash);
            }
        }
        return subClasses;
    }

    public static Set<Integer> getAllSubClassIndices(String superClassHash) {
        Set<Integer> temp = allReachableSubClassesMap.get(superClassHash);
        if (temp != null) {
            return new HashSet<Integer>(temp);
        } else {
            return null;
        }
    }

    public static String getSuperClassOf(String classHash) {
        return immediateSuperClassMap.get(classHash);
    }

    public static void setImplementedInterfaces(Map<String, Set<String>> implementedInterfaces) {
        CallGraphDataStructures.directlyImplementedInterfaces = implementedInterfaces;
    }

    public static void addToDirectlyImplementedInterfaces(String classOrInterface, String interfaceHash) {
        Set<String> interfaces;
        if (!directlyImplementedInterfaces.containsKey(classOrInterface)) {
            interfaces = ConcurrentHashMap.newKeySet();
        } else {
            interfaces = directlyImplementedInterfaces.get(classOrInterface);
        }
        interfaces.add(interfaceHash);
        directlyImplementedInterfaces.put(classOrInterface, interfaces);
    }

    public static void addToImplementedInterfaces(String classOrInterface, Set<String> interfaceHashes) {
        Set<String> interfaces;
        if (!directlyImplementedInterfaces.containsKey(classOrInterface)) {
            interfaces = ConcurrentHashMap.newKeySet();
        } else {
            interfaces = directlyImplementedInterfaces.get(classOrInterface);
        }
        interfaces.addAll(interfaceHashes);
        directlyImplementedInterfaces.put(classOrInterface, interfaces);
    }

    public static void removeRootMethod(String methodHash) {
        rootMethods.remove(methodHash);
    }

    public static Map<String, Set<String>> getInnerClassMap() {
        return innerClassMap;
    }

    public static void addToInnerClassesMap(String parentClass, String innerClass) {
        Set<String> innerClasses = innerClassMap.get(parentClass);
        if (innerClasses == null) {
            innerClasses = ConcurrentHashMap.newKeySet();
            innerClasses.add(innerClass);
            innerClassMap.put(parentClass, innerClasses);
        } else {
            innerClasses.add(innerClass);
        }
    }

    public static void setInnerClassMap(Map<String, Set<String>> innerClassMap) {
        CallGraphDataStructures.innerClassMap = innerClassMap;
    }

    public static Map<String, String> getImmediateEnclosingClassMap() {
        return immediateEnclosingClassMap;
    }

    public static void addToImmediateEnclosingClassMap(String innerClass, String enclosingClassHash) {
        immediateEnclosingClassMap.putIfAbsent(innerClass, enclosingClassHash);
    }

    public static boolean isAnInnerClass(String classHash) {
        return immediateEnclosingClassMap.containsKey(classHash);
    }

    public static String getEnclosingClassHash(String classHash) {
        return immediateEnclosingClassMap.get(classHash);
    }

    public static void setImmediateEnclosingEntityMap(Map<String, String> immediateEnclosingEntityMap) {
        CallGraphDataStructures.immediateEnclosingClassMap = immediateEnclosingEntityMap;
    }

    public static Map<String, String> getImmediateEnclosingMethodMap() {
        return immediateEnclosingMethodMap;
    }

    public static boolean isAMethodLocalInnerClass(String classHash) {
        return immediateEnclosingMethodMap.containsKey(classHash);
    }

    public static void addToImmediateEnclosingMethodMap(String innerClass, String enclosingMethodHash) {
        immediateEnclosingMethodMap.putIfAbsent(innerClass, enclosingMethodHash);
    }

    public static String getEnclosingMethodHash(String classHash) {
        return immediateEnclosingMethodMap.get(classHash);
    }

    public static void setImmediateEnclosingMethodMap(Map<String, String> immediateEnclosingEntityMap) {
        CallGraphDataStructures.immediateEnclosingMethodMap = immediateEnclosingEntityMap;
    }

    /**
     * <p>Populate file path to imports data structure. File path key should contain all imported package names in that
     * file.
     *
     * @param filePath String file path key to populate
     * @param imports Set of import package names
     */
    public static void addToFilePathToImportsMap(String filePath, Set<String> imports) {
        Set<String> tempImports;
        if (!filePathToImportsMap.containsKey(filePath)) {
            tempImports = ConcurrentHashMap.newKeySet();
        } else {
            tempImports = filePathToImportsMap.get(filePath);
        }
        for (String importString : imports) {
            tempImports.add(importString);
        }
        if (!filePathToImportsMap.containsKey(filePath)) {
            allImportedPackages.addAll(tempImports);
            filePathToImportsMap.put(filePath, imports);
        }
    }

    public static Map<String, Set<String>> getFilePathToImportsMap() {
        return filePathToImportsMap;
    }

    public static Set<String> getAllImports() {
        return allImportedPackages;
    }

    public static void setAllImportedPackages(Set<String> allImports) {
        allImportedPackages = allImports;
    }

    public static void setFilePathToImportsMap(Map<String, Set<String>> importsMap) {
        filePathToImportsMap = importsMap;
    }

    public static void addToContainerHashToFieldsList(String classHash, TokenRange fieldTokenRange) {
        List<TokenRange> currentFields = containerHashToFieldsList.get(classHash);
        if (currentFields != null) {
            LinkedListUtility.insertIntoList(currentFields, fieldTokenRange, new TokenRangeComparator());
        } else {
            currentFields = new LinkedList<>();
            currentFields.add(fieldTokenRange);
            containerHashToFieldsList.put(classHash, currentFields);
        }
    }

    public static void addSuperClassFieldsInContainerHashToFieldList(
            String classHash, List<TokenRange> superFieldsRange) {
        List<TokenRange> currentFields = containerHashToFieldsList.get(classHash);
        if (currentFields != null) {
            currentFields.addAll(0, superFieldsRange);
        } else {
            currentFields = new LinkedList<>();
            currentFields.addAll(superFieldsRange);
            containerHashToFieldsList.put(classHash, currentFields);
        }
    }

    public static boolean containsFieldsForContainer(String containerHash) {
        return containerHashToFieldsList.containsKey(containerHash);
    }

    public static List<TokenRange> getFieldsFromContainerHash(String containerHash) {
        return containerHashToFieldsList.get(containerHash);
    }

    public static void setContainerHashToFieldsList(Map<String, List<TokenRange>> containerHashToFieldsList) {
        CallGraphDataStructures.containerHashToFieldsList = containerHashToFieldsList;
    }

    public static List<TokenRange> getFieldsFromClassHash(String classHash) {
        return containerHashToFieldsList.get(classHash);
    }

    public static Set<String> getDirectlyImplementedInterfaceListOfClass(String classHash) {
        return directlyImplementedInterfaces.get(classHash);
    }

    public static Set<String> getAllImplementedInterfacesOfClass(String classHash) {
        Pair<Set<Integer>, Set<Integer>> allSuperClassAndInterfaces =
                classHashToSuperClassesAndReachableInterfaces.get(classHash);
        if (allSuperClassAndInterfaces == null
                || allSuperClassAndInterfaces.snd == null
                || allSuperClassAndInterfaces.snd.isEmpty()) {
            return null;
        }

        Set<String> allSuperInterfaces = new HashSet<>();
        Iterator<Integer> iterator = allSuperClassAndInterfaces.snd.iterator();
        while (iterator.hasNext()) {
            String interfaceHash = getClassHashFromBitIndex(iterator.next());
            if (interfaceHash != null) {
                allSuperInterfaces.add(interfaceHash);
            }
        }
        return allSuperInterfaces;
    }

    /**
     * Get all super classes of a class.
     *
     * @param classHash Hash of target class
     * @return A set containing super classes hash of target class
     */
    public static Set<String> getAllSuperClassesOfClass(String classHash) {
        Pair<Set<Integer>, Set<Integer>> allSuperClassAndInterfaces =
                classHashToSuperClassesAndReachableInterfaces.get(classHash);
        if (allSuperClassAndInterfaces == null
                || allSuperClassAndInterfaces.fst == null
                || allSuperClassAndInterfaces.fst.isEmpty()) {
            return null;
        }

        Set<String> allSuperClasses = new HashSet<>();
        Iterator<Integer> iterator = allSuperClassAndInterfaces.fst.iterator();
        while (iterator.hasNext()) {
            String superClassHash = getClassHashFromBitIndex(iterator.next());
            if (superClassHash != null) {
                allSuperClasses.add(superClassHash);
            }
        }
        return allSuperClasses;
    }

    /**
     * Get all super classes and interfaces of a class.
     *
     * @param classHash the hash of the class
     * @return a set containing hash of all super classes and interfaces of target class
     */
    public static Set<String> getAllSuperClassesAndImplementedInterfacesOfClass(String classHash) {
        Set<String> allSuperClasses = getAllSuperClassesOfClass(classHash);
        Set<String> allSuperInterfaces = getAllImplementedInterfacesOfClass(classHash);
        if (allSuperClasses == null && allSuperInterfaces == null) {
            return null;
        }
        if (allSuperClasses != null && allSuperInterfaces != null) {
            allSuperClasses.addAll(allSuperInterfaces);
            return allSuperClasses;
        }
        return allSuperClasses != null ? allSuperClasses : allSuperInterfaces;
    }

    /**
     * Look for the name of all the inherited library super classes and super interfaces and put them in a
     * set as well as the original class
     *
     * @param classHash the class to search from
     * @return a Set classHash of all the inherited library super classes and super interfaces
     */
    public static Set<String> collectClassAndAllInheritedLibraryParents(String classHash) {
        Set<String> targetClasses = new HashSet<>();
        Set<String> superClasses = CallGraphDataStructures.getAllSuperClassesAndImplementedInterfacesOfClass(classHash);

        if (classHash.startsWith(Constants.LIB_TYPE)) {
            // The class itself is from library,
            // all of its super must be from library
            // add their names
            targetClasses.add(classHash);
            if (superClasses != null) {
                for (String superClassHash : superClasses) {
                    targetClasses.add(superClassHash);
                }
            }
        } else if (superClasses != null) {
            // The class itself is not from library, it will not give use all the parent library classes
            // get the library classes it extends, then get all the other super library classes from that.
            for (String superClassHash : superClasses) {
                if (superClassHash.startsWith(Constants.LIB_TYPE)) {
                    targetClasses.add(superClassHash);
                    Set<String> nextLevelsuperClasses =
                            CallGraphDataStructures.getAllSuperClassesAndImplementedInterfacesOfClass(superClassHash);
                    if (nextLevelsuperClasses != null) {
                        for (String nextLevelsuperClasseHash : nextLevelsuperClasses) {
                            targetClasses.add(nextLevelsuperClasseHash);
                        }
                    }
                }
            }
        }
        return targetClasses;
    }

    /**
     * Look for all the parents of the class (super classes and super interfaces) and put them in a set as
     * well as the original class
     *
     * @param classHash the class to search from
     * @return a Set that contains the class as well as all of its super classes and super interfaces
     */
    public static Set<String> collectClassAndAllParents(String classHash) {
        Set<String> allClasses = new HashSet<>(7);
        String thisClassName = CallGraphUtility.getClassNameFromClassHash(classHash);
        allClasses.add(thisClassName);

        Pair<Set<Integer>, Set<Integer>> allSuperClassAndInterfaces =
                classHashToSuperClassesAndReachableInterfaces.get(classHash);
        if (allSuperClassAndInterfaces != null) {
            if (allSuperClassAndInterfaces.fst != null) {
                Iterator<Integer> iterator = allSuperClassAndInterfaces.fst.iterator();
                while (iterator.hasNext()) {
                    String superClassHash = getClassHashFromBitIndex(iterator.next());
                    if (superClassHash != null) {
                        String superClassName = CallGraphUtility.getClassNameFromClassHash(superClassHash);
                        allClasses.add(superClassName);
                    }
                }
            }

            if (allSuperClassAndInterfaces.snd != null) {
                Iterator<Integer> iterator = allSuperClassAndInterfaces.snd.iterator();
                while (iterator.hasNext()) {
                    String interfaceHash = getClassHashFromBitIndex(iterator.next());
                    if (interfaceHash != null) {
                        String interfaceName = CallGraphUtility.getClassNameFromClassHash(interfaceHash);
                        allClasses.add(interfaceName);
                    }
                }
            }
        }
        return allClasses;
    }

    public static Map<String, List<TokenRange>> getContainerHashToFieldsList() {
        return containerHashToFieldsList;
    }

    /** Used for loading summary only Do not use otherwise */
    public static Map<String, Integer> getMethodHashToIndexMap() {
        return methodHashToIndexMap;
    }

    /** Used for loading summary only Do not use otherwise */
    public static List<String> getIndexToMethodHashList() {
        return indexToMethodHashList;
    }

    /** Used for loading summary only Do not use otherwise */
    public static Map<String, Map<String, Set<Integer>>> getClassToMethodsMap() {
        return classToMethodsMap;
    }

    /** Used for loading summary only Do not use otherwise */
    public static List<MethodInfoBundle> getHashToMethodInfoBundleList() {
        return hashToMethodInfoBundleList;
    }

    /** Used for loading summary only Do not use otherwise */
    public static Map<TokenRange, String> getMethodDeclarationTokenRangeToMethodHash() {
        return methodDeclarationTokenRangeToMethodHash;
    }

    /**
     * Get a set of matching method hash and {@link MethodIdentity} from a container
     *
     * @param containerHash the container's hash
     * @param methodName the method's name
     * @return a set of matching method hashes
     */
    public static Set<Integer> getMatchingMethodHashes(String containerHash, String methodName) {
        Map<String, Set<Integer>> methodsInsideContainer = classToMethodsMap.get(containerHash);

        if (methodsInsideContainer != null) {
            // Get the method hashes for the methods that have the same name
            return methodsInsideContainer.get(methodName);
        }
        return null;
    }

    public static MethodIdentity getMatchingMethodIdentity(String methodHash) {
        if (methodHashToIndexMap.containsKey(methodHash)) {
            return getMatchingMethodIdentity(methodHashToIndexMap.get(methodHash));
        }
        return null;
    }

    public static MethodIdentity getMatchingMethodIdentity(int methodHashIndex) {
        return hashToMethodInfoBundleList.get(methodHashIndex).getIdentity();
    }

    public static String getMethodHashFromIndex(int methodHashIndex) {
        return indexToMethodHashList.get(methodHashIndex);
    }

    public static int getMethodIndexFromHash(String methodHash) {
        if (methodHashToIndexMap.containsKey(methodHash)) {
            return methodHashToIndexMap.get(methodHash).intValue();
        }
        return Constants.INVALID_METHOD_HASH_INDEX;
    }

    public static void addToClassToMethodsMap(String classHash, String methodName, int methodHashIndex) {
        if (classToMethodsMap.containsKey(classHash)) {
            Map<String, Set<Integer>> innerMap = classToMethodsMap.get(classHash);
            if (innerMap.containsKey(methodName)) {
                innerMap.get(methodName).add(methodHashIndex);
            } else {
                Set<Integer> methodSet = new HashSet<Integer>(1);
                methodSet.add(methodHashIndex);
                classToMethodsMap.get(classHash).put(methodName, methodSet);
            }
        } else {
            Map<String, Set<Integer>> innerMap = new ConcurrentHashMap<>();
            Set<Integer> methodSet = new HashSet<Integer>(1);
            methodSet.add(methodHashIndex);
            innerMap.put(methodName, methodSet);
            classToMethodsMap.put(classHash, innerMap);
        }
    }

    /**
     * Performs update in multiple data structures Hence the method is synchronized
     *
     * @param methodHash the hash to add
     * @return if the hash is available, return the index if the hash is not available, create the entry in the map and
     *     the reverse list. Also create the entry in the method bundle list
     */
    public static synchronized int getMethodHashIndexAndPotentiallyUpdateOtherInitialStructures(
            String methodHash, String methodSignature) {
        int index;
        if (methodHashToIndexMap.containsKey(methodHash)) {
            return methodHashToIndexMap.get(methodHash).intValue();
        } else {
            index = methodHashToIndexMap.size();
            indexToMethodHashList.add(methodHash);
            methodHashToIndexMap.put(methodHash, index);
            hashToMethodInfoBundleList.add(new MethodInfoBundle(methodSignature));
            return index;
        }
    }

    public static String getMethodSignatureFromHash(String hash) {
        // For special root method hash and signature are same
        if (hash.equals(Constants.SPECIAL_ROOT_HASH)) {
            return hash;
        }
        if (methodHashToIndexMap.containsKey(hash)) {
            int index = methodHashToIndexMap.get(hash).intValue();
            return hashToMethodInfoBundleList.get(index).getSignature();
        }
        return null;
    }

    public static void addMethodIdentity(int methodHashIndex, MethodIdentity identity) {
        hashToMethodInfoBundleList.get(methodHashIndex).setIdentity(identity);
    }

    public static void addMethodInvocationType(int methodHashIndex, MethodInvocationType invocType) {
        hashToMethodInfoBundleList.get(methodHashIndex).setMethodInvocationType(invocType);
    }

    public static void addMethodInvocationTypesFromSubclasses(
            int methodHashIndex, Set<Integer> methodInvocationTypesFromSubClasses) {
        hashToMethodInfoBundleList
                .get(methodHashIndex)
                .setMethodInvocationTypesFromSubClasses(methodInvocationTypesFromSubClasses);
    }

    public static synchronized MethodInvocationType getMethodInvocationType(int methodHashIndex) {
        return hashToMethodInfoBundleList.get(methodHashIndex).getMethodInvocationType();
    }

    public static synchronized Set<Integer> getMethodInvocationTypes(int methodHashIndex) {
        return hashToMethodInfoBundleList.get(methodHashIndex).getMethodInvocationTypesFromSubClasses();
    }

    public static Map<TokenRange, IntObjectPair<Set<Integer>>> getMethodInvocationToCalleeCandidatesMap() {
        return methodInvocationToCalleeCandidatesMap;
    }

    public static void setMethodInvocationToCalleeCandidatesMap(
            Map<TokenRange, IntObjectPair<Set<Integer>>> methodInvocationToCalleeCandidatesMap) {
        CallGraphDataStructures.methodInvocationToCalleeCandidatesMap = methodInvocationToCalleeCandidatesMap;
    }

    public static Set<Integer> getCalleeCandidatesFor(TokenRange tokenRange) {
        IntObjectPair<Set<Integer>> resultPair = methodInvocationToCalleeCandidatesMap.get(tokenRange);
        if (resultPair != null) {
            return resultPair.snd;
        }
        return null;
    }

    public static int getServicingMethodIndexFor(TokenRange tokenRange) {
        IntObjectPair<Set<Integer>> resultPair = methodInvocationToCalleeCandidatesMap.get(tokenRange);
        if (resultPair != null) {
            return resultPair.fst;
        }
        return Constants.INVALID_METHOD_HASH_INDEX;
    }

    public static void addToMethodInvocationToCalleeCandidatesMap(
            TokenRange range, int servicingMethodHashIndex, Set<Integer> methodInvocationTypeIndices) {
        methodInvocationToCalleeCandidatesMap.putIfAbsent(
                range, IntObjectPair.of(servicingMethodHashIndex, methodInvocationTypeIndices));
    }

    public static synchronized boolean hasInfoAboutMethodInvocation(TokenRange range) {
        return methodInvocationToCalleeCandidatesMap.containsKey(range);
    }

    public static String getMethodHashFromMethodDeclarationTokenRange(TokenRange tokenRange) {
        return methodDeclarationTokenRangeToMethodHash.get(tokenRange);
    }

    public static void addToMethodDeclarationTokenRangeToMethodHashMap(TokenRange tr, String methodHash) {
        methodDeclarationTokenRangeToMethodHash.putIfAbsent(tr, methodHash);
    }

    public static void addToClassToDefaultConstructorMap(String containerHash, String defaultConstructorHash) {
        classToDefaultConstructorMap.putIfAbsent(containerHash, defaultConstructorHash);
    }

    public static void addToClassToStaticConstructorMap(String containerHash, String staticConstructorHash) {
        classToStaticConstructorMap.putIfAbsent(containerHash, staticConstructorHash);
    }

    public static String getDefaultConstructorFor(String containerHash) {
        return classToDefaultConstructorMap.get(containerHash);
    }

    public static String getStaticConstructorFor(String containerHash) {
        return classToStaticConstructorMap.get(containerHash);
    }

    public static void setClassToDefaultConstructorMap(Map<String, String> classToDefaultConstructorMap) {
        CallGraphDataStructures.classToDefaultConstructorMap = classToDefaultConstructorMap;
    }

    public static void setClassToStaticConstructorMap(Map<String, String> classToStaticConstructorMap) {
        CallGraphDataStructures.classToStaticConstructorMap = classToStaticConstructorMap;
    }

    public static Map<String, Integer> getCgMethodNameToIndex() {
        return cgMethodNameToIndex;
    }

    public static void setCgMethodNameToIndex(Map<String, Integer> cgMethodNameToIndex) {
        CallGraphDataStructures.cgMethodNameToIndex = cgMethodNameToIndex;
    }

    public static boolean isExcludedFile(String filePath) {
        return filesToExclude.contains(filePath);
    }

    public static void addFileToExcludedList(String testFile) {
        filesToExclude.add(testFile);
    }

    public static int sizeOfFilesToExcludeSet() {
        return filesToExclude.size();
    }

    public static boolean isAProtoFile(String filePath) {
        return protobufFilesToExclude.contains(filePath);
    }

    public static void addToProtobufFilesToExcludeList(String filePath) {
        protobufFilesToExclude.add(filePath);
    }

    public static int sizeOfProtobufFilesToExclude() {
        return protobufFilesToExclude.size();
    }

    public static Map<String, String> getClassToDefaultConstructorMap() {
        return classToDefaultConstructorMap;
    }

    public static Map<String, String> getClassToStaticConstructorMap() {
        return classToStaticConstructorMap;
    }

    public static Map<TokenRange, String> getTokenRangeToClassHashMap() {
        return tokenRangeToClassHashMap;
    }

    public static Map<String, String> getBindingHashToClassHashMap() {
        return bindingHashToClassHashMap;
    }

    public static void setTokenRangeToClassHashMap(Map<TokenRange, String> tokenRangeToClassHashMap) {
        CallGraphDataStructures.tokenRangeToClassHashMap = tokenRangeToClassHashMap;
    }

    public static void setBindingHashToClassHashMap(Map<String, String> bindingHashToClassHashMap) {
        CallGraphDataStructures.bindingHashToClassHashMap = bindingHashToClassHashMap;
    }

    public static Map<String, TypeInfo> getClassIdToSoftTypeInfoMap() {
        return classIdToSoftTypeInfoMap;
    }

    public static void setClassIdToSoftTypeInfoMap(Map<String, TypeInfo> classIdToSoftTypeInfoMap) {
        CallGraphDataStructures.classIdToSoftTypeInfoMap = classIdToSoftTypeInfoMap;
    }

    public static Map<String, TypeInfo> getClassIdToProperTypeInfoMap() {
        return classIdToProperTypeInfoMap;
    }

    public static Map<String, TypeInfo> getParametricClassTypeInfoMap() {
        return parametricClassToTypeInfoMap;
    }

    public static void setClassIdToProperTypeInfoMap(Map<String, TypeInfo> classIdToProperTypeInfoMap) {
        CallGraphDataStructures.classIdToProperTypeInfoMap = classIdToProperTypeInfoMap;
    }

    public static Set<String> getNestedClassHashes() {
        return nestedClassHashes;
    }

    public static void setNestedClassHashes(Set<String> nestedClassHashes) {
        CallGraphDataStructures.nestedClassHashes = nestedClassHashes;
    }

    public static Set<String> getStaticClassHashes() {
        return staticClassHashes;
    }

    public static void setStaticClassHashes(Set<String> staticClassHashes) {
        CallGraphDataStructures.staticClassHashes = staticClassHashes;
    }

    public static void setClassHashToSuperClassesAndReachableInterfaces(
            Map<String, Pair<Set<Integer>, Set<Integer>>> classHashToSuperClassesAndReachableInterfaces) {
        CallGraphDataStructures.classHashToSuperClassesAndReachableInterfaces =
                classHashToSuperClassesAndReachableInterfaces;
    }

    public static void setMethodHashToIndexMap(Map<String, Integer> methodHashToIndexMap) {
        CallGraphDataStructures.methodHashToIndexMap = methodHashToIndexMap;
    }

    public static void setIndexToMethodHashList(List<String> indexToMethodHashList) {
        CallGraphDataStructures.indexToMethodHashList = indexToMethodHashList;
    }

    public static void setClassToMethodsMap(Map<String, Map<String, Set<Integer>>> classToMethodsMap) {
        CallGraphDataStructures.classToMethodsMap = classToMethodsMap;
    }

    public static void setHashToMethodInfoBundleList(List<MethodInfoBundle> hashToMethodInfoBundleList) {
        CallGraphDataStructures.hashToMethodInfoBundleList = hashToMethodInfoBundleList;
    }

    public static void setMethodDeclarationTokenRangeToMethodHash(
            Map<TokenRange, String> methodDeclarationTokenRangeToMethodHash) {
        CallGraphDataStructures.methodDeclarationTokenRangeToMethodHash = methodDeclarationTokenRangeToMethodHash;
    }

    public static Set<String> getFilesToExclude() {
        return filesToExclude;
    }

    public static void setFilesToExclude(Set<String> filesToExclude) {
        CallGraphDataStructures.filesToExclude = filesToExclude;
    }

    public static Set<String> getProtobufFilesToExclude() {
        return protobufFilesToExclude;
    }

    public static void setProtobufFilesToExclude(Set<String> protobufFilesToExclude) {
        CallGraphDataStructures.protobufFilesToExclude = protobufFilesToExclude;
    }

    public static Map<TokenRange, FieldInfo> getFieldTokenRangeToFieldInfoMap() {
        return fieldTokenRangeToFieldInfoMap;
    }

    public static void setFieldTokenRangeToFieldInfoMap(Map<TokenRange, FieldInfo> fieldTokenRangeToFieldInfoMap) {
        CallGraphDataStructures.fieldTokenRangeToFieldInfoMap = fieldTokenRangeToFieldInfoMap;
    }

    public static void addToFieldTokenRangeToFieldInfoMap(TokenRange fieldTokenRange, FieldInfo fieldInfo) {
        fieldTokenRangeToFieldInfoMap.putIfAbsent(fieldTokenRange, fieldInfo);
    }

    public static FieldInfo getFieldInfoFromTokenRange(TokenRange fieldTokenRange) {
        return fieldTokenRangeToFieldInfoMap.get(fieldTokenRange);
    }

    public static void removeFieldInfoEntry(TokenRange fieldTokenRange) {
        fieldTokenRangeToFieldInfoMap.remove(fieldTokenRange);
    }

    public static void printObjectSizes(IProgressReporter progressReporter) {
        // Method not reliable any more
        int cgSize = (callGraph.getSize() * 160) / 1000;
        progressReporter.showProgress("Call Graph size: " + cgSize + " KB");
        int methodsSize = (hashToMethodInfoBundleList.size() * 200 + hashToMethodInfoBundleList.size() * 40) / 1000;
        progressReporter.showProgress("Methods Hash size: " + methodsSize + " KB");
        int classSize = (hashToClassSignatureMap.size() * 200 + hashToClassSignatureMap.size() * 40) / 1000;
        progressReporter.showProgress("Classes Hash size: " + classSize + " KB");
        int subClassesSize = 0;
        for (String cls : allReachableSubClassesMap.keySet()) {
            subClassesSize += 40;
            subClassesSize += 40 * allReachableSubClassesMap.get(cls).size();
        }
        subClassesSize = subClassesSize / 1000;
        progressReporter.showProgress("Sub Classes size: " + subClassesSize + " KB");
        int rootMethodSize = (rootMethods.size() * 40) / 1000;
        progressReporter.showProgress("Root Methods size: " + rootMethodSize + " KB");
        int cgMethodNameToIndexSize = (cgMethodNameToIndex.size() * 44) / 1000;
        progressReporter.showProgress("CG Method Index size: " + cgMethodNameToIndexSize + " KB");
        int classFieldSize = (containerHashToFieldsList.size() * 248 + containerHashToFieldsList.size() * 248) / 1000;
        progressReporter.showProgress("Class to Fields size: " + classFieldSize + " KB");
        int annToClassSize = 0;
        annToClassSize = annToClassSize / 1000;
        progressReporter.showProgress("Annotation to Class size: " + annToClassSize + " KB");
        int classToBits = (classHashToBitMap.size() * 88) / 1000;
        progressReporter.showProgress("Class To Bit Index Size: " + classToBits + " KB");
        int innerClassesSize = 0;
        for (String inner : innerClassMap.keySet()) {
            innerClassesSize += 40;
            innerClassesSize += innerClassMap.get(inner).size() * 40;
        }
        innerClassesSize = innerClassesSize / 1000;
        progressReporter.showProgress("Inner Classes size: " + innerClassesSize + " KB");
        int interfacesSize = 0;
        for (String inter : directlyImplementedInterfaces.keySet()) {
            interfacesSize += 40;
            interfacesSize += directlyImplementedInterfaces.get(inter).size() * 40;
        }
        interfacesSize = interfacesSize / 1000;
        progressReporter.showProgress("Interface size: " + interfacesSize + " KB");
    }
}
