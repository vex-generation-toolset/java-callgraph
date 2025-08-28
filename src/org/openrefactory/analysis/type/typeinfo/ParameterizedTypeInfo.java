/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.IntObjectPair;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents parameterized type information in the call graph system.
 *
 * <p>ParameterizedTypeInfo is a concrete implementation of TypeInfo that handles Java
 * generic types with type parameters. It provides comprehensive information about
 * parameterized types including their raw type, type arguments, and field mappings.</p>
 *
 * <p>The class supports various parameterized type scenarios:</p>
 * <ul>
 *   <li><strong>Simple Generics:</strong> List&lt;String&gt;, Set&lt;Integer&gt;</li>
 *   <li><strong>Multi-Parameter Generics:</strong> Map&lt;K,V&gt;, BiFunction&lt;T,U,R&gt;</li>
 *   <li><strong>Nested Generics:</strong> List&lt;Map&lt;String,Integer&gt;&gt;</li>
 *   <li><strong>Bounded Generics:</strong> List&lt;? extends Number&gt;, Set&lt;? super String&gt;</li>
 *   <li><strong>Inner Class Generics:</strong> Map.Entry&lt;K,V&gt;</li>
 * </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Type Argument Management:</strong> Stores and manages generic type parameters</li>
 *   <li><strong>Field Mapping:</strong> Maps type arguments to actual field names</li>
 *   <li><strong>Source vs Library:</strong> Distinguishes between source and library types</li>
 *   <li><strong>Type Compatibility:</strong> Handles generic type matching and covering</li>
 *   <li><strong>Symbol Resolution:</strong> Manages symbolic types and wildcards</li>
 *   <li><strong>Immutable Design:</strong> Thread-safe with unmodifiable collections</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class ParameterizedTypeInfo extends TypeInfo {

    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The raw type name or hash identifier of this parameterized type.
     *
     * <p>This field stores the type hash without the generic type parameters.
     * For source types, it contains the hash of the raw type. For library types,
     * it uses the format {@code LIB__OR__TYPE::<type-name>}.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>List&lt;String&gt;:</strong> name = "List" (raw type)</li>
     *   <li><strong>Map&lt;K,V&gt;:</strong> name = "Map" (raw type)</li>
     *   <li><strong>ArrayList&lt;Integer&gt;:</strong> name = "ArrayList" (raw type)</li>
     * </ul>
     */
    private final String name;

    /**
     * The number of type arguments in this parameterized type.
     *
     * <p>This field stores the count of generic type parameters. It's used to
     * validate type argument consistency and for type matching operations.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>List&lt;String&gt;:</strong> elementTypeSize = 1</li>
     *   <li><strong>Map&lt;K,V&gt;:</strong> elementTypeSize = 2</li>
     *   <li><strong>BiFunction&lt;T,U,R&gt;:</strong> elementTypeSize = 3</li>
     * </ul>
     */
    private final int elementTypeSize;

    /**
     * List of TypeInfo objects representing the type arguments.
     *
     * <p>This field stores the actual type information for each generic type parameter.
     * The list is immutable and thread-safe, containing TypeInfo objects that can be
     * any valid type including other parameterized types, wildcards, or symbolic types.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>List&lt;String&gt;:</strong> elementTypes = [ScalarTypeInfo("String")]</li>
     *   <li><strong>Map&lt;K,V&gt;:</strong> elementTypes = [SymbolicTypeInfo("K"), SymbolicTypeInfo("V")]</li>
     *   <li><strong>List&lt;? extends Number&gt;:</strong> elementTypes = [WildCardTypeInfo("? extends Number")]</li>
     * </ul>
     */
    private final List<TypeInfo> elementTypes;

    /**
     * Flag indicating whether this parameterized type is from source code.
     *
     * <p>This field distinguishes between parameterized types defined in source code
     * versus those from external libraries. Source types have additional capabilities
     * like field mapping and token range information.</p>
     *
     * <p>Usage implications:</p>
     * <ul>
     *   <li><strong>Source Types (true):</strong> Full field information, token ranges, type argument mapping</li>
     *   <li><strong>Library Types (false):</strong> Basic type information only</li>
     * </ul>
     */
    private final boolean isFromSource;

    /**
     * Map of field names to field information tuples.
     *
     * <p>This field stores comprehensive field information where each field name
     * maps to a tuple containing token range, container hash, and field type details.
     * The collection is immutable and thread-safe for concurrent access.</p>
     *
     * <p>Field information includes:</p>
     * <ul>
     *   <li><strong>Token Range:</strong> Source code location of the field</li>
     *   <li><strong>Container Hash:</strong> Hash of the class containing the field</li>
     *   <li><strong>Field Type:</strong> Type information for the field</li>
     * </ul>
     *
     * <p>For library types, this map is typically empty.</p>
     */
    private final Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields;

    /**
     * Mapping from type argument indices to field names.
     *
     * <p>This field maps the position of type arguments to the actual field names
     * they represent in the source code. It's only populated for source types and
     * provides a way to correlate generic type parameters with concrete field names.</p>
     *
     * <p>Mapping structure:</p>
     * <ul>
     *   <li><strong>Key:</strong> Type argument index (0-based)</li>
     *   <li><strong>Value:</strong> Field name that the type argument represents</li>
     * </ul>
     *
     * <p>Example for class {@code Container<T>}:</p>
     * <ul>
     *   <li><strong>typeArgsToFields.get(0):</strong> Returns the field name that T represents</li>
     * </ul>
     *
     * <p>This field is null for library types since they don't have source code mapping.</p>
     */
    private final Map<Integer, String> typeArgsToFields;

    /**
     * Flag indicating whether this parameterized type represents an inner class.
     *
     * <p>This field identifies parameterized types that are nested within other classes.
     * Inner class generics have special semantics and may require different handling
     * during type resolution and matching operations.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>Map.Entry&lt;K,V&gt;:</strong> isInner = true</li>
     *   <li><strong>List&lt;String&gt;:</strong> isInner = false</li>
     *   <li><strong>Outer.Inner&lt;T&gt;:</strong> isInner = true</li>
     * </ul>
     */
    private final boolean isInner;

    public ParameterizedTypeInfo(String name) {
        this(name, -1, Collections.emptyList(), false, Collections.emptyMap(), null, false);
    }

    public ParameterizedTypeInfo(String name, List<TypeInfo> elementTypes) {
        this(name, -1, elementTypes, false, Collections.emptyMap(), null, false);
    }

    public ParameterizedTypeInfo(String name, int elementTypeSize, List<TypeInfo> elementTypes) {
        this(name, elementTypeSize, elementTypes, false, Collections.emptyMap(), null, false);
    }

    public ParameterizedTypeInfo(
            String name,
            int elementTypeSize,
            List<TypeInfo> elementTypes,
            boolean isFromSource,
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields,
            Map<Integer, String> typeArgsToFields,
            boolean isInner) {
        this.name = name;
        this.elementTypeSize = elementTypeSize;
        List<TypeInfo> copyElementTypes = new ArrayList<>();
        copyElementTypes.addAll(elementTypes);
        this.elementTypes = Collections.unmodifiableList(copyElementTypes);
        this.isFromSource = isFromSource;
        // Copy the map and make it unmodifiable
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> copiedFields = new HashMap<>();
        copiedFields.putAll(fields);
        this.fields = Collections.unmodifiableMap(copiedFields);
        if (typeArgsToFields == null) {
            this.typeArgsToFields = null;
        } else {
            Map<Integer, String> copyTypeArgsToFields = new HashMap<>();
            copyTypeArgsToFields.putAll(typeArgsToFields);
            this.typeArgsToFields = Collections.unmodifiableMap(copyTypeArgsToFields);
        }
        this.isInner = isInner;
    }

    /**
     * Creates and returns a new ParameterizedTypeInfo with updated type arguments.
     *
     * <p>This method creates a new instance with modified type arguments while preserving
     * all other properties. The method is useful for type substitution scenarios where
     * specific type arguments need to be replaced with concrete types.</p>
     *
     * <p>The update process:</p>
     * <ul>
     *   <li><strong>Index-based Updates:</strong> Each update specifies an index and new TypeInfo</li>
     *   <li><strong>Element Replacement:</strong> Elements are removed and added at the specified positions</li>
     *   <li><strong>New Instance Creation:</strong> A new ParameterizedTypeInfo is created with updated elements</li>
     *   <li><strong>Memoization:</strong> The result is cached using TypeCalculator for reuse</li>
     * </ul>
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * // Original: List<T>
     * List<IntObjectPair<TypeInfo>> updates = Arrays.asList(
     *     IntObjectPair.of(0, new ScalarTypeInfo("String"))
     * );
     * // Result: List<String>
     * }</pre>
     *
     * <p><strong>Note:</strong> This method doesn't modify the current instance;
     * it creates a new one with the updated elements.</p>
     *
     * @param elemetsToUpdate the list of pairs containing index and new TypeInfo value for the elements list
     * @return a new ParameterizedTypeInfo with updated type arguments
     */
    public ParameterizedTypeInfo updateElemets(List<IntObjectPair<TypeInfo>> elemetsToUpdate) {
        List<TypeInfo> elements = new ArrayList<>();
        elements.addAll(elementTypes);
        for (IntObjectPair<TypeInfo> pair : elemetsToUpdate) {
            elements.remove(pair.fst);
            elements.add(pair.fst, pair.snd);
        }
        ParameterizedTypeInfo typeInfo = new ParameterizedTypeInfo(
                name, elementTypeSize, elements, isFromSource, fields, typeArgsToFields, isInner);
        return (ParameterizedTypeInfo) TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
    }

    /**
     * Creates and returns a new ParameterizedTypeInfo with updated name and type arguments.
     *
     * <p>This method creates a new instance with both a modified raw type name and updated
     * type arguments. It's useful for scenarios where both the base type and its generic
     * parameters need to be changed simultaneously.</p>
     *
     * <p>The update process:</p>
     * <ul>
     *   <li><strong>Name Update:</strong> The raw type name is changed to the new value</li>
     *   <li><strong>Element Updates:</strong> Type arguments are updated at specified positions</li>
     *   <li><strong>New Instance Creation:</strong> A new ParameterizedTypeInfo is created with all updates</li>
     *   <strong>Memoization:</strong> The result is cached using TypeCalculator for reuse</li>
     * </ul>
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * // Original: Container<T>
     * List<IntObjectPair<TypeInfo>> updates = Arrays.asList(
     *     IntObjectPair.of(0, new ScalarTypeInfo("Integer"))
     * );
     * // Result: NewContainer<Integer>
     * }</pre>
     *
     * <p><strong>Note:</strong> This method doesn't modify the current instance;
     * it creates a new one with the updated name and elements.</p>
     *
     * @param updatedName the new name for the raw type
     * @param elemetsToUpdate the list of pairs containing index and new TypeInfo value for the elements list
     * @return a new ParameterizedTypeInfo with updated name and type arguments
     */
    public ParameterizedTypeInfo updateNameAndElemets(
            String updatedName, List<IntObjectPair<TypeInfo>> elemetsToUpdate) {
        List<TypeInfo> elements = new ArrayList<>();
        elements.addAll(elementTypes);
        for (IntObjectPair<TypeInfo> pair : elemetsToUpdate) {
            elements.remove(pair.fst);
            elements.add(pair.fst, pair.snd);
        }
        ParameterizedTypeInfo typeInfo = new ParameterizedTypeInfo(
                updatedName, elementTypeSize, elements, isFromSource, fields, typeArgsToFields, isInner);
        typeInfo = (ParameterizedTypeInfo) TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        return typeInfo;
    }

    public boolean isInner() {
        return isInner;
    }

    public Map<Integer, String> getTypeArgsToFields() {
        return typeArgsToFields;
    }

    public String getTypeArgsToFieldsValue(Integer index) {
        return typeArgsToFields.get(index);
    }

    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return fields;
    }

    public Pair<Pair<TokenRange, Integer>, TypeInfo> getFieldsTypeInfo(String fieldName) {
        return fields.get(fieldName);
    }

    public List<TypeInfo> getElementTypes() {
        return elementTypes;
    }

    public int getElementTypeSize() {
        return elementTypeSize;
    }

    public boolean isFromSource() {
        return isFromSource;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Returns the erasure of this parameterized type.
     *
     * <p>For parameterized types, the erasure is the raw type name without any generic
     * type parameters. This method returns the same value as {@link #getName()}, which
     * represents the base class or interface that has been parameterized.</p>
     *
     * <p>Type erasure is a key concept in Java generics where generic type information
     * is removed at runtime. This method provides access to the erased type that the
     * JVM actually uses.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>List&lt;String&gt;:</strong> Returns "List" (erased type)</li>
     *   <li><strong>Map&lt;K,V&gt;:</strong> Returns "Map" (erased type)</li>
     *   <li><strong>ArrayList&lt;Integer&gt;:</strong> Returns "ArrayList" (erased type)</li>
     *   <li><strong>Optional&lt;T&gt;:</strong> Returns "Optional" (erased type)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Generic Information Lost:</strong> Type arguments are not included in erasure</li>
     *   <li><strong>Runtime Behavior:</strong> Reflects how the JVM sees the type</li>
     *   <strong>Same as getName:</strong> Returns identical value to getName() method</li>
     * </ul>
     *
     * @return the type erasure (raw type name without generic parameters)
     */
    @Override
    public String getTypeErasure() {
        return this.name;
    }

    /**
     * Checks whether this parameterized type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this parameterized type can be used where the declaration type is expected.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Parameterized-to-Parameterized Matching:</strong> Same raw type and compatible structure</li>
     *   <li><strong>Class Type Compatibility:</strong> Matches against class types and interfaces</li>
     *   <li><strong>Generic Type Compatibility:</strong> Handles wildcards and type variables</li>
     * </ul>
     *
     * <p><strong>Parameterized Type Matching Rules:</strong></p>
     * <ul>
     *   <li><strong>Raw Type Match:</strong> Raw types must be compatible (same or subtype)</li>
     *   <li><strong>Erasure Comparison:</strong> Only raw types are compared, not type arguments</li>
     *   <li><strong>No Element Type Checking:</strong> Type arguments are not considered for matching</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>List&lt;String&gt; → List&lt;String&gt;:</strong> Match (same raw type)</li>
     *   <li><strong>List&lt;String&gt; → List&lt;Object&gt;:</strong> Match (List is compatible)</li>
     *   <li><strong>ArrayList&lt;Integer&gt; → List&lt;Number&gt;:</strong> Match (ArrayList extends List)</li>
     *   <li><strong>List&lt;String&gt; → Collection&lt;Object&gt;:</strong> Match (List implements Collection)</li>
     *   <li><strong>List&lt;String&gt; → T:</strong> Match if T extends List or Object</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Type Erasure Focus:</strong> Only raw types are compared, not generic parameters</li>
     *   <li><strong>No Covariance:</strong> List&lt;String&gt; is not a subtype of List&lt;Object&gt;</li>
     *   <li><strong>Method Overriding:</strong> Java doesn't allow overriding with different generic parameters</li>
     * </ul>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this parameterized type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof ParameterizedTypeInfo
                && (matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this))) {
            // A parameterized type can be used in place of another parameterized type
            // if both of them are of the same type or it is a sub type of the
            // required parameterized type.
            // Here we are only comparing the erasure of the types
            // Because java doesn't allow method overriding or over loading
            // with Same parameterized with different element.
            // The following cases are matched here,
            // Foo<Buzz> ---> Foo<Buzz>
            // Foo<Buzz> ---> Bar<Buzz> where Bar is a sub type of Foo
            // N.B. Foo<Buzz> is not a sub type of Foo<Supp> even if Buzz is a sub type of Supp
            return true;
        } else if (declarationType instanceof ClassTypeInfo
                || declarationType instanceof WildCardTypeInfo
                || declarationType instanceof SymbolicTypeInfo) {
            // An parameterized type can be used in place of Type variable
            // and wild card in the following scenarios
            // Bar ---> Foo<T> where Foo<T> extends Bar
            // Bar ---> T where <T>, <T extends Bar>, <T extends Foo> where Bar is a sub type of Foo
            // Bar ---> ? where <?>, <? extends Bar>, <? extends Foo> where Bar is a sub type of Foo
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
     * Checks whether this parameterized type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this parameterized type can accept the invocation type.</p>
     *
     * <p>The method implements comprehensive covering rules with special cases:</p>
     * <ul>
     *   <li><strong>Object Coverage:</strong> java.lang.Object covers all types</li>
     *   <li><strong>Enum Coverage:</strong> java.lang.Enum covers all enum types</li>
     *   <li><strong>Type Compatibility:</strong> Raw type compatibility checking</li>
     *   <li><strong>Generic Support:</strong> Handles various generic type scenarios</li>
     * </ul>
     *
     * <p><strong>Covering Rules:</strong></p>
     * <ul>
     *   <li><strong>Object Superclass:</strong> Object covers everything</li>
     *   <li><strong>Enum Superclass:</strong> Enum covers all enum types</li>
     *   <li><strong>Raw Type Match:</strong> Raw types must be compatible</li>
     *   <li><strong>Subtype Support:</strong> Covers subtypes of the raw type</li>
     *   <li><strong>No Element Type Checking:</strong> Type arguments are not considered</li>
     * </ul>
     *
     * <p><strong>Covering Examples:</strong></p>
     * <ul>
     *   <li><strong>Object → List&lt;String&gt;:</strong> Cover (Object covers all types)</li>
     *   <li><strong>Object → Map&lt;K,V&gt;:</strong> Cover (Object covers all types)</li>
     *   <li><strong>Enum → Color:</strong> Cover (Enum covers all enum types)</li>
     *   <li><strong>List&lt;T&gt; → ArrayList&lt;String&gt;:</strong> Cover (ArrayList extends List)</li>
     *   <li><strong>Collection&lt;T&gt; → List&lt;String&gt;:</strong> Cover (List implements Collection)</li>
     *   <li><strong>List&lt;T&gt; → T:</strong> Cover if T extends List</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Type Erasure Focus:</strong> Only raw types are compared, not generic parameters</li>
     *   <li><strong>No Covariance:</strong> List&lt;String&gt; is not covered by List&lt;Object&gt;</li>
     *   <li><strong>Method Overriding:</strong> Java doesn't allow overriding with different generic parameters</li>
     * </ul>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this parameterized type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        if (this.getName().equals(Constants.JAVA_LANG_OBJECT)) {
            // java.lang.Object is the super class for every other type
            // So, it covers everything
            return true;
        } else if (this.getName().equals(Constants.JAVA_LANG_ENUM) && invocationType instanceof EnumTypeInfo) {
            // java.lang.Enum is the super type for all enums
            // So, it covers all enums
            return true;
        }
        if (invocationType instanceof ClassTypeInfo
                || invocationType instanceof SymbolicTypeInfo
                || invocationType instanceof WildCardTypeInfo
                || invocationType instanceof ParameterizedTypeInfo) {
            // A parameterized type covers another parameterized type of same type
            // A parameterized type also covers classType
            // Foo<T> ---> Foo<T>
            // Foo<T> ---> Bar if Bar is a sub type of Foo
            // Foo<T> ---> T if <T extends Foo> or <T extends Bar> where Bar is sub type of Foo
            // Foo<T> ---> ? if <? extends Foo> or <? extends Bar> where Bar is sub type of Foo
            // Foo<T> ---> Bar where Bar extends Foo<T>
            // or its sub types.
            // We are not checking element types here as java doesn't allow method
            // overriding or over loading with Same parameterized with different element.
            if (matchTypeErasure(this, invocationType) || isErasureSubTypeOf(this, invocationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this parameterized type contains symbolic or wildcard types that need replacement.
     *
     * <p>This method examines all type arguments to determine if any of them contain
     * symbolic types, wildcards, or other elements that require replacement during
     * type resolution. It returns true if any type argument needs replacement.</p>
     *
     * <p>The method implements a recursive checking strategy:</p>
     * <ul>
     *   <li><strong>Element Type Iteration:</strong> Checks each type argument individually</li>
     *   <li><strong>Recursive Checking:</strong> Delegates to each element type's needsReplacement method</li>
     *   <li><strong>Early Termination:</strong> Returns true as soon as any element needs replacement</li>
     *   <li><strong>Complete Coverage:</strong> Only returns false if no elements need replacement</li>
     * </ul>
     *
     * <p>Examples of parameterized types that need replacement:</p>
     * <ul>
     *   <li><strong>List&lt;T&gt;:</strong> T is a type variable that needs replacement</li>
     *   <li><strong>Map&lt;K,V&gt;:</strong> K and V are type variables that need replacement</li>
     *   <li><strong>Set&lt;? extends Number&gt;:</strong> Wildcard needs replacement</li>
     *   <li><strong>Container&lt;List&lt;T&gt;&gt;:</strong> Nested T needs replacement</li>
     * </ul>
     *
     * <p>Examples of parameterized types that don't need replacement:</p>
     * <ul>
     *   <li><strong>List&lt;String&gt;:</strong> String is a concrete type</li>
     *   <li><strong>Set&lt;Integer&gt;:</strong> Integer is a concrete type</li>
     *   <li><strong>Map&lt;String,Object&gt;:</strong> Both are concrete types</li>
     * </ul>
     *
     * @return true if any type argument needs replacement, false otherwise
     */
    @Override
    public boolean needsReplacement() {
        for (TypeInfo elementType : elementTypes) {
            if (elementType.needsReplacement()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses this parameterized type to find and map symbolic and wildcard types.
     *
     * <p>This method implements symbolic type resolution for parameterized types by
     * recursively processing type arguments. It only continues processing if the target
     * type is also a parameterized type with the same number of type arguments.</p>
     *
     * <p>The method implements a sophisticated matching and traversal strategy:</p>
     * <ul>
     *   <li><strong>Type Compatibility Check:</strong> Verifies both types are parameterized with same arity</li>
     *   <li><strong>Canonical Type Matching:</strong> Uses canonicalized type names for comparison</li>
     *   <li><strong>Element-by-Element Processing:</strong> Processes each type argument individually</li>
     *   <li><strong>Container Path Tracking:</strong> Maintains traversal context for each element</li>
     *   <li><strong>Recursive Symbol Resolution:</strong> Delegates to element types for processing</li>
     * </ul>
     *
     * <p><strong>Processing Flow:</strong></p>
     * <ol>
     *   <li>Check if target type is ParameterizedTypeInfo with same element count</li>
     *   <li>Canonicalize both type names for comparison</li>
     *   <li>Verify at least one canonical type name matches</li>
     *   <li>For each matching element position, process type arguments recursively</li>
     *   <li>Update container paths with element position information</li>
     *   <li>Delegate symbol resolution to individual element types</li>
     * </ol>
     *
     * <p><strong>Canonical Type Matching:</strong></p>
     * <ul>
     *   <li><strong>Package Removal:</strong> Strips package information for comparison</li>
     *   <li><strong>Library Type Handling:</strong> Special handling for LIB__OR__TYPE prefixes</li>
     *   <li><strong>Hierarchy Inclusion:</strong> Includes superclasses and implemented interfaces</li>
     * </ul>
     *
     * <p>This method is essential for resolving complex generic type scenarios like
     * {@code List<T>} vs {@code List<String>} or {@code Map<K,V>} vs {@code Map<String,Integer>}.</p>
     *
     * @param targetTypeInfo the target type to match against
     * @param pastContainers list of parameterized types encountered during traversal
     * @param seenTraversalPaths set of already processed traversal paths
     * @param capturedSymbolicTypes map of symbolic types to their captured values
     * @param capturedWildCardTypes map of wildcard types to their captured values
     */
    @Override
    public void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        if (targetTypeInfo instanceof ParameterizedTypeInfo
                && (((ParameterizedTypeInfo) targetTypeInfo).getElementTypeSize() == this.getElementTypeSize())) {
            Set<String> thisParamTypeNames = canonicalizeType(this.getName());
            Set<String> targetParamTypeNames = canonicalizeType(targetTypeInfo.getName());
            boolean hasOneMatch = false;
            for (String temp : thisParamTypeNames) {
                for (String temp2 : targetParamTypeNames) {
                    if (temp.equals(temp2)) {
                        hasOneMatch = true;
                        break;
                    }
                }
                if (hasOneMatch) {
                    break;
                }
            }
            if (hasOneMatch) {
                for (int elementTypePosition = 0; elementTypePosition < elementTypes.size(); elementTypePosition++) {
                    List<ObjectIntPair<TypeInfo>> ongoingContainers =
                            (pastContainers == null) ? new ArrayList<>(3) : new ArrayList<>(pastContainers.size() + 3);
                    if (pastContainers != null) {
                        for (ObjectIntPair<TypeInfo> pastEntry : pastContainers) {
                            ongoingContainers.add(
                                    ObjectIntPair.of(pastEntry.fst, pastEntry.snd * 10 + elementTypePosition));
                        }
                    }
                    TypeInfo elementType = elementTypes.get(elementTypePosition);
                    TypeInfo targetElementType = ((ParameterizedTypeInfo) targetTypeInfo)
                            .getElementTypes()
                            .get(elementTypePosition);
                    elementType.parseAndMapSymbols(
                            targetElementType,
                            ongoingContainers,
                            seenTraversalPaths,
                            capturedSymbolicTypes,
                            capturedWildCardTypes);
                }
            }
        }
    }

    /**
     * Replaces symbolic types in this parameterized type with their captured concrete types.
     *
     * <p>This method implements symbolic type replacement for parameterized types by
     * recursively processing each type argument. If any type arguments are modified,
     * a new parameterized type is created with the updated elements.</p>
     *
     * <p>The method implements a comprehensive replacement strategy:</p>
     * <ul>
     *   <li><strong>Element-by-Element Processing:</strong> Processes each type argument individually</li>
     *   <li><strong>Container Path Tracking:</strong> Maintains traversal context for each element</li>
     *   <li><strong>Recursive Replacement:</strong> Delegates to element types for replacement</li>
     *   <strong>New Instance Creation:</strong> Creates new type if any elements were modified</li>
     * </ul>
     *
     * <p><strong>Replacement Process:</strong></p>
     * <ol>
     *   <li>Iterate through all type arguments</li>
     *   <li>Create ongoing container paths for each element position</li>
     *   <li>Attempt to replace symbolic types in each element</li>
     *   <li>Collect all successful replacements</li>
     *   <li>Create new parameterized type if any replacements occurred</li>
     * </ol>
     *
     * <p><strong>Container Path Management:</strong></p>
     * <ul>
     *   <li><strong>Position Encoding:</strong> Each element position is encoded in the path</li>
     *   <li><strong>Hierarchy Tracking:</strong> Maintains the full traversal hierarchy</li>
     *   <li><strong>Duplicate Prevention:</strong> Avoids infinite recursion through path tracking</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>List&lt;T&gt; → List&lt;String&gt;:</strong> If T is captured as String</li>
     *   <li><strong>Map&lt;K,V&gt; → Map&lt;String,Integer&gt;:</strong> If K→String, V→Integer</li>
     *   <li><strong>Container&lt;List&lt;T&gt;&gt; → Container&lt;List&lt;Number&gt;&gt;:</strong> If T→Number</li>
     * </ul>
     *
     * <p><strong>Return Value:</strong></p>
     * <ul>
     *   <li><strong>First Element (Boolean):</strong> true if replacement occurred, false otherwise</li>
     *   <li><strong>Second Element (TypeInfo):</strong> New parameterized type or this unchanged</li>
     * </ul>
     *
     * @param pastContainers list of parameterized types encountered during traversal
     * @param alreadyReplacedPaths set of already processed replacement paths
     * @param capturedSymbolicTypes map of symbolic types to their captured values
     * @param capturedWildCardTypes map of wildcard types to their captured values
     * @return a pair indicating whether replacement occurred and the resulting type
     */
    @Override
    public Pair<Boolean, TypeInfo> replaceSymbol(
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> alreadyReplacedPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        List<IntObjectPair<TypeInfo>> updatedElements = new ArrayList<>();
        for (int elementTypePosition = 0; elementTypePosition < elementTypes.size(); elementTypePosition++) {
            TypeInfo elementType = elementTypes.get(elementTypePosition);
            List<ObjectIntPair<TypeInfo>> ongoingContainers =
                    ((pastContainers == null) ? new ArrayList<>(3) : new ArrayList<>(pastContainers.size() + 3));
            if (pastContainers != null) {
                for (ObjectIntPair<TypeInfo> pastEntry : pastContainers) {
                    ongoingContainers.add(ObjectIntPair.of(pastEntry.fst, pastEntry.snd * 10 + elementTypePosition));
                }
            }
            ongoingContainers.add(ObjectIntPair.of(this, elementTypePosition));
            Pair<Boolean, TypeInfo> result = elementType.replaceSymbol(
                    ongoingContainers, alreadyReplacedPaths, capturedSymbolicTypes, capturedWildCardTypes);
            if (result.fst && result.snd != null) {
                updatedElements.add(IntObjectPair.of(elementTypePosition, result.snd));
            }
        }
        if (!updatedElements.isEmpty()) {
            return Pair.of(true, this.updateElemets(updatedElements));
        } else {
            return Pair.of(false, this);
        }
    }

    /**
     * Bounds wildcard or symbolic types in this parameterized type to their concrete types.
     *
     * <p>This method attempts to resolve wildcard and symbolic types in all type arguments
     * by binding them to concrete types. If any type arguments are modified, a new
     * parameterized type is created with the bounded elements.</p>
     *
     * <p>The method implements a comprehensive binding strategy:</p>
     * <ul>
     *   <li><strong>Element-by-Element Processing:</strong> Processes each type argument individually</li>
     *   <li><strong>Change Detection:</strong> Compares original and potentially modified element types</li>
     *   <li><strong>New Instance Creation:</strong> Creates new type if any elements were modified</li>
     *   <li><strong>Efficient Updates:</strong> Only creates new instances when necessary</li>
     * </ul>
     *
     * <p><strong>Binding Process:</strong></p>
     * <ol>
     *   <li>Iterate through all type arguments</li>
     *   <li>Call boundWildCardOrSymbolicType on each element type</li>
     *   <li>Check if the element type was modified</li>
     *   <li>Collect all modified elements</li>
     *   <li>Create new parameterized type if any changes occurred</li>
     * </ol>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>List&lt;?&gt; → List&lt;Object&gt;:</strong> If ? is bounded to Object</li>
     *   <li><strong>Map&lt;T,? extends Number&gt; → Map&lt;String,Integer&gt;:</strong> If T→String, ?→Integer</li>
     *   <li><strong>Set&lt;? super String&gt; → Set&lt;Object&gt;:</strong> If ? is bounded to Object</li>
     *   <li><strong>Container&lt;List&lt;T&gt;&gt; → Container&lt;List&lt;Number&gt;&gt;:</strong> If T→Number</li>
     * </ul>
     *
     * <p><strong>Return Value:</strong></p>
     * <ul>
     *   <li><strong>This Type:</strong> If no binding occurred (all elements unchanged)</li>
     *   <li><strong>New Type:</strong> If any elements were bounded (new parameterized type)</li>
     * </ul>
     *
     * @return this parameterized type if no binding occurred, or a new one with bounded elements
     */
    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        List<IntObjectPair<TypeInfo>> updatedElements = new ArrayList<>();
        for (int elementTypePosition = 0; elementTypePosition < elementTypes.size(); elementTypePosition++) {
            TypeInfo elementType = elementTypes.get(elementTypePosition);
            TypeInfo possiblyModifiedElementType = elementType.boundWildCardOrSymbolicType();
            if (possiblyModifiedElementType == elementType) {
                // Do nothing since no change was made underneath
            } else {
                // Register the changes
                updatedElements.add(IntObjectPair.of(elementTypePosition, possiblyModifiedElementType));
            }
        }
        if (!updatedElements.isEmpty()) {
            return this.updateElemets(updatedElements);
        } else {
            return this;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + elementTypeSize;
        result = prime * result + ((elementTypes == null) ? 0 : elementTypes.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + (isFromSource ? 1231 : 1237);
        result = prime * result + (isInner ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((typeArgsToFields == null) ? 0 : typeArgsToFields.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ParameterizedTypeInfo other = (ParameterizedTypeInfo) obj;
        if (elementTypeSize != other.elementTypeSize) return false;
        if (elementTypes == null) {
            if (other.elementTypes != null) return false;
        } else if (!elementTypes.equals(other.elementTypes)) return false;
        if (fields == null) {
            if (other.fields != null) return false;
        } else if (!fields.equals(other.fields)) return false;
        if (isFromSource != other.isFromSource) return false;
        if (isInner != other.isInner) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (typeArgsToFields == null) {
            if (other.typeArgsToFields != null) return false;
        } else if (!typeArgsToFields.equals(other.typeArgsToFields)) return false;
        return true;
    }

    @Override
    public String toString() {
        String typeName = CallGraphUtility.getClassNameFromClassHash(name);
        // A heuristic to get only the class name
        // Previously we only took the name after last dot. But that
        // gave false results for some cases e.g. java.util.Map.Entry
        // we were getting 'Entry' instead of 'Map.Entry'.
        int dotIndex = typeName.indexOf(".");
        while (!Character.isUpperCase(typeName.charAt(0)) && dotIndex >= 0) {
            // Take the substring when the character after dot is an uppercase
            // letter. Usually package names are lowercase in Java.
            if (dotIndex < typeName.length() - 1 && Character.isUpperCase(typeName.charAt(dotIndex + 1))) {
                typeName = typeName.substring(dotIndex + 1);
                break;
            }
            dotIndex = typeName.indexOf(".", dotIndex + 1);
        }
        // typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
        StringBuilder builder = new StringBuilder();
        builder.append(typeName);
        if (elementTypeSize > 0) {
            builder.append('<');
            Iterator<TypeInfo> elementIterator = elementTypes.iterator();
            builder.append(elementIterator.next());
            while (elementIterator.hasNext()) {
                builder.append(", ").append(elementIterator.next());
            }
            builder.append('>');
        }
        return builder.toString();
    }

    @Override
    public TypeInfo clone() {
        return new ParameterizedTypeInfo(
                this.name,
                this.elementTypeSize,
                this.elementTypes,
                this.isFromSource,
                this.fields,
                this.typeArgsToFields,
                this.isInner);
    }

    /**
     * Canonicalizes a type name by removing package information and library prefixes.
     *
     * <p>This private helper method processes type names to create a canonical form
     * suitable for type matching. It handles both source types and library types,
     * removing package information and special prefixes to enable type comparison.</p>
     *
     * <p>The canonicalization process includes:</p>
     * <ul>
     *   <li><strong>Library Type Processing:</strong> Removes LIB__OR__TYPE:: prefixes</li>
     *   <li><strong>Package Removal:</strong> Strips package information for comparison</li>
     *   <li><strong>Hierarchy Inclusion:</strong> Includes superclasses and implemented interfaces</li>
     *   <li><strong>Set Creation:</strong> Returns a set of all canonical type names</li>
     * </ul>
     *
     * <p><strong>Processing Steps:</strong></p>
     * <ol>
     *   <li>Get all superclasses and implemented interfaces for the type</li>
     *   <li>Add the original type name to the set</li>
     *   <li>Remove LIB__OR__TYPE:: prefixes from library types</li>
     *   <li>Strip package information from all type names</li>
     *   <li>Return the set of canonical type names</li>
     * </ol>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>com.example.MyClass:</strong> → "MyClass"</li>
     *   <li><strong>java.util.List:</strong> → "List"</li>
     *   <li><strong>LIB__OR__TYPE::String:</strong> → "String"</li>
     *   <li><strong>com.company.Outer.Inner:</strong> → "Inner"</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Package Stripping:</strong> May cause different packages with same class name to match</li>
     *   <li><strong>Library Handling:</strong> Special handling for external library types</li>
     *   <li><strong>Hierarchy Inclusion:</strong> Provides comprehensive type matching capabilities</li>
     * </ul>
     *
     * @param typeName the type name to canonicalize
     * @return a set of canonical type names for matching purposes
     */
    private Set<String> canonicalizeType(String typeName) {
        Set<String> allTypes = CallGraphDataStructures.getAllSuperClassesAndImplementedInterfacesOfClass(typeName);
        if (allTypes == null) {
            allTypes = new HashSet<>(1);
        }
        allTypes.add(typeName);

        Set<String> results = new HashSet<>(allTypes.size());
        for (String type : allTypes) {
            String result = type;
            if (type.startsWith(Constants.LIB_TYPE)) {
                // We will purge LIB__OR__TYPE and ::,
                // so purging all the way till the length
                result = type.substring(Constants.LIB_TYPE.length() + 2);
            }
            // Now remove the package info if any
            // This may cause a.b.Foo to be matched with c.d.Foo
            // But it is unlikely to happen in this matching context
            int dotIndex = result.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < result.length()) {
                result = result.substring(dotIndex + 1);
            }
            results.add(result);
        }
        return results;
    }
}
