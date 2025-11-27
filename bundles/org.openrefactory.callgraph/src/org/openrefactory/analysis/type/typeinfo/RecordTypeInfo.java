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
 * Represents record type information in the call graph system.
 *
 * <p>RecordTypeInfo is a concrete implementation of TypeInfo that handles Java
 * record types. It provides type information including component fields and
 * inner class relationships.</p>
 *
 * @author Ridwanul Haque
 */
public final class RecordTypeInfo extends TypeInfo {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields;
    private final boolean isInner;

    public RecordTypeInfo(String name) {
        this.name = name;
        this.fields = Collections.emptyMap();
        this.isInner = false;
    }

    public RecordTypeInfo(
            String name,
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields,
            boolean isInner) {
        this.name = name;
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> copiedFields = new HashMap<>();
        copiedFields.putAll(fields);
        this.fields = Collections.unmodifiableMap(copiedFields);
        this.isInner = isInner;
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

    @Override
    public boolean matches(TypeInfo declarationType) {
        if (declarationType instanceof RecordTypeInfo) {
            return matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this);
        } else if (declarationType instanceof ClassTypeInfo) {
             // Records are classes (implicitly extend java.lang.Record)
             // So they can match ClassTypeInfo if the class is Object or Record or an interface implemented by the record
            return matchTypeErasure(declarationType, this) || isErasureSubTypeOf(declarationType, this);
        } else if (declarationType instanceof WildCardTypeInfo
                || declarationType instanceof SymbolicTypeInfo
                || declarationType instanceof ParameterizedTypeInfo) {
            return declarationType.covers(this);
        }
        return false;
    }

    @Override
    public boolean covers(TypeInfo invocationType) {
        if (this.getName().equals(Constants.JAVA_LANG_OBJECT) || this.getName().equals("java.lang.Record")) {
            return true;
        }
        if (invocationType instanceof RecordTypeInfo) {
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
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RecordTypeInfo other = (RecordTypeInfo) obj;
        if (fields == null) {
            if (other.fields != null) return false;
        } else if (!fields.equals(other.fields)) return false;
        if (isInner != other.isInner) return false;
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
        return new RecordTypeInfo(this.name, this.fields, this.isInner);
    }
}
