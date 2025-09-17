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
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents enum type information in the call graph system.
 *
 * <p>EnumTypeInfo is a concrete implementation of TypeInfo that handles Java
 * enum types. It provides comprehensive information about enum declarations,
 * including field definitions and constant values.</p>
 * 
 * <p>The class supports various enum scenarios:</p>
 * <ul>
 *   <li><strong>Simple Enums:</strong> Basic enum declarations with constants</li>
 *   <li><strong>Enum with Fields:</strong> Enums that contain additional fields</li>
 *   <li><strong>Library Enums:</strong> External library enum types with special naming</li>
 *   <li><strong>Interface Implementation:</strong> Enums that implement interfaces</li>
 * </ul>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Constant Management:</strong> Stores enum constant names</li>
 *   <li><strong>Field Support:</strong> Handles enum fields with token range information</li>
 *   <li><strong>Type Compatibility:</strong> Enum matching and covering logic</li>
 *   <li><strong>Interface Support:</strong> Handles enums that implement interfaces</li>
 *   <li><strong>Immutable Design:</strong> Thread-safe with unmodifiable collections</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 * @see TypeInfo
 * @see TokenRange
 */
public final class EnumTypeInfo extends TypeInfo {
    /** 
     * Serialization version identifier for this class.
     * 
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /** 
     * The name or hash identifier of this enum type.
     * 
     * <p>This field stores either the hash of the enum if it's from source code,
     * or a special library type identifier in the format {@code LIB__OR__TYPE::<type-name>}
     * for external library enums.</p>
     */
    private final String name;

    /** 
     * Map of field names to field information tuples.
     * 
     * <p>This field stores comprehensive field information where each field name
     * maps to a tuple containing:</p>
     * <ul>
     *   <li><strong>Token Range:</strong> The source code location of the field</li>
     *   <li><strong>Container Hash:</strong> The hash of the enum containing the field</li>
     *   <li><strong>Field Type:</strong> The TypeInfo representing the field's type</li>
     * </ul>
     * 
     * <p>The collection is immutable and thread-safe for concurrent access.</p>
     */
    private final Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields;

    /** 
     * List of enum constant names.
     * 
     * <p>This field stores the names of all constants declared in this enum.
     * Enum constants are the predefined values that instances of the enum can take.
     * The list is immutable to ensure thread safety.</p>
     * 
     * <p>Example enum constants:</p>
     * <ul>
     *   <li><strong>Color enum:</strong> ["RED", "GREEN", "BLUE"]</li>
     *   <li><strong>Direction enum:</strong> ["NORTH", "SOUTH", "EAST", "WEST"]</li>
     * </ul>
     */
    private final List<String> constants;

    public EnumTypeInfo(String name) {
        this.name = name;
        this.fields = Collections.emptyMap();
        this.constants = Collections.emptyList();
    }

    public EnumTypeInfo(
            String name, Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields, List<String> constants) {
        this.name = name;
        // Copy the map and make it unmodifiable
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> copiedFields = new HashMap<>();
        copiedFields.putAll(fields);
        this.fields = Collections.unmodifiableMap(copiedFields);
        // Copy the list and make it immutable
        this.constants = List.of(constants.toArray(new String[] {}));
    }

    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return fields;
    }

    public List<String> getConstants() {
        return constants;
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
     * Checks whether this enum type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this enum type can be used where the declaration type is expected.</p>
     * 
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Enum-to-Enum Matching:</strong> Exact type match (enums cannot be extended)</li>
     *   <li><strong>Interface Implementation:</strong> Enum can match interface it implements</li>
     *   <li><strong>Generic Type Compatibility:</strong> Handles wildcards and type variables</li>
     * </ul>
     * 
     * <p><strong>Enum Matching Rules:</strong></p>
     * <ul>
     *   <li><strong>Exact Match:</strong> Enum can only match the exact same enum type</li>
     *   <li><strong>Interface Match:</strong> Enum can match interfaces it implements</li>
     *   <li><strong>No Inheritance:</strong> Enums cannot extend other enums in Java</li>
     * </ul>
     * 
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>Color.RED → Color:</strong> Exact match (same enum type)</li>
     *   <li><strong>Color.RED → Comparable:</strong> Match if Color implements Comparable</li>
     *   <li><strong>Color.RED → T:</strong> Match if T extends Color or Object</li>
     *   <li><strong>Color.RED → ?:</strong> Match if ? extends Color, Object, or Enum</li>
     * </ul>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this enum type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof EnumTypeInfo && matchTypeErasure(this, declarationType)) {
            // A enum type can be used in place of
            // another enum type if it is the exact same type
            // as enums can't be extended
            // Bar ---> Bar
            return true;
        } else if (declarationType instanceof ClassTypeInfo) {
            // A enum type can be used in place of
            // another class type if the class is an interface and the enum implements
            // that interface
            // Bar ---> Foo where Bar implements Foo
            // For library type enums specified in specs, we create them as ClassTypeInfo.
            return matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this);
        } else if (declarationType instanceof WildCardTypeInfo || declarationType instanceof SymbolicTypeInfo) {
            // An enum can be used in place of Type variable
            // and wild card in the following scenarios
            // Bar ---> T where <T>, <T extends Bar>, <T extends Foo> where Bar implements Foo
            // Bar ---> ? where <?>, <? extends Bar>, <? extends Foo> where Bar implements Foo
            // N.B.: ? can not be used as a stand-alone type
            // It can only be used inside other generic type
            // This scenario may occur in
            // List<Bar> ---> List<?>
            // List<Bar> ---> List<? extends Bar>
            // List<Bar> ---> List<? extends Object>
            // List<Bar> ---> List<? extends Enum>
            // List<Bar> ---> List<? extends Foo> where Bar implements Foo
            // List<Bar> ---> List<? super Bar>
            return declarationType.covers(this);
        }
        return false;
    }

    /**
     * Checks whether this enum type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this enum type can accept the invocation type.</p>
     * 
     * <p>The method implements strict enum covering rules:</p>
     * <ul>
     *   <li><strong>Exact Type Match:</strong> Enum can only cover the exact same enum type</li>
     *   <li><strong>Library Enum Handling:</strong> Special handling for library enums created as ClassTypeInfo</li>
     *   <li><strong>No Subtype Coverage:</strong> Enums cannot cover subtypes since they cannot be extended</li>
     * </ul>
     * 
     * <p><strong>Covering Examples:</strong></p>
     * <ul>
     *   <li><strong>Color → Color:</strong> Cover (exact same enum type)</li>
     *   <li><strong>Color → Direction:</strong> No cover (different enum types)</li>
     *   <li><strong>Color → Color.RED:</strong> No cover (constant vs. enum type)</li>
     *   <li><strong>Color → Object:</strong> No cover (Object is not an enum)</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> Unlike the {@link #matches(TypeInfo)} method, this method
     * does not handle interface implementation or generic type scenarios. It focuses solely
     * on exact enum type matching.</p>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this enum type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        // An enum can cover enum of the exact same type
        // For library type enums specified in specs, we create them as ClassTypeInfo.
        if (invocationType instanceof EnumTypeInfo || invocationType instanceof ClassTypeInfo) {
            return matchTypeErasure(invocationType, this);
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
        // Nothing to be done for an enum type
    }

    /**
     * Replaces symbolic types in this enum with their captured concrete types.
     *
     * <p>This method always returns a pair indicating no replacement occurred and
     * returns this enum unchanged. Since enums cannot contain symbolic types,
     * no replacement is ever needed or possible.</p>
     * 
     * <p>Enum type characteristics that prevent symbol replacement:</p>
     * <ul>
     *   <li><strong>No Generic Parameters:</strong> Enums cannot be generic</li>
     *   <li><strong>No Type Variables:</strong> Enums cannot contain type parameters</li>
     *   <li><strong>No Wildcards:</strong> Enums cannot contain wildcard types</li>
     *   <li><strong>Fixed Structure:</strong> Enum structure is compile-time determined</li>
     * </ul>
     * 
     * <p>The method returns:</p>
     * <ul>
     *   <li><strong>First Element (Boolean):</strong> false (no replacement occurred)</li>
     *   <li><strong>Second Element (TypeInfo):</strong> this enum (unchanged)</li>
     * </ul>
     * 
     * <p>This method is provided to satisfy the TypeInfo interface contract but
     * performs no actual work since enum types don't require symbolic type replacement.</p>
     *
     * @param pastContainers list of parameterized types encountered during traversal (unused)
     * @param alreadyReplacedPaths set of already processed replacement paths (unused)
     * @param capturedSymbolicTypes map of symbolic types to their captured values (unused)
     * @param capturedWildCardTypes map of wildcard types to their captured values (unused)
     * @return a pair indicating no replacement occurred and this enum unchanged
     */
    @Override
    public Pair<Boolean, TypeInfo> replaceSymbol(
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> alreadyReplacedPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        return Pair.of(false, this);
    }

    /**
     * Bounds wildcard or symbolic types in this enum to their concrete types.
     *
     * <p>This method always returns this enum unchanged. Since enums cannot contain
     * wildcard types, symbolic types, or generic parameters, no bounding is ever
     * needed or possible.</p>
     * 
     * <p>Enum type characteristics that prevent bounding:</p>
     * <ul>
     *   <li><strong>No Generic Parameters:</strong> Enums cannot be generic</li>
     *   <li><strong>No Type Variables:</strong> Enums cannot contain type parameters</li>
     *   <li><strong>No Wildcards:</strong> Enums cannot contain wildcard types</li>
     *   <li><strong>Concrete Types:</strong> Enums are always concrete, resolved types</li>
     * </ul>
     * 
     * <p>This method is provided to satisfy the TypeInfo interface contract but
     * performs no actual work since enum types don't require wildcard or symbolic
     * type bounding.</p>
     *
     * @return this enum unchanged (no bounding needed)
     */
    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((constants == null) ? 0 : constants.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        EnumTypeInfo other = (EnumTypeInfo) obj;
        if (constants == null) {
            if (other.constants != null) return false;
        } else if (!constants.equals(other.constants)) return false;
        if (fields == null) {
            if (other.fields != null) return false;
        } else if (!fields.equals(other.fields)) return false;
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
        return new EnumTypeInfo(this.name, fields, constants);
    }
}
