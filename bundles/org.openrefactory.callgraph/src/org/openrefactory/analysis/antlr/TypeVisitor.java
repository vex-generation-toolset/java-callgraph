// Generated from Type.g4 by ANTLR 4.9.1
/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TypeParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TypeVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TypeParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(TypeParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#scalarType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarType(TypeParser.ScalarTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(TypeParser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#numericType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericType(TypeParser.NumericTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#integralType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegralType(TypeParser.IntegralTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#floatingPointType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatingPointType(TypeParser.FloatingPointTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#referenceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferenceType(TypeParser.ReferenceTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#classOrParamOrSymbolicType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrParamOrSymbolicType(TypeParser.ClassOrParamOrSymbolicTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#classType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassType(TypeParser.ClassTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#paramType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamType(TypeParser.ParamTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#typeVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeVariable(TypeParser.TypeVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#arrayType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayType(TypeParser.ArrayTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#dims}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDims(TypeParser.DimsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#typeBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBound(TypeParser.TypeBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#additionalBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditionalBound(TypeParser.AdditionalBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(TypeParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#typeArgumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgumentList(TypeParser.TypeArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#typeArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgument(TypeParser.TypeArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#wildcard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcard(TypeParser.WildcardContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#wildcardBounds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcardBounds(TypeParser.WildcardBoundsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(TypeParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#scalarBoxed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarBoxed(TypeParser.ScalarBoxedContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#symbolicType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolicType(TypeParser.SymbolicTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#at}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAt(TypeParser.AtContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(TypeParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeParser#upperCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpperCase(TypeParser.UpperCaseContext ctx);
}