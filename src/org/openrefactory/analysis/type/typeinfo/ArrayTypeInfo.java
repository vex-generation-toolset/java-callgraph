/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents array type information in the call graph system.
 *
 * <p>ArrayTypeInfo is a concrete implementation of TypeInfo that handles Java
 * array types. It provides comprehensive information about array dimensions,
 * element types, and special handling for varargs parameters.</p>
 *
 * <p>The class supports various array scenarios:</p>
 * <ul>
 *   <li><strong>Single-dimensional Arrays:</strong> int[], String[], etc.</li>
   <li><strong>Multi-dimensional Arrays:</strong> int[][], String[][][], etc.</li>
   <li><strong>Varargs Parameters:</strong> Special handling for method varargs</li>
   <li><strong>Generic Arrays:</strong> Arrays with generic element types</li>
 </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Dimension Tracking:</strong> Maintains array dimension count</li>
 *   <li><strong>Element Type Management:</strong> Stores the type of array elements</li>
 *   <li><strong>Varargs Support:</strong> Special handling for variable argument methods</li>
 *   <li><strong>Type Compatibility:</strong> Array matching with dimension and element type checking</li>
 *   <li><strong>Symbol Resolution:</strong> Propagates symbolic type resolution to element types</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class ArrayTypeInfo extends TypeInfo {
    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The number of dimensions in this array type.
     *
     * <p>This field stores the array dimension count. For example:
     * <ul>
     *   <li><strong>1:</strong> Single-dimensional arrays like int[], String[]</li>
     *   <li><strong>2:</strong> Two-dimensional arrays like int[][], String[][]</li>
     *   <li><strong>3:</strong> Three-dimensional arrays like int[][][], etc.</li>
     * </ul></p>
     */
    private final int dimension;

    /**
     * The type information of the array elements.
     *
     * <p>This field stores the TypeInfo representing the type of elements
     * that can be stored in the array. It can be any valid TypeInfo including
     * scalar types, class types, other array types, or generic types.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li><strong>int[]:</strong> elementType = ScalarTypeInfo("int")</li>
     *   <li><strong>String[]:</strong> elementType = ClassTypeInfo("String")</li>
     *   <li><strong>int[][]:</strong> elementType = ArrayTypeInfo(1, ScalarTypeInfo("int"))</li>
     * </ul>
     */
    private final TypeInfo elementType;

    /**
     * Indicates whether this array type represents a varargs parameter.
     *
     * <p>This field is used to identify arrays that represent variable argument
     * parameters in method signatures. Varargs arrays have special semantics
     * in Java and are denoted with "..." in method declarations.</p>
     *
     * <p><strong>Note:</strong> This field is used for identification purposes
     * only and does not affect type matching or covering logic.</p>
     */
    private final boolean isVarArgs;

    public ArrayTypeInfo(int dimension, TypeInfo elementType, boolean isVarArgs) {
        this.dimension = dimension;
        this.elementType = elementType;
        this.isVarArgs = isVarArgs;
    }

    public int getDimension() {
        return dimension;
    }

    public TypeInfo getElementType() {
        return elementType;
    }

    @Override
    public String getName() {
        return elementType.getName();
    }

    public boolean isVarArgsType() {
        return isVarArgs;
    }

    @Override
    public String getTypeErasure() {
        // Returns the erasure of the element type.
        // When we are calculating erasure,
        // we are just interested in the element name's erasured type.
        // But when actual matching happens then we have the dimension also considered.
        // So, getting erasure for just the element type does not create problems.
        return elementType.getTypeErasure();
    }

    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return null;
    }

    /**
     * Checks whether this array type matches the required declaration type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method invocation type against a method declaration type. It determines
     * whether this array type can be used where the declaration type is expected.</p>
     *
     * <p>The method handles several matching scenarios:</p>
     * <ul>
     *   <li><strong>Array-to-Array Matching:</strong> Same dimension and compatible element types</li>
     *   <li><strong>Object Compatibility:</strong> All arrays are compatible with java.lang.Object</li>
     *   <li><strong>Generic Type Compatibility:</strong> Handles wildcards and type variables</li>
     * </ul>
     *
     * <p><strong>Array Matching Rules:</strong></p>
     * <ul>
     *   <li><strong>Dimension Match:</strong> Arrays must have the same number of dimensions</li>
     *   <li><strong>Element Type Compatibility:</strong> Element types must be compatible</li>
     *   <li><strong>Subtype Support:</strong> Bar[][] can match Foo[][] if Bar extends Foo</li>
     * </ul>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>Bar[][] → Bar[][]:</strong> Exact match (same type and dimension)</li>
     *   <li><strong>Bar[][] → Foo[][]:</strong> Match if Bar extends Foo</li>
     *   <li><strong>Bar[][] → Object:</strong> Always matches (Object covers all types)</li>
     *   <li><strong>Bar[][] → T[][]:</strong> Match if T extends Bar or Object</li>
     * </ul>
     *
     * @param declarationType the type required by the method declaration
     * @return true if this array type can be used in place of the declaration type, false otherwise
     */
    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof ArrayTypeInfo) {
            // An array type can be used in place of another
            // array type of the same dimension
            // and of the same type or if the required type is its super type
            // Bar[][] ---> Bar[][]
            // Bar[][] ---> Foo[][] if Foo is a super type of Bar
            // Bar[][] ---> T[][] where <T>, <T extends Bar>, <T extends Foo>
            //     Here invocation's type Bar will match declaration's type
            //     even though <T>, <T extends Bar>, <T extends Foo> may contain
            //     types that are sub types of Bar. Since Bar must be in the set
            //     of all cases (<T>, <T extends Bar>, <T extends Foo>) in the declaration
            //     the matching will work. The unmatched things in the declaration's
            //     set of types will not hanper the matching
            // <T extends Bar> T[][] ---> <T extends Bar> T[][]
            // <T extends Bar> T[][] ---> <T extends Foo> T[][]
            if (((ArrayTypeInfo) declarationType).getDimension() != getDimension()) {
                return false;
            }

            if (matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this)) {
                return true;
            }
        } else if (declarationType instanceof ClassTypeInfo
                && declarationType.getName().equals(Constants.JAVA_LANG_OBJECT)) {
            // An array type can be used in place of java.lang.Object
            return true;
        } else if (declarationType instanceof WildCardTypeInfo || declarationType instanceof SymbolicTypeInfo) {
            // An array can be used in place of Type variable
            // and wild card in the following scenarios
            // Bar[][] ---> T where <T> or <T extends Object>
            // Bar[][] ---> ? where <?> or <? extends Object>
            // N.B.: ? can not be used as a stand-alone type
            // It can only be used inside other generic type
            // This scenario may occur in
            // List<Bar[][]> ---> List<?>
            // List<Bar[][]> ---> List<? extends Bar[][]>
            // List<Bar[][]> ---> List<? extends Foo[][]> where Foo is a super type of Bar
            // List<Bar[][]> ---> List<? super Bar[][]>
            // List<Bar[][]> ---> List<? super Buzz[][]> where Buzz is a sub type of Bar
            return declarationType.covers(this);
        }
        return false;
    }

    /**
     * Checks whether this array type covers the given invocation type.
     *
     * <p>This method implements type compatibility checking from the perspective of
     * a method declaration type against a method invocation type. It determines
     * whether this array type can accept the invocation type.</p>
     *
     * <p>The method implements strict array covering rules:</p>
     * <ul>
     *   <li><strong>Dimension Match:</strong> Arrays must have exactly the same number of dimensions</li>
     *   <li><strong>Element Type Compatibility:</strong> This array's element type must cover the invocation array's element type</li>
     *   <li><strong>Subtype Support:</strong> Foo[][] can cover Bar[][] if Foo is a supertype of Bar</li>
     * </ul>
     *
     * <p><strong>Covering Examples:</strong></p>
     * <ul>
     *   <li><strong>Foo[][] → Foo[][]:</strong> Exact match (same type and dimension)</li>
     *   <li><strong>Foo[][] → Bar[][]:</strong> Cover if Bar extends Foo</li>
     *   <li><strong>Object[] → String[]:</strong> Cover since Object is supertype of String</li>
     *   <li><strong>Foo[] → Bar[][]:</strong> No cover (different dimensions)</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Unlike the {@link #matches(TypeInfo)} method, this method
     * does not handle Object compatibility or generic type scenarios. It focuses solely
     * on array-to-array compatibility with strict dimension and element type requirements.</p>
     *
     * @param invocationType the type being used in the method invocation
     * @return true if this array type can accept the invocation type, false otherwise
     */
    @Override
    public boolean covers(TypeInfo invocationType) {
        // An array type covers another array of same dimension
        // and of the same type or its sub types
        // Foo[][] ---> Foo[][]
        // Foo[][] ---> Bar[][] if Bar is a sub type of Foo
        // Foo[][] ---> T[][] if <T extends Foo> or <T extends Bar> where Bar is sub type of Foo
        if (invocationType instanceof ArrayTypeInfo && ((ArrayTypeInfo) invocationType).getDimension() == dimension) {
            if (matchTypeErasure(this, invocationType) || isErasureSubTypeOf(this, invocationType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean needsReplacement() {
        return elementType.needsReplacement();
    }

    @Override
    public void parseAndMapSymbols(
            TypeInfo targetTypeInfo,
            List<ObjectIntPair<TypeInfo>> pastContainers,
            Set<ObjectIntPair<TypeInfo>> seenTraversalPaths,
            Map<TypeInfo, TypeInfo> capturedSymbolicTypes,
            Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes) {
        // Only if the target type is also an array we continue
        // matching to the next level. Array matching is done
        // at the current depth level and the current position level
        if (targetTypeInfo instanceof ArrayTypeInfo) {
            elementType.parseAndMapSymbols(
                    ((ArrayTypeInfo) targetTypeInfo).getElementType(),
                    pastContainers,
                    seenTraversalPaths,
                    capturedSymbolicTypes,
                    capturedWildCardTypes);
        }
    }

    /**
     * Replaces symbolic types in this array with their captured concrete types.
     *
     * <p>This method implements symbolic type replacement for arrays. If the element
     * type contains symbolic types that need replacement, it attempts to replace
     * them and creates a new array with the updated element type.</p>
     *
     * <p>The method follows a conditional replacement strategy:</p>
     * <ul>
     *   <li><strong>Replacement Check:</strong> Only proceeds if {@link #needsReplacement()} returns true</li>
     *   <li><strong>Element Type Replacement:</strong> Attempts to replace symbolic types in the element type</li>
     *   <li><strong>New Array Creation:</strong> Creates a new array if replacement was successful</li>
     *   <li><strong>Memoization:</strong> Uses TypeCalculator to cache the new array type</li>
     * </ul>
     *
     * <p><strong>Replacement Process:</strong></p>
     * <ol>
     *   <li>Check if this array needs replacement</li>
     *   <li>Attempt to replace symbolic types in the element type</li>
     *   <li>If successful, create a new ArrayTypeInfo with the updated element type</li>
     *   <li>Cache the new type using TypeCalculator.putOrGetTypeInfoFromMemo</li>
     *   <li>Return the new array type with replacement flag set to true</li>
     * </ol>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>T[] → String[]:</strong> If T is captured as String</li>
     *   <li><strong>List&lt;?&gt;[] → List&lt;Integer&gt;[]:</strong> If ? is captured as Integer</li>
     *   <li><strong>Map&lt;K,V&gt;[] → Map&lt;String,Object&gt;[]:</strong> If K→String, V→Object</li>
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
        if (needsReplacement()) {
            Pair<Boolean, TypeInfo> result = elementType.replaceSymbol(
                    pastContainers, alreadyReplacedPaths, capturedSymbolicTypes, capturedWildCardTypes);
            if (result.fst) {
                // We were able to replace the array's element type
                // So create a new array
                TypeInfo typeInfo = new ArrayTypeInfo(dimension, result.snd, isVarArgs);
                typeInfo = TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
                return Pair.of(true, typeInfo);
            }
        }
        return Pair.of(false, this);
    }

    /**
     * Bounds wildcard or symbolic types in this array to their concrete types.
     *
     * <p>This method attempts to resolve wildcard and symbolic types in the element
     * type by binding them to concrete types. If the element type is modified,
     * a new array is created with the bounded element type.</p>
     *
     * <p>The method implements a conditional binding strategy:</p>
     * <ul>
     *   <li><strong>Element Type Binding:</strong> Attempts to bound wildcard/symbolic types in the element type</li>
     *   <li><strong>Change Detection:</strong> Compares the original and potentially modified element types</li>
     *   <li><strong>New Array Creation:</strong> Creates a new array if the element type was modified</li>
     *   <li><strong>Memoization:</strong> Uses TypeCalculator to cache the new array type</li>
     * </ul>
     *
     * <p><strong>Binding Process:</strong></p>
     * <ol>
     *   <li>Call boundWildCardOrSymbolicType on the element type</li>
     *   <li>Check if the element type was modified</li>
     *   <li>If unchanged, return this array (no modification needed)</li>
     *   <li>If changed, create a new array with the bounded element type</li>
     *   <li>Cache the new array type for reuse</li>
     * </ol>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>?[] → Integer[]:</strong> If ? is bounded to Integer</li>
     *   <li><strong>T[] → String[]:</strong> If T is bounded to String</li>
     *   <li><strong>List&lt;?&gt;[] → List&lt;Number&gt;[]:</strong> If ? is bounded to Number</li>
     * </ul>
     *
     * @return this array if no binding occurred, or a new array with bounded element type
     */
    @Override
    public TypeInfo boundWildCardOrSymbolicType() {
        TypeInfo possiblyModifiedType = elementType.boundWildCardOrSymbolicType();
        if (possiblyModifiedType == elementType) {
            return this;
        } else {
            TypeInfo typeInfo = new ArrayTypeInfo(dimension, possiblyModifiedType, isVarArgs);
            return TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dimension;
        result = prime * result + ((elementType == null) ? 0 : elementType.hashCode());
        result = prime * result + (isVarArgs ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ArrayTypeInfo other = (ArrayTypeInfo) obj;
        if (dimension != other.dimension) return false;
        if (elementType == null) {
            if (other.elementType != null) return false;
        } else if (!elementType.equals(other.elementType)) return false;
        if (isVarArgs != other.isVarArgs) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(elementType.toString());
        for (int i = 0; i < dimension; i++) {
            builder.append("[]");
        }
        if (isVarArgs) {
            builder.append("...");
        }
        return builder.toString();
    }

    @Override
    public TypeInfo clone() {
        return new ArrayTypeInfo(this.dimension, this.elementType, this.isVarArgs);
    }
}
