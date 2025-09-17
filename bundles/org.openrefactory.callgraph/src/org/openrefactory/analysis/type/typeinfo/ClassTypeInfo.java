/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents class and interface type information in the call graph system.
 *
 * <p>ClassTypeInfo is a concrete implementation of TypeInfo that handles Java
 * classes and interfaces. It provides comprehensive type information including
 * field definitions, interface status, and inner class relationships.</p>
 *
 * <p>The class supports various class types:</p>
 * <ul>
 *   <li><strong>Regular Classes:</strong> Standard Java classes with fields and methods</li>
 *   <li><strong>Interfaces:</strong> Java interfaces that define contracts</li>
 *   <li><strong>Inner Classes:</strong> Classes defined within other classes</li>
 *   <li><strong>Library Types:</strong> External library classes with special naming</li>
 * </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Field Management:</strong> Comprehensive field information with token ranges</li>
 *   <li><strong>Type Classification:</strong> Distinguishes between classes, interfaces, and inner classes</li>
 *   <li><strong>Inheritance Support:</strong> Handles subtype relationships and Object compatibility</li>
 *   <li><strong>Library Integration:</strong> Special handling for external library types</li>
 *   <li><strong>Immutable Design:</strong> Thread-safe with unmodifiable field collections</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class ClassTypeInfo extends TypeInfo {

    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name or hash identifier of this class type.
     *
     * <p>This field stores either the hash of the class if it's from source code,
     * or a special library type identifier in the format {@code LIB__OR__TYPE::<type-name>}
     * for external library classes.</p>
     */
    private final String name;

    /**
     * Map of field names to field information tuples.
     *
     * <p>This field stores comprehensive field information where each field name
     * maps to a tuple containing:</p>
     * <ul>
     *   <li><strong>Token Range:</strong> The source code location of the field</li>
     *   <li><strong>Container Hash:</strong> The hash of the class containing the field</li>
     *   <li><strong>Field Type:</strong> The TypeInfo representing the field's type</li>
     * </ul>
     *
     * <p>The collection is immutable and thread-safe for concurrent access.</p>
     */
    private final Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields;

    /**
     * Indicates whether this type represents an interface.
     *
     * <p>This field distinguishes between classes and interfaces, which is important
     * for type compatibility checking and method resolution.</p>
     */
    private final boolean isInterface;

    /**
     * Indicates whether this type is an inner class.
     *
     * <p>This field identifies nested classes that are defined within other classes.
     * Inner classes have special access rules and scope considerations.</p>
     */
    private final boolean isInner;

    public ClassTypeInfo(String name) {
        this.name = name;
        this.fields = Collections.emptyMap();
        this.isInterface = false;
        this.isInner = false;
    }

    public ClassTypeInfo(
            String name,
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields,
            boolean isInterface,
            boolean isInner) {
        this.name = name;
        // Copy the map and make it unmodifiable
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> copiedFields = new HashMap<>();
        copiedFields.putAll(fields);
        this.fields = Collections.unmodifiableMap(copiedFields);
        this.isInterface = isInterface;
        this.isInner = isInner;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isInner() {
        return isInner;
    }

    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return this.fields;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getTypeErasure() {
        return this.name;
    }

    /**
     * Checks whether this class type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this class type can be used where the declaration type is expected.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Exact Match:</strong> Same class type (Bar → Bar)</li>
     *   <li><strong>Subtype Match:</strong> This type is a subtype of the declaration type (Bar → Foo if Bar extends Foo)</li>
     *   <li><strong>Enum Compatibility:</strong> Library enum types created as ClassTypeInfo</li>
     *   <li><strong>Generic Type Compatibility:</strong> Handles wildcards, type variables, and parameterized types</li>
     * </ul>
     *
     * <p>For generic types, the method delegates to the declaration type's {@link #covers(TypeInfo)}
     * method to handle complex type relationships involving wildcards and type variables.</p>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this class type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof ClassTypeInfo) {
            // A class type can be used in place of
            // another class type if it is the exact same type
            // Or it is a sub type of the required type
            // Bar ---> Bar
            // Bar ---> Foo if Bar is sub type of Foo
            return matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this);
        } else if (declarationType instanceof EnumTypeInfo) {
            // For library type enums specified in specs, we create them as ClassTypeInfo.
            return matchTypeErasure(declarationType, this);
        } else if (declarationType instanceof WildCardTypeInfo
                || declarationType instanceof SymbolicTypeInfo
                || declarationType instanceof ParameterizedTypeInfo) {
            // An class type can be used in place of Type variable
            // and wild card in the following scenarios
            // Bar ---> T where <T>, <T extends Bar>, <T extends Foo> where Bar is a sub type of Foo
            // Bar ---> ? where <?>, <? extends Bar>, <? extends Foo> where Bar is a sub type of Foo
            // Foo<T> ---> Bar where Bar extends Foo<T>
            // N.B.: ? can not be used as a stand-alone type
            // It can only be used inside other generic type
            // This scenario may occur in
            // List<Bar> ---> List<?>
            // List<Bar> ---> List<? extends Bar>
            // List<Bar> ---> List<? extends Foo> where Foo is a super type of Bar
            // List<Bar> ---> List<? super Bar>
            // List<Bar> ---> List<? super Buzz> where Buzz is a sub type of Bar
            return declarationType.covers(this);
        }
        return false;
    }

    /**
     * Checks whether this class type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this class type can accept the invocation type.</p>
     *
     * <p>The method handles several covering scenarios:</p>
     * <ul>
     *   <li><strong>Object Coverage:</strong> {@code java.lang.Object} covers all types</li>
     *   <li><strong>Enum Coverage:</strong> {@code java.lang.Enum} covers all enum types</li>
     *   <li><strong>Exact Match:</strong> Same class type (Foo → Foo)</li>
     *   <li><strong>Supertype Coverage:</strong> This type covers its subtypes (Foo → Bar if Bar extends Foo)</li>
     *   <li><strong>Generic Type Coverage:</strong> Handles type variables, wildcards, and parameterized types</li>
     * </ul>
     *
     * <p>This method is the inverse of {@link #matches(TypeInfo)} and is essential
     * for bidirectional type compatibility checking in the call graph system.</p>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this class type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        if (this.getName().equals(Constants.JAVA_LANG_OBJECT)) {
            // java.lang.Object is the super class for every other type
            // So, it covers everything
            return true;
        } else if (invocationType instanceof EnumTypeInfo) {
            // java.lang.Enum is the super type for all enums
            // So, it covers all enums
            // For library type enums specified in specs, we create them as ClassTypeInfo.
            return getName().equals(Constants.JAVA_LANG_ENUM) || matchTypeErasure(this, invocationType);
        }

        if (invocationType instanceof ClassTypeInfo
                || invocationType instanceof SymbolicTypeInfo
                || invocationType instanceof WildCardTypeInfo
                || invocationType instanceof ParameterizedTypeInfo) {
            // A class type covers another class type of same type or its sub types
            // Foo ---> Foo
            // Foo ---> Bar if Bar is a sub type of Foo
            // Foo ---> T if <T extends Foo> or <T extends Bar> where Bar is sub type of Foo
            // Foo ---> ? if <? extends Foo> or <? extends Bar> where Bar is sub type of Foo
            // Foo ---> Bar<T> where Bar<T> extends Foo
            if (matchTypeErasure(this, invocationType) || isErasureSubTypeOf(this, invocationType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean needsReplacement() {
        return false;
    }

    @Override
    public void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        // Nothing to be done for a class type
    }

    @Override
    public Pair<Boolean, TypeInfo> replaceSymbol(
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> alreadyReplacedPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        return Pair.of(false, this);
    }

    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + (isInner ? 1231 : 1237);
        result = prime * result + (isInterface ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ClassTypeInfo other = (ClassTypeInfo) obj;
        if (fields == null) {
            if (other.fields != null) return false;
        } else if (!fields.equals(other.fields)) return false;
        if (isInner != other.isInner) return false;
        if (isInterface != other.isInterface) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        return true;
    }

    @Override
    public String toString() {
        String typeName = CallGraphUtility.getClassNameFromClassHash(name);
        return typeName.substring(typeName.lastIndexOf(".") + 1);
    }

    @Override
    public TypeInfo clone() {
        return new ClassTypeInfo(this.name, this.fields, this.isInterface, this.isInner);
    }
}
