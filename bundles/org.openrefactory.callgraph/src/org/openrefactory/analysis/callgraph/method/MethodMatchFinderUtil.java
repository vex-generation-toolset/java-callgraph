/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.type.typeinfo.ArrayTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.EnumTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ParameterizedTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TriBool;

/**
 * A utility class that houses static methods to match methods and find correct methods.
 *
 * <p>The MethodMatchFinderUtil class provides comprehensive functionality for method resolution
 * and matching in the call graph analysis system. It handles various scenarios including:</p>
 * <ul>
 *   <li>Method lookup in class hierarchies (superclasses and interfaces)</li>
 *   <li>Method matching based on signature compatibility</li>
 *   <li>Inner class method resolution</li>
 *   <li>Constructor matching and resolution</li>
 *   <li>Method invocation type calculation and propagation</li>
 * </ul>
 *
 * <p>The class implements sophisticated method matching algorithms that consider:</p>
 * <ul>
 *   <li>Type compatibility and conversions</li>
 *   <li>Inheritance relationships</li>
 *   <li>Method overriding and polymorphism</li>
 *   <li>Varargs and array type handling</li>
 *   <li>Library type resolution</li>
 * </ul>
 *
 * <p>This utility is central to the call graph construction process, as it determines
 * which methods are called at each invocation site, enabling accurate call graph
 * representation.</p>
 *
 * @author Mohammad Rafid Ul Islam, Munawar Hafiz
 */
public class MethodMatchFinderUtil {

    /**
     * A method mismatch may happen for many reasons
     *
     * @author Munawar Hafiz
     */
    public enum MismatchKind {
        /** Mismatch because the formal parameter's type could not be calculated */
        NULL_TYPE_FORMAL,

        /** Mismatch because the actual parameter's type could not be calculated */
        NULL_TYPE_ACTUAL,

        /** Mismatch because both parameters' types could not be calculated */
        NULL_TYPE_BOTH,

        /** Mismatch because the formal parameter is a supertype */
        SUPER_IN_FORMAL,

        /**
         * Mismatch because the formal parameter is a library type.
         * This is the weakest match candidate in the mismatch kind hierarchy.
         */
        LIBRARY_TYPE_FORMAL,

        /** Mismatch because both parameters are library types */
        LIBRARY_TYPE_BOTH,

        /**
         * Mismatch because the actual type is numeric, allowing auto-conversion
         * to the declared type (e.g., int to long, float to double)
         */
        NUMERIC_TYPE_AUTOCOVERT
    }

    /**
     * Finds the hash index of the servicing method for a given method invocation.
     *
     * <p>This method performs comprehensive method resolution starting from the calling contex
     * class. It searches for the best matching method in the following order:</p>
     * <ol>
     *   <li>Current class and its superclass hierarchy</li>
     *   <li>Implemented interfaces</li>
     *   <li>Outer class (for inner/anonymous classes)</li>
     * </ol>
     *
     * <p>The method handles both normal method invocations and super method invocations.
     * For inner or anonymous classes, it can resolve methods from outer classes when
     * no calling context expression is present.</p>
     *
     * <p>This is the primary entry point for method resolution in the call graph system.</p>
     *
     * @param callingContextClassHash the class hash of the calling context. The servicing method
     *                                will be in that class or in the nearest reachable superclass
     *                                or interface
     * @param methodInvocation the callee method invocation. May be a normal or a super method invocation
     * @return the method hash index of the servicing method that may be in the original container
     *         class or any of its super classes, or {@link Constants#INVALID_METHOD_HASH_INDEX}
     *         if no match is found
     */
    public static int getHashOfServicingMethod(String callingContextClassHash, ASTNode methodInvocation) {
        MethodIdentity identityFromInvocation = MethodHandler.process(methodInvocation);
        // First we check if the method is in this class or any of its super types
        int matchedMethodHashIndex = lookUpServicingMethodInThisAndSuperClass(
                callingContextClassHash, identityFromInvocation, new HashSet<>(1));
        // If we do not find any matching method in this class or any of its super classes,
        // we will look for a match in its outer class if exist.
        // For inner or anonymous class, we can invoke method from outer class directly
        // without any calling context expression.
        if (matchedMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX
                && methodInvocation instanceof MethodInvocation
                && ((MethodInvocation) methodInvocation).getExpression() == null) {
            matchedMethodHashIndex = getMatchingHashIndexInOuterClass(callingContextClassHash, identityFromInvocation);
        }
        return matchedMethodHashIndex;
    }

    /**
     * Searches for a servicing method in the current class and its superclass hierarchy.
     *
     * <p>This method implements the core method lookup algorithm that traverses the
     * inheritance hierarchy to find the best matching method. It processes the hierarchy
     * in a specific order:</p>
     * <ol>
     *   <li>Current class methods</li>
     *   <li>Superclass methods (recursively)</li>
     *   <li>Interface methods</li>
     * </ol>
     *
     * <p>The method uses a visited classes set to prevent infinite recursion in
     * complex inheritance scenarios. It prioritizes exact matches over partial matches
     * and considers method overriding relationships.</p>
     *
     * <p>When moving to higher levels in the hierarchy, the method creates data structures
     * that distinguish superclasses from interfaces and processes the superclass hierarchy first.</p>
     *
     * @param callingContextClassHash the class hash to start searching from
     * @param identityFromInvocation the method identity extracted from the invocation
     * @param visitedClasses set of already visited classes to prevent infinite recursion
     * @return the method hash index of the best matching method, or
     *         {@link Constants#INVALID_METHOD_HASH_INDEX} if no match is found
     */
    public static int lookUpServicingMethodInThisAndSuperClass(
            String callingContextClassHash, MethodIdentity identityFromInvocation, Set<String> visitedClasses) {
        int matchedMethodHashIndex = Constants.INVALID_METHOD_HASH_INDEX;
        if (!visitedClasses.contains(callingContextClassHash)) {
            visitedClasses.add(callingContextClassHash);
            // First we check if the method is in this class or not
            matchedMethodHashIndex =
                    getBestMatchedMethodServicingInvocation(callingContextClassHash, identityFromInvocation);
            if (matchedMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                // Not found the matching method in this class
                // now we will look up in super classes and interfaces
                matchedMethodHashIndex = lookupBestMatchingMethodInSuper(
                        callingContextClassHash, identityFromInvocation, visitedClasses);
            }
        }
        return matchedMethodHashIndex;
    }

    /**
     * Updates the call graph and retrieves a single servicing method invocation.
     *
     * <p>This method is responsible for updating the call graph data structures with
     * method invocation information and returning the appropriate method invocation
     * type. It handles method overriding scenarios and ensures proper call graph
     * construction.</p>
     *
     * <p>The method considers whether method overriding is allowed and updates
     * the call graph accordingly. It may create new method invocation types or
     * modify existing ones based on the method resolution results.</p>
     *
     * @param methodName the name of the method being invoked
     * @param servicingCalleeMethodIndex the index of the servicing callee method
     * @param servicingCalleeMethodHash the hash of the servicing callee method
     * @param allowOverriding whether method overriding is allowed in this contex
     * @return a set of method invocation type indices representing the resolved method calls
     */
    public static Set<Integer> updateCGAndGetSingleServicingMethodInvocation(
            String methodName,
            int servicingCalleeMethodIndex,
            String servicingCalleeMethodHash,
            boolean allowOverriding) {
        Set<Integer> calleeContenderMethodInvocationTypes = new HashSet<>(1);
        // The method invocation types have not been calculated before,
        // So calculate them here
        if (allowOverriding) {
            Map<Integer, MethodInvocationCalculationScratchPad> scratchPadMap =
                    MethodMatchFinderUtil.calculateMethodInvocationTypesFromSubtypes(servicingCalleeMethodIndex);
            if (scratchPadMap != null) {
                Iterator<Map.Entry<Integer, MethodInvocationCalculationScratchPad>> iterator =
                        scratchPadMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    MethodInvocationCalculationScratchPad scratchPad =
                            iterator.next().getValue();
                    MethodInvocationType invocationType = scratchPad.getInvocationType();
                    if (invocationType != null) {
                        // Update CG data structures
                        CallGraphDataStructures.addMethodInvocationType(
                                scratchPad.getRelevantMethodIndex(), invocationType);
                        CallGraphDataStructures.addMethodInvocationTypesFromSubclasses(
                                scratchPad.getRelevantMethodIndex(),
                                scratchPad.getMethodInvocationTypesFromSubclasses());
                        if (servicingCalleeMethodIndex == scratchPad.getRelevantMethodIndex()) {
                            // There should be only one method invocation type in the set:
                            // the one that has been passed
                            // We are setting it here after making sure that there are
                            // actual method invocations to back this index entry
                            calleeContenderMethodInvocationTypes.add(scratchPad.getRelevantMethodIndex());
                        }
                    }
                }
            }
        } else {
            MethodInvocationType invocationType =
                    CallGraphDataStructures.getMethodInvocationType(servicingCalleeMethodIndex);
            if (invocationType == null) {
                invocationType = new MethodInvocationType(servicingCalleeMethodIndex);
                String classHash = CallGraphUtility.getClassHashFromMethodHash(servicingCalleeMethodHash);
                int classHashIndex = CallGraphDataStructures.getBitIndexFromClassHash(classHash);
                if (classHashIndex != Constants.INVALID_CLASS_HASH_INDEX) {
                    invocationType.addToClassesThatContainOrInheritMethod(classHashIndex);
                    // For a static method, we cannot override a method.
                    // However, we still add the method to sub classes and implementing interfaces
                    // as classes that will also inherit this method, if available.
                    if (CallGraphUtility.isStaticMethod(servicingCalleeMethodIndex)) {
                        Set<String> subClasses = CallGraphDataStructures.getAllSubClass(classHash);
                        if (subClasses != null && !subClasses.isEmpty()) {
                            for (String subClass : subClasses) {
                                classHashIndex = CallGraphDataStructures.getBitIndexFromClassHash(subClass);
                                if (classHashIndex != Constants.INVALID_CLASS_HASH_INDEX) {
                                    invocationType.addToClassesThatContainOrInheritMethod(classHashIndex);
                                }
                            }
                        }
                    }
                }
                CallGraphDataStructures.addMethodInvocationType(servicingCalleeMethodIndex, invocationType);
                calleeContenderMethodInvocationTypes.add(servicingCalleeMethodIndex);
            } else {
                calleeContenderMethodInvocationTypes.add(invocationType.getCalleeMethodHashIndex());
            }
        }
        return calleeContenderMethodInvocationTypes;
    }

    /**
     * Calculates method invocation types from subtypes for a given method.
     *
     * <p>This method analyzes the inheritance hierarchy to determine how a method
     * is invoked across different subtypes. It creates a comprehensive mapping
     * of method invocation types that considers:</p>
     * <ul>
     *   <li>Direct method implementations in subtypes</li>
     *   <li>Method inheritance patterns</li>
     *   <li>Polymorphic method resolution</li>
     *   <li>Interface method implementations</li>
     * </ul>
     *
     * <p>The method returns a map that associates class indices with calculation
     * scratch pads, enabling detailed analysis of method invocation patterns
     * across the type hierarchy.</p>
     *
     * @param methodIndex the index of the method to analyze
     * @return a map of class indices to method invocation calculation scratch pads
     */
    public static Map<Integer, MethodInvocationCalculationScratchPad> calculateMethodInvocationTypesFromSubtypes(
            int methodIndex) {
        MethodIdentity topMethodIdentity = CallGraphDataStructures.getMatchingMethodIdentity(methodIndex);
        Map<Integer, MethodInvocationCalculationScratchPad> scratchPadMap = new HashMap<>();
        if (topMethodIdentity != null) {
            String topMethodHash = CallGraphDataStructures.getMethodHashFromIndex(methodIndex);
            // For a static method, we only need to calculate the top method hash
            // Nowhere
            String containerClassHashOfTopMethod = CallGraphUtility.getClassHashFromMethodHash(topMethodHash);
            if (containerClassHashOfTopMethod != null) {
                Set<String> subClassesAndInterfaceImplementors =
                        CallGraphDataStructures.getAllSubClass(containerClassHashOfTopMethod);
                if (subClassesAndInterfaceImplementors == null) {
                    subClassesAndInterfaceImplementors = new HashSet<>(1);
                }
                subClassesAndInterfaceImplementors.add(containerClassHashOfTopMethod);
                // Create a set of all the classes in scope, while going
                // upwards, we will stop calculation if a class to be processed is not in scope
                // This is the upper bound of the inheritance hierarchy
                // This is set here then used everywhere in a readonly manner.
                Set<Integer> classesInScope = new HashSet<>();
                for (String subClassOrInterfaceImplementor : subClassesAndInterfaceImplementors) {
                    int classIndex = CallGraphDataStructures.getBitIndexFromClassHash(subClassOrInterfaceImplementor);
                    classesInScope.add(classIndex);
                }
                // The classes that have already been visited
                for (String subClassOrInterfaceImplementor : subClassesAndInterfaceImplementors) {
                    int classHashIndex =
                            CallGraphDataStructures.getBitIndexFromClassHash(subClassOrInterfaceImplementor);
                    int overridingMethodHashIndex = MethodMatchFinderUtil.getExactMatchingMethod(
                            subClassOrInterfaceImplementor, topMethodIdentity);
                    if (overridingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                        // An overriding method was found
                        // So, create the method invocation type at this level
                        // Pass this information upwards
                        // and update the subclasses information structure
                        // in the separate set and also in the method invocation
                        // types above
                        boolean newlyCreated = false;
                        MethodInvocationCalculationScratchPad scratchPad = scratchPadMap.get(classHashIndex);
                        if (scratchPad != null) {
                            // If we had created a scratch pad here while processing another method,
                            // that will be created with an invalid relevant method index.
                            // Now we update it since we have found a matching relevant method here.
                            scratchPad.setRelevantMethodIndex(overridingMethodHashIndex);
                        } else {
                            // Create a new scratch pad with the appropriate method and container
                            scratchPad = new MethodInvocationCalculationScratchPad(
                                    overridingMethodHashIndex, classHashIndex);
                            newlyCreated = true;
                        }
                        MethodInvocationType invocation = new MethodInvocationType(overridingMethodHashIndex);
                        scratchPad.setMethodInvocationType(invocation);
                        scratchPad.addToSubclasses(classHashIndex);
                        scratchPad.addSubscribersToMethodInvocationOtherTypes();
                        scratchPad.addToMethodInvocationTypes(overridingMethodHashIndex);
                        if (newlyCreated) {
                            scratchPadMap.put(classHashIndex, scratchPad);
                        }

                        Set<Integer> subClassesOfProcessedClass =
                                CallGraphDataStructures.getAllSubClassIndices(subClassOrInterfaceImplementor);
                        if (subClassesOfProcessedClass == null) {
                            subClassesOfProcessedClass = new HashSet<>(1);
                        }
                        subClassesOfProcessedClass.add(classHashIndex);
                        propagateMethodInvocationInformationUpstream(
                                scratchPadMap,
                                overridingMethodHashIndex,
                                classHashIndex,
                                classHashIndex,
                                subClassesOfProcessedClass,
                                new HashSet<>(),
                                classesInScope);
                    } else {
                        // No overriding method found,
                        // So, just pass the current class info as a contender in upstream classes
                        propagateContenderInformationUpstream(
                                scratchPadMap, classHashIndex, classHashIndex, new HashSet<>(), classesInScope);
                    }
                }
            }
        }
        return scratchPadMap;
    }

    /**
     * Looks up the best matching method in the superclass hierarchy.
     *
     * <p>This method searches for method matches in the superclass hierarchy starting
     * from a given class. It implements sophisticated matching algorithms that consider
     * type compatibility, method signatures, and inheritance relationships.</p>
     *
     * <p>The method handles various matching scenarios including exact matches,
     * partial matches, and type conversions. It prioritizes matches based on
     * the mismatch kind hierarchy defined in the {@link MismatchKind} enum.</p>
     *
     * @param startingClassHash the class hash to start searching from
     * @param methodInvocation the method invocation AST node
     * @param visitedClasses set of already visited classes to prevent infinite recursion
     * @return the method hash index of the best matching method, or
     *         {@link Constants#INVALID_METHOD_HASH_INDEX} if no match is found
     */
    public static int lookupBestMatchingMethodInSuper(
            String startingClassHash, ASTNode methodInvocation, Set<String> visitedClasses) {
        MethodIdentity identityFromInvocation = MethodHandler.process(methodInvocation);
        return lookupBestMatchingMethodInSuper(startingClassHash, identityFromInvocation, visitedClasses);
    }

    /**
     * <p>Starting from a super class or a super interface, lookup for a matching method. We come to this class from
     * other helper methods that find best matching method (getHashOfServicingMethod). We also come to this directly
     * when processing a super.foo() call when the lookup for foo starts at the super level.
     *
     * <p>In java, inherited methods are matched under the following rules.
     * <li>Instance methods are preferred over default methods. Example:
     *
     *     <pre>
     * interface MyInterface {
     *      default void foo() {
     *          System.out.println("MyInterface::foo");
     *      }
     * }
     *
     * class MySuperClass {
     *      public void foo() {
     *          System.out.println("MySuperClass::foo");
     *      }
     * }
     *
     * class MyClass extends MySuperClass implements MyInterface {
     *      void bar() {
     *          foo(); // Should match with MySuperClass::foo
     *      }
     * }
     * </pre>
     *
     * <li>Methods that are already overridden by other candidates are ignored. Example:
     *
     *     <pre>
     * interface MyInterface {
     *      default void foo() {
     *          System.out.println("MyInterface::foo");
     *      }
     * }
     *
     * interface SubInterface extends MyInterface {
     *      default void foo() {
     *          System.out.println("SubInterface::foo");
     *      }
     * }
     *
     * interface AnotherSubInterface extends SubInterface {
     *      default void foo() {
     *          System.out.println("AnotherSubInterface::foo");
     *      }
     * }
     *
     * class MySuperClass implements MyInterface {
     *
     * }
     *
     * class MyClass extends MySuperClass implements MyInterface, SubInterface, AnotherSubInterface {
     *      void bar() {
     *          foo();  // Should match with AnotherSubInterface::foo
     *      }
     * }
     * </pre>
     *
     * @param startingClassHash the class from which we start and go to the super classes/interfaces
     * @param identityFromInvocation the callee method invocation. May be a normal or a super method invocation
     * @param visitedClasses we will find the match in a recursive manner The visited class prevents us from going in an
     *     infinite loop
     * @return the method hash of the servicing method that may be in any of its super classes or invalid hash index.
     */
    public static int lookupBestMatchingMethodInSuper(
            String startingClassHash, MethodIdentity identityFromInvocation, Set<String> visitedClasses) {
        int methodHashIndexFromSuperClass = Constants.INVALID_METHOD_HASH_INDEX;
        String superclassHash = CallGraphDataStructures.getSuperClassOf(startingClassHash);
        if (superclassHash != null) {
            methodHashIndexFromSuperClass =
                    lookUpServicingMethodInThisAndSuperClass(superclassHash, identityFromInvocation, visitedClasses);
            if (methodHashIndexFromSuperClass != Constants.INVALID_METHOD_HASH_INDEX) {
                MethodIdentity superclassMethodIdentity =
                        CallGraphDataStructures.getMatchingMethodIdentity(methodHashIndexFromSuperClass);
                // For default/abstract method found in interfaces, even if we have a match
                // another interface may have the same method at a closer level in
                // inheritance hierarchy. So, we need to continue matching.
                if (!(superclassMethodIdentity.isDefaultMethodInAnInterface()
                        || superclassMethodIdentity.isAnAbstactOrInterfaceMethodWithNoBody())) {
                    return methodHashIndexFromSuperClass;
                }
            }
        }

        // If we reach here,
        //     (1) we have not found a matching method in a super class
        //     (2) we may have found a method but it is a default/abstract method.
        //         So, another interface may have a default method that is
        //         closer in inheritance hierarchy.
        //
        // So, visit all the super interfaces now to find a match.
        Set<String> superInterfaces =
                CallGraphDataStructures.getImplementedInterfaces().get(startingClassHash);
        if (superInterfaces != null) {
            // For interfaces, we want to get the method in the lowest level
            // So, we continue looking for better matches
            int bestMatchingInterfaceMethodIndex = methodHashIndexFromSuperClass;
            for (String superInterfaceHash : superInterfaces) {
                int methodHashIndexFromSuperInterface = lookUpServicingMethodInThisAndSuperClass(
                        superInterfaceHash, identityFromInvocation, visitedClasses);
                if (methodHashIndexFromSuperInterface != Constants.INVALID_METHOD_HASH_INDEX) {
                    if (bestMatchingInterfaceMethodIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                        bestMatchingInterfaceMethodIndex = methodHashIndexFromSuperInterface;
                    } else {
                        // See if the method is a better match than the previous
                        String bestMatchingInterfaceMethodHash =
                                CallGraphDataStructures.getMethodHashFromIndex(bestMatchingInterfaceMethodIndex);
                        String bestMatchingInterfaceHash =
                                CallGraphUtility.getClassHashFromMethodHash(bestMatchingInterfaceMethodHash);
                        Set<String> subclassHashesOfBestMatchingInterface =
                                CallGraphDataStructures.getAllSubClass(bestMatchingInterfaceHash);
                        String currentlyFoundInterfaceMethodHash =
                                CallGraphDataStructures.getMethodHashFromIndex(methodHashIndexFromSuperInterface);
                        String currentlyFoundInterfaceHash =
                                CallGraphUtility.getClassHashFromMethodHash(currentlyFoundInterfaceMethodHash);
                        if (subclassHashesOfBestMatchingInterface.contains(currentlyFoundInterfaceHash)) {
                            // The currently found one is lower in the interface hierarchy, so get it
                            bestMatchingInterfaceMethodIndex = methodHashIndexFromSuperInterface;
                        }
                    }
                }
            }
            return bestMatchingInterfaceMethodIndex;
        }
        return methodHashIndexFromSuperClass;
    }

    /**
     * Finds an exact matching method in a given class.
     *
     * <p>This method searches for an exact method match in a specific class based on
     * the provided method identity. It's used when precise method resolution is required,
     * such as in method overriding scenarios or when exact signature matching is needed.</p>
     *
     * <p>The method performs strict signature matching, considering parameter types,
     * return types, and method names. It's particularly useful for resolving
     * method declarations and ensuring method signature consistency.</p>
     *
     * @param classHash the hash of the class to search in
     * @param identityFromSuperDeclaration the method identity to match agains
     * @return the method hash index of the exact match, or
     *         {@link Constants#INVALID_METHOD_HASH_INDEX} if no exact match is found
     */
    public static int getExactMatchingMethod(String classHash, MethodIdentity identityFromSuperDeclaration) {
        Map<String, Set<Integer>> methodNamesToHashesMap =
                CallGraphDataStructures.getClassToMethodsMap().get(classHash);
        if (methodNamesToHashesMap != null) {
            Set<Integer> hashIndicesOfMethodsWithSameName =
                    methodNamesToHashesMap.get(identityFromSuperDeclaration.getMethodName());
            if (hashIndicesOfMethodsWithSameName != null && !hashIndicesOfMethodsWithSameName.isEmpty()) {
                int actualParamCount =
                        identityFromSuperDeclaration.getArgParamTypeInfos().size();
                Iterator<Integer> methodsIterator = hashIndicesOfMethodsWithSameName.iterator();
                while (methodsIterator.hasNext()) {
                    int hashIndexOfMethodWithSameSame = methodsIterator.next();
                    MethodIdentity identityOfDeclaredMethodInThis =
                            CallGraphDataStructures.getMatchingMethodIdentity(hashIndexOfMethodWithSameSame);
                    if (identityOfDeclaredMethodInThis != null) {
                        int formalParamCount = identityOfDeclaredMethodInThis
                                .getArgParamTypeInfos()
                                .size();
                        // A basic sniff test of params matching, the name already matches
                        if (formalParamCount == actualParamCount) {
                            boolean matchInfo = MethodMatchFinderUtil.getExactMatchInfo(
                                    identityFromSuperDeclaration, identityOfDeclaredMethodInThis);
                            if (matchInfo) {
                                return hashIndexOfMethodWithSameSame;
                            } else {
                                // If this does not match, we look for a better one
                            }
                        }
                    }
                }
            }
        }
        return Constants.INVALID_METHOD_HASH_INDEX;
    }

    /**
     * Finds a matching constructor based on parameter count.
     *
     * <p>This method searches for a constructor in a given class that matches the
     * specified parameter count. It's useful for constructor resolution when
     * the exact parameter types are not critical, such as in simple objec
     * instantiation scenarios.</p>
     *
     * <p>The method considers both regular constructors and default constructors.
     * It returns the first constructor found with the matching parameter count.</p>
     *
     * @param classHash the hash of the class to search in
     * @param paramCount the number of parameters the constructor should have
     * @return the method hash of the matching constructor, or {@code null} if no match is found
     */
    public static String getMatchingConstructorBasedOnParamCount(String classHash, int paramCount) {
        Set<Integer> matchingMethodCandidates = CallGraphDataStructures.getMatchingMethodHashes(
                classHash, CallGraphUtility.getClassNameFromClassHash(classHash));
        if (matchingMethodCandidates != null) {
            Iterator<Integer> matchingMethodsIterator = matchingMethodCandidates.iterator();
            while (matchingMethodsIterator.hasNext()) {
                int matchingMethodHashIndex = matchingMethodsIterator.next();
                MethodIdentity matchingMethodIdentity =
                        CallGraphDataStructures.getMatchingMethodIdentity(matchingMethodHashIndex);
                if (matchingMethodIdentity.getArgParamTypeInfos().size() == paramCount) {
                    return CallGraphDataStructures.getMethodHashFromIndex(matchingMethodHashIndex);
                }
            }
        }
        return null;
    }

    /**
     * Checks if a class has a regular (non-default) constructor.
     *
     * <p>This method determines whether a class has explicitly defined constructors
     * or only relies on the default constructor. It's useful for understanding
     * class construction patterns and constructor resolution strategies.</p>
     *
     * <p>A class is considered to have a regular constructor if it has at leas
     * one explicitly declared constructor, regardless of the parameter count.</p>
     *
     * @param classHash the hash of the class to check
     * @return {@code true} if the class has a regular constructor, {@code false} otherwise
     */
    public static boolean hasRegularConstructor(String classHash) {
        Set<Integer> matchingMethodCandidates = CallGraphDataStructures.getMatchingMethodHashes(
                classHash, CallGraphUtility.getClassNameFromClassHash(classHash));
        if (matchingMethodCandidates != null && matchingMethodCandidates.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Gets the best matched method that services a given invocation.
     *
     * <p>This method performs comprehensive method matching to find the bes
     * candidate method for a given invocation. It considers various factors
     * including type compatibility, inheritance relationships, and method
     * overriding scenarios.</p>
     *
     * <p>The method implements sophisticated matching algorithms that can handle
     * complex scenarios such as:</p>
     * <ul>
     *   <li>Type conversions and promotions</li>
     *   <li>Varargs method resolution</li>
     *   <li>Generic type handling</li>
     *   <li>Interface method resolution</li>
     * </ul>
     *
     * <p>This is one of the core methods for method resolution in the call
     * graph construction process.</p>
     *
     * @param classHash the hash of the class to search in
     * @param identityFromInvocation the method identity extracted from the invocation
     * @return the method hash index of the best matched method, or
     *         {@link Constants#INVALID_METHOD_HASH_INDEX} if no match is found
     * @see #getBestMatchInfo(MethodIdentity, MethodIdentity, boolean)
     */
    public static int getBestMatchedMethodServicingInvocation(String classHash, MethodIdentity identityFromInvocation) {
        int bestMatchedMethodHashIndex = Constants.INVALID_METHOD_HASH_INDEX;
        Map<String, Set<Integer>> methodNamesToHashesMap =
                CallGraphDataStructures.getClassToMethodsMap().get(classHash);
        Set<Integer> hashIndicesOfMethodsWithSameName = null;
        if (methodNamesToHashesMap != null) {
            hashIndicesOfMethodsWithSameName = methodNamesToHashesMap.get(identityFromInvocation.getMethodName());
        }
        if (hashIndicesOfMethodsWithSameName != null && !hashIndicesOfMethodsWithSameName.isEmpty()) {
            int actualParamCount = identityFromInvocation.getArgParamTypeInfos().size();
            Iterator<Integer> methodsIterator = hashIndicesOfMethodsWithSameName.iterator();

            List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>> mismatchInfo = null;
            while (methodsIterator.hasNext()) {
                int hashIndexOfMethodWithSameSame = methodsIterator.next();
                MethodIdentity identityOfDeclaredMethod =
                        CallGraphDataStructures.getMatchingMethodIdentity(hashIndexOfMethodWithSameSame);
                if (identityOfDeclaredMethod != null) {
                    int formalParamCount =
                            identityOfDeclaredMethod.getArgParamTypeInfos().size();
                    // Check if there is varargs in the last formal parameter
                    boolean varArgsMatch = false;
                    if (formalParamCount > 0) {
                        TypeInfo lastParam =
                                identityOfDeclaredMethod.getArgParamTypeInfos().get(formalParamCount - 1);
                        if (lastParam instanceof ArrayTypeInfo && ((ArrayTypeInfo) lastParam).isVarArgsType()) {
                            varArgsMatch = true;
                            // For varargs, the size of formal parameters must be equal or smaller
                            // than actual parameters. Filter that case first
                            if (formalParamCount <= actualParamCount) {
                                // The parameters must match up till the last formal parameter
                                // The actual parameters for the last formal could be one of the two
                                //    (1) comma separated params,
                                //    (2) an Array of params
                                // Match without the variadic part
                                Pair<TriBool, List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>>> matchInfo =
                                        MethodMatchFinderUtil.getBestMatchInfo(
                                                identityFromInvocation, identityOfDeclaredMethod, true);
                                if (matchInfo.fst.isTrue()) {
                                    // Now match the variadic part in the actual parameters
                                    // To do the matching create artificial types from the formal and actual parameters.
                                    // Get the actual parameters in variadic position and create a false formal
                                    // parameter
                                    // that matches the count.
                                    int variadicParamStartPosition = formalParamCount - 1;
                                    TypeInfo firstVariadicActualParamType = identityFromInvocation
                                            .getArgParamTypeInfos()
                                            .get(variadicParamStartPosition);
                                    if (formalParamCount == actualParamCount
                                            && firstVariadicActualParamType instanceof ArrayTypeInfo) {
                                        // Case 2
                                        // Just match the array types
                                        List<TypeInfo> tempActual = new ArrayList<>(1);
                                        tempActual.add(firstVariadicActualParamType);
                                        MethodIdentity fakeActualParam = new MethodIdentity(
                                                identityFromInvocation.getMethodName(),
                                                identityFromInvocation.getReturnTypeInfo(),
                                                tempActual);
                                        List<TypeInfo> tempFormal = new ArrayList<>(1);
                                        tempFormal.add(lastParam);
                                        MethodIdentity fakeAFormalParam = new MethodIdentity(
                                                identityOfDeclaredMethod.getMethodName(),
                                                identityOfDeclaredMethod.getReturnTypeInfo(),
                                                tempFormal);
                                        matchInfo = MethodMatchFinderUtil.getBestMatchInfo(
                                                fakeActualParam, fakeAFormalParam, false);
                                        if (matchInfo.fst.isTrue()) {
                                            return hashIndexOfMethodWithSameSame;
                                        }
                                    } else {
                                        // Case 1
                                        // Comma separated actual parameters
                                        List<TypeInfo> tempActual = new ArrayList<>(identityFromInvocation
                                                        .getArgParamTypeInfos()
                                                        .size()
                                                - variadicParamStartPosition
                                                + 1);
                                        for (int i = variadicParamStartPosition;
                                                i
                                                        < identityFromInvocation
                                                                .getArgParamTypeInfos()
                                                                .size();
                                                i++) {
                                            tempActual.add(identityFromInvocation
                                                    .getArgParamTypeInfos()
                                                    .get(i));
                                        }
                                        MethodIdentity fakeActualParam = new MethodIdentity(
                                                identityFromInvocation.getMethodName(),
                                                identityFromInvocation.getReturnTypeInfo(),
                                                tempActual);
                                        List<TypeInfo> tempFormal = new ArrayList<>(tempActual.size());
                                        for (int i = variadicParamStartPosition;
                                                i
                                                        < identityFromInvocation
                                                                .getArgParamTypeInfos()
                                                                .size();
                                                i++) {
                                            tempFormal.add(((ArrayTypeInfo) lastParam).getElementType());
                                        }
                                        MethodIdentity fakeAFormalParam = new MethodIdentity(
                                                identityOfDeclaredMethod.getMethodName(),
                                                identityOfDeclaredMethod.getReturnTypeInfo(),
                                                tempFormal);
                                        matchInfo = MethodMatchFinderUtil.getBestMatchInfo(
                                                fakeActualParam, fakeAFormalParam, false);
                                        if (matchInfo.fst.isTrue()) {
                                            return hashIndexOfMethodWithSameSame;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // A basic sniff test of params matching, the name already matches
                    if (!varArgsMatch && formalParamCount == actualParamCount) {
                        Pair<TriBool, List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>>> matchInfo =
                                MethodMatchFinderUtil.getBestMatchInfo(
                                        identityFromInvocation, identityOfDeclaredMethod, false);
                        if (matchInfo.fst.isTrue()) {
                            return hashIndexOfMethodWithSameSame;
                        } else if (matchInfo.fst.isMayBe()) {
                            if (bestMatchedMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                                bestMatchedMethodHashIndex = hashIndexOfMethodWithSameSame;
                                mismatchInfo = matchInfo.snd;
                            } else {
                                // There was a previous mismatch
                                // Check if this is a better match
                                if (isABetterMatch(mismatchInfo, matchInfo.snd)) {
                                    bestMatchedMethodHashIndex = hashIndexOfMethodWithSameSame;
                                    mismatchInfo = matchInfo.snd;
                                }
                            }
                        } else {
                            // If this does not match, we look for a better one
                        }
                    }
                }
            }
        }
        return bestMatchedMethodHashIndex;
    }

    /**
     * At a container class, we have found a matched method that overrides the method that we are calculating
     * for. Now, we pass this information upstream. For a method override, (1) We create a method invocation type in
     * that class and put it in its scratch pad (2) We add the method invocation type index to this class's invocation
     * types set (3) We add the current class as the subscriber sub class. We pass the current class and all sub classes
     * up the tree to be removed until we find another method invocation. The matched method in this class acts as a
     * wall since all the other methods or classes under this class hierarchy will not have visibility to anything above
     * this class for this method. So, if the information was passed for some reason before, we purge that info. When we
     * find another method invocation type, we just update that class and stop the calculation by clearing the purged
     * data type. Beyond that no impact on the sub class set will be seen.
     *
     * @param scratchPadMap the scratch pad map which is the central data structure that is passed around.
     * @param methodHashIndex the method hash index for the method for which we are calculating in the scratch pad
     * @param classHashIndex the container class hash which will be used to navigate the inheritance hierarchy.
     * @param originatingClassHashIndex the class hash index from which we started going upstream. This is used to
     *     distinguish the operation between the starting class and all other upstream classes.
     * @param subclassesToBePurged the classes that should be removed from all classes above but not this class.
     * @param visitedClasses all the classes that has been visited. Used to prevent stack overflow when processing
     *     cycles in the inheritance tree.
     * @param classesInScope the classes that will participate in the calculation. The top method in the inheritance
     *     hierarchy starts this calculation. This method and all its subclasses are in scope.
     */
    private static void propagateMethodInvocationInformationUpstream(
            Map<Integer, MethodInvocationCalculationScratchPad> scratchPadMap,
            int methodHashIndex,
            int classHashIndex,
            int originatingClassHashIndex,
            Set<Integer> subclassesToBePurged,
            Set<Integer> visitedClasses,
            Set<Integer> classesInScope) {
        if (classHashIndex >= 0
                && classesInScope.contains(classHashIndex)
                && !visitedClasses.contains(classHashIndex)) {
            visitedClasses.add(classHashIndex);
            String classHash = CallGraphDataStructures.getClassHashFromBitIndex(classHashIndex);
            if (classHash != null) {
                MethodInvocationCalculationScratchPad scratchPadAtProcessedClass = scratchPadMap.get(classHashIndex);
                boolean newlyCreated = false;
                if (scratchPadAtProcessedClass == null) {
                    scratchPadAtProcessedClass = new MethodInvocationCalculationScratchPad(
                            Constants.INVALID_METHOD_HASH_INDEX, classHashIndex);
                    newlyCreated = true;
                }
                // We will ignore the class from which we started calculation
                // and ONLY update the other classes upstream
                if (classHashIndex != originatingClassHashIndex) {
                    // For all upstream classes, we add to the method invocation types set
                    // the method index that is used to point to the created method invocation type
                    scratchPadAtProcessedClass.addToMethodInvocationTypes(methodHashIndex);
                    // For upstream classes, we will purge from them the class from
                    // which we started the traversal and all its sub classes. We do this
                    // all the way up to the point in which we find another method invocation
                    // type. From that to upstream, the calculation was already done when we processed
                    // that class and walked upstream. So, we clear the sub classes to be purged list from that point.
                    if (!subclassesToBePurged.isEmpty()) {
                        scratchPadAtProcessedClass.removeSubclasses(subclassesToBePurged);
                        MethodInvocationType invocationAtProcessedClass =
                                scratchPadAtProcessedClass.getInvocationType();
                        if (invocationAtProcessedClass != null) {
                            invocationAtProcessedClass.removeFromClassesThatContainOrInheritMethod(
                                    subclassesToBePurged);
                            // Since we have already found a method invocation index at this level
                            // we do not need to propagate subclass purging calculation upstream any more
                            subclassesToBePurged.clear();
                        }
                    }
                }
                if (newlyCreated) {
                    scratchPadMap.put(classHashIndex, scratchPadAtProcessedClass);
                }
                // Done with this scratch pad, move upwards
                String superclassHash = CallGraphDataStructures.getSuperClassOf(classHash);
                if (superclassHash != null) {
                    int bitIndexOfSuper = CallGraphDataStructures.getBitIndexFromClassHash(superclassHash);
                    propagateMethodInvocationInformationUpstream(
                            scratchPadMap,
                            methodHashIndex,
                            bitIndexOfSuper,
                            originatingClassHashIndex,
                            subclassesToBePurged,
                            visitedClasses,
                            classesInScope);
                }
                Set<String> superInterfaces =
                        CallGraphDataStructures.getImplementedInterfaces().get(classHash);
                if (superInterfaces != null) {
                    for (String superInterfaceHash : superInterfaces) {
                        int bitIndexOfSuper = CallGraphDataStructures.getBitIndexFromClassHash(superInterfaceHash);
                        propagateMethodInvocationInformationUpstream(
                                scratchPadMap,
                                methodHashIndex,
                                bitIndexOfSuper,
                                originatingClassHashIndex,
                                subclassesToBePurged,
                                visitedClasses,
                                classesInScope);
                    }
                }
            }
        }
    }

    /**
     * At a container class, we did not find an overriding method for the one we are calculating. So, this
     * method must be using this method from some upstream class. We pass this information upstream and create contender
     * lists for a method implementation.
     *
     * <p>While we go upstream, we add the contender class to the list of contender sub classes. We only do this until
     * we find a method invocation type. From that point, this information does not need to flow upward
     *
     * @param scratchPadMap the scratch pad map which is the central data structure that is passed around.
     * @param classHashIndex the container class hash which will be used to navigate the inheritance hierarchy.
     * @param contenderClassHashIndex the class hash index from which we started going upstream. This is used to
     *     distinguish the operation between the starting class and all other upstream classes.
     * @param visitedClasses all the classes that has been visited. Used to prevent stack overflow when processing
     *     cycles in the inheritance tree.
     * @param classesInScope the classes that will participate in the calculation. The top method in the inheritance
     *     hierarchy starts this calculation. This method and all its subclasses are in scope.
     */
    private static void propagateContenderInformationUpstream(
            Map<Integer, MethodInvocationCalculationScratchPad> scratchPadMap,
            int classHashIndex,
            int contenderClassHashIndex,
            Set<Integer> visitedClasses,
            Set<Integer> classesInScope) {
        // For the last test expression, we are checking if we have found a method invocation type
        // while going upstream. This is to block us from going any further.
        if (classHashIndex >= 0
                && classesInScope.contains(classHashIndex)
                && !visitedClasses.contains(classHashIndex)
                && contenderClassHashIndex != Constants.INVALID_CLASS_HASH_INDEX) {
            visitedClasses.add(classHashIndex);
            String classHash = CallGraphDataStructures.getClassHashFromBitIndex(classHashIndex);
            if (classHash != null) {
                MethodInvocationCalculationScratchPad scratchPadAtProcessedClass = scratchPadMap.get(classHashIndex);
                boolean newlyCreated = false;
                if (scratchPadAtProcessedClass == null) {
                    newlyCreated = true;
                    scratchPadAtProcessedClass = new MethodInvocationCalculationScratchPad(
                            Constants.INVALID_METHOD_HASH_INDEX, classHashIndex);
                }
                scratchPadAtProcessedClass.addToSubclasses(contenderClassHashIndex);
                // Update the method invocation index if available
                MethodInvocationType invocationAtProcessedClass = scratchPadAtProcessedClass.getInvocationType();
                if (invocationAtProcessedClass != null) {
                    invocationAtProcessedClass.addToClassesThatContainOrInheritMethod(contenderClassHashIndex);
                    // Setting this to invalid index, to block calculation while we go above
                    contenderClassHashIndex = Constants.INVALID_CLASS_HASH_INDEX;
                }
                if (newlyCreated) {
                    scratchPadMap.put(classHashIndex, scratchPadAtProcessedClass);
                }
                // Add to the contender list
                // Done with this scratch pad, move upwards
                String superclassHash = CallGraphDataStructures.getSuperClassOf(classHash);
                if (superclassHash != null) {
                    int bitIndexOfSuper = CallGraphDataStructures.getBitIndexFromClassHash(superclassHash);
                    propagateContenderInformationUpstream(
                            scratchPadMap, bitIndexOfSuper, contenderClassHashIndex, visitedClasses, classesInScope);
                }
                Set<String> superInterfaces =
                        CallGraphDataStructures.getImplementedInterfaces().get(classHash);
                if (superInterfaces != null) {
                    for (String superInterfaceHash : superInterfaces) {
                        int bitIndexOfSuper = CallGraphDataStructures.getBitIndexFromClassHash(superInterfaceHash);
                        propagateContenderInformationUpstream(
                                scratchPadMap,
                                bitIndexOfSuper,
                                contenderClassHashIndex,
                                visitedClasses,
                                classesInScope);
                    }
                }
            }
        }
    }

    /**
     * Check for a set of method indices that have the same name in a class or any of its outer classes if it
     * is an inner class.
     *
     * @param classHash hash of the class suspected to be inner class
     * @param identityFromInvocation the identity of the method invocation
     * @return a set of method hash indices for all overloaded methods in a class or its nearest outer class.
     */
    private static int getMatchingHashIndexInOuterClass(String classHash, MethodIdentity identityFromInvocation) {
        int matchedMethodHashIndex = Constants.INVALID_METHOD_HASH_INDEX;
        // We have already populated the inner and outer class data structure
        // in CG phase 4, get the enclosing class hash from there.
        String enclosingClassHash = CallGraphDataStructures.getEnclosingClassHash(classHash);
        if (enclosingClassHash != null) {
            // Inside an inner or anonymous class, a method can access members
            // from its outer class or any of its super classes.
            // first check in the direct outer class and its super types
            matchedMethodHashIndex = lookUpServicingMethodInThisAndSuperClass(
                    enclosingClassHash, identityFromInvocation, new HashSet<>(1));
            if (matchedMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                // Neither the outer class nor its super classes contain any method
                // that match our target method, check if the outer class itself is an inner class
                // then it can also access methods form there (for multi level inner class)
                matchedMethodHashIndex = getMatchingHashIndexInOuterClass(enclosingClassHash, identityFromInvocation);
            }
        }
        return matchedMethodHashIndex;
    }

    /**
     * Checks if the method identity coming from a method invocation "best" matches the identity coming
     * from a declared method. Because of overloading, a class may have several implementors for a method. When we match
     * identities, (a) we may find an exact match, we are done (b) we may have a complete mismatch, just ignore and move
     * on to the next (c) we may have a partial match. We keep the match information, and move on until we get a better
     * match.
     *
     * <p>We do not match return types, because of covariance in return types, the return type may match the type of the
     * container class.
     *
     * @param identityFromInvocation the MethodIdentity, that comes from invocation
     * @param identityFromDeclaration the MethodIdentity, that comes from declaration
     * @param varargsMatching false in most cases; resembles normal matching. all actual parameters are matched with
     *     corresponding formal parameters. if true, we match excluding the last formal parameter (which is the varags
     *     parameter).
     * @return TriBool indicating exact match, partial match, or no match. For a partial match denote the parts that do
     *     not match. This is in a list form with most params in the list being null, because there is a match. For the
     *     params that do not match the list element is a pair of pairs---the first element describes the mismatch kind
     *     and the second element is a pair with the first element the desired type from the the formal parameter and
     *     the second element the actual parameter type passed
     */
    private static Pair<TriBool, List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>>> getBestMatchInfo(
            MethodIdentity identityFromInvocation, MethodIdentity identityFromDeclaration, boolean varargsMatching) {
        int paramCount = varargsMatching
                ? identityFromDeclaration.getArgParamTypeInfos().size() - 1
                : identityFromDeclaration.getArgParamTypeInfos().size();
        // we need to match type for parameters and return type
        int targetsToMatchCount = paramCount + 1;
        List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>> mismatchInfo = new ArrayList<>(targetsToMatchCount);
        // Filling up the list with null
        // Later we will set some of the entries with mis match info
        for (int i = 0; i < targetsToMatchCount; i++) {
            mismatchInfo.add(null);
        }
        TriBool matchResult = TriBool.True;
        // Get the actual types of arguments and return value
        List<TypeInfo> actualTypes = new ArrayList<>(targetsToMatchCount);
        if (varargsMatching) {
            for (int i = 0; i < paramCount; i++) {
                actualTypes.add(identityFromInvocation.getArgParamTypeInfos().get(i));
            }
        } else {
            actualTypes.addAll(identityFromInvocation.getArgParamTypeInfos());
        }
        actualTypes.add(identityFromInvocation.getReturnTypeInfo());
        // Get the formal types of parameters and return value
        List<TypeInfo> declaredTypes = new ArrayList<>(targetsToMatchCount);
        if (varargsMatching) {
            for (int i = 0; i < paramCount; i++) {
                declaredTypes.add(identityFromDeclaration.getArgParamTypeInfos().get(i));
            }
        } else {
            declaredTypes.addAll(identityFromDeclaration.getArgParamTypeInfos());
        }
        declaredTypes.add(identityFromDeclaration.getReturnTypeInfo());
        for (int position = 0; position < targetsToMatchCount; position++) {
            TypeInfo actualType = actualTypes.get(position);
            TypeInfo declaredType = declaredTypes.get(position);
            // if any typeinfo is null, that means we can not calculate typeInfo of that
            // ASTNode. So we will skip this argument
            if (actualType == null || declaredType == null) {
                matchResult = TriBool.MayBe;
                if (actualType == null && declaredType == null) {
                    mismatchInfo.set(position, Pair.of(MismatchKind.NULL_TYPE_BOTH, Pair.of(declaredType, actualType)));
                } else if (actualType == null) {
                    mismatchInfo.set(
                            position, Pair.of(MismatchKind.NULL_TYPE_ACTUAL, Pair.of(declaredType, actualType)));
                } else {
                    mismatchInfo.set(
                            position, Pair.of(MismatchKind.NULL_TYPE_FORMAL, Pair.of(declaredType, actualType)));
                }
            } else {
                if (!declaredType.equals(actualType)
                        && !((declaredType instanceof ParameterizedTypeInfo)
                                && (actualType instanceof ParameterizedTypeInfo)
                                && declaredType.getTypeErasure().equals(actualType.getTypeErasure()))) {
                    // Automatic type conversion is done on
                    // Numbers (i.e. int, long, short, byte, float and double)
                    // by the compiler. So, instead of matching the exact type
                    // we are matching if the argument falls under this number category.
                    // See if the mismatch is from this category
                    if (isNumberType(declaredType) && isNumberType(actualType)) {
                        // The parameters can still be allowed
                        if (allowedConversion((ScalarTypeInfo) actualType, (ScalarTypeInfo) declaredType)) {
                            // Previously we consider this case like exact match,
                            // which causes early return leaving search for better
                            // match further, so now we consider this as new kind of
                            // mismatch which indicates it matches through
                            // auto-conversion of numeric types.
                            matchResult = TriBool.MayBe;
                            mismatchInfo.set(
                                    position,
                                    Pair.of(MismatchKind.NUMERIC_TYPE_AUTOCOVERT, Pair.of(declaredType, actualType)));
                        } else {
                            return Pair.of(TriBool.False, null);
                        }
                    } else if (isEqualArrayType(actualType, declaredType)) {
                        // Matched array type, move on
                    } else if (actualType.getName().equals(Constants.NULL_OBJECT_TYPE)) {
                        // Null matches with everything, so we move on
                    } else if (actualType.getName().equals(Constants.DUMMY_TYPE_CLASS)) {
                        // When we fail to resolve binding, create dummy
                        // class type for the return type of a method.
                        // That means return type is not available for this invocation.
                        // So relax return type check and move on.
                    } else if (declaredType.getName().equals(Constants.JAVA_LANG_OBJECT)) {
                        // A formal type of Object will match with everyone, but there may
                        // be a better match somewhere else, so we update the mismatch info
                        // Note, we already know from the top most check that the actual is not an Object
                        // So, we can update the mismatch info
                        matchResult = TriBool.MayBe;
                        mismatchInfo.set(
                                position, Pair.of(MismatchKind.SUPER_IN_FORMAL, Pair.of(declaredType, actualType)));
                    } else if (actualType.getName().equals(Constants.JAVA_LANG_OBJECT)) {
                        // We come here if the actual param is an Object
                        // but the formal param is not.
                        // This check is positional, must be left here,
                        // since it depends on the failure of the previous check
                        return Pair.of(TriBool.False, null);
                    } else if (declaredType instanceof EnumTypeInfo
                            && actualType instanceof ClassTypeInfo
                            && actualType.getName().contains(Constants.LIB_TYPE)
                            && TypeInfo.matchTypeErasure(actualType, declaredType)) {
                        // When we find a library type enum for parameter in method declaration,
                        // we create an EnumTypeInfo. If a library method return value is passed
                        // as actual argument, from spec-info we get ClassTypeInfo which causes
                        // a missmatch, as we can not distinguish between class and enum in spec.
                        // So found a match, move on
                    } else {
                        // If the two argument/parameter types do not match, it does not necessarily mean
                        // that they are not same.
                        // Because one of them can be a subclass of the other.
                        // Like, B is a subclass of A. If there is a method signature foo(A a), and an
                        // object `B b`
                        // then we can call on foo(A a) like this,
                        // foo(b);
                        // So, if the actual param is a subclass of the other, we consider it to be same
                        // and do nothing.
                        // Otherwise, we return false to indicate that the two methods do not have the same
                        // signature.

                        // We can only consider the case, where the param types of invocation is subtype
                        // of the param types of declaration. Vice versa is not allowed.
                        if (actualType.matches(declaredType)) {
                            matchResult = TriBool.MayBe;
                            mismatchInfo.set(
                                    position, Pair.of(MismatchKind.SUPER_IN_FORMAL, Pair.of(declaredType, actualType)));
                        } else {
                            // The formal and actual param does not match at all
                            // No match
                            // But we do one final check for these cases.
                            //   1. Actual is a library, formal is in source code
                            //           We return false. Based on the criteria that a library does not
                            //           extend a class in source code.
                            //   2. Actual is in source, formal is in library.
                            //           We return SUPER_IN_FORMAL mismatch.
                            //           Based on the criteria that a class may extend a library
                            //           but we do not know about the exact match.
                            //   3. Actual and formal both in library.
                            //           We return SUPER_IN_FORMAL mismatch.
                            //           Based on the criteria that we have no info about the class
                            //           hierarchy. So making a bet. This may lead to bad method
                            //           mismatch, but we are taking the risk.
                            //           What are the odds of a method call returning
                            //   4. Actual and formal both in source code.
                            //          Nothing to do, there is no match. Go with false.
                            if (declaredType.getTypeErasure().startsWith(Constants.LIB_TYPE)) {
                                if (actualType.getTypeErasure().startsWith(Constants.LIB_TYPE)) {
                                    // Case 3
                                    // First match the erasure types
                                    if (TypeInfo.matchTypeErasure(declaredType, actualType)) {
                                        // Matched the erasure types, do nothing
                                    }
                                    matchResult = TriBool.MayBe;
                                    mismatchInfo.set(
                                            position,
                                            Pair.of(MismatchKind.LIBRARY_TYPE_BOTH, Pair.of(declaredType, actualType)));
                                } else {
                                    // Case 2
                                    matchResult = TriBool.MayBe;
                                    mismatchInfo.set(
                                            position,
                                            Pair.of(
                                                    MismatchKind.LIBRARY_TYPE_FORMAL,
                                                    Pair.of(declaredType, actualType)));
                                }
                            } else {
                                // Case 1 and 4
                                return Pair.of(TriBool.False, null);
                            }
                        }
                    }
                } else {
                    // if the params matched, move on
                }
            }
        }
        if (matchResult.isTrue()) {
            return Pair.of(TriBool.True, null);
        } else {
            return Pair.of(matchResult, mismatchInfo);
        }
    }

    /**
     * Checks if the new mismatch info is a better match than the existing mismatch info.
     *
     * @param existingMismatchInfo the existing mismatch info which contains for each formal param the mismatch kind and
     *     the pair of the formal param specified and the actual param passed.
     * @param newMismatchInfo the the new mismatch infor
     * @return true if the new mismatch info is better, false otherwise.
     */
    private static boolean isABetterMatch(
            List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>> existingMismatchInfo,
            List<Pair<MismatchKind, Pair<TypeInfo, TypeInfo>>> newMismatchInfo) {
        int mismatchCountInExisting = 0;
        int mismatchCountInNew = 0;
        for (Pair<MismatchKind, Pair<TypeInfo, TypeInfo>> temp : existingMismatchInfo) {
            if (temp != null) {
                mismatchCountInExisting++;
            }
        }
        for (Pair<MismatchKind, Pair<TypeInfo, TypeInfo>> temp : newMismatchInfo) {
            if (temp != null) {
                mismatchCountInNew++;
            }
        }
        if (mismatchCountInNew > mismatchCountInExisting) {
            return false;
        } else if (mismatchCountInNew < mismatchCountInExisting) {
            return true;
        } else {
            // The mismatch counts are the same
            // So a deeper match is needed
            // In this case, we will only attempt to handle the cases
            // so that the new mismatch info is fully aligned with the
            // existing mis match info  and in some cases it is getting a
            // better match because the type in the new match is better than
            // the type in the existing match
            int newMatchBeingBetterCount = 0;
            for (int i = 0; i < existingMismatchInfo.size(); i++) {
                Pair<MismatchKind, Pair<TypeInfo, TypeInfo>> existingMismatch = existingMismatchInfo.get(i);
                Pair<MismatchKind, Pair<TypeInfo, TypeInfo>> newMismatch = newMismatchInfo.get(i);
                if (existingMismatch != null) {
                    if (newMismatch == null) {
                        return false;
                    } else {
                        switch (existingMismatch.fst) {
                            case NULL_TYPE_ACTUAL: // elide
                            case NULL_TYPE_BOTH:
                                // Null type in actual means, we could not
                                // determine the type in the actual param
                                // So, we cannot make a judgment
                                break;
                            case NULL_TYPE_FORMAL:
                                if (newMismatch.fst == MismatchKind.NULL_TYPE_FORMAL) {
                                    // Cannot do any better
                                    // Move on if another can do better
                                } else if (newMismatch.fst == MismatchKind.SUPER_IN_FORMAL
                                        || newMismatch.fst == MismatchKind.LIBRARY_TYPE_FORMAL
                                        || newMismatch.fst == MismatchKind.LIBRARY_TYPE_BOTH
                                        || newMismatch.fst == MismatchKind.NUMERIC_TYPE_AUTOCOVERT) {
                                    // Making things better
                                    newMatchBeingBetterCount++;
                                    // We still cannot make a decision because now
                                    // we need to ensure that the alignment is alright
                                    // as in all the new mismatches are in the same position
                                    // of the old mismatches and are making things better in
                                    // some cases.
                                } else {
                                    // Other options cannot happen since an actual being
                                    // null will be impacting both the existing and new
                                    // mismatches. So, nothing to do here.
                                }
                                break;
                            case LIBRARY_TYPE_FORMAL:
                                if (newMismatch.fst == MismatchKind.NULL_TYPE_FORMAL) {
                                    // Doing worse
                                    return false;
                                } else if (newMismatch.fst == MismatchKind.LIBRARY_TYPE_FORMAL) {
                                    // The actual is a source type
                                    // and the formal are different libraries in different cases.
                                    // We cannot do any better.
                                    // Move on if another can do better
                                } else if (newMismatch.fst == MismatchKind.SUPER_IN_FORMAL) {
                                    // A better match found
                                    // Use that
                                    newMatchBeingBetterCount++;
                                } else if (newMismatch.fst == MismatchKind.NUMERIC_TYPE_AUTOCOVERT) {
                                    // Actual type is a numeric value that can be auto-casted
                                    // to the formal type, this is a better match
                                    newMatchBeingBetterCount++;
                                } else {
                                    // Other options cannot happen. Doing nothing here
                                    // LIBRARY_TYPE_BOTH: the actual has already been established as a source type.
                                    // It cannot be a library type now. So cannot happen.
                                    // NULL types: same reason. Actual has already been set as source
                                    // It cannot be a null type now.
                                }
                                break;
                            case LIBRARY_TYPE_BOTH:
                                if (newMismatch.fst == MismatchKind.NULL_TYPE_FORMAL) {
                                    // Doing worse
                                    return false;
                                } else if (newMismatch.fst == MismatchKind.LIBRARY_TYPE_BOTH) {
                                    // Cannot do any better
                                    // Move on if another can do better
                                } else if (newMismatch.fst == MismatchKind.SUPER_IN_FORMAL) {
                                    // A better match found
                                    // Use that
                                    newMatchBeingBetterCount++;
                                } else {
                                    // Other options cannot happen. Doing nothing here
                                    // LIBRARY_TYPE_FORMAL: the actual has already been established as a library type.
                                    // It cannot be a source type now. So cannot happen.
                                    // NULL types: same reason. Actual has already been set as a library
                                    // It cannot be a null type now.
                                }
                                break;
                            case SUPER_IN_FORMAL:
                                if (newMismatch.fst == MismatchKind.NULL_TYPE_FORMAL) {
                                    // Doing worse
                                    return false;
                                } else if (newMismatch.fst == MismatchKind.LIBRARY_TYPE_FORMAL
                                        || newMismatch.fst == MismatchKind.LIBRARY_TYPE_BOTH) {
                                    // Doing worse
                                    return false;
                                } else if (newMismatch.fst == MismatchKind.SUPER_IN_FORMAL) {
                                    // The first part in the tuple is the specified formal param
                                    // in the method, and the second part is the specified actual param.
                                    // The actual params are the same in both cases. We need to
                                    // see if the first param is closer to the actual param
                                    if (newMismatch.snd.fst.equals(existingMismatch.snd.fst)) {
                                        // Same types, not making a better match
                                    } else {
                                        String existingMismatchClassHash = existingMismatch.snd.fst.getName();
                                        String newMismatchClassHash = newMismatch.snd.fst.getName();
                                        Set<String> subclassesOfNewMismatch =
                                                CallGraphDataStructures.getAllSubClass(newMismatchClassHash);
                                        // Since this was encoded as super in formal, we had expected
                                        // that the formal will have a super class and will therefore
                                        // give some sub classes from which we will pick.
                                        // But the formal may also be a symbolic type and technically
                                        // a symbolic type has type erasure Object, i.e., it is an Object-like
                                        // So, in that case, the subclasses will be null. Adding this subclass null
                                        // check here.
                                        // If the existing mismatch was with an actual parameterized type and the new
                                        // mismatch
                                        // was with a symbolic type, we should stay with the existing mismatch, since
                                        // the
                                        // new one is not better.
                                        if (subclassesOfNewMismatch == null
                                                || subclassesOfNewMismatch.contains(existingMismatchClassHash)) {
                                            // The existing mismatch is a sub class of the newly
                                            // mismatched class
                                            // This means that the existing mismatch is closer to
                                            // the actual type
                                            // So the new mismatch is not better
                                            return false;
                                        } else {
                                            // The new class is in the upstream of the
                                            // current mismatch. So it is a better fit
                                            newMatchBeingBetterCount++;
                                        }
                                    }
                                }
                                break;
						case NUMERIC_TYPE_AUTOCOVERT:
							break;
						default:
							break;
                        }
                    }
                }
            }
            if (newMatchBeingBetterCount > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the method identity coming from a method declaration "exactly" matches the identity coming
     * from a declared method in a super class, therefore ensuring that this method overrides the one above. Because of
     * overloading, a class may have several implementors for a method. When we match identities, (a) we may find an
     * exact match, we are done (b) we may have a complete mismatch, just ignore and move on to the next (c) we may have
     * a partial match. We also ignore.
     *
     * <p>We do not match return types, because of covariance in return types, the return type may match the type of the
     * container class.
     *
     * @param identityFromSuperDeclaration the MethodIdentity, that comes from a method declared in a superclass
     * @param identityFromOverridingCandidate the MethodIdentity, that comes from declaration
     * @return TriBool indicating exact match, partial match, or no match. For a partial match denote the parts that do
     *     not match. This is in a list form with most params in the list being null, because there is a match. For the
     *     params that do not match the list element is a pair of pairs---the first element describes the mismatch kind
     *     and the second element is a pair with the first element the desired type from the the formal parameter and
     *     the second element the actual parameter type passed
     */
    private static boolean getExactMatchInfo(
            MethodIdentity identityFromSuperDeclaration, MethodIdentity identityFromOverridingCandidate) {
        int paramCount = identityFromSuperDeclaration.getArgParamTypeInfos().size();
        for (int paramPosition = 0; paramPosition < paramCount; paramPosition++) {
            TypeInfo formalParamAtSuper =
                    identityFromSuperDeclaration.getArgParamTypeInfos().get(paramPosition);
            TypeInfo formalParamAtCandidate =
                    identityFromOverridingCandidate.getArgParamTypeInfos().get(paramPosition);
            // if any typeinfo is null, that means we can not calculate typeInfo of that
            // ASTNode. So we will skip this argument
            if (formalParamAtSuper == null || formalParamAtCandidate == null) {
                return false;
            } else {
                if (!formalParamAtCandidate.equals(formalParamAtSuper)) {
                    return false;
                } else {
                    // if the params matched, move on
                }
            }
        }
        return true;
    }

    /**
     * Checks whether the given type info represents a number
     *
     * @param typeInfo the type info that needs to be tested
     * @return true if it is a number, false otherwise
     */
    private static boolean isNumberType(TypeInfo typeInfo) {
        if (typeInfo instanceof ScalarTypeInfo) {
            ScalarTypeInfo givenType = (ScalarTypeInfo) typeInfo;
            if ((givenType.getName().equals("long")
                    || givenType.getName().equals("int")
                    || givenType.getName().equals("short")
                    || givenType.getName().equals("byte")
                    || givenType.getName().equals("float")
                    || givenType.getName().equals("double"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the number conversion allowed
     *
     * @param actualParamTypeInfo the actual param's type info
     * @param formalParamTypeInfo the formal param's type info
     * @return true if the conversion is allowed, false otherwise
     */
    private static boolean allowedConversion(ScalarTypeInfo actualParamTypeInfo, ScalarTypeInfo formalParamTypeInfo) {
        if (formalParamTypeInfo.getName().equals("int")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("long")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("short")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("byte")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("char")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("float")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")
                    || actualParamTypeInfo.getName().equals("float")
                    || actualParamTypeInfo.getName().equals("double")) {
                return true;
            }
        } else if (formalParamTypeInfo.getName().equals("double")) {
            if (actualParamTypeInfo.getName().equals("long")
                    || actualParamTypeInfo.getName().equals("int")
                    || actualParamTypeInfo.getName().equals("short")
                    || actualParamTypeInfo.getName().equals("byte")
                    || actualParamTypeInfo.getName().equals("char")
                    || actualParamTypeInfo.getName().equals("float")
                    || actualParamTypeInfo.getName().equals("double")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Array types getName method sometimes contain [] operator and sometimes doesn't It depends on from what
     * we are creating this type info for example: `int[] a` contains the [] but `int a[]` doesn't Again if we create
     * the type info from binding it also contains the [] symbol. Due to this variation we can't match methods properly.
     * Thus, we need to strip the [] symbol from the name while matching.
     *
     * @param fst the first type info
     * @param snd the second type info
     * @return true if the array criteria meets and there is a match after canonicalizing by stripping the [] false
     *     otherwise
     */
    private static boolean isEqualArrayType(TypeInfo fst, TypeInfo snd) {
        if (fst instanceof ArrayTypeInfo && snd instanceof ArrayTypeInfo && fst.matches(snd)) {
            return true;
        }
        return false;
    }
}
