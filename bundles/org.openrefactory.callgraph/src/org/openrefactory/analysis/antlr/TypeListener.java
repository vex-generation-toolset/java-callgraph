// Generated from Type.g4 by ANTLR 4.9.1
/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TypeParser}.
 */
public interface TypeListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TypeParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(TypeParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(TypeParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#scalarType}.
	 * @param ctx the parse tree
	 */
	void enterScalarType(TypeParser.ScalarTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#scalarType}.
	 * @param ctx the parse tree
	 */
	void exitScalarType(TypeParser.ScalarTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(TypeParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(TypeParser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#numericType}.
	 * @param ctx the parse tree
	 */
	void enterNumericType(TypeParser.NumericTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#numericType}.
	 * @param ctx the parse tree
	 */
	void exitNumericType(TypeParser.NumericTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#integralType}.
	 * @param ctx the parse tree
	 */
	void enterIntegralType(TypeParser.IntegralTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#integralType}.
	 * @param ctx the parse tree
	 */
	void exitIntegralType(TypeParser.IntegralTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#floatingPointType}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPointType(TypeParser.FloatingPointTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#floatingPointType}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPointType(TypeParser.FloatingPointTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#referenceType}.
	 * @param ctx the parse tree
	 */
	void enterReferenceType(TypeParser.ReferenceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#referenceType}.
	 * @param ctx the parse tree
	 */
	void exitReferenceType(TypeParser.ReferenceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#classOrParamOrSymbolicType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrParamOrSymbolicType(TypeParser.ClassOrParamOrSymbolicTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#classOrParamOrSymbolicType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrParamOrSymbolicType(TypeParser.ClassOrParamOrSymbolicTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#classType}.
	 * @param ctx the parse tree
	 */
	void enterClassType(TypeParser.ClassTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#classType}.
	 * @param ctx the parse tree
	 */
	void exitClassType(TypeParser.ClassTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#paramType}.
	 * @param ctx the parse tree
	 */
	void enterParamType(TypeParser.ParamTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#paramType}.
	 * @param ctx the parse tree
	 */
	void exitParamType(TypeParser.ParamTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#typeVariable}.
	 * @param ctx the parse tree
	 */
	void enterTypeVariable(TypeParser.TypeVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#typeVariable}.
	 * @param ctx the parse tree
	 */
	void exitTypeVariable(TypeParser.TypeVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void enterArrayType(TypeParser.ArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void exitArrayType(TypeParser.ArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#dims}.
	 * @param ctx the parse tree
	 */
	void enterDims(TypeParser.DimsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#dims}.
	 * @param ctx the parse tree
	 */
	void exitDims(TypeParser.DimsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(TypeParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(TypeParser.TypeBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#additionalBound}.
	 * @param ctx the parse tree
	 */
	void enterAdditionalBound(TypeParser.AdditionalBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#additionalBound}.
	 * @param ctx the parse tree
	 */
	void exitAdditionalBound(TypeParser.AdditionalBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(TypeParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(TypeParser.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#typeArgumentList}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentList(TypeParser.TypeArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#typeArgumentList}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentList(TypeParser.TypeArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(TypeParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(TypeParser.TypeArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void enterWildcard(TypeParser.WildcardContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void exitWildcard(TypeParser.WildcardContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#wildcardBounds}.
	 * @param ctx the parse tree
	 */
	void enterWildcardBounds(TypeParser.WildcardBoundsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#wildcardBounds}.
	 * @param ctx the parse tree
	 */
	void exitWildcardBounds(TypeParser.WildcardBoundsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(TypeParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(TypeParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#scalarBoxed}.
	 * @param ctx the parse tree
	 */
	void enterScalarBoxed(TypeParser.ScalarBoxedContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#scalarBoxed}.
	 * @param ctx the parse tree
	 */
	void exitScalarBoxed(TypeParser.ScalarBoxedContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#symbolicType}.
	 * @param ctx the parse tree
	 */
	void enterSymbolicType(TypeParser.SymbolicTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#symbolicType}.
	 * @param ctx the parse tree
	 */
	void exitSymbolicType(TypeParser.SymbolicTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#at}.
	 * @param ctx the parse tree
	 */
	void enterAt(TypeParser.AtContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#at}.
	 * @param ctx the parse tree
	 */
	void exitAt(TypeParser.AtContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(TypeParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(TypeParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeParser#upperCase}.
	 * @param ctx the parse tree
	 */
	void enterUpperCase(TypeParser.UpperCaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeParser#upperCase}.
	 * @param ctx the parse tree
	 */
	void exitUpperCase(TypeParser.UpperCaseContext ctx);
}