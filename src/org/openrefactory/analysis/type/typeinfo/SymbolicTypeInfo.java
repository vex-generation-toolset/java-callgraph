/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents symbolic type information in the call graph system.
 *
 * <p>SymbolicTypeInfo is a concrete implementation of TypeInfo that handles Java
 * type variables (symbolic types) used in generic declarations. It provides comprehensive
 * information about type parameters including their bounds and constraints.</p>
 *
 * <p>The class supports various symbolic type scenarios:</p>
 * <ul>
 *   <li><strong>Simple Type Variables:</strong> T, U, V in generic declarations</li>
 *   <li><strong>Bounded Type Variables:</strong> T extends Number, U extends Comparable</li>
 *   <li><strong>Intersection Types:</strong> T extends Foo & Bar (multiple bounds)</li>
 *   <li><strong>Unbounded Types:</strong> T with no explicit bounds (defaults to Object)</li>
 * </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Type Variable Management:</strong> Handles generic type parameters</li>
 *   <li><strong>Bound Processing:</strong> Manages upper bounds and intersection types</li>
 *   <li><strong>Type Resolution:</strong> Resolves symbolic types to concrete types</li>
 *   <li><strong>Symbolic Matching:</strong> Implements type variable compatibility</li>
 *   <li><strong>Immutable Design:</strong> Thread-safe with unmodifiable collections</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class SymbolicTypeInfo extends TypeInfo {
    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the symbolic type variable.
     *
     * <p>This field stores the identifier for the type variable, typically a single
     * uppercase letter or descriptive name. The name represents the type parameter
     * in generic declarations and is used for type matching and resolution.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>Simple Variables:</strong> "T", "U", "V", "E", "K"</li>
     *   <li><strong>Descriptive Names:</strong> "ElementType", "KeyType", "ValueType"</li>
     *   <li><strong>Generic Parameters:</strong> "T" in List&lt;T&gt;</li>
     *   <li><strong>Method Parameters:</strong> "U" in &lt;U&gt; void process(U item)</li>
     * </ul>
     */
    private final String name;

    /**
     * List of upper bound types for this symbolic type.
     *
     * <p>This field stores the upper bounds that constrain the symbolic type.
     * Symbolic types can have upper bounds but not lower bounds, and may support
     * intersection types for multiple bounds.</p>
     *
     * <p>Bound characteristics:</p>
     * <ul>
     *   <li><strong>Upper Bounds Only:</strong> Symbolic types cannot have lower bounds</li>
     *   <li><strong>Intersection Support:</strong> Multiple bounds using &amp; operator</li>
     *   <li><strong>Inheritance Rules:</strong> First bound can be class, others must be interfaces</li>
     *   <li><strong>Generic Method Scope:</strong> Bounds only appear in generic method parameters</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>Single Bound:</strong> T extends Number</li>
     *   <li><strong>Multiple Bounds:</strong> T extends Comparable & Serializable</li>
     *   <li><strong>Class + Interface:</strong> T extends ArrayList & List</li>
     *   <li><strong>No Bounds:</strong> null (defaults to Object)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Null when Unbounded:</strong> No bounds means the type defaults to Object</li>
     *   <li><strong>Immutable List:</strong> The returned list cannot be modified</li>
     *   <li><strong>Order Matters:</strong> First bound determines primary type constraints</li>
     * </ul>
     */
    private final List<TypeInfo> boundTypes;

    public SymbolicTypeInfo(String name) {
        this.name = name;
        boundTypes = null;
    }

    public SymbolicTypeInfo(String name, List<TypeInfo> boundTypes) {
        this.name = name;
        if (boundTypes.isEmpty()) {
            this.boundTypes = null;
        } else {
            // Copy the list and make it unmodifiable
            List<TypeInfo> copiedBoundTypes = new ArrayList<>(boundTypes.size());
            copiedBoundTypes.addAll(boundTypes);
            this.boundTypes = Collections.unmodifiableList(copiedBoundTypes);
        }
    }

    public List<TypeInfo> getBoundTypes() {
        return boundTypes;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Returns the erasure of this symbolic type.
     *
     * <p>For symbolic types, the erasure depends on whether the type has upper bounds.
     * If no bounds are specified, the erasure defaults to Object. If bounds exist,
     * the erasure is taken from the first (primary) bound.</p>
     *
     * <p>The erasure logic follows these rules:</p>
     * <ul>
     *   <li><strong>No Bounds:</strong> Returns Object (default erasure)</li>
     *   <li><strong>Single Bound:</strong> Returns the erasure of the bound type</li>
     *   <li><strong>Multiple Bounds:</strong> Returns the erasure of the first bound</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>T (unbounded):</strong> Returns "Object"</li>
     *   <li><strong>T extends Number:</strong> Returns "Number"</li>
     *   <li><strong>T extends String:</strong> Returns "String"</li>
     *   <li><strong>T extends List & Serializable:</strong> Returns "List" (first bound)</li>
     *   <li><strong>U extends Comparable:</strong> Returns "Comparable"</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>First Bound Priority:</strong> Only the first bound affects erasure</li>
     *   <li><strong>Intersection Types:</strong> Additional bounds don't change erasure</li>
     *   <strong>Runtime Behavior:</strong> Reflects how the JVM sees the type</li>
     * </ul>
     *
     * @return the type erasure (Object for unbounded types, first bound for bounded types)
     */
    @Override
    public String getTypeErasure() {
        if (boundTypes == null) {
            // No upper bound
            // So, Type erasure will be Object
            return Constants.JAVA_LANG_OBJECT;
        } else {
            // There is upper bound(s)
            // Take the erasure of the first bound
            return boundTypes.get(0).getTypeErasure();
        }
    }

    /**
     * Gets the fields contained in this symbolic type.
     *
     * <p>Symbolic types in Java do not have fields in the traditional sense. They are
     * type variables that represent placeholder types during compilation and are erased
     * to their bounds at runtime. Since this method is designed for concrete types like
     * classes and interfaces, it returns null for symbolic types.</p>
     *
     * <p>Symbolic type characteristics that prevent field access:</p>
     * <ul>
     *   <li><strong>Type Variables:</strong> Represent placeholder types, not concrete types</li>
     *   <li><strong>No Instance Creation:</strong> Cannot be instantiated directly</li>
     *   <li><strong>Compile-time Only:</strong> Exist only during compilation</li>
     *   <li><strong>Runtime Erasure:</strong> Replaced with concrete types at runtime</li>
     * </ul>
     *
     * <p>If you need to access fields, you should:</p>
     * <ul>
     *   <li><strong>Resolve the Symbolic Type:</strong> Use {@link #boundWildCardOrSymbolicType()}</li>
     *   <li><strong>Check Bounds:</strong> Use {@link #getBoundTypes()} to get constraint types</li>
     *   <li><strong>Type Matching:</strong> Use {@link #matches(TypeInfo)} for compatibility checks</li>
     * </ul>
     *
     * @return null (symbolic types do not have fields)
     */
    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return null;
    }

    /**
     * Checks whether this symbolic type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this symbolic type can be used where the declaration type is expected.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Symbolic-to-Symbolic Matching:</strong> Type variable compatibility</li>
     *   <li><strong>Class Type Compatibility:</strong> Matches against class types</li>
     *   <li><strong>Wildcard Compatibility:</strong> Matches against wildcard types</li>
     * </ul>
     *
     * <p><strong>Symbolic Type Matching Rules:</strong></p>
     * <ul>
     *   <strong>Erasure Compatibility:</strong> Types must have compatible erasures</li>
     *   <li><strong>Subtype Support:</strong> This type's erasure must be compatible with declaration</li>
     *   <li><strong>Bound Consideration:</strong> Upper bounds affect compatibility</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>T → T:</strong> Match (same symbolic type)</li>
     *   <li><strong>T extends Number → T extends Number:</strong> Match (same bounds)</li>
     *   <li><strong>T extends Integer → T extends Number:</strong> Match (Integer extends Number)</li>
     *   <li><strong>T extends String → ? extends Object:</strong> Match (String extends Object)</li>
     *   <li><strong>T extends Number → Number:</strong> Match (T is bounded to Number)</li>
     *   <li><strong>T extends Comparable → Comparable:</strong> Match (T is bounded to Comparable)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Erasure Focus:</strong> Matching is based on type erasure, not bounds</li>
     *   <li><strong>Bound Compatibility:</strong> Bounds must be compatible for successful matching</li>
     *   <li><strong>Wildcard Support:</strong> Can match against wildcard types with compatible bounds</li>
     * </ul>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this symbolic type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof SymbolicTypeInfo
                && (matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this))) {
            // A symbolic type can be used in place of another symbolic type
            // if both of them has the same erasure or erasure of this type is
            // a sub type of the erasure of the required type
            // T ---> U (T and U both are type variables)
            // T extends Foo ---> T extends Foo
            // T extends Bar ---> T extends Foo where Bar is a sub type of Foo
            return true;
        } else if (declarationType instanceof ClassTypeInfo || declarationType instanceof WildCardTypeInfo) {
            // A sysmbolic type can be used in place of a wildcard or class type
            // if wildcard or class type covers the symbolic type
            // i.e. has same erasure or the erasure of symbolic type
            // is a subtype of erasure of wildcard type
            // ? extends Foo ---> T extends Foo
            // ? extends Foo ---> T extends Bar where Bar is a subtype of Foo
            // Foo ---> T if <T extends Foo> or <T extends Bar> where Bar is sub type of Foo
            return declarationType.covers(this);
        }
        return false;
    }

    /**
     * Checks whether this symbolic type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this symbolic type can accept the invocation type based on its bounds.</p>
     *
     * <p>The method implements comprehensive covering rules with bound consideration:</p>
     * <ul>
     *   <li><strong>Unbounded Types:</strong> Cover all types (default to Object)</li>
     *   <li><strong>Single Bound:</strong> Cover types compatible with the bound</li>
     *   <li><strong>Multiple Bounds:</strong> Cover types compatible with all bounds</li>
     *   <strong>Object Bound:</strong> Special handling for Object-bounded types</li>
     * </ul>
     *
     * <p><strong>Covering Rules by Bound Type:</strong></p>
     * <ul>
     *   <li><strong>No Bounds (null):</strong> Covers all types (defaults to Object)</li>
     *   <li><strong>Single Bound:</strong> Covers types that the bound covers</li>
     *   <li><strong>Object Bound:</strong> Covers all types (Object is universal)</li>
     *   <li><strong>Multiple Bounds:</strong> Covers types that satisfy all bounds</li>
     * </ul>
     *
     * <p><strong>Covering Examples:</strong></p>
     * <ul>
     *   <li><strong>T (unbounded) → String:</strong> Cover (T defaults to Object)</li>
     *   <li><strong>T (unbounded) → Integer:</strong> Cover (T defaults to Object)</li>
     *   <li><strong>T extends Number → Integer:</strong> Cover (Integer extends Number)</li>
     *   <li><strong>T extends Number → String:</strong> No cover (String doesn't extend Number)</li>
     *   <li><strong>T extends Object → AnyType:</strong> Cover (Object covers all types)</li>
     *   <li><strong>T extends Comparable & Serializable → String:</strong> Cover (String implements both)</li>
     *   <li><strong>T extends List & Serializable → ArrayList:</strong> Cover (ArrayList extends List, implements Serializable)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Multiple Bound Logic:</strong> All bounds must be satisfied (AND logic)</li>
     *   <li><strong>No Lower Bounds:</strong> Symbolic types only support upper bounds</li>
     *   <strong>Object Special Case:</strong> Object-bounded types cover everything</li>
     * </ul>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this symbolic type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        if (boundTypes == null) {
            // If there is no bound any type is covered
            return true;
        } else if (boundTypes.size() == 1) {
            if (getTypeErasure().equals(Constants.JAVA_LANG_OBJECT)) {
                // If the erasure is java.lang.Object
                // any type is covered
                return true;
            } else {
                // Otherwise check coverage of the bound
                return boundTypes.get(0).covers(invocationType);
            }
        } else if (boundTypes.size() > 1) {
            // In case of multiple bound check coverage of each bound
            for (TypeInfo bound : boundTypes) {
                if (!bound.covers(invocationType)) {
                    return false;
                }
            }
            return true;
        }
        // In wild card type ?, we do not look for multiple bounds
        // since multiple bounds are not allowed for wild card type
        // Also wild card has upper and lower bounds, but in symbolic
        // we only will have upper bounds. So no checking for lower bounds.
        return false;
    }

    /**
     * Checks whether this symbolic type contains symbolic or wildcard types that need replacement.
     *
     * <p>This method always returns true for symbolic types. Symbolic types are type
     * variables that represent placeholder types during compilation and must be
     * replaced with concrete types during type resolution.</p>
     *
     * <p>Symbolic types always need replacement because:</p>
     * <ul>
     *   <li><strong>Type Variables:</strong> Represent placeholder types, not concrete types</li>
     *   <li><strong>Compile-time Only:</strong> Exist only during compilation</li>
     *   <li><strong>Runtime Erasure:</strong> Must be replaced with concrete types</li>
     *   <li><strong>Type Resolution:</strong> Required for call graph construction</li>
     * </ul>
     *
     * <p>Examples of symbolic types that need replacement:</p>
     * <ul>
     *   <li><strong>T:</strong> Simple type variable</li>
     *   <li><strong>U extends Number:</strong> Bounded type variable</li>
     *   <li><strong>K extends Comparable & Serializable:</strong> Intersection type variable</li>
     *   <li><strong>E:</strong> Element type variable</li>
     * </ul>
     *
     * <p>This method is essential for the type resolution system to identify
     * which types require processing during call graph construction.</p>
     *
     * @return true (symbolic types always need replacement)
     */
    @Override
    public boolean needsReplacement() {
        return true;
    }

    /**
     * Traverses this symbolic type to find and map symbolic and wildcard types.
     *
     * <p>This method implements symbolic type resolution by capturing the mapping
     * between this symbolic type and a target type. It prevents duplicate processing
     * and maintains the traversal context for proper type resolution.</p>
     *
     * <p>The method implements a sophisticated mapping strategy:</p>
     * <ul>
     *   <li><strong>Duplicate Prevention:</strong> Checks if this type has been processed before</li>
     *   <li><strong>Symbolic Type Capture:</strong> Maps this symbolic type to the target type</li>
     *   <li><strong>Traversal Path Tracking:</strong> Maintains context for infinite loop prevention</li>
     *   <li><strong>Container Context:</strong> Preserves the container hierarchy information</li>
     * </ul>
     *
     * <p><strong>Processing Flow:</strong></p>
     * <ol>
     *   <li>Check if this symbolic type has been processed before</li>
     *   <li>If not processed, capture the mapping to target type</li>
     *   <li>Update traversal paths to prevent infinite loops</li>
     *   <li>Mark container paths as seen</li>
     * </ol>
     *
     * <p><strong>Duplicate Prevention Logic:</strong></p>
     * <ul>
     *   <li><strong>Past Container Check:</strong> Examines previous traversal context</li>
     *   <li><strong>Seen Path Detection:</strong> Identifies already processed paths</li>
     *   <li><strong>Early Termination:</strong> Skips processing if already handled</li>
     * </ul>
     *
     * <p><strong>Mapping Strategy:</strong></p>
     * <ul>
     *   <li><strong>One-to-One Mapping:</strong> Each symbolic type maps to one target type</li>
     *   <li><strong>Context Preservation:</strong> Maintains container hierarchy information</li>
     *   <li><strong>Path Tracking:</strong> Prevents infinite recursion in complex type hierarchies</li>
     * </ul>
     *
     * <p>This method is essential for resolving symbolic types like {@code T} in
     * {@code List<T>} to concrete types like {@code String} in {@code List<String>}.</p>
     *
     * @param targetTypeInfo the target type to map this symbolic type to
     * @param pastContainers list of parameterized types encountered during traversal
     * @param seenTraversalPaths set of already processed traversal paths
     * @param capturedSymbolicTypes map of symbolic types to their captured values
     * @param capturedWildCardTypes map of wildcard types to their captured values (unused for symbolic types)
     */
    @Override
    public void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        // Only proceed if we have not matched this before
        // Prevents duplicate matching
        boolean hasProcessedBefore = false;
        if (pastContainers != null) {
            for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
                if (seenTraversalPaths.contains(pastContainerInfo)) {
                    hasProcessedBefore = true;
                    break;
                }
            }
        }
        if (!hasProcessedBefore) {
            if (!capturedSymbolicTypes.containsKey(this)) {
                capturedSymbolicTypes.put(this, targetTypeInfo);
                if (pastContainers != null) {
                    for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
                        seenTraversalPaths.add(pastContainerInfo);
                    }
                }
            }
        }
    }

    /**
     * Replaces this symbolic type with its captured concrete type.
     *
     * <p>This method implements symbolic type replacement by retrieving the previously
     * captured mapping and returning the concrete type that this symbolic type represents.
     * It prevents duplicate processing and handles special cases like array types.</p>
     *
     * <p>The method implements a comprehensive replacement strategy:</p>
     * <ul>
     *   <li><strong>Duplicate Prevention:</strong> Checks if this type has been processed before</li>
     *   <li><strong>Symbolic Type Resolution:</strong> Retrieves the captured concrete type</li>
     *   <li><strong>Bound Type Processing:</strong> Applies bound wildcard/symbolic type resolution</li>
     *   <li><strong>Proper Type Resolution:</strong> Gets the proper type from class ID</li>
     *   <li><strong>Array Type Handling:</strong> Special handling for array types</li>
     * </ul>
     *
     * <p><strong>Replacement Process:</strong></p>
     * <ol>
     *   <li>Check if this symbolic type has been processed before</li>
     *   <li>Retrieve the captured concrete type from the mapping</li>
     *   <li>Apply bound wildcard/symbolic type resolution</li>
     *   <li>Get the proper type information from class ID</li>
     *   <li>Handle special cases like array types</li>
     *   <li>Return the replacement type or this unchanged</li>
     * </ol>
     *
     * <p><strong>Special Case Handling:</strong></p>
     * <ul>
     *   <li><strong>Array Types:</strong> Reconstructs ArrayTypeInfo with proper element types</li>
     *   <li><strong>Proper Type Resolution:</strong> Uses CallGraphDataStructures for type lookup</li>
     *   <li><strong>Bound Processing:</strong> Applies wildcard/symbolic type resolution</li>
     * </ul>
     *
     * <p><strong>Return Value:</strong></p>
     * <ul>
     *   <li><strong>First Element (Boolean):</strong> true if replacement occurred, false otherwise</li>
     *   <li><strong>Second Element (TypeInfo):</strong> The replacement type or this unchanged</li>
     * </ul>
     *
     * <p>This method is essential for resolving symbolic types like {@code T} to
     * concrete types like {@code String} during call graph construction.</p>
     *
     * @param pastContainers list of parameterized types encountered during traversal
     * @param alreadyReplacedPaths set of already processed replacement paths
     * @param capturedSymbolicTypes map of symbolic types to their captured values
     * @param capturedWildCardTypes map of wildcard types to their captured values (unused for symbolic types)
     * @return a pair indicating whether replacement occurred and the resulting type
     */
    @Override
    public Pair<Boolean, TypeInfo> replaceSymbol(
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> alreadyReplacedPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        // Only proceed if we have not matched this before
        // Prevents duplicate matching
        boolean hasProcessedBefore = false;
        if (pastContainers != null) {
            for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
                if (alreadyReplacedPaths.contains(pastContainerInfo)) {
                    hasProcessedBefore = true;
                    break;
                }
            }
        }
        if (!hasProcessedBefore) {
            TypeInfo matchedType = capturedSymbolicTypes.get(this);
            if (matchedType != null) {
                matchedType = matchedType.boundWildCardOrSymbolicType();
                TypeInfo matchedProperType =
                        CallGraphDataStructures.getProperTypeInfoFromClassId(matchedType.getName());
                if (pastContainers != null) {
                    for (ObjectIntPair<TypeInfo> pastContainerInfo : pastContainers) {
                        alreadyReplacedPaths.add(pastContainerInfo);
                    }
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
            }
        }
        return Pair.of(false, this);
    }

    /**
     * Bounds wildcard or symbolic types in this symbolic type to their concrete types.
     *
     * <p>For symbolic types, this method returns the first upper bound if bounds exist,
     * or the symbolic type itself if no bounds are specified. This simplifies the
     * handling of intersection types by focusing on the primary bound.</p>
     *
     * <p>The method implements a simplified bounding strategy:</p>
     * <ul>
     *   <li><strong>Primary Bound Focus:</strong> Returns only the first bound type</li>
     *   <li><strong>Intersection Type Simplification:</strong> Ignores additional bounds</li>
     *   <li><strong>No Bounds Handling:</strong> Returns this symbolic type unchanged</li>
     *   <li><strong>Bound Priority:</strong> First bound is considered the primary constraint</li>
     * </ul>
     *
     * <p><strong>Bounding Behavior:</p>
     * <ul>
     *   <li><strong>No Bounds:</strong> Returns this symbolic type unchanged</li>
     *   <li><strong>Single Bound:</strong> Returns the bound type</li>
     *   <li><strong>Multiple Bounds:</strong> Returns only the first bound (ignores others)</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>T (unbounded):</strong> Returns T (this symbolic type)</li>
     *   <li><strong>T extends Number:</strong> Returns Number (first bound)</li>
     *   <li><strong>T extends String:</strong> Returns String (first bound)</li>
     *   <li><strong>T extends List & Serializable:</strong> Returns List (first bound only)</li>
     *   <li><strong>U extends Comparable & Cloneable:</strong> Returns Comparable (first bound only)</li>
     * </ul>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Intersection Simplification:</strong> Additional bounds are ignored</li>
     *   <li><strong>Primary Constraint Focus:</strong> First bound determines the result</li>
     *   <li><strong>No Recursive Processing:</strong> Does not process bound types further</li>
     * </ul>
     *
     * <p>This method is useful for simplifying complex symbolic types during
     * type resolution and call graph construction.</p>
     *
     * @return the first bound type if bounds exist, or this symbolic type if no bounds
     */
    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        if (boundTypes != null && !boundTypes.isEmpty()) {
            // Ignoring the impact of intersection types,
            // only get the first one to which the type is bound
            return boundTypes.get(0);
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((boundTypes == null) ? 0 : boundTypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SymbolicTypeInfo other = (SymbolicTypeInfo) obj;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (boundTypes == null) {
            if (other.boundTypes != null) return false;
        } else if (!boundTypes.equals(other.boundTypes)) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.name);
        if (boundTypes != null) {
            builder.append(" extends ");
            Iterator<TypeInfo> typeIterator = boundTypes.iterator();
            builder.append(typeIterator.next().toString());
            while (typeIterator.hasNext()) {
                builder.append(" & ").append(typeIterator.next().toString());
            }
        }
        return builder.toString();
    }

    @Override
    public TypeInfo clone() {
        return new SymbolicTypeInfo(name, boundTypes);
    }
}
