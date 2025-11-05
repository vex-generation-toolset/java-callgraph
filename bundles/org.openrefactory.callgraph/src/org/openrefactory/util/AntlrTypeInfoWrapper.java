/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.util.List;

import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ParameterizedTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;

/**
 * 
 * Wrapper object for Antlr TypeInfo construction. Necessary because of Parameterized and Wildcard
 * TypeInfo creation where we can not construct and return TypeInfo from visitor methods but need to
 * contain additional information.
 * 
 * @author Ridwanul Haque
 */
public class AntlrTypeInfoWrapper {
    // TypeInfo object that is ultimately created
    private TypeInfo typeInfo;

    // For the following grammar,
    // type --> type typeBound*
    // typeBound -->  'extends' type
    // visitor first goes to create typeBound.
    // then later creates type.
    // Store the extends info in wrapper and pass upwards
    private TypeInfo extendsTypeInfo;

    // Similar to extendsTypeInfo
    // Store the super type info in wrapper and pass upwards
    private TypeInfo superTypeInfo;

    // Similar to extendsTypeInfo
    // Used for Parameterized TypeInfo
    // Store the typeArguments in wrapper and pass upwards
    private List<TypeInfo> typeArguments;

    // Similar to typeArguments
    // Store the additionalBounds in wrapper and pass upwards
    private List<TypeInfo> additionalBounds;

    // ArrayTypeInfo dimension count pass upwards
    private int dimension;

    // Contain annotations in Type String and pass upwards
    private List<String> annotations;

    public AntlrTypeInfoWrapper() {
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public TypeInfo getExtendsTypeInfo() {
        return extendsTypeInfo;
    }

    public void setExtendsTypeInfo(TypeInfo extendsTypeInfo) {
        this.extendsTypeInfo = extendsTypeInfo;
    }

    public TypeInfo getSuperTypeInfo() {
        return superTypeInfo;
    }

    public void setSuperTypeInfo(TypeInfo superTypeInfo) {
        this.superTypeInfo = superTypeInfo;
    }

    public List<TypeInfo> getTypeArguments() {
        return typeArguments;
    }

    public void setTypeArguments(List<TypeInfo> typeArguments) {
        this.typeArguments = typeArguments;
    }
    
    public List<TypeInfo> getAdditionalBounds() {
        return additionalBounds;
    }

    public void setAdditionalBounds(List<TypeInfo> additionalBounds) {
        this.additionalBounds = additionalBounds;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    @Override
    public String toString() {
        String typeInfoStr = "";
        if (typeInfo instanceof ScalarTypeInfo) {
            ScalarTypeInfo temp = (ScalarTypeInfo)typeInfo;
            typeInfoStr += "{type=Scaler,  \nname=" + temp.getName() + ", \nisClassType=" + temp.isClassType()
                + ", \nisBoxType=" + temp.isBoxType() + "}";
        } else if (typeInfo instanceof ClassTypeInfo) {
            ClassTypeInfo temp = (ClassTypeInfo)typeInfo;
            typeInfoStr += "{type=ClassTypeInfo, \nname=" + temp.getName() + ", \nfiled=" + temp.getFields()
                + ", \nisInteface=" + temp.isInterface() + ", \nisInner=" + temp.isInner() + "}";
        } else if (typeInfo instanceof ParameterizedTypeInfo) {
            ParameterizedTypeInfo temp = (ParameterizedTypeInfo)typeInfo;
            typeInfoStr += "{type=ParameterizedTypeInfo, \nname=" + temp.getName() + ", \nelementTypeSize" + temp.getElementTypeSize() + ", \nelementTypes=(";
            for (TypeInfo info : temp.getElementTypes()) {
                typeInfoStr += info + ", ";
            }
            typeInfoStr += ") \nisFromSource=" + temp.isFromSource() + ", \nfields=" + temp.getFields()
                + ", \ntypeArgsToFields" + temp.getTypeArgsToFields() + "}";

        } else {
            typeInfoStr = typeInfo.toString();
        }
        return "AntlrTypeInfoWrapper [typeInfo=" + typeInfoStr + ", type=" + typeInfo.getClass() + ", extendsTypeInfo="
            + extendsTypeInfo + ", superTypeInfo=" + superTypeInfo + ", typeArguments=" + typeArguments
            + ", additionalBounds=" + additionalBounds + ", dimension=" + dimension + ", annotations=" + annotations
            + "]";
    }
}
