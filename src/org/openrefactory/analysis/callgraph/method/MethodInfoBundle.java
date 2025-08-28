/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A single place to store three critical method related data structures.
 *
 * <p>The MethodInfoBundle class serves as a container that consolidates all essential
 * information about a method in one place, facilitating efficient access during
 * call graph analysis. It contains:</p>
 * <ul>
 *   <li><strong>Method signature:</strong> Used to create the method hash for indexing</li>
 *   <li><strong>Method identity:</strong> Contains type information about method parameters, return type, etc.</li>
 *   <li><strong>Method invocation type:</strong> Contains information about the servicing method index and the classes which contain or inherit the method</li>
 * </ul>
 *
 * <p>This bundle is particularly useful for managing method information across
 * inheritance hierarchies and tracking polymorphic method relationships. It provides
 * a centralized way to access all method-related data without navigating multiple
 * data structures.</p>
 *
 * <p>The class also maintains information about subclasses that override the method,
 * enabling complete analysis of the method type hierarchy.</p>
 *
 * @author Munawar Hafiz
 */
public class MethodInfoBundle implements Serializable {

    /** Serialization version identifier */
    private static final long serialVersionUID = 1L;

    /** Method signature used for creating method hash and identification */
    private String signature;

    /** Method identity containing type information and characteristics */
    private MethodIdentity identity;

    /** Method invocation type with servicing method index and class information */
    private MethodInvocationType methodInvocationType;

    /**
     * Set of subclass method invocation type indices that override this method.
     *
     * <p>This collection provides a complete picture of the method type hierarchy
     * by storing the indices of subclass method hashes. The actual information
     * comes from the method invocation type entry at that class, which also
     * contains the index of this method.</p>
     *
     * <p>This bidirectional relationship enables efficient traversal of the
     * inheritance hierarchy during call graph analysis.</p>
     */
    private Set<Integer> methodInvocationTypesFromSubClasses;

    public MethodInfoBundle(String signature) {
        super();
        this.signature = signature;
    }

    public MethodIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(MethodIdentity identity) {
        this.identity = identity;
    }

    public MethodInvocationType getMethodInvocationType() {
        return methodInvocationType;
    }

    public void setMethodInvocationType(MethodInvocationType methodInvocationType) {
        this.methodInvocationType = methodInvocationType;
    }

    public String getSignature() {
        return signature;
    }

    public void setMethodInvocationTypesFromSubClasses(Set<Integer> subclassesMethodInvocationType) {
        if (methodInvocationTypesFromSubClasses == null) {
            methodInvocationTypesFromSubClasses = new HashSet<>(subclassesMethodInvocationType);
        }
    }

    public Set<Integer> getMethodInvocationTypesFromSubClasses() {
        if (methodInvocationTypesFromSubClasses != null) {
            return new HashSet<>(methodInvocationTypesFromSubClasses);
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((signature == null) ? 0 : signature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MethodInfoBundle other = (MethodInfoBundle) obj;
        if (signature == null) {
            if (other.signature != null) return false;
        } else if (!signature.equals(other.signature)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MethodInfoBundle [signature=" + signature + ", identity=" + identity + ", methodInvocationType="
                + methodInvocationType + "]";
    }
}
