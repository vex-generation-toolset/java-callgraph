/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Abstract base class for representing Java type information in the call graph system.
 *
 * <p>TypeInfo is the foundation of the type system that represents various Java types
 * including primitive types, classes, interfaces, arrays, generics, and wildcards. It
 * provides a unified interface for type operations such as matching, covering, and
 * symbol resolution that are essential for accurate method resolution and call graph
 * construction.</p>
 *
 * <p>The class hierarchy includes:</p>
 * <ul>
 *   <li><strong>ScalarTypeInfo:</strong> Represents primitive types and their wrappers</li>
 *   <li><strong>ClassTypeInfo:</strong> Represents class and interface types</li>
 *   <li><strong>EnumTypeInfo:</strong> Represents enum types</li>
 *   <li><strong>ArrayTypeInfo:</strong> Represents array types with element type information</li>
 *   <li><strong>ParameterizedTypeInfo:</strong> Represents generic types with type arguments</li>
 *   <li><strong>SymbolicTypeInfo:</strong> Represents type variables in generic declarations</li>
 *   <li><strong>WildCardTypeInfo:</strong> Represents wildcard types with bounds</li>
 * </ul>
 *
 * <p>Key capabilities include:</p>
 * <ul>
 *   <li><strong>Type Matching:</strong> Bidirectional type compatibility checking</li>
 *   <li><strong>Symbol Resolution:</strong> Capturing and replacing symbolic types</li>
 *   <li><strong>Type Erasure:</strong> Providing raw type information for generic types</li>
 *   <li><strong>Subtype Checking:</strong> Determining inheritance relationships</li>
 *   <li><strong>Field Access:</strong> Retrieving field information for composite types</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public abstract class TypeInfo implements Serializable {
    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns the erasure of this type.
     *
     * @return
     *     <li>{@link ScalarTypeInfo}: the name of the type.
     *     <li>{@link ClassTypeInfo} : the name of the type.
     *     <li>{@link EnumTypeInfo}: the name of the enum.
     *     <li>{@link ArrayTypeInfo} : the erasure of the element type. Doesn't contain [] symbol.
     *     <li>{@link ParameterizedTypeInfo}: the hash of the type without the <> part.
     *     <li>{@link SymbolicTypeInfo}: the erasure of the upper bound if there is any,
     *         {@link Constants#JAVA_LANG_OBJECT} otherwise
     *     <li>{@link WildCardTypeInfo}: the erasure of the upper bound if there is any,
     *         {@link Constants#JAVA_LANG_OBJECT} otherwise
     */
    public abstract String getTypeErasure();

    /**
     * Creates a deep copy of this TypeInfo object.
     *
     * <p>This method creates a new instance that is independent of the original,
     * with all internal state properly copied. This is essential for type
     * manipulation operations that need to preserve the original while
     * creating modified versions.</p>
     *
     * @return a deep copy of this TypeInfo object
     */
    public abstract TypeInfo clone();

    /**
     * This method checks whether the type used in a method invocation matches the type used in declaration.
     * This method is called from the point of view of the type in method invocation and we are looking to match the
     * type in method declaration.
     *
     * <p>Should be used when matching method invocation with declaration
     *
     * <p>For example:
     *
     * <pre>
     * {@code}
     * void method1(Foo o) {
     *    // Do something
     * }
     *
     * void method2 (Bar b) {
     *    method1(b);
     * }
     * </pre>
     *
     * Here, Type info of invocation ({@code b}) matches type info of declaration ({@code o})
     *
     * <p>This method does the opposite work of covers method. We still have covers method to allow double dispatching
     * when handling symbolic and wild card type info cases.
     *
     * @param declarationType the type info of the declaration
     * @return true if the type matches the required type
     */
    public abstract boolean matches(TypeInfo declarationType);

    /**
     * This method checks whether the type used in a method declaration matches the type used in invocation.
     * This method is called from the point of view of the type in method declaration and we are looking to match the
     * type in method invocation.
     *
     * <p>Should be used when matching method declaration with invocation
     *
     * <p>For example:
     *
     * <pre>
     * {@code}
     * void method1(Foo o) {
     *    // Do something
     * }
     *
     * void method2 (Bar b) {
     *    method1(b);
     * }
     * </pre>
     *
     * Here, Type info of declaration ({@code o}) matches type info of invocation ({@code b})
     *
     * <p>This method does the opposite work of covers method. We still have covers method to allow double dispatching
     * when handling symbolic and wild card type info cases.
     *
     * @param invocationType the type info of the invocation
     * @return true if the type covers the given type
     */
    public abstract boolean covers(TypeInfo invocationType);

    /**
     * Checks whether the type contains symbolic or wildcard types that need to be captured and replaced.
     *
     * <p>This method determines if the type contains any type variables, wildcards, or other
     * symbolic elements that require resolution during type matching operations. Types that
     * need replacement cannot be used directly for method resolution and must first undergo
     * symbol capture and replacement.</p>
     *
     * <p>Examples of types that need replacement:</p>
     * <ul>
     *   <li><strong>Symbolic Types:</strong> Type variables like {@code T}, {@code E}, {@code K}</li>
     *   <li><strong>Wildcard Types:</strong> Unbounded or bounded wildcards like {@code ?}, {@code ? extends T}</li>
     *   <li><strong>Nested Generics:</strong> Complex types like {@code Map<?, List<T>>}</li>
     * </ul>
     *
     * @return true if the type contains symbolic or wildcard types at any depth, false otherwise
     */
    public abstract boolean needsReplacement();

    /**
     * Traverses a type info and finds the mappings between symbolic and wild card types with the actual
     * types.
     *
     * @param targetTypeInfo the target type info against which the matching will be done
     * @param pastContainers list of parameterized types that are the containers and their element that we are are
     *     traversing. For example, Map<?, Map<Map<?, ?>, Map<?, ?>>> Here for the fourth ? type, we will have
     *     pastContainers to be Map<?, Map<Map<?, ?>, Map<?, ?>>>, "110" Map<Map<?, ?>, Map<?, ?>>, "10" Map<?, ?>, "0"
     *     We store the same entry because when we match to get the return type later, the match request may come from
     *     any depth.
     * @param seenTraversalPaths the traversal paths that we have seen before so that we do not process them again.
     * @param capturedSymbolicTypes the map storing the symbolic types to the types that they capture
     * @param capturedWildCardTypes the mapping between a wild card type and the type matched. In the map, the key is
     *     the container inside which we will have the wild cards. For example, Map<Map<?, ?>, Set<Set<?>>> being
     *     matched with Map<Map<A, B>, Set<C>> We will have the following entries, Map<Map<?, ?>, Set<?>>, "00" ---> A
     *     Map<?, ?>, "0" ---> A Map<Map<?, ?>, Set<?>>, "01" ---> B Map<?, ?>, "1" ---> B Map<Map<?, ?>, Set<?>>, "10"
     *     ---> C Set<?>, "0" ---> C
     */
    public abstract void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes);

    /**
     * Replace one entry with the calculated mapping. We cannot do multiple replacement since the objects are
     * final and one replacement will give us something else
     *
     * @param pastContainers list of parameterized types that are the containers and their element that we are are
     *     traversing. For example, Map<?, Map<Map<?, ?>, Map<?, ?>>> Here for the fourth ? type, we will have
     *     pastContainers to be Map<?, Map<Map<?, ?>, Map<?, ?>>>, "110" Map<Map<?, ?>, Map<?, ?>>, "10" Map<?, ?>, "0"
     *     We store the same entry because when we match to get the return type later, the match request may come from
     *     any depth.
     * @param alreadyReplacedPaths the traversal paths that we have seen before so that we do not process them again.
     * @param capturedSymbolicTypes the map storing the symbolic types to the types that they capture
     * @param capturedWildCardTypes the mapping between a wild card type and the type matched. In the map, the key is
     *     the container inside which we will have the wild cards. For example, Map<Map<?, ?>, Set<Set<?>>> being
     *     matched with Map<Map<A, B>, Set<C>> We will have the following entries, Map<Map<?, ?>, Set<?>>, "00" ---> A
     *     Map<?, ?>, "0" ---> A Map<Map<?, ?>, Set<?>>, "01" ---> B Map<?, ?>, "1" ---> B Map<Map<?, ?>, Set<?>>, "10"
     *     ---> C Set<?>, "0" ---> C
     */
    public abstract Pair<Boolean, TypeInfo> replaceSymbol(
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> alreadyReplacedPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes);

    /**
     * Bound the wild card/symbolic type inside a type if possible.
     *
     * @return the type info with wild card/symbolic type bounded if possible
     */
    public abstract TypeInfo boundWildCardOrSymbolicType();

    /**
     * Gets the name of this type.
     *
     * <p>This method returns a human-readable representation of the type name.
     * The exact format depends on the concrete type implementation and may
     * include generic parameters, array dimensions, or other type-specific
     * information.</p>
     *
     * @return the name of this type as a string
     */
    public abstract String getName();

    /**
     * Gets the fields contained in this type information.
     *
     * <p>This method returns field information for composite types that can contain
     * fields. The returned map contains field names mapped to tuples of token range,
     * field index, and field type information.</p>
     *
     * <p>Field information is only available for:</p>
     * <ul>
     *   <li><strong>ClassTypeInfo:</strong> Regular classes and interfaces</li>
     *   <li><strong>ParameterizedTypeInfo:</strong> Generic types with type arguments</li>
     * </ul>
     *
     * <p>For other type categories (scalar types, arrays, etc.), this method returns null.</p>
     *
     * @return a map of field names to field information tuples, or null if this type
     *         cannot contain fields
     */
    public abstract Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields();

    /**
     * Checks if the specified type is an inner type.
     *
     * <p>This static utility method determines whether a given TypeInfo represents
     * an inner class, inner interface, or other nested type. It handles different
     * type categories appropriately:</p>
     *
     * <ul>
     *   <li><strong>ClassTypeInfo:</strong> Directly checks the inner status</li>
     *   <li><strong>ParameterizedTypeInfo:</strong> Checks the underlying class type</li>
     *   <li><strong>ArrayTypeInfo:</strong> Recursively checks the element type</li>
     *   <li><strong>Other Types:</strong> Returns false (scalars, enums, etc.)</li>
     * </ul>
     *
     * <p>This method is useful for determining the scope and accessibility of types
     * during call graph construction and method resolution.</p>
     *
     * @param typeInfo the TypeInfo object to check for inner type status
     * @return true if the type is an inner type, false otherwise
     */
    public static boolean isTypeInner(TypeInfo typeInfo) {
        if (typeInfo instanceof ClassTypeInfo) {
            return ((ClassTypeInfo) typeInfo).isInner();
        } else if (typeInfo instanceof ParameterizedTypeInfo) {
            return ((ParameterizedTypeInfo) typeInfo).isInner();
        } else if (typeInfo instanceof ArrayTypeInfo) {
            return TypeInfo.isTypeInner(((ArrayTypeInfo) typeInfo).getElementType());
        } else {
            return false;
        }
    }

    /**
     * Checks whether one type is a subtype of another type based on their erasures.
     *
     * <p>This protected static method is a helper for the {@link #covers(TypeInfo)} and
     * {@link #matches(TypeInfo)} methods. It performs subtype checking using type erasures,
     * which removes generic information and focuses on the raw type relationships.</p>
     *
     * <p>The method implements several subtype checking strategies:</p>
     * <ul>
     *   <li><strong>Object Class:</strong> All types are considered subtypes of {@code java.lang.Object}</li>
     *   <li><strong>Inheritance Hierarchy:</strong> Checks if the subtype inherits from the supertype</li>
     *   <li><strong>Interface Implementation:</strong> Checks if the subtype implements the supertype interface</li>
     *   <li><strong>Library Type Handling:</strong> Special handling for library types without package names</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method should not be used directly by external code.
     * It is designed as an internal helper for the public type matching methods.</p>
     *
     * @param superType the candidate supertype to check against
     * @param subType the candidate subtype to check
     * @return true if the second type is a subtype of the first type, false otherwise
     */
    protected static boolean isErasureSubTypeOf(TypeInfo superType, TypeInfo subType) {
        String superTypeErasure = superType.getTypeErasure();
        String subTypeErasure = subType.getTypeErasure();
        if (superTypeErasure.equals(Constants.JAVA_LANG_OBJECT)) {
            // Everything is a sub type if the super type erasure
            // gives Object class
            return true;
        }
        // If any of the super or sub type is library type without package name
        //        LIB__OR__TYPE::Handler
        // We will not get the the super or sub types from that.
        //  1) Match the 'super-type' against the 'super-types-of-the-sub-type'.
        //  2) Match the 'sub-type' against the 'sub-types-of-the-super-type'.
        Set<String> superTypesOfSubErasure =
                CallGraphDataStructures.getAllSuperClassesAndImplementedInterfacesOfClass(subTypeErasure);
        if (superTypesOfSubErasure != null) {
            return matchTypeNameAny(superTypeErasure, superTypesOfSubErasure);
        } else {
            Set<String> subTypesOfSuperErasure = CallGraphDataStructures.getAllSubClass(superTypeErasure);
            return subTypesOfSuperErasure != null && matchTypeNameAny(subTypeErasure, subTypesOfSuperErasure);
        }
    }

    /**
     * Checks if the type erasures of two types match.
     *
     * <p>This static method compares the type erasures of two TypeInfo objects.
     * Type erasure removes generic information and provides the raw type that the
     * JVM actually uses at runtime.</p>
     *
     * <p>The method delegates to the concrete implementations' {@link #getTypeErasure()}
     * methods and then compares the resulting strings for equality.</p>
     *
     * <p>This method is useful for:</p>
     * <ul>
     *   <li>Type compatibility checking without generic information</li>
     *   <li>Runtime type matching scenarios</li>
     *   <li>Simplifying complex type comparisons</li>
     * </ul>
     *
     * @param type1 the first type to compare
     * @param type2 the second type to compare
     * @return true if the type erasures match, false otherwise
     */
    public static boolean matchTypeErasure(TypeInfo type1, TypeInfo type2) {
        return matchTypeName(type1.getTypeErasure(), type2.getTypeErasure());
    }

    /**
     * Checks if a target type name matches any type in a provided set of types.
     *
     * <p>This private static method performs type name matching with special handling
     * for library types and qualified vs. unqualified names. It's designed to handle
     * the complexities of type name matching in the call graph system.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Direct Matching:</strong> Simple string equality for non-library types</li>
     *   <strong>Library Type Matching:</strong> Special handling for types with {@code LIB__OR__TYPE::} prefixes</li>
     *   <li><strong>Qualified vs. Unqualified:</strong> Handles both package-qualified and simple type names</li>
     *   <li><strong>Manual Iteration:</strong> Falls back to manual matching when direct contains() fails</li>
     * </ul>
     *
     * <p>This method is used internally by the subtype checking logic to determine
     * inheritance relationships between types.</p>
     *
     * @param target the name of the target type to match
     * @param types the set of type names to match against
     * @return true if the target matches any type in the set, false otherwise
     */
    private static boolean matchTypeNameAny(String target, Set<String> types) {
        if (types != null) {
            // The set of types are from sub or super classes, loaded form
            // spec files and have full qualified name having package names.
            // But the target may not contain the package name for some limitations
            //       LIB__OR__TYPE::Handler
            // So it will not match directly with the elements of the set.
            //      [
            //          LIB__OR__TYPE::java.util.logging.StreamHandler,
            //          LIB__OR__TYPE::java.util.logging.Handler
            //      ]
            // It will cause the types.contains check to fail, need to match manually.
            if (!target.contains(Constants.LIB_TYPE) || target.contains(".")) {
                // This is not a library type, or a library type
                // in proper form with the full package name.
                return types.contains(target);
            }
            // Match the elements manually.
            for (String type : types) {
                if (matchTypeName(target, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compares two type names for equality with special handling for library types and qualified names.
     *
     * <p>This private static method performs sophisticated type name matching that handles
     * various naming conventions and formats used in the call graph system.</p>
     *
     * <p>The method implements several normalization strategies:</p>
     * <ul>
     *   <li><strong>Library Type Prefix Removal:</strong> Strips {@code LIB__OR__TYPE::} prefixes
     *       before comparison</li>
     *   <li><strong>Qualified vs. Unqualified Handling:</strong> Automatically normalizes
     *       package-qualified names to simple names when comparing with unqualified names</li>
     *   <li><strong>Package Name Extraction:</strong> Extracts simple class names from
     *       fully qualified names for comparison</li>
     * </ul>
     *
     * <p>This method is essential for:</p>
     * <ul>
     *   <li>Type compatibility checking across different naming conventions</li>
     *   <li>Handling library types with special prefixes</li>
     *   <li>Resolving qualified vs. unqualified type references</li>
     * </ul>
     *
     * @param type1 the first type name to compare
     * @param type2 the second type name to compare
     * @return true if the type names match after normalization, false otherwise
     */
    private static boolean matchTypeName(String type1, String type2) {
        // Remove library type prefix before match
        if (type1.contains(Constants.LIB_TYPE)) {
            type1 = CallGraphUtility.getLibraryName(type1);
        }
        if (type2.contains(Constants.LIB_TYPE)) {
            type2 = CallGraphUtility.getLibraryName(type2);
        }
        // If one of the expected or actual parameter is of qualified type
        // as in
        //   java.util.concurrent.Foo
        // and we want to match with simple Foo, we will match by
        // getting Foo from the qualified part and matching with simple if needed
        boolean isType1Qualified = type1.contains(".");
        boolean isType2Qualified = type2.contains(".");
        if (isType1Qualified && !isType2Qualified) {
            // Expected qualified type and found simple type
            // Expected is java.util.concurrent.Foo and found is Foo
            type1 = type1.substring(type1.lastIndexOf('.') + 1);
        } else if (!isType1Qualified && isType2Qualified) {
            // Expected simple type and found qualified type
            // Expected is Foo and found is java.util.concurrent.Foo
            type2 = type2.substring(type2.lastIndexOf('.') + 1);
        }
        return type1.equals(type2);
    }
}
