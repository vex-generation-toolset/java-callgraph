/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type.typeinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Represents scalar type information including primitive types and their wrapper classes.
 *
 * <p>ScalarTypeInfo is a concrete implementation of TypeInfo that handles Java's
 * primitive types (int, boolean, etc.) and their corresponding wrapper classes
 * (Integer, Boolean, etc.). It provides comprehensive type information and
 * compatibility checking for scalar types.</p>
 *
 * <p>The class supports all Java scalar types:</p>
 * <ul>
 *   <li><strong>Numeric Types:</strong> byte, short, int, long, float, double</li>
 *   <li><strong>Character Types:</strong> char, Character</li>
 *   <li><strong>Boolean Types:</strong> boolean, Boolean</li>
 *   <li><strong>String Type:</strong> String</li>
 *   <li><strong>Void Type:</strong> void, Void</li>
 * </ul>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Primitive vs. Wrapper:</strong> Distinguishes between primitive and boxed types</li>
 *   <li><strong>Auto-casting Support:</strong> Handles automatic type conversions (e.g., int to double)</li>
 *   <li><strong>Type Compatibility:</strong> Comprehensive matching and covering logic</li>
 *   <li><strong>Number Classification:</strong> Identifies numeric vs. non-numeric types</li>
 *   <li><strong>Object Compatibility:</strong> All scalar types are compatible with java.lang.Object</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class ScalarTypeInfo extends TypeInfo {

    /**
     * Serialization version identifier for this class.
     *
     * <p>This field is used by Java's serialization mechanism to ensure version
     * compatibility when deserializing instances of this class.</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the scalar type.
     *
     * <p>This field stores the name of the scalar type, which can be either
     * a primitive type name (e.g., "int", "boolean") or a wrapper class name
     * (e.g., "Integer", "Boolean").</p>
     */
    private final String name;

    /**
     * Indicates whether this scalar type is also a class type.
     *
     * <p>This field is true for wrapper classes (Integer, Boolean, etc.) and
     * false for primitive types (int, boolean, etc.). It's used to determine
     * whether the type can be treated as a class in certain contexts.</p>
     */
    private final boolean isClassType;

    /**
     * Indicates whether this scalar type is a boxed (wrapper) type.
     *
     * <p>This field is true for wrapper classes like Integer, Boolean, etc.,
     * and false for primitive types like int, boolean, etc. It's used to
     * distinguish between primitive and boxed representations.</p>
     */
    private final boolean isBoxType;

    /**
     * Enumeration of all supported scalar types with their metadata.
     *
     * <p>This enum provides comprehensive information about each scalar type including
     * primitive names, boxed type names, qualified names, and numeric classification.
     * It serves as a central repository for scalar type information and provides
     * utility methods for type parsing and validation.</p>
     *
     * <p>Each enum constant contains:</p>
     * <ul>
     *   <li><strong>Primitive Name:</strong> The Java primitive type name (e.g., "int")</li>
     *   <li><strong>Boxed Name:</strong> The wrapper class name (e.g., "Integer")</li>
     *   <li><strong>Qualified Name:</strong> The fully qualified wrapper class name (e.g., "java.lang.Integer")</li>
     *   <li><strong>Numeric Flag:</strong> Whether the type represents a numeric value</li>
     * </ul>
     *
     * @author Rifat Rubayatul Islam
     */
    public static enum Scalars {
        BYTE("byte", "Byte", "java.lang.Byte", true),
        SHORT("short", "Short", "java.lang.Short", true),
        INT("int", "Integer", "java.lang.Integer", true),
        LONG("long", "Long", "java.lang.Long", true),
        FLOAT("float", "Float", "java.lang.Float", true),
        DOUBLE("double", "Double", "java.lang.Double", true),
        CHAR("char", "Character", "java.lang.Character", false),
        STRING("String", "String", "java.lang.String", false),
        NUMBER("Number", "Number", "java.lang.Number", true),
        BOOLEAN("boolean", "Boolean", "java.lang.Boolean", false),
        VOID("void", "Void", "java.lang.Void", false);

        /**
         * The primitive name of this scalar type.
         *
         * <p>This field stores the Java primitive type name (e.g., "int", "boolean")
         * that corresponds to this scalar type.</p>
         */
        public final String name;

        /**
         * The boxed type name of this scalar type.
         *
         * <p>This field stores the wrapper class name (e.g., "Integer", "Boolean")
         * that corresponds to the primitive type.</p>
         */
        public final String boxedName;

        /**
         * The qualified name of the boxed type.
         *
         * <p>This field stores the fully qualified wrapper class name (e.g.,
         * "java.lang.Integer", "java.lang.Boolean") including the package.</p>
         */
        public final String qualifiedBoxedName;

        /**
         * Whether this type represents a numeric value.
         *
         * <p>This field indicates whether the scalar type can be used in
         * mathematical operations and numeric contexts.</p>
         */
        public final boolean isNumber;

        private Scalars(String name, String boxedName, String qualifiedBoxedName, boolean isNumber) {
            this.name = name;
            this.boxedName = boxedName;
            this.qualifiedBoxedName = qualifiedBoxedName;
            this.isNumber = isNumber;
        }

        /**
         * Parses a type name and creates a ScalarTypeInfo instance accordingly.
         *
         * <p>This method analyzes the given type name and determines whether it represents
         * a primitive type or a boxed type. It then creates an appropriate ScalarTypeInfo
         * instance with the correct flags set.</p>
         *
         * <p>The method handles several naming formats:</p>
         * <ul>
         *   <li><strong>Primitive Names:</strong> "int", "boolean", "char" → creates primitive type info</li>
         *   <li><strong>Boxed Names:</strong> "Integer", "Boolean", "Character" → creates boxed type info</li>
         *   <li><strong>Qualified Names:</strong> "java.lang.Integer" → creates boxed type info</li>
         * </ul>
         *
         * @param typeName the name of the scalar type to parse
         * @return a ScalarTypeInfo instance representing the parsed type
         * @throws IllegalArgumentException if the given name doesn't match any known scalar type
         */
        protected static ScalarTypeInfo parse(String typeName) {
            for (Scalars sc : values()) {
                if (sc.boxedName.equals(typeName) || sc.qualifiedBoxedName.equals(typeName)) {
                    return new ScalarTypeInfo(sc.name, true, sc != NUMBER);
                } else if (sc.name.equals(typeName)) {
                    return new ScalarTypeInfo(typeName, false, false);
                }
            }
            // Should never reach here
            throw new IllegalArgumentException("Invalid Scalar type name : " + typeName);
        }

        /**
         * Returns the actual type name based on the primitive name and boxed type information.
         *
         * <p>This method converts between primitive and boxed type names. It's useful for
         * determining the appropriate type name when you have a primitive type but need
         * the corresponding wrapper class name, or vice versa.</p>
         *
         * <p>Examples:</p>
         * <ul>
         *   <li><strong>Primitive to Boxed:</strong> "int" + true → "Integer"</li>
         *   <li><strong>Primitive to Primitive:</strong> "int" + false → "int"</li>
         *   <li><strong>Boxed to Boxed:</strong> "Integer" + true → "Integer"</li>
         * </ul>
         *
         * @param typeName the name of the scalar type (primitive or boxed)
         * @param isBoxedType whether the result should be a boxed type name
         * @return the actual type name based on the requested format
         */
        protected static String getActualName(String typeName, boolean isBoxedType) {
            for (Scalars sc : values()) {
                if (sc.name.equals(typeName)) {
                    if (isBoxedType) {
                        return sc.boxedName;
                    } else {
                        return sc.name;
                    }
                }
            }
            // Should never reach here
            // Just to pass the compiler error
            return null;
        }

        /**
         * Checks if the given type name belongs to a boxed scalar type.
         *
         * <p>This method determines whether a type name represents a wrapper class
         * rather than a primitive type. It checks both the simple boxed name and
         * the fully qualified name.</p>
         *
         * <p>Examples of boxed scalar types:</p>
         * <ul>
         *   <li><strong>Simple Names:</strong> "Integer", "Boolean", "Character"</li>
         *   <li><strong>Qualified Names:</strong> "java.lang.Integer", "java.lang.Boolean"</li>
         * </ul>
         *
         * <p>Examples of non-boxed types:</p>
         * <ul>
         *   <li><strong>Primitives:</strong> "int", "boolean", "char"</li>
         *   <li><strong>Other Classes:</strong> "String", "Object", "List"</li>
         * </ul>
         *
         * @param typeName the name of the type to check
         * @return true if the name represents a boxed scalar type, false otherwise
         */
        public static boolean isBoxedScalar(String typeName) {
            for (Scalars sc : values()) {
                if (sc.boxedName.equals(typeName) || sc.qualifiedBoxedName.equals(typeName)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether the given type name belongs to a scalar type.
         *
         * <p>This method determines whether a type name represents any scalar type,
         * including both primitive types and their wrapper classes. It's more
         * inclusive than {@link #isBoxedScalar(String)} as it accepts both
         * primitive and boxed representations.</p>
         *
         * <p>Examples of scalar types:</p>
         * <ul>
         *   <li><strong>Primitives:</strong> "int", "boolean", "char", "double"</li>
         *   <li><strong>Boxed Types:</strong> "Integer", "Boolean", "Character", "Double"</li>
         *   <li><strong>Qualified Boxed:</strong> "java.lang.Integer", "java.lang.Boolean"</li>
         * </ul>
         *
         * <p>Examples of non-scalar types:</p>
         * <ul>
         *   <li><strong>Reference Types:</strong> "String", "Object", "List", "Map"</li>
         *   <li><strong>Array Types:</strong> "int[]", "String[]"</li>
         * </ul>
         *
         * @param typeName the name of the type to check
         * @return true if the name represents a scalar type, false otherwise
         */
        public static boolean isScalar(String typeName) {
            for (Scalars sc : values()) {
                if (sc.name.equals(typeName)
                        || sc.boxedName.equals(typeName)
                        || sc.qualifiedBoxedName.equals(typeName)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether the given type name represents a numeric scalar type.
         *
         * <p>This method determines whether a type name represents a scalar type
         * that can be used in mathematical operations. Numeric types support
         * arithmetic operations, comparisons, and other mathematical functions.</p>
         *
         * <p>Numeric scalar types include:</p>
         * <ul>
         *   <li><strong>Integer Types:</strong> byte, short, int, long</li>
         *   <li><strong>Floating Point Types:</strong> float, double</li>
         *   <li><strong>Wrapper Classes:</strong> Byte, Short, Integer, Long, Float, Double</li>
         *   <li><strong>Number Class:</strong> java.lang.Number</li>
         * </ul>
         *
         * <p>Non-numeric scalar types include:</p>
         * <ul>
         *   <li><strong>Character Types:</strong> char, Character</li>
         *   <li><strong>Boolean Types:</strong> boolean, Boolean</li>
         *   <li><strong>String Type:</strong> String</li>
         *   <li><strong>Void Type:</strong> void, Void</li>
         * </ul>
         *
         * @param typeName the name of the type to check
         * @return true if the type represents a numeric scalar, false otherwise
         */
        public static boolean isNumber(String typeName) {
            for (Scalars sc : values()) {
                if (sc.name.equals(typeName)
                        || sc.boxedName.equals(typeName)
                        || sc.qualifiedBoxedName.equals(typeName)) {
                    return sc.isNumber;
                }
            }
            return false;
        }
    }

    public ScalarTypeInfo(String name) {
        this(Scalars.parse(name));
    }

    public ScalarTypeInfo(String name, boolean isClassType, boolean isBoxType) {
        this.name = name;
        this.isClassType = isClassType;
        this.isBoxType = isBoxType;
    }

    private ScalarTypeInfo(ScalarTypeInfo info) {
        this.name = info.name;
        this.isClassType = info.isClassType;
        this.isBoxType = info.isBoxType;
    }

    public boolean isClassType() {
        return isClassType;
    }

    public boolean isBoxType() {
        return isBoxType;
    }

    public boolean isVoidType() {
        return name.equals(Scalars.VOID.name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getTypeErasure() {
        return this.name;
    }

    @Override
    public Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFields() {
        return null;
    }

    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof ScalarTypeInfo
                && (this.getName().equals(declarationType.getName())
                        || isAutoCastable(getName(), declarationType.getName()))) {
            // An scalar type can be used in place of the same scalar type
            // or in place of a type that is auto castable in the required type
            return true;
        } else if (declarationType instanceof ClassTypeInfo
                && declarationType.getName().equals(Constants.JAVA_LANG_OBJECT)) {
            // An scalar type can be used in place of java.lang.Object
            return true;
        } else if (declarationType instanceof WildCardTypeInfo || declarationType instanceof SymbolicTypeInfo) {
            // An scalar can be used in place of Type variable
            // and wild card in the following scenarios
            // Bar ---> T where <T>, <T extends Bar>, <T extends Foo> where Bar is a sub type of Foo
            // Bar ---> ? where <?>, <? extends Bar>, <? extends Foo> where Bar is a sub type of Foo
            // N.B.: ? can not be used as a stand-alone type
            // It can only be used inside other generic type
            // This scenario may occur in
            // List<Bar> ---> List<?>
            // List<Bar> ---> List<? extends Bar>
            // List<Bar> ---> List<? extends Foo> where Bar is a sub type of Foo
            // List<Bar> ---> List<? super Bar>
            return declarationType.covers(this);
        }
        return false;
    }

    @Override
    public boolean covers(TypeInfo invocationType) {
        if (invocationType instanceof ScalarTypeInfo
                && (this.getName().equals(invocationType.getName())
                        || isAutoCastable(invocationType.getName(), getName()))) {
            // An scalar type can cover the same scalar type
            // or a type that is auto castable in this type
            return true;
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
        // Nothing to be done for a scalar type
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
        result = prime * result + (isBoxType ? 1231 : 1237);
        result = prime * result + (isClassType ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ScalarTypeInfo other = (ScalarTypeInfo) obj;
        if (isBoxType != other.isBoxType) return false;
        if (isClassType != other.isClassType) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        return true;
    }

    @Override
    public String toString() {
        return Scalars.getActualName(name, isBoxType);
    }

    @Override
    public TypeInfo clone() {
        return new ScalarTypeInfo(this.name, isClassType, isBoxType);
    }

    /**
     * Checks if one scalar type can be automatically cast to another scalar type.
     *
     * <p>This method implements Java's automatic type conversion rules for scalar types.
     * It determines whether a value of one type can be automatically converted to
     * another type without explicit casting.</p>
     *
     * <p>Java's automatic conversion rules follow a widening hierarchy:</p>
     * <ul>
     *   <li><strong>byte → short → int → long → float → double</strong></li>
     *   <li><strong>char → int → long → float → double</strong></li>
     * </ul>
     *
     * <p>Examples of automatic conversions:</p>
     * <ul>
     *   <li><strong>int → double:</strong> Allowed (widening)</li>
     *   <li><strong>byte → int:</strong> Allowed (widening)</li>
     *   <li><strong>double → int:</strong> Not allowed (narrowing)</li>
     *   <li><strong>boolean → int:</strong> Not allowed (incompatible)</li>
     * </ul>
     *
     * @param from the source type that will be cast
     * @param to the target type to cast to
     * @return true if automatic casting is possible, false otherwise
     */
    private boolean isAutoCastable(String from, String to) {
        if (to.equals(Scalars.FLOAT.name)) {
            if (from.equals(Scalars.INT.name)
                    || from.equals(Scalars.SHORT.name)
                    || from.equals(Scalars.BYTE.name)
                    || from.equals(Scalars.LONG.name)) {
                return true;
            }
        } else if (to.equals(Scalars.DOUBLE.name)) {
            if (from.equals(Scalars.INT.name)
                    || from.equals(Scalars.SHORT.name)
                    || from.equals(Scalars.BYTE.name)
                    || from.equals(Scalars.LONG.name)
                    || from.equals(Scalars.FLOAT.name)) {
                return true;
            }
        } else if (to.equals(Scalars.LONG.name)) {
            if (from.equals(Scalars.INT.name) || from.equals(Scalars.SHORT.name) || from.equals(Scalars.BYTE.name)) {
                return true;
            }
        } else if (to.equals(Scalars.INT.name)) {
            if (from.equals(Scalars.SHORT.name) || from.equals(Scalars.BYTE.name)) {
                return true;
            }
        } else if (to.equals(Scalars.SHORT.name)) {
            if (from.equals(Scalars.BYTE.name)) {
                return true;
            }
        }
        return false;
    }
}
