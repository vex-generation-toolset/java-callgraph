/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;

/**
 * Contains the information about the callee method hash and all the classes that either contain
 * the method or inherit the method.
 *
 * <p>The MethodInvocationType class represents a method invocation relationship in the call graph.
 * It tracks which classes can invoke a particular method, taking into account inheritance
 * relationships and inner class access patterns.</p>
 *
 * <p>This class is crucial for understanding method accessibility across the class hierarchy.
 * It handles complex scenarios including:</p>
 * <ul>
 *   <li>Direct method containment in classes</li>
 *   <li>Method inheritance through class hierarchies</li>
 *   <li>Inner class access to outer class methods</li>
 *   <li>Interface method implementation and inheritance</li>
 * </ul>
 *
 * <p>The class automatically propagates method accessibility information both upstream
 * (to superclasses) and downstream (to subclasses) during construction, ensuring
 * comprehensive coverage of the inheritance hierarchy.</p>
 *
 * @author Munawar Hafiz
 */
public class MethodInvocationType implements Serializable {

    /** Serialization version identifier */
    private static final long serialVersionUID = 1L;

    /**
     * The hash index of the callee method for this invocation.
     *
     * <p>This field stores the index of the method hash that represents the
     * method being invoked. It serves as the primary identifier for the
     * method invocation relationship.</p>
     */
    int calleeMethodHashIndex;

    /**
     * Set of class indices that can call this callee method.
     *
     * <p>This collection keeps track of all classes by which this callee method
     * can be called. It considers both direct containment and inheritance.</p>
     *
     * <p><strong>Example:</strong> If we have A <- B <- C <- D <- E (inheritance hierarchy)
     * and there is a foo() method implemented in A and C:</p>
     * <ul>
     *   <li>For A:foo(), the callsiteHashes will contain A and B</li>
     *   <li>For C:foo(), it contains C, D, E</li>
     * </ul>
     *
     * <p>This accounts for the fact that subclasses can access methods from
     * their superclasses, and inner classes can access methods from their
     * containing outer classes.</p>
     */
    Set<Integer> indicesOfClassesThatContainOrInheritMethod;

    /**
     * Constructs a new MethodInvocationType with the specified method hash index.
     *
     * <p>This constructor initializes the method invocation type and automatically
     * populates the class accessibility information by analyzing the inheritance
     * hierarchy and inner class relationships.</p>
     *
     * <p>During construction, the method performs several important operations:</p>
     * <ol>
     *   <li>Initializes the basic structure with the method hash index</li>
     *   <li>For non-constructor methods, adds inner classes of the containing class</li>
     *   <li>Propagates accessibility information upstream to superclasses</li>
     *   <li>Propagates accessibility information downstream to subclasses</li>
     * </ol>
     *
     * <p>The constructor handles special cases for inner classes, ensuring tha
     * methods defined in outer classes are accessible to their inner classes,
     * and vice versa where appropriate.</p>
     *
     * @param methodHashIndex the hash index of the method being invoked
     */
    public MethodInvocationType(int methodHashIndex) {
        super();
        this.calleeMethodHashIndex = methodHashIndex;
        this.indicesOfClassesThatContainOrInheritMethod = new HashSet<>(2);
        // For a method that is defined in an outer class,
        // Add information such that the inner classes inside the outer class
        // are registered as containers of the method.
        // This is then propagated both upstream and downstream.
        // Description below.
        if (methodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
            String methodHash = CallGraphDataStructures.getMethodHashFromIndex(methodHashIndex);
            if (methodHash != null) {
                MethodIdentity identity = CallGraphDataStructures.getMatchingMethodIdentity(methodHash);
                if (identity != null && !identity.isConstructor()) {
                    Set<String> visitedClasses = new HashSet<>(2);
                    String containingClassHash = CallGraphUtility.getClassHashFromMethodHash(methodHash);
                    addInnerClassesToMethodInvocationType(containingClassHash, visitedClasses);
                    // The information is propagated upstream.
                    // For a super class that has an inner class, that inner class
                    // will also have access to a method in the sub class since the
                    // inner class will be owned in the subclass and it can call a method
                    // there with a this.foo() or a foo() method call (See test o07).
                    //
                    //    class Super {
                    //        void foo() {
                    //            // Impl in Super
                    //        }
                    //        class Inner {
                    //              void bar() { this.foo(); }
                    //        }
                    //    }
                    //    class Sub extends Super {
                    //        void foo() {
                    //           // Override in sub
                    //        }
                    //    }
                    //
                    // In this case, foo method in Sub class also will be available for
                    // use in the bar () method in the Inner class of its super class.
                    // So the inner class of the super class is added as a class that
                    // also inherits the method.
                    Set<String> upstreamClasses =
                            CallGraphDataStructures.getAllSuperClassesAndImplementedInterfacesOfClass(
                                    containingClassHash);
                    if (upstreamClasses != null && !upstreamClasses.isEmpty()) {
                        for (String upstreamClass : upstreamClasses) {
                            addInnerClassesToMethodInvocationType(upstreamClass, visitedClasses);
                        }
                    }
                    // The information is propagated downstream.
                    // This is more easier to grasp.
                    // Any public or protected method will be available downstream (in a subclass)
                    // and it can be accessed by an inner class inside that subclass.
                    Set<String> subClasses = CallGraphDataStructures.getAllSubClass(containingClassHash);
                    if (subClasses != null && !subClasses.isEmpty()) {
                        for (String subClass : subClasses) {
                            addInnerClassesToMethodInvocationType(subClass, visitedClasses);
                        }
                    }
                }
            }
        }
    }

    public int getCalleeMethodHashIndex() {
        return calleeMethodHashIndex;
    }

    public void addToClassesThatContainOrInheritMethod(int callSiteHashIndex) {
        indicesOfClassesThatContainOrInheritMethod.add(callSiteHashIndex);
    }

    public void addToClassesThatContainOrInheritMethod(Set<Integer> callSiteHashIndices) {
        indicesOfClassesThatContainOrInheritMethod.addAll(callSiteHashIndices);
    }

    public void removeFromClassesThatContainOrInheritMethod(Set<Integer> callSiteHashIndices) {
        indicesOfClassesThatContainOrInheritMethod.removeAll(callSiteHashIndices);
    }

    public void copyOtherClassInfoFromAnotherInvocation(MethodInvocationType mit) {
        indicesOfClassesThatContainOrInheritMethod.addAll(mit.indicesOfClassesThatContainOrInheritMethod);
    }

    public Iterator<Integer> getIteratorForClassesThatContianOrInheritMethod() {
        return indicesOfClassesThatContainOrInheritMethod.iterator();
    }

    /**
     * Retrieves the set of class hashes that contain or inherit the method.
     *
     * <p>This method converts the internal class indices to actual class hashes,
     * providing a more meaningful representation of the accessible classes.</p>
     *
     * @return a set of class hashes that can access the method
     */
    public Set<String> getClassesThatContainOrInheritMethod() {
        Set<String> classHashes = new HashSet<>();
        Iterator<Integer> iter = indicesOfClassesThatContainOrInheritMethod.iterator();
        while (iter.hasNext()) {
            int classHashIndex = iter.next();
            String classHash = CallGraphDataStructures.getClassHashFromBitIndex(classHashIndex);
            if (classHash != null) {
                classHashes.add(classHash);
            }
        }
        return classHashes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + calleeMethodHashIndex;
        result = prime * result
                + ((indicesOfClassesThatContainOrInheritMethod == null)
                        ? 0
                        : indicesOfClassesThatContainOrInheritMethod.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MethodInvocationType other = (MethodInvocationType) obj;
        if (calleeMethodHashIndex != other.calleeMethodHashIndex) return false;
        if (indicesOfClassesThatContainOrInheritMethod == null) {
            if (other.indicesOfClassesThatContainOrInheritMethod != null) return false;
        } else if (!indicesOfClassesThatContainOrInheritMethod.equals(other.indicesOfClassesThatContainOrInheritMethod))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<Integer> iterator = indicesOfClassesThatContainOrInheritMethod.iterator();
        while (iterator.hasNext()) {
            int hashIndex = iterator.next();
            String hash = CallGraphDataStructures.getClassHashFromBitIndex(hashIndex);
            // Check for NullPointerException
            if (CallGraphDataStructures.getClassSignatureFromHash(hash) == null) {
                builder.append(hash);
            } else {
                builder.append(CallGraphUtility.getClassNameFromClassSignature(
                        CallGraphDataStructures.getClassSignatureFromHash(hash)));
            }
            builder.append(", ");
        }
        if (builder.length() > 1) {
            builder.setLength(builder.length() - 2);
        }
        builder.append("]");
        return "MethodInvocationType [ other methods =" + builder.toString() + ", calleeMethodHash="
                + (calleeMethodHashIndex == Constants.INVALID_METHOD_HASH_INDEX
                        ? "null"
                        : CallGraphUtility.getClassNameFromMethodSignature(
                                CallGraphDataStructures.getMethodSignatureFromHash(
                                        CallGraphDataStructures.getMethodHashFromIndex(calleeMethodHashIndex))))
                + "]";
    }

    /**
     * Adds inner classes to the method invocation type for a given containing class.
     *
     * <p>This method handles the case where an inner class method calls a method in the
     * containing outer class. It recursively adds the outer class as well as all inner
     * classes to the set of classes that can contain or inherit the outer class method.</p>
     *
     * <p>The method uses a visited classes set to prevent infinite recursion when
     * dealing with nested inner classes. It traverses the inner class hierarchy
     * and updates the method invocation type accordingly.</p>
     *
     * <p>This is important for call graph analysis because inner classes have
     * access to methods in their containing classes, and this access pattern
     * needs to be properly represented in the call graph.</p>
     *
     * @param containingClassHash the outer class hash
     * @param visitedClasses the set of already visited classes to prevent infinite recursion
     */
    private void addInnerClassesToMethodInvocationType(String containingClassHash, Set<String> visitedClasses) {
        if (containingClassHash != null) {
            if (!visitedClasses.contains(containingClassHash)) {
                visitedClasses.add(containingClassHash);
                Set<String> innerClasses =
                        CallGraphDataStructures.getInnerClassMap().get(containingClassHash);
                if (innerClasses != null && !innerClasses.isEmpty()) {
                    for (String innerClassHash : innerClasses) {
                        if (!innerClassHash.equals(containingClassHash)) {
                            int innerClassIndex =
                                    CallGraphDataStructures.updateOrGetBitIndexFromClassHash(innerClassHash);
                            if (innerClassIndex != Constants.INVALID_CLASS_HASH_INDEX) {
                                indicesOfClassesThatContainOrInheritMethod.add(innerClassIndex);
                            }
                        }
                        addInnerClassesToMethodInvocationType(innerClassHash, visitedClasses);
                    }
                }
            }
        }
    }
}
