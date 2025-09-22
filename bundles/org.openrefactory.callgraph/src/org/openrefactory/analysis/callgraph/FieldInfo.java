/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Contains comprehensive information about a field in the call graph analysis system.
 *
 * <p>The FieldInfo class represents a field declaration within a Java class, storing
 * essential metadata needed for call graph construction and analysis. It maintains
 * information about the field's characteristics, access modifiers, type information,
 * and initialization patterns.</p>
 * 
 * <p>Key features of this class:</p>
 * <ul>
 *   <li><strong>Field Identity:</strong> Name, container class, and type information</li>
 *   <li><strong>Access Modifiers:</strong> Support for public, private, static, and final modifiers</li>
 *   <li><strong>Initialization Tracking:</strong> Maps class hashes to initialization token ranges</li>
 *   <li><strong>Thread Safety:</strong> Synchronized methods for concurrent access during call graph construction</li>
 *   <li><strong>Serialization Support:</strong> Implements Serializable for persistence and transfer</li>
 * </ul>
 * 
 * <p>This class is used throughout the call graph construction process to:</p>
 * <ul>
 *   <li>Track field declarations and their properties</li>
 *   <li>Analyze field initialization patterns across inheritance hierarchies</li>
 *   <li>Create call graph edges from constructors to field initializers</li>
 *   <li>Support points-to analysis and type resolution</li>
 * </ul>
 *
 * @author Rifat Rubayatul Islam
 */
public final class FieldInfo implements Serializable {

    /** 
     * Serialization version identifier for this class.
     * 
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /** 
     * The name of the field.
     * 
     * <p>This field stores the simple name of the field as declared in the source code.
     * For example, for a field declaration "private String userName;", this would
     * contain "userName".</p>
     */
    private String fieldName;

    /** 
     * The hash of the container class.
     * 
     * <p>This field stores the unique hash identifier of the class that declares
     * this field. The hash is used throughout the call graph system to identify
     * classes and establish relationships between them.</p>
     */
    private String containerHash;

    /** 
     * The type information of the field.
     * 
     * <p>This field contains detailed type information including the field's
     * declared type, generic parameters, and other type-related metadata. This
     * information is essential for method resolution and type checking during
     * call graph analysis.</p>
     */
    private TypeInfo fieldTypeInfo;

    /** 
     * Access modifiers of the field.
     * 
     * <p>This field stores the access modifiers as a byte value using the
     * {@link AccessModifiers} encoding scheme. It can represent combinations
     * of modifiers such as public, private, static, final, etc.</p>
     * 
     * @see AccessModifiers
     */
    private byte modifiers;

    /** 
     * Mapping from class hash to initialization token ranges.
     * 
     * <p>This field tracks where and how field initializations occur across the
     * inheritance hierarchy. It maps class hashes (where initialization happens)
     * to sets of token ranges representing the initialization code locations.</p>
     * 
     * <p>The information is used to create call graph edges from constructors
     * to method invocations and other calls that occur during field initialization.
     * This data is populated in Phase 1 of call graph construction but processed
     * in Phase 4 when all necessary information is available.</p>
     * 
     * <p><strong>Key Design Points:</strong></p>
     * <ul>
     *   <li><strong>Class Hash as Key:</strong> The key represents where the
     *       initialization happens, not necessarily where the field is declared</li>
     *   <li><strong>Multiple Initializations:</strong> A field can be initialized
     *       in multiple classes in the inheritance hierarchy</li>
     *   <li><strong>Constructor Linking:</strong> Only initializers from the current
     *       class are added to that class's virtual default constructor's callee list</li>
     *   <li><strong>Inheritance Impact:</strong> Different initializations have different
     *       impacts on default constructor composition</li>
     * </ul>
     * 
     * <p><strong>Example Scenario:</strong></p>
     * <p>If a field is declared in class A and initialized in both class A and its
     * subclass B, this map will contain two entries: one with key A (class A's hash)
     * and one with key B (class B's hash). This allows the system to distinguish
     * between the two initialization contexts and create appropriate call graph edges.</p>
     */
    Map<String, Set<TokenRange>> initializerTokenRanges;

    public FieldInfo(String fieldName, String containerClassHash, byte modifiers) {
        this.fieldName = fieldName;
        this.containerHash = containerClassHash;
        this.modifiers = modifiers;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getContainerClassHash() {
        return containerHash;
    }

    public TypeInfo getFieldTypeInfo() {
        return fieldTypeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.fieldTypeInfo = typeInfo;
    }

    /**
     * Checks if the field has the static modifier.
     *
     * @return true if the field is static, false otherwise
     */
    public boolean isStatic() {
        return AccessModifiers.isStatic(modifiers);
    }

    /**
     * Checks if the field has the final modifier.
     *
     * @return true if the field is final, false otherwise
     */
    public boolean isFinal() {
        return AccessModifiers.isFinal(modifiers);
    }

    /**
     * Checks if the field has the public modifier.
     *
     * @return true if the field is public, false otherwise
     */
    public boolean isPublic() {
        return AccessModifiers.isPublic(modifiers);
    }

    /**
     * Checks if the field has the private modifier.
     *
     * @return true if the field is private, false otherwise
     */
    public boolean isPrivate() {
        return AccessModifiers.isPrivate(modifiers);
    }

    /**
     * Adds an initializer token range for a specific class.
     *
     * <p>This synchronized method adds a token range representing field initialization
     * code to the specified class. It handles the case where a field may be initialized
     * in multiple classes across the inheritance hierarchy.</p>
     *
     * <p>The method automatically initializes the data structure if it doesn't exist
     * and efficiently manages the token range collections. It's designed to handle
     * the common case where initialization occurs in only one class (hence the initial
     * capacity of 1 for the collections).</p>
     *
     * <p><strong>Thread Safety:</strong> This method is synchronized to ensure
     * thread-safe access during concurrent call graph construction.</p>
     *
     * @param classHash the hash of the class where the initialization occurs
     * @param initializerRange the token range representing the initialization code
     */
    public synchronized void addToInitializerTokenRanges(String classHash, TokenRange initializerRange) {
        if (initializerTokenRanges == null) {
            // Most of the time initialization in only one class
            initializerTokenRanges = new HashMap<>(1);
        }
        if (initializerTokenRanges.containsKey(classHash)) {
            initializerTokenRanges.get(classHash).add(initializerRange);
        } else {
            Set<TokenRange> tokenRanges = new HashSet<>(1);
            tokenRanges.add(initializerRange);
            initializerTokenRanges.put(classHash, tokenRanges);
        }
    }

    /**
     * Gets the initializer token ranges for a specific class.
     *
     * @param classHash the hash of the class to get initializer ranges for
     * @return a set of token ranges representing initialization code in the specified class,
     *         or null if no initializers exist for that class
     */
    public Set<TokenRange> getInitializerTokenRanges(String classHash) {
        return initializerTokenRanges != null ? initializerTokenRanges.get(classHash) : null;
    }

    /**
     * Gets an iterator over all initializer mappings.
     *
     * <p>This method provides access to iterate over all class hash to initializer
     * token range mappings. This is useful for processing all initializations
     * across the inheritance hierarchy.</p>
     *
     * @return an iterator over the initializer map entries, or null if no initializers exist
     */
    public Iterator<Map.Entry<String, Set<TokenRange>>> getInitializerMapIterator() {
        return initializerTokenRanges != null
                ? initializerTokenRanges.entrySet().iterator()
                : null;
    }

    /**
     * Checks if the initializer map is empty or null.
     *
     * <p>This utility method provides a convenient way to check if there are
     * any field initializers recorded for this field.</p>
     *
     * @return true if no initializers exist, false if there are initializers
     */
    public boolean isEmptyOrNullInitializerMap() {
        return initializerTokenRanges == null || initializerTokenRanges.isEmpty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((containerHash == null) ? 0 : containerHash.hashCode());
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
        result = prime * result + modifiers;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FieldInfo other = (FieldInfo) obj;
        if (containerHash == null) {
            if (other.containerHash != null) return false;
        } else if (!containerHash.equals(other.containerHash)) return false;
        if (fieldName == null) {
            if (other.fieldName != null) return false;
        } else if (!fieldName.equals(other.fieldName)) return false;
        if (modifiers != other.modifiers) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FieldInfo [fieldName=" + fieldName + ", containerHash=" + containerHash + ", fieldTypeInfo="
                + fieldTypeInfo + ", modifiers=" + modifiers + ", initializerTokenRanges=" + initializerTokenRanges
                + "]";
    }
}
