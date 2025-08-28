/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.openrefactory.analysis.type.typeinfo.TypeInfo;

/**
 * This class represents a method's identity or signature. It has the method's name, whether it is a
 * constructor or not, its return type, and a ordered list of types of parameters/arguments.
 *
 * <p>The MethodIdentity class serves as a comprehensive representation of a method's characteristics
 * in the call graph analysis system. It encapsulates:</p>
 * <ul>
 *   <li>Method name and signature information</li>
 *   <li>Return type details</li>
 *   <li>Parameter type information</li>
 *   <li>Method modifier flags (static, constructor, abstract, etc.)</li>
 * </ul>
 *
 * <p>The class uses a {@link BitSet} to efficiently store various method characteristics
 * that are determined during call graph processing. These bits are used to categorize
 * methods for analysis purposes.</p>
 *
 * <p>MethodIdentity objects are immutable once constructed, ensuring thread safety
 * and consistent behavior during call graph analysis.</p>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class MethodIdentity implements Serializable {

    /** Serialization version identifier */
    private static final long serialVersionUID = 1L;

    /** Ordered list of parameter/argument type information */
    private final List<TypeInfo> argParamTypeInfos;

    /** Return type information of the method */
    private final TypeInfo returnTypeInfo;

    /** Name of the method */
    private final String methodName;

    /**
     * Bit set storing various method characteristics and modifiers.
     *
     * <p>The bit set is initialized to be empty and is populated during call graph processing
     * using various setter methods. The intent is to use these bits later for method
     * categorization and analysis.</p>
     *
     * <p>When a method identity is created for method invocations, constructor calls, etc.,
     * the bits are typically not set as they represent call sites rather than method
     * declarations.</p>
     */
    private final BitSet methodBits;

    /**
     * Constructs a placeholder MethodIdentity instance.
     *
     * <p>This constructor creates a placeholder identity similar to null. It's used
     * when a method identity is needed but the actual method information is not ye
     * available. No bits are set in this case.</p>
     */
    public MethodIdentity() {
        argParamTypeInfos = Collections.emptyList();
        returnTypeInfo = null;
        methodName = null;
        methodBits = null;
        // This is a placeholder method identity
        // similar to null. So, no bits need to be se
    }

    /**
     * Constructs a complete MethodIdentity instance with all method information.
     *
     * <p>This constructor creates a fully specified method identity with the given
     * name, return type, and parameter types. The parameter list is defensively
     * copied and made immutable to ensure thread safety.</p>
     *
     * @param methodName the name of the method, must not be {@code null}
     * @param returnTypeInfo the return type information of the method, must not be {@code null}
     * @param argParamTypeInfos the list of parameter/argument type information, must not be {@code null}
     */
	public MethodIdentity(String methodName, TypeInfo returnTypeInfo, List<TypeInfo> argParamTypeInfos) {
		this.methodName = methodName;
		this.returnTypeInfo = returnTypeInfo;
		List<TypeInfo> copyArgTypeInfos = new ArrayList<>();
		copyArgTypeInfos.addAll(argParamTypeInfos);
		this.argParamTypeInfos = Collections.unmodifiableList(copyArgTypeInfos);
		methodBits = new BitSet(MethodIdentityBits.values().length);
	}

    /**
     * Retrieves the parameter/argument type information list.
     *
     * <p>The returned list is immutable and contains the types of all parameters
     * in the order they appear in the method signature.</p>
     *
     * @return an immutable list of parameter type information, never {@code null}
     */
    public List<TypeInfo> getArgParamTypeInfos() {
        return argParamTypeInfos;
    }

    /**
     * Retrieves the return type information.
     *
     * @return the return type information, or {@code null} for placeholder identities
     */
    public TypeInfo getReturnTypeInfo() {
        return returnTypeInfo;
    }

    /**
     * Sets the bodyless bit indicating the method has no implementation body.
     *
     * <p>This bit is typically set for abstract methods, interface methods,
     * and native methods that lack a concrete implementation.</p>
     */
    public void setBodylessBit() {
        methodBits.set(MethodIdentityBits.BODYLESS.ordinal());
    }

    /**
     * Sets the possibly polymorphic bit indicating the method may be overridden.
     *
     * <p>This bit is set for methods that could potentially be overridden
     * in subclasses, such as non-final instance methods.</p>
     */
    public void setPossiblyPolymorphicBit() {
        methodBits.set(MethodIdentityBits.POSSIBLY_POLYMORPHIC.ordinal());
    }

    /**
     * Checks if the method is an abstract or interface method with no body.
     *
     * <p>This method checks if both the bodyless and possibly polymorphic bits
     * are set, indicating a method that lacks implementation and may be overridden.</p>
     *
     * @return {@code true} if the method is both bodyless and possibly polymorphic, {@code false} otherwise
     */
    public boolean isAnAbstactOrInterfaceMethodWithNoBody() {
        // A method that is possibly polymorphic as well as bodyless
        return methodBits.get(MethodIdentityBits.BODYLESS.ordinal())
                && methodBits.get(MethodIdentityBits.POSSIBLY_POLYMORPHIC.ordinal());
    }

    /**
     * Sets the constructor bit indicating this is a constructor method.
     *
     * <p>Constructor methods have special semantics in Java and are treated
     * differently during call graph analysis.</p>
     */
    public void setConstructorBit() {
        methodBits.set(MethodIdentityBits.CONSTRUCTOR.ordinal());
    }

    /**
     * Checks if the method is a constructor.
     *
     * @return {@code true} if the method is a constructor, {@code false} otherwise
     */
    public boolean isConstructor() {
        return methodBits.get(MethodIdentityBits.CONSTRUCTOR.ordinal());
    }

    /**
     * Sets the static bit indicating this is a static method.
     *
     * <p>Static methods belong to the class rather than instances and
     * cannot be overridden.</p>
     */
    public void setStaticBit() {
        methodBits.set(MethodIdentityBits.STATIC.ordinal());
    }

    /**
     * Checks if the method is static.
     *
     * @return {@code true} if the method is static, {@code false} otherwise
     */
    public boolean isStatic() {
        return methodBits.get(MethodIdentityBits.STATIC.ordinal());
    }

    /**
     * Sets the virtual method bit indicating this is a virtual method.
     *
     * <p>Virtual methods can be overridden in subclasses and are subjec
     * to dynamic dispatch.</p>
     */
    public void setVirtualMethodBit() {
        methodBits.set(MethodIdentityBits.VIRTUAL.ordinal());
    }

    /**
     * Sets the default bit indicating this is a default method in an interface.
     *
     * <p>Default methods in interfaces provide a default implementation
     * that can be overridden by implementing classes.</p>
     */
    public void setDefaultBit() {
        methodBits.set(MethodIdentityBits.DEFAULT.ordinal());
    }

    /**
     * Checks if the method is a default method in an interface.
     *
     * @return {@code true} if the method is a default interface method, {@code false} otherwise
     */
    public boolean isDefaultMethodInAnInterface() {
        return methodBits.get(MethodIdentityBits.DEFAULT.ordinal());
    }

    /**
     * Retrieves the method name.
     *
     * @return the method name, or {@code null} for placeholder identities
     */
    public String getMethodName() {
        return methodName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argParamTypeInfos == null) ? 0 : argParamTypeInfos.hashCode());
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + ((returnTypeInfo == null) ? 0 : returnTypeInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MethodIdentity other = (MethodIdentity) obj;
        if (argParamTypeInfos == null) {
            if (other.argParamTypeInfos != null) return false;
        } else if (!argParamTypeInfos.equals(other.argParamTypeInfos)) return false;
        if (methodName == null) {
            if (other.methodName != null) return false;
        } else if (!methodName.equals(other.methodName)) return false;
        if (returnTypeInfo == null) {
            if (other.returnTypeInfo != null) return false;
        } else if (!returnTypeInfo.equals(other.returnTypeInfo)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MethodIdentity [argParamTypeInfos=" + argParamTypeInfos + ", returnTypeInfo=" + returnTypeInfo
                + ", methodName=" + methodName + ", methodBits=" + methodBits + "]";
    }
}
