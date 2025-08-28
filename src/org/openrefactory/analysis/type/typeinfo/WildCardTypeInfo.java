/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents wildcard type information in the call graph system.
 *
 * <p>WildCardTypeInfo is a concrete implementation of TypeInfo that handles Java
 * wildcard types used in generic declarations. It provides comprehensive information
 * about wildcard types including their bounds and constraints.</p>
 *
 * <p>The class supports various wildcard type scenarios:</p>
 * <ul>
 *   <li><strong>Unbounded Wildcards:</strong> ? (represents any type)</li>
 *   <li><strong>Upper Bounded Wildcards:</strong> ? extends Number (represents Number and subtypes)</li>
 *   <li><strong>Lower Bounded Wildcards:</strong> ? super String (represents String and supertypes)</li>
 *   <li><strong>Complex Bounds:</strong> ? extends Comparable & Serializable</li>
 * </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Bound Management:</strong> Handles upper and lower bounds</li>
 *   <li><strong>Type Compatibility:</strong> Implements wildcard type matching and covering</li>
 *   <li><strong>Symbol Resolution:</strong> Manages wildcard type resolution</li>
 *   <li><strong>Bound Direction:</strong> Distinguishes between upper and lower bounds</li>
 *   <li><strong>Immutable Design:</strong> Thread-safe with final fields</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class WildCardTypeInfo extends TypeInfo {

    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The bound type that constrains this wildcard type.
     *
     * <p>This field stores the type that serves as either an upper or lower bound
     * for the wildcard. The bound type determines what types the wildcard can represent
     * and is used for type compatibility checking.</p>
     *
     * <p>Bound type characteristics:</p>
     * <ul>
     *   <li><strong>Upper Bound:</strong> When isUpperBound is true, represents the supertype</li>
     *   <li><strong>Lower Bound:</strong> When isUpperBound is false, represents the subtype</li>
     *   <li><strong>Null Value:</strong> Indicates an unbounded wildcard (represents any type)</li>
     *   <li><strong>Type Constraints:</strong> Limits what types the wildcard can represent</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>? extends Number:</strong> boundType = Number, isUpperBound = true</li>
     *   <li><strong>? super String:</strong> boundType = String, isUpperBound = false</li>
     *   <li><strong>? (unbounded):</strong> boundType = null, isUpperBound = false</li>
     *   <li><strong>? extends Comparable:</strong> boundType = Comparable, isUpperBound = true</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Bound Direction:</strong> The meaning depends on isUpperBound field</li>
     *   <li><strong>Null Handling:</strong> null indicates no bounds (unbounded wildcard)</li>
     *   <li><strong>Type Equality:</strong> Used for precise type equality checks</li>
     * </ul>
     */
    private final TypeInfo boundType;

    /**
     * Flag indicating whether the bound is an upper bound.
     *
     * <p>This field determines the direction of the bound constraint. It works in
     * conjunction with the boundType field to define the wildcard's type constraints.</p>
     *
     * <p>The field has three distinct states:</p>
     * <ul>
     *   <li><strong>true + non-null boundType:</strong> Upper bounded wildcard (? extends T)</li>
     *   <li><strong>false + non-null boundType:</strong> Lower bounded wildcard (? super T)</li>
     *   <li><strong>false + null boundType:</strong> Unbounded wildcard (?)</li>
     * </ul>
     *
     * <p>Bound semantics:</p>
     * <ul>
     *   <li><strong>Upper Bound (true):</strong> Wildcard represents the bound type and all its subtypes</li>
     *   <li><strong>Lower Bound (false):</strong> Wildcard represents the bound type and all its supertypes</li>
     *   <li><strong>No Bound (false + null):</strong> Wildcard represents any type (equivalent to Object)</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>? extends Number:</strong> isUpperBound = true (Number and subtypes)</li>
     *   <li><strong>? super String:</strong> isUpperBound = false (String and supertypes)</li>
     *   <li><strong>? (unbounded):</strong> isUpperBound = false (any type)</li>
     *   <li><strong>? extends Comparable:</strong> isUpperBound = true (Comparable and subtypes)</li>
     * </ul>
     */
    private final boolean isUpperBound;

    public WildCardTypeInfo() {
        boundType = null;
        isUpperBound = false;
    }

    public WildCardTypeInfo(TypeInfo boundType, boolean isUpperBound) {
        this.boundType = boundType;
        this.isUpperBound = isUpperBound;
    }

    public TypeInfo getBoundType() {
        return boundType;
    }

    public boolean isUpperBound() {
        return isUpperBound;
    }

    /**
     * Gets the name of this wildcard type.
     *
     * <p>This method returns a constant string representing the wildcard type.
     * Since wildcard types are denoted by the "?" symbol in Java, this method
     * returns a standardized representation that identifies the type as a wildcard.</p>
     *
     * <p>The returned name is consistent across all wildcard instances and is
     * used for type identification and comparison operations.</p>
     *
     * <p><strong>Return Value:</strong></p>
     * <ul>
     *   <li><strong>Constant String:</strong> Always returns the same wildcard identifier</li>
     *   <li><strong>Type Identification:</strong> Used to identify wildcard types in the system</li>
     *   <li><strong>Comparison Operations:</strong> Used for type equality and matching</li>
     * </ul>
     *
     * <p>This method is essential for distinguishing wildcard types from other
     * type categories during type resolution and call graph construction.</p>
     *
     * @return the constant wildcard type identifier
     */
    @Override
    public String getName() {
        return Constants.WILDCARD_TYPE;
    }

    /**
     * Returns the erasure of this wildcard type.
     *
     * <p>For wildcard types, the erasure depends on whether the type has an upper bound.
     * If no upper bound exists or if it's a lower bound, the erasure defaults to Object.
     * If an upper bound exists, the erasure is taken from the bound type.</p>
     *
     * <p>The erasure logic follows these rules:</p>
     * <ul>
     *   <li><strong>No Upper Bound:</strong> Returns Object (default erasure)</li>
     *   <li><strong>Lower Bound:</strong> Returns Object (lower bounds don't affect erasure)</li>
     *   <li><strong>Upper Bound:</strong> Returns the erasure of the bound type</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>? (unbounded):</strong> Returns "Object"</li>
     *   <li><strong>? extends Number:</strong> Returns "Number"</li>
     *   <li><strong>? super String:</strong> Returns "Object" (lower bound ignored)</li>
     *   <li><strong>? extends Comparable:</strong> Returns "Comparable"</li>
     *   <li><strong>? extends List:</strong> Returns "List"</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Upper Bound Priority:</strong> Only upper bounds affect erasure</li>
     *   <li><strong>Lower Bound Ignored:</strong> Lower bounds don't change erasure</li>
     *   <strong>Object Default:</strong> Unbounded and lower-bounded wildcards default to Object</li>
     * </ul>
     *
     * <p>This method is essential for understanding how the JVM sees wildcard types
     * at runtime, as generic information is erased during compilation.</p>
     *
     * @return the type erasure (Object for unbounded/lower-bounded, bound type for upper-bounded)
     */
    @Override
    public String getTypeErasure() {
        if (boundType == null || !isUpperBound) {
            // No upper bound
            // So, Type erasure will be Object
            return Constants.JAVA_LANG_OBJECT;
        } else {
            // There is a upper bound
            // Take the erasure of the bound
            return boundType.getTypeErasure();
        }
    }

    /**
     * Gets the fields contained in this wildcard type.
     *
     * <p>Wildcard types in Java do not have fields in the traditional sense. They are
     * type placeholders that represent unknown types during compilation and are erased
     * to their bounds at runtime. Since this method is designed for concrete types like
     * classes and interfaces, it returns null for wildcard types.</p>
     *
     * <p>Wildcard type characteristics that prevent field access:</p>
     * <ul>
     *   <li><strong>Type Placeholders:</strong> Represent unknown types, not concrete types</li>
     *   <li><strong>No Instance Creation:</strong> Cannot be instantiated directly</li>
     *   <li><strong>Compile-time Only:</strong> Exist only during compilation</li>
     *   <li><strong>Runtime Erasure:</strong> Replaced with concrete types at runtime</li>
     * </ul>
     *
     * <p>If you need to access fields, you should:</p>
     * <ul>
     *   <li><strong>Resolve the Wildcard Type:</strong> Use {@link #boundWildCardOrSymbolicType()}</li>
     *   <li><strong>Check Bounds:</strong> Use {@link #getBoundType()} to get constraint types</li>
     *   <li><strong>Type Matching:</strong> Use {@link #matches(TypeInfo)} for compatibility checks</li>
     * </ul>
     *
     * @return null (wildcard types do not have fields)
     */
    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return null;
    }

    /**
     * Checks whether this wildcard type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this wildcard type can be used where the declaration type is expected.</p>
     *
     * <p><strong>Important Note:</strong> This method should rarely be invoked since
     * wildcard types cannot be used as standalone types in Java. It's kept for
     * consistency and as a fail-safe mechanism.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Wildcard-to-Wildcard Matching:</strong> Wildcard type compatibility</li>
     *   <li><strong>Class Type Compatibility:</strong> Matches against class types</li>
     *   <li><strong>Symbolic Type Compatibility:</strong> Matches against symbolic types</li>
     * </ul>
     *
     * <p><strong>Wildcard Type Matching Rules:</strong></p>
     * <ul>
     *   <li><strong>Erasure Compatibility:</strong> Types must have compatible erasures</li>
     *   <li><strong>Subtype Support:</strong> This type's erasure must be compatible with declaration</li>
     *   <li><strong>Bound Consideration:</strong> Upper and lower bounds affect compatibility</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>? → ?:</strong> Match (same wildcard type)</li>
     *   <li><strong>? extends Number → ? extends Number:</strong> Match (same bounds)</li>
     *   <li><strong>? extends Integer → ? extends Number:</strong> Match (Integer extends Number)</li>
     *   <li><strong>? extends String → ? super String:</strong> Match (compatible bounds)</li>
     *   <li><strong>? extends Number → Number:</strong> Match (Number covers ? extends Number)</li>
     *   <li><strong>? extends Comparable → T extends Comparable:</strong> Match (T covers ? extends Comparable)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Standalone Limitation:</strong> Wildcards cannot be used as standalone types</li>
     *   <li><strong>Erasure Focus:</strong> Matching is based on type erasure, not bounds</li>
     *   <li><strong>Bound Compatibility:</strong> Bounds must be compatible for successful matching</li>
     * </ul>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this wildcard type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        // As wildcard types can't be used as a stand alone type
        // This method should never be invoked
        // Keeping this code for consistency and as a fail safe
        if (declarationType instanceof WildCardTypeInfo
                && (matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this))) {
            // A wildcard type can be used in place of another wildcard type
            // if both of them has the same erasure or erasure of this type is
            // a sub type of the erasure of the required type
            // ? ---> ? (T and U both are type variables)
            // ? extends Foo ---> ? extends Foo
            // ? extends Bar ---> ? extends Foo where Bar is a sub type of Foo
            // ? extends Bar ---> ? super Bar
            // ? extends Foo ---> ? super Bar where Foo is a super type of Bar
            return true;
        } else if (declarationType instanceof ClassTypeInfo || declarationType instanceof SymbolicTypeInfo) {
            // A wildcard can be used in place of a symbolic or class type
            // If the symbolic or class type covers the wildcard type
            // Foo ---> ? extends Foo
            // Foo ---> ? extends Bar where Bar is a subtype of Foo
            // T extends Foo ---> ? extends Foo
            // T extends Foo ---> ? extends Bar where Bar is a subtype of Foo
            return declarationType.covers(this);
        }
        return false;
    }

    /**
     * Checks whether this wildcard type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this wildcard type can accept the invocation type based on its bounds.</p>
     *
     * <p><strong>Important Note:</strong> This method should rarely be invoked since
     * wildcard types cannot be used as standalone types in Java. It's kept for
     * consistency and as a fail-safe mechanism.</p>
     *
     * <p>The method implements comprehensive covering rules with bound consideration:</p>
     * <ul>
     *   <li><strong>Unbounded Wildcards:</strong> Cover all types (default to Object)</li>
     *   <li><strong>Upper Bounded Wildcards:</strong> Cover types compatible with the upper bound</li>
     *   <li><strong>Lower Bounded Wildcards:</strong> Cover types that are supertypes of the lower bound</li>
     *   <strong>Object Bound:</strong> Special handling for Object-bounded types</li>
     * </ul>
     *
     * <p><strong>Covering Rules by Bound Type:</strong></p>
     * <ul>
     *   <li><strong>No Bounds (null):</strong> Covers all types (defaults to Object)</li>
     *   <li><strong>Upper Bound (isUpperBound = true):</strong> Covers types that the bound covers</li>
     *   <li><strong>Lower Bound (isUpperBound = false):</strong> Covers types that are supertypes of the bound</li>
     *   <li><strong>Object Bound:</strong> Covers all types (Object is universal)</li>
     * </ul>
     *
     * <p><strong>Covering Examples:</strong></p>
     * <ul>
     *   <li><strong>? (unbounded) → String:</strong> Cover (defaults to Object)</li>
     *   <li><strong>? (unbounded) → Integer:</strong> Cover (defaults to Object)</li>
     *   <li><strong>? extends Number → Integer:</strong> Cover (Integer extends Number)</li>
     *   <li><strong>? extends Number → String:</strong> No cover (String doesn't extend Number)</li>
     *   <li><strong>? super String → Object:</strong> Cover (Object is supertype of String)</li>
     *   <li><strong>? super String → CharSequence:</strong> Cover (CharSequence is supertype of String)</li>
     *   <li><strong>? super String → Integer:</strong> No cover (Integer is not supertype of String)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Standalone Limitation:</strong> Wildcards cannot be used as standalone types</li>
     *   <li><strong>Lower Bound Logic:</strong> Lower bounds use inverse coverage checking</li>
     *   <strong>Object Special Case:</strong> Object-bounded types cover everything</li>
     * </ul>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this wildcard type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        // As wildcard types can't be used as a stand alone type
        // This method should never be invoked
        // Keeping this code for consistency and as a fail safe
        if (boundType == null) {
            // If there is no bound any type is covered
            return true;
        } else if (isUpperBound) {
            if (getTypeErasure().equals(Constants.JAVA_LANG_OBJECT)) {
                // If the erasure is java.lang.Object
                // any type is covered
                return true;
            } else {
                // Otherwise check coverage of the bound
                return boundType.covers(invocationType);
            }
        } else {
            if (invocationType.covers(boundType)) {
                // In case of lower bound the covered type should be
                // of that type or any of its super class
                // So, check the inverse coverage
                return invocationType.covers(boundType);
            }
        }
        // Following symbolic type T, we do not look for multiple bounds
        // since multiple bounds are not allowed for wild card type
        // Also wild card has upper and lower bounds, but in symbolic
        // we only will have upper bounds
        return false;
    }

    /**
     * Checks whether this wildcard type contains symbolic or wildcard types that need replacement.
     *
     * <p>This method always returns true for wildcard types. Wildcard types are type
     * placeholders that represent unknown types during compilation and must be
     * replaced with concrete types during type resolution.</p>
     *
     * <p>Wildcard types always need replacement because:</p>
     * <ul>
     *   <li><strong>Type Placeholders:</strong> Represent unknown types, not concrete types</li>
     *   <li><strong>Compile-time Only:</strong> Exist only during compilation</li>
     *   <li><strong>Runtime Erasure:</strong> Must be replaced with concrete types</li>
     *   <li><strong>Type Resolution:</strong> Required for call graph construction</li>
     *   <li><strong>Bound Processing:</strong> Need to resolve bound types</li>
     * </ul>
     *
     * <p>Examples of wildcard types that need replacement:</p>
     * <ul>
     *   <li><strong>? (unbounded):</strong> Represents any type</li>
     *   <li><strong>? extends Number:</strong> Represents Number and subtypes</li>
     *   <li><strong>? super String:</strong> Represents String and supertypes</li>
     *   <li><strong>? extends Comparable:</strong> Represents Comparable and subtypes</li>
     * </ul>
     *
     * <p>This method is essential for the type resolution system to identify
     * which types require processing during call graph construction.</p>
     *
     * @return true (wildcard types always need replacement)
     */
    @Override
    public boolean needsReplacement() {
        return true;
    }

    /**
     * Traverses this wildcard type to find and map symbolic and wildcard types.
     *
     * <p>This method implements wildcard type resolution by capturing the mapping
     * between this wildcard type and a target type. It prevents duplicate processing
     * and maintains the traversal context for proper type resolution.</p>
     *
     * <p><strong>Important Assumption:</strong> This method assumes that pastContainers
     * will always be non-null since wildcard types cannot appear in isolation in Java.</p>
     *
     * <p>The method implements a sophisticated mapping strategy:</p>
     * <ul>
     *   <strong>Duplicate Prevention:</strong> Checks if this type has been processed before</li>
     *   <li><strong>Wildcard Type Capture:</strong> Maps this wildcard type to the target type</li>
     *   <li><strong>Container Mapping:</strong> Maps all container types to the target type</li>
     *   <li><strong>Traversal Path Tracking:</strong> Maintains context for infinite loop prevention</li>
     * </ul>
     *
     * <p><strong>Processing Flow:</strong></p>
     * <ol>
     *   <li>Check if this wildcard type has been processed before</li>
     *   <li>If not processed, capture the mapping for all container types</li>
     *   <li>Update traversal paths to prevent infinite loops</li>
     *   <li>Mark all container paths as seen</li>
     * </ol>
     *
     * <p><strong>Container Mapping Strategy:</strong></p>
     * <ul>
     *   <li><strong>All Containers:</strong> Maps every container in pastContainers</li>
     *   <li><strong>Same Target:</strong> All containers map to the same target type</li>
     *   <li><strong>Path Tracking:</strong> Prevents infinite recursion in complex hierarchies</li>
     * </ul>
     *
     * <p>This method is essential for resolving wildcard types like {@code ?} in
     * {@code List<?>} to concrete types like {@code String} in {@code List<String>}.</p>
     *
     * @param targetTypeInfo the target type to map this wildcard type to
     * @param pastContainers list of parameterized types encountered during traversal (assumed non-null)
     * @param seenTraversalPaths set of already processed traversal paths
     * @param capturedSymbolicTypes map of symbolic types to their captured values (unused for wildcard types)
     * @param capturedWildCardTypes map of wildcard types to their captured values
     */
    @Override
    public void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        // We are assuming that pastContainers will always be non-null
        // since ? cannot appear in isolation.

        // Only proceed if we have not matched this before
        // Prevents duplicate matching
        boolean hasProcessedBefore = false;
        for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
            if (seenTraversalPaths.contains(pastContainerInfo)) {
                hasProcessedBefore = true;
                break;
            }
        }

        if (!hasProcessedBefore) {
            // For each of the containers create an entry
            for (ObjectIntPair<TypeInfo> container : pastContainers) {
                capturedWildCardTypes.put(container, targetTypeInfo);
                seenTraversalPaths.add(container);
            }
        }
    }

    /**
     * Replaces this wildcard type with its captured concrete type.
     *
     * <p>This method implements wildcard type replacement by retrieving the previously
     * captured mapping and returning the concrete type that this wildcard type represents.
     * It prevents duplicate processing and handles special cases like array types and bound types.</p>
     *
     * <p><strong>Important Assumption:</strong> This method assumes that pastContainers
     * will always be non-null since wildcard types cannot appear in isolation in Java.</p>
     *
     * <p>The method implements a comprehensive replacement strategy:</p>
     * <ul>
     *   <li><strong>Duplicate Prevention:</strong> Checks if this type has been processed before</li>
     *   <li><strong>Wildcard Type Resolution:</strong> Retrieves the captured concrete type</li>
     *   <li><strong>Bound Type Processing:</strong> Handles bound wildcard types</li>
     *   <li><strong>Proper Type Resolution:</strong> Gets the proper type from class ID</li>
     *   <li><strong>Array Type Handling:</strong> Special handling for array types</li>
     *   <li><strong>Fallback Strategies:</strong> Multiple fallback mechanisms for type resolution</li>
     * </ul>
     *
     * <p><strong>Replacement Process:</strong></p>
     * <ol>
     *   <li>Check if this wildcard type has been processed before</li>
     *   <li>Retrieve the captured concrete type from container mapping</li>
     *   <li>Apply bound wildcard/symbolic type resolution</li>
     *   <li>Get the proper type information from class ID</li>
     *   <li>Handle special cases like array types</li>
     *   <li>Fall back to bound type processing if needed</li>
     *   <li>Return the replacement type or this unchanged</li>
     * </ol>
     *
     * <p><strong>Fallback Strategies:</strong></p>
     * <ul>
     *   <li><strong>Container Mapping:</strong> Primary strategy using captured wildcard types</li>
     *   <li><strong>Bound Type Resolution:</strong> Fallback using bound type information</li>
     *   <strong>Upper Bound Handling:</strong> For ? extends T, returns T</li>
     *   <li><strong>Lower Bound Handling:</strong> For ? super T, returns Object</li>
     * </ul>
     *
     * <p><strong>Return Value:</strong></p>
     * <ul>
     *   <li><strong>First Element (Boolean):</strong> true if replacement occurred, false otherwise</li>
     *   <li><strong>Second Element (TypeInfo):</strong> The replacement type or this unchanged</li>
     * </ul>
     *
     * <p>This method is essential for resolving wildcard types like {@code ?} to
     * concrete types like {@code String} during call graph construction.</p>
     *
     * @param pastContainers list of parameterized types encountered during traversal (assumed non-null)
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
        // We are assuming that pastContainers will always be non-null
        // since ? cannot appear in isolation.

        // Only proceed if we have not matched this before
        // Prevents duplicate matching
        boolean hasProcessedBefore = false;
        for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
            if (alreadyReplacedPaths.contains(pastContainerInfo)) {
                hasProcessedBefore = true;
                break;
            }
        }

        if (!hasProcessedBefore) {
            // For each of the containers create an entry
            for (ObjectIntPair<TypeInfo> container : pastContainers) {
                TypeInfo matchedType = capturedWildCardTypes.get(container);
                if (matchedType != null) {
                    matchedType = matchedType.boundWildCardOrSymbolicType();
                    TypeInfo matchedProperType =
                            CallGraphDataStructures.getProperTypeInfoFromClassId(matchedType.getName());
                    for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
                        alreadyReplacedPaths.add(pastContainerInfo);
                    }
                    if (matchedProperType != null) {
                        // If the matched type is ArrayTypeInfo, the matchedProperType
                        // found from the element type is not ArrayTypeInfo. For this case,
                        // reconstruct the ArrayTypeInfo again with that proper type as element type.
                        if (matchedType instanceof ArrayTypeInfo) {
                            ArrayTypeInfo arrayType = (ArrayTypeInfo) matchedType;
                            matchedProperType = new ArrayTypeInfo(
                                    arrayType.getDimension(), matchedProperType, arrayType.isVarArgsType());
                        }
                        return Pair.of(true, matchedProperType);
                    } else {
                        return Pair.of(true, matchedType);
                    }
                } else if (boundType != null) {
                    matchedType = capturedSymbolicTypes.get(boundType);
                    if (matchedType != null) {
                        // Since we could not match with ?
                        // We are now trying to match with
                        //   ? extends T
                        //   ? super T
                        if (isUpperBound) {
                            // ? extends T
                            // T constitutes the upper bound
                            // Since ? is T or lower types, we make it T
                            return Pair.of(true, matchedType);
                        } else {
                            // ? super T
                            // T constitutes the lower bound
                            // Since ? is T or above, we make it Object type
                            return Pair.of(true, new ClassTypeInfo(Constants.JAVA_LANG_OBJECT));
                        }
                    } else {
                        if (!isUpperBound) {
                            // ? super T
                            // T constitutes the lower bound
                            // Since ? is T or above, we make it Object type
                            return Pair.of(true, new ClassTypeInfo(Constants.JAVA_LANG_OBJECT));
                        } else {
                            if (boundType instanceof ClassTypeInfo
                                    || (boundType instanceof ParameterizedTypeInfo && !boundType.needsReplacement())) {
                                // ? extends T
                                // T constitutes the upper bound
                                // Since ? is T or lower types, we make it T
                                return Pair.of(true, boundType);
                            }
                        }
                    }
                }
            }
        }
        return Pair.of(false, this);
    }

    /**
     * Bounds wildcard or symbolic types in this wildcard type to their concrete types.
     *
     * <p>For wildcard types, this method returns the bound type if one exists, or the
     * wildcard type itself if no bounds are specified. This simplifies the handling
     * of bounded wildcards by focusing on their constraint types.</p>
     *
     * <p>The method implements a simplified bounding strategy:</p>
     * <ul>
     *   <li><strong>Bound Type Priority:</strong> Returns the bound type if it exists</li>
     *   <li><strong>No Bounds Handling:</strong> Returns this wildcard type unchanged</li>
     *   <li><strong>Bound Resolution:</strong> Delegates to the bound type for further processing</li>
     * </ul>
     *
     * <p><strong>Bounding Behavior:</p>
     * <ul>
     *   <li><strong>No Bounds:</strong> Returns this wildcard type unchanged</li>
     *   <li><strong>Upper Bound:</strong> Returns the upper bound type (? extends T → T)</li>
     *   <li><strong>Lower Bound:</strong> Returns the lower bound type (? super T → T)</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>? (unbounded):</strong> Returns ? (this wildcard type)</li>
     *   <li><strong>? extends Number:</strong> Returns Number (upper bound)</li>
     *   <li><strong>? super String:</strong> Returns String (lower bound)</li>
     *   <li><strong>? extends Comparable:</strong> Returns Comparable (upper bound)</li>
     *   <li><strong>? extends List:</strong> Returns List (upper bound)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Bound Type Focus:</strong> Only considers the bound type, not the wildcard</li>
     *   <li><strong>No Recursive Processing:</strong> Does not process bound types further</li>
     *   <li><strong>Simplified Resolution:</strong> Provides a straightforward way to access constraints</li>
     * </ul>
     *
     * <p>This method is useful for simplifying bounded wildcard types during
     * type resolution and call graph construction.</p>
     *
     * @return the bound type if bounds exist, or this wildcard type if no bounds
     */
    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        if (boundType != null) {
            return boundType;
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((boundType == null) ? 0 : boundType.hashCode());
        result = prime * result + (isUpperBound ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        WildCardTypeInfo other = (WildCardTypeInfo) obj;
        if (boundType == null) {
            if (other.boundType != null) return false;
        } else if (!boundType.equals(other.boundType)) return false;
        if (isUpperBound != other.isUpperBound) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("?");
        if (boundType != null) {
            if (isUpperBound) {
                builder.append(" extends ");
            } else {
                builder.append(" super ");
            }
            builder.append(boundType.toString());
        }
        return builder.toString();
    }

    @Override
    public TypeInfo clone() {
        return new WildCardTypeInfo(boundType, isUpperBound);
    }
}
