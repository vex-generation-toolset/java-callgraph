/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to contain various data structures that are to be used when calculating a method invocation type.
 *
 * <p>This data structure is kept in a map for each class when we are looking for a method. 
 * The matched method is kept in the relevant method index field, when a match is found.</p>
 *
 * <p>This class serves as a temporary workspace during method invocation type calculations. It tracks
 * method matches, inheritance relationships, and subscriber information across the class hierarchy.</p>
 *
 * <p><strong>Example Inheritance Scenario:</strong></p>
 * <p>Say we have 
 *        A <- B <- C <- D <- E 
 * meaning A is the superclass and E is the class lowest in the inheritance hierarchy. 
 * Say we are matching for a method m which is described in an interface A (not implemented) and then
 * implemented in C and E. 
 * For class E, we will have only one method invocation type. The subscriber subclasses type
 * should be E. 
 * For class D, one method invocation type. The subscriber subclasses type should be D (not E, because E has
 * its own method). 
 * For class C, two method invocation types, the one from E and the one from C. The subscriber
 * subclasses type should be C and D. 
 * For class B, two method invocation types, the one from E and the one from C.
 * The subscriber subclasses type should be B. 
 * For class A, two method invocation types, the one from E and the one from C.
 * The subscriber subclasses type should be A and B.</p>
 *
 * @author Munawar Hafiz
 */
public class MethodInvocationCalculationScratchPad {

    /**
     * The method hash index of the matched method.
     *
     * <p>If the class has a matched method, this field contains the relevant method index
     * and there is also a method invocation. If the class does not have a matched method,
     * this field contains an invalid index.</p>
     */
    int relevantMethodIndex;

    /**
     * The container class index.
     *
     * <p>This field stores the class index to avoid some future calculations
     * and provides quick access to the containing class information.</p>
     */
    int containerClassIndex;

    /**
     * The method invocation if there is a matched method.
     *
     * <p>This field contains the MethodInvocationType object when a method match
     * is found for the current class.</p>
     */
    MethodInvocationType invocation;

    /**
     * The indices of other matched methods which have method invocations.
     *
     * <p>This set keeps the indices of method hashes for several reasons:</p>
     * <ul>
     *   <li>The data structure is simpler when using integer indices</li>
     *   <li>The method invocation type is not created in one go - it's created and then
     *       undergoes changes when subclasses change</li>
     *   <li>We keep the data in one place and let it change there</li>
     *   <li>The integer index is created against the method hash (which is stable) rather
     *       than against the changing data structure</li>
     * </ul>
     *
     * <p>Each method hash has its own method info bundle where the data structure is kept.</p>
     */
    Set<Integer> subclassMethodInvocationTypes;

    /**
     * All the classes that contain or inherit the method for which we are calculating a match.
     *
     * <p>This set includes the current class where the method resides as well as all
     * subclasses that inherit or override the method. It represents the complete
     * subscriber hierarchy for the method.</p>
     */
    Set<Integer> subscriberSubclasses;

    /**
     * Constructs a new MethodInvocationCalculationScratchPad.
     *
     * <p>This constructor initializes the scratch pad with the given method index and
     * container class index. It also initializes the collections for tracking
     * subclass method invocation types and subscriber subclasses.</p>
     *
     * @param relevantMethodIndex the index of the relevant method hash
     * @param containerClassHashIndex the index of the container class hash
     */
    public MethodInvocationCalculationScratchPad(int relevantMethodIndex, int containerClassHashIndex) {
        this.relevantMethodIndex = relevantMethodIndex;
        this.containerClassIndex = containerClassHashIndex;
        subclassMethodInvocationTypes = new HashSet<>(4);
        subscriberSubclasses = new HashSet<>();
    }

    public int getRelevantMethodIndex() {
        return relevantMethodIndex;
    }

    public void setRelevantMethodIndex(int methodIndex) {
        this.relevantMethodIndex = methodIndex;
    }

    public MethodInvocationType getInvocationType() {
        return invocation;
    }

    public void setMethodInvocationType(MethodInvocationType invocation) {
        this.invocation = invocation;
    }

    public void addToMethodInvocationTypes(int methodInvocationTypeIndex) {
        subclassMethodInvocationTypes.add(methodInvocationTypeIndex);
    }

    public Set<Integer> getMethodInvocationTypesFromSubclasses() {
        return subclassMethodInvocationTypes;
    }

    public void addToSubclasses(int subclassHash) {
        subscriberSubclasses.add(subclassHash);
    }

    public void removeSubclasses(Set<Integer> subclasses) {
        subscriberSubclasses.removeAll(subclasses);
    }

    /**
     * Adds the subscriber classes to the method invocation other types.
     *
     * <p>This method takes the classes in the subscriber classes set and adds them
     * to the other types in the method invocation type. This is typically called
     * when finalizing the method invocation type calculation.</p>
     *
     * <p>If no invocation type exists, this method does nothing.</p>
     */
    public void addSubscribersToMethodInvocationOtherTypes() {
        if (invocation != null) {
            Set<Integer> subscriberCopy = new HashSet<>();
            subscriberCopy.addAll(subscriberSubclasses);
            invocation.addToClassesThatContainOrInheritMethod(subscriberCopy);
        }
    }

    @Override
    public String toString() {
        return "MethodInvocationCalculationScratchPad [relevantMethodIndex="
                + relevantMethodIndex + ", invocation=" + invocation + ", subclassMethodInvocationTypes="
                + subclassMethodInvocationTypes + ", subscriberSubclasses=" + subscriberSubclasses + "]";
    }
}
