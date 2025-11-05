/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;

import org.openrefactory.analysis.antlr.TypeBaseVisitor;
import org.openrefactory.analysis.antlr.TypeParser.AdditionalBoundContext;
import org.openrefactory.analysis.antlr.TypeParser.AnnotationContext;
import org.openrefactory.analysis.antlr.TypeParser.ArrayTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.ClassOrParamOrSymbolicTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.ClassTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.DimsContext;
import org.openrefactory.analysis.antlr.TypeParser.FloatingPointTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.IntegralTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.NumericTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.ParamTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.PrimitiveTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.ReferenceTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.ScalarBoxedContext;
import org.openrefactory.analysis.antlr.TypeParser.ScalarTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.SymbolicTypeContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeArgumentContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeArgumentListContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeArgumentsContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeBoundContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeContext;
import org.openrefactory.analysis.antlr.TypeParser.TypeVariableContext;
import org.openrefactory.analysis.antlr.TypeParser.WildcardBoundsContext;
import org.openrefactory.analysis.antlr.TypeParser.WildcardContext;
import org.openrefactory.util.Constants;
import org.openrefactory.analysis.type.typeinfo.ArrayTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ParameterizedTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.SymbolicTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.analysis.type.typeinfo.WildCardTypeInfo;
import org.openrefactory.analysis.type.TypeCalculator;

/**
 * 
 * Implements a visitor for TypeInfo creation extending the Antlr generated generic visitor class.
 * Constructs a TypeInfoWrapper object while viisting the parse tree. TypeInfoWrapper is necessary
 * because of Parameterized and Wildcard TypeInfo creation where we can not construct and return
 * TypeInfo from visitor methods but need to contain additional information.
 * 
 */
public class AntlrTypeVisitor extends TypeBaseVisitor<AntlrTypeInfoWrapper> {

    @Override
    public AntlrTypeInfoWrapper visitType(TypeContext ctx) {
        return super.visitType(ctx);
    }

    @Override
    public AntlrTypeInfoWrapper visitScalarType(ScalarTypeContext ctx) {
        /*
         * scalarType: primitiveType | *annotation scalarBoxed;
         */
        if (ctx.primitiveType() != null) { 
            return super.visit(ctx.primitiveType());
        } else {
            return super.visit(ctx.scalarBoxed());
        }
    }

    @Override
    public AntlrTypeInfoWrapper visitPrimitiveType(PrimitiveTypeContext ctx) {
        /*
         * primitiveType
         *  :   annotation* numericType
         *  |   annotation* 'boolean'
         *  ;
         */
        if (ctx.numericType() != null) {
            return super.visitPrimitiveType(ctx);
        } 
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        ScalarTypeInfo typeInfo = new ScalarTypeInfo("boolean");
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitNumericType(NumericTypeContext ctx) {
        /*
         * numericType
         *  :   integralType
         *  |   floatingPointType
         *  ;
         */
        return super.visitNumericType(ctx);
    }

    @Override
    public AntlrTypeInfoWrapper visitIntegralType(IntegralTypeContext ctx) {
        /*
         * integralType
         *  :   'byte'
         *  |   'short'
         *  |   'int'
         *  |   'long'
         *  |   'char'
         *  ;
         */
        ScalarTypeInfo typeInfo = new ScalarTypeInfo(ctx.getText());
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitFloatingPointType(FloatingPointTypeContext ctx) {
        /*
         * floatingPointType
         *  :   'float'
         *  |   'double'
         *  ;
         */
        ScalarTypeInfo typeInfo = new ScalarTypeInfo(ctx.getText());
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitReferenceType(ReferenceTypeContext ctx) {
        /*
         * referenceType
         *  :   classOrParamOrSymbolicType
         *  |   typeVariable
         *  |   arrayType
         *  ;
         */
        return super.visitReferenceType(ctx);
    }

    @Override
    public AntlrTypeInfoWrapper visitClassOrParamOrSymbolicType(ClassOrParamOrSymbolicTypeContext ctx) {
        /*
         * classOrParamOrSymbolicType
         *    :  (  paramType
         *    |     classType
         *    |     symbolicType
         *    )
         *     ;
         * */
        return super.visitClassOrParamOrSymbolicType(ctx);
    }

    @Override
    public AntlrTypeInfoWrapper visitClassType(ClassTypeContext ctx) {
        /*
            classType
            :   annotation* identifier typeBound?
            |   classType '.' annotation* identifier typeBound?
            ;
         * 
         */
        StringBuilder typeName = new StringBuilder();
        if (ctx.classType() != null) {
            AntlrTypeInfoWrapper classTypeData = super.visit(ctx.classType());
            typeName.append(CallGraphUtility.getLibraryName(classTypeData.getTypeInfo().getName()));
            typeName.append(".");
        }
        ClassTypeInfo typeInfo = new ClassTypeInfo(
            Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + typeName.toString() + ctx.identifier().getText());
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitParamType(ParamTypeContext ctx) {
        /*
         * 
        paramType
            :   annotation* upperCase typeArguments typeBound?
            |   classType '.' annotation* upperCase typeArguments typeBound?
            |   upperCase '.' annotation* upperCase typeArguments typeBound?
            |   paramType '.' annotation* upperCase typeArguments typeBound?
            |   annotation* identifier typeArguments  typeBound?
            |   classType '.' annotation* identifier typeArguments typeBound?
            |   upperCase '.' annotation* identifier typeArguments typeBound?
            |   paramType '.' annotation* identifier typeArguments typeBound?
            ;
        * 
        */
        String name = ctx.getText();
        // Parameterized type name shouldn't contain <> part
        // So, trim the <> part
        int lastIndex = name.indexOf('<');
        String typeName = Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR 
            + (lastIndex < 0 ? name : name.substring(0, lastIndex));
        
        AntlrTypeInfoWrapper typeArgsWrapper = super.visit(ctx.typeArguments());
        List<TypeInfo> elementTypes = typeArgsWrapper.getTypeArguments();

        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        ParameterizedTypeInfo typeInfo = new ParameterizedTypeInfo(typeName,
            elementTypes == null ? 0 : elementTypes.size(), elementTypes, false, Collections.emptyMap(),
            Collections.emptyMap(), false);
        wrapper.setTypeInfo(typeInfo);

        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitTypeVariable(TypeVariableContext ctx) {
        /*
         * typeVariable
            :   annotation* identifier
            ;
         */
        ClassTypeInfo typeInfo = new ClassTypeInfo(Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + ctx.identifier().getText());
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitArrayType(ArrayTypeContext ctx) {
        /*
         arrayType
            :   (scalarType dims
            |   classOrParamOrSymbolicType dims
            |   typeVariable dims) typeBound?
            ;
         *
         */
        AntlrTypeInfoWrapper type = super.visit(ctx.getChild(0));
        AntlrTypeInfoWrapper dimesion = super.visit(ctx.dims());

        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        ArrayTypeInfo typeInfo = new ArrayTypeInfo(dimesion.getDimension(),
            type.getTypeInfo(), false);
        typeInfo = (ArrayTypeInfo)TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitDims(DimsContext ctx) {
        /*
         * dims
            :   annotation* '[' ']' (annotation* '[' ']')*
            ;
         * */
        String dimensionStr = ctx.getText();
        int dimension = 0;
        for (int i = 0; i < dimensionStr.length(); i++) {
            if (dimensionStr.toCharArray()[i] == '[') {
                dimension++;
            }
        }
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setDimension(dimension);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitTypeBound(TypeBoundContext ctx) {
        /*
         * typeBound
            :   'extends' typeVariable
            |   'extends' referenceType additionalBound*
            ;
         * */
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        AntlrTypeInfoWrapper extendsTypeWrapper = null;
        if(ctx.typeVariable() != null) {
            extendsTypeWrapper = super.visit(ctx.typeVariable());
        } else {
            extendsTypeWrapper = super.visit(ctx.referenceType());
        }
        wrapper.setExtendsTypeInfo(extendsTypeWrapper.getTypeInfo());
        
        if (ctx.getChildCount() > 2) {
            List<TypeInfo> additionalBounds = new ArrayList<>();
            for (int i = 2; i < ctx.getChildCount(); i++) {
                TypeInfo info = super.visit(ctx.getChild(i)).getTypeInfo();
                additionalBounds.add(info);
            }
            wrapper.setAdditionalBounds(additionalBounds);
        }
        
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitAdditionalBound(AdditionalBoundContext ctx) {
        /*
         * additionalBound
            :   '&' classType
            ;
         * */
        return super.visit(ctx.classType());
    }

    @Override
    public AntlrTypeInfoWrapper visitTypeArguments(TypeArgumentsContext ctx) {
        /*
         typeArguments
            :   '<' typeArgumentList '>'
            ;
         * */
        return super.visit(ctx.typeArgumentList());
    }

    @Override
    public AntlrTypeInfoWrapper visitTypeArgumentList(TypeArgumentListContext ctx) {
        /*
         * typeArgumentList
            :   typeArgument (',' typeArgument)*
            ;
         */
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        List<TypeInfo> arguments = new ArrayList<>();

        for (int i = 0; i < ctx.typeArgument().size(); i++) {
            AntlrTypeInfoWrapper wrapperArg = super.visit(ctx.typeArgument(i));
            arguments.add(wrapperArg.getTypeInfo());
        }
        wrapper.setTypeArguments(arguments);

        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitTypeArgument(TypeArgumentContext ctx) {
        /*
         * typeArgument
            :   referenceType
            |   wildcard
            |   scalarBoxed
            ;
         */
        return super.visitTypeArgument(ctx);
    }

    @Override
    public AntlrTypeInfoWrapper visitWildcard(WildcardContext ctx) {
        /*
         * wildcard
            :   annotation* '?' wildcardBounds?
            ;
         * */
        TypeInfo boundType = null;
        TypeInfo wildCardInfo = null;
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        if (ctx.wildcardBounds() != null) {
            AntlrTypeInfoWrapper wildBoundWrapper = super.visit(ctx.wildcardBounds());
            if (wildBoundWrapper.getExtendsTypeInfo() != null) {
                boundType = wildBoundWrapper.getExtendsTypeInfo();
                wildCardInfo = new WildCardTypeInfo(boundType, true);
            } else if (wildBoundWrapper.getSuperTypeInfo() != null) {
                boundType = wildBoundWrapper.getSuperTypeInfo();
                wildCardInfo = new WildCardTypeInfo(boundType, false);
            }
        } else {
            wildCardInfo = new WildCardTypeInfo();
        }
        wrapper.setTypeInfo(wildCardInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitWildcardBounds(WildcardBoundsContext ctx) {
        /*
         * wildcardBounds
            :   'extends' referenceType
            |   'super' referenceType
            ;
         * */
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        TypeInfo boundType = super.visit(ctx.referenceType()).getTypeInfo();
        if(ctx.getChild(0).getText().equals("extends")) {
            wrapper.setExtendsTypeInfo(boundType);
        } else {
            wrapper.setSuperTypeInfo(boundType);
        }
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitAnnotation(AnnotationContext ctx) {
        /*
         * annotation: at identifier;
         */
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        List<String> annotations = new ArrayList<>();
        annotations.add(ctx.identifier().getText());
        wrapper.setAnnotations(annotations);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitScalarBoxed(ScalarBoxedContext ctx) {
        /*
         * scalarBoxed: SCALAR;
         */
        ScalarTypeInfo typeInfo = new ScalarTypeInfo(ctx.getText());
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        wrapper.setTypeInfo(typeInfo);
        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visitSymbolicType(SymbolicTypeContext ctx) {
        /*
         * symbolicType: upperCase typeBound?
         * 
         */
        AntlrTypeInfoWrapper wrapper = new AntlrTypeInfoWrapper();
        List<TypeInfo> typeBounds = new ArrayList<>();

        if (ctx.typeBound() != null) {
            AntlrTypeInfoWrapper boundWrapper = super.visit(ctx.typeBound());

            if (boundWrapper.getExtendsTypeInfo() != null) {
                typeBounds.add(boundWrapper.getExtendsTypeInfo());
            }
            if (boundWrapper.getAdditionalBounds() != null) {
                for (TypeInfo additionalBound : boundWrapper.getAdditionalBounds()) {
                    typeBounds.add(additionalBound);
                }
            }
        }

        SymbolicTypeInfo typeInfo = new SymbolicTypeInfo(ctx.upperCase().getText(), typeBounds);
        wrapper.setTypeInfo(typeInfo);

        return wrapper;
    }

    @Override
    public AntlrTypeInfoWrapper visit(ParseTree tree) {
        return super.visit(tree);
    }
}
