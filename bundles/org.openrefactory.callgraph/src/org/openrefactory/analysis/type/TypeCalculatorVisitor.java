/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.FieldInfo;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.type.typeinfo.ArrayTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.EnumTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ParameterizedTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.SymbolicTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.analysis.type.typeinfo.WildCardTypeInfo;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.NeoLRUCache;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.manager.C2PManager;
import org.openrefactory.util.manager.FNDSpecInfo;
import org.openrefactory.util.manager.FNDSpecManager;

/**
 * AST visitor for calculating TypeInfo representations of Java types from AST nodes.
 *
 * <p>This visitor traverses AST nodes and calculates their corresponding TypeInfo objects,
 * handling various Java expressions and type declarations.</p>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class TypeCalculatorVisitor extends ASTVisitor {

    // The TypeInfo object calculated by the visit methods
    private TypeInfo calculatedTypeInfo;

    // Path to the source file where the type node is used
    private String filePath;

    // true if we want to calculate soft type info and we only calculate
    // the type info based on the main class, false otherwise
    // in which we get the proper type info in which the fields
    // of a class are considered.
    // This is used by the visitor methods only,
    // When the actual calculation is done in the recursive methods,
    // we use a boolean parameter to dictate whether soft type of proper type
    // will be calculated.
    private boolean visitorCalculatesSoftType;

    // A cache that stores the type of a method calling context type.
    // It maps from the calling context expression string to the
    // pair of it's type info and scope if it is local variable or field of it.
    private NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache;

    public TypeCalculatorVisitor(String filePath, boolean visitorCalculatesSoftType) {
        this.filePath = filePath;
        this.visitorCalculatesSoftType = visitorCalculatesSoftType;
    }

    /**
     * @param filePath the containing file path
     * @param methodCallingContextCache A cache that stores the type of a method calling context type
     */
    public TypeCalculatorVisitor(
            String filePath, NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache) {
        this.filePath = filePath;
        this.visitorCalculatesSoftType = false;
        this.methodCallingContextCache = methodCallingContextCache;
    }

    public TypeInfo getCalculatedTypeInfo() {
        return calculatedTypeInfo;
    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(AnnotationTypeMemberDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        calculatedTypeInfo = getTypeInfo(node.resolveBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(ArrayAccess node) {
        // if exp1[exp2] is an ArrayAccess, exp1 is the expression
        // that represents the array variable/data-structure. It can
        // be any expressions like,
        // a, this.a, foo().a, A.a, aa.a etc.
        // So, we treat it as any other Expression and get its TypeInfo
        // through this visitor
        ITypeBinding typeBinding = node.resolveTypeBinding();
        if (typeBinding != null) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
        } else {
            node.getArray().accept(this);
            if (calculatedTypeInfo instanceof ArrayTypeInfo) {
                // The calculated type info is from the array expression,
                // we need to get the type for its element as
                //  - the element type, if it is single dimension array
                //  - array type with dimension decremented by 1, otherwise.
                ArrayTypeInfo arrayType = (ArrayTypeInfo) calculatedTypeInfo;
                if (arrayType.getDimension() == 1) {
                    calculatedTypeInfo = arrayType.getElementType();
                } else {
                    calculatedTypeInfo = new ArrayTypeInfo(
                            arrayType.getDimension() - 1, arrayType.getElementType(), arrayType.isVarArgsType());
                }
            }
        }
        return false;
    }

    @Override
    public boolean visit(ArrayCreation node) {
        // It's an Expression like,
        // new int[10], new int[10][], etc.
        // Since calling `getType()` on this `node` always
        // gets us an ArrayType. We don't need to send the
        // `TypeBinding` to process the ArrayTypeInfo correctly.
        calculatedTypeInfo = getTypeInfo(node.getType(), visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        // {1, 2, 3}, {a, b, c}, etc.
        // Here, I am assuming these can either be the arguments of an ArrayCreation or the RHS expression
        // of an assignment. So, only handling these two cases so that we don't face an unexpected StackOverflow
        // situation.
        // Another approach could have been that we check the content of the array to determine the type
        // But this was not done for two reasons.
        // 1. The above two cases should be sufficient to find the type anyway
        // 2. The content of the array may be a subtype of an array that is declared as a supertype
        // Say, A is a supertype and B, C are sub types
        // An array declared as A[] may have the following initializer {new B(), new C()}
        // In this case, if we were to get the type from the content,
        // we would have got B[] or C[], but both are wrong.
        // The type is A[] which only comes from the declaration context, as is done here
        ArrayCreation arrCreate = ASTNodeUtility.findNearestAncestor(node, ArrayCreation.class);
        if (arrCreate != null) {
            arrCreate.accept(this);
        } else {
            Assignment assignment = ASTNodeUtility.findNearestAncestor(node, Assignment.class);
            if (assignment != null) {
                assignment.accept(this);
            }
        }
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(AssertStatement node) {
        return false;
    }

    @Override
    public boolean visit(Assignment node) {
        // If we need to ever get the Type of the
        // entire Assignment like, `a = b`, I am
        // considering the LHS expression's type as its Type.
        // However, if we fail to get the TypeInfo for the LHS,
        // we will then get it from the RHS expression.
        node.getLeftHandSide().accept(this);
        if (calculatedTypeInfo == null) {
            node.getRightHandSide().accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(Block node) {
        return false;
    }

    @Override
    public boolean visit(BlockComment node) {
        return false;
    }

    @Override
    public boolean visit(BooleanLiteral node) {
        calculatedTypeInfo = new ScalarTypeInfo("boolean");
        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        return false;
    }

    @Override
    public boolean visit(CastExpression node) {
        calculatedTypeInfo = getTypeInfo(node.getType(), visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(CatchClause node) {
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        calculatedTypeInfo = new ScalarTypeInfo("char");
        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        AnonymousClassDeclaration anonymousClassDecl = node.getAnonymousClassDeclaration();
        if (anonymousClassDecl != null) {
            calculatedTypeInfo =
                    getTypeInfo(anonymousClassDecl.resolveBinding(), null, null, visitorCalculatesSoftType);
        } else {
            calculatedTypeInfo = getTypeInfo(node.getType(), visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(CompilationUnit node) {
        return false;
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        // Get the type from the then expression
        // If then expression fails to return a type or
        // if it returns a class type with null (happens if
        // we have null on the then side), then we get the type
        // from the else part
        node.getThenExpression().accept(this);
        if (calculatedTypeInfo == null
                || ((calculatedTypeInfo instanceof ClassTypeInfo)
                        && (calculatedTypeInfo.getName().equals(Constants.NULL_OBJECT_TYPE)))) {
            node.getElseExpression().accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        calculatedTypeInfo = new ScalarTypeInfo("void");
        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        return false;
    }

    @Override
    public boolean visit(CreationReference node) {
        return false;
    }

    @Override
    public boolean visit(Dimension node) {
        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        return false;
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        // Like ClassInstanceCreation, if there is anonymous class declaration, get type from that.
        // Otherwise get type from the constant declaration simple name.
        AnonymousClassDeclaration anonymousClassDecl = node.getAnonymousClassDeclaration();
        if (anonymousClassDecl != null) {
            calculatedTypeInfo =
                    getTypeInfo(anonymousClassDecl.resolveBinding(), null, null, visitorCalculatesSoftType);
        } else {
            node.getName().accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        calculatedTypeInfo = getTypeInfo(node.resolveBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(ExportsDirective node) {
        return false;
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        node.getExpression().accept(this);
        return false;
    }

    @Override
    public boolean visit(FieldAccess node) {
        // Directly taking the type from field binding gets us incorrect result
        // in some cases of ParameterizedType. So, we are going to the actual
        // ASTNode of the field's declaration and getting the type from the node directly.
        //
        // But, it is different in the case of Array types. If the array type field
        // declared like, int[] a, String[] a, etc. There is no problem with taking
        // the type or the type binding. However, if the declaration is like this -
        // int a[], String a[], etc., then getting the TypeInfo from the Type gets us
        // the wrong information.
        // In this case, the type binding gives us the desired ArrayType, but the `getType`
        // gives us a SimpleType instead of the actual array type. But again, taking type info
        // from the Type gets us correct type in the case of parameterized type.
        // So, we are opting to get the TypeInfo from type binding only in the case of array types.
        // For others, we are taking the information from the Type node.
        IVariableBinding fieldBinding = node.resolveFieldBinding();
        if (fieldBinding != null) {
            ASTNode varDec = ASTNodeUtility.getDeclaringNode(fieldBinding);
            ITypeBinding fieldTypeBinding = fieldBinding.getType();
            if (varDec != null && !fieldTypeBinding.isArray()) {
                varDec.accept(this);
                // For symbolic type we need to search for the actual type
                // from the calling context declared type and found type
                if (calculatedTypeInfo instanceof SymbolicTypeInfo) {
                    TypeInfo actualContainerType =
                            TypeCalculator.typeOf(node.getExpression(), visitorCalculatesSoftType);
                    TypeDeclaration containerClass =
                            ASTNodeUtility.findNearestAncestor(varDec, TypeDeclaration.class);
                    TypeInfo declaredContainerType = TypeCalculator.typeOf(containerClass, visitorCalculatesSoftType);
                    if (actualContainerType != null && !actualContainerType.equals(declaredContainerType)) {
                        TypeInfo modifiedTypeInfo = TypeCalculator.getReplacementForSymbolicTypeByMatchingTypesFrom(
                                calculatedTypeInfo, actualContainerType, null, declaredContainerType);
                        if (modifiedTypeInfo != null) {
                            calculatedTypeInfo = modifiedTypeInfo;
                        }
                    }
                }
            } else {
                calculatedTypeInfo = getTypeInfo(fieldTypeBinding, null, null, visitorCalculatesSoftType);
            }
        }
        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        // To handle parameterized types properly, we need to send Type
        // in getTypeInfo(), but that will give us false results for
        //
        // int arr[];
        //
        // types of field declarations. `int[] arr` gives us Type `int[]`
        // but `int arr[]` gives us Types as `int` which is clearly not an
        // ArrayType. So, to handle such cases, I need to get inside the field
        // fragments and take their binding (because JDT does not resolve binding
        // for the whole FieldDeclarartion) so that we can get the TypeBinding which
        // gives us `int[]` for both types of declarations.
        Type type = node.getType();
        ITypeBinding typeBinding = null;
        @SuppressWarnings("unchecked")
        IVariableBinding nodeBinding =
                ((List<VariableDeclarationFragment>) node.fragments()).get(0).resolveBinding();
        if (nodeBinding != null) {
            typeBinding = nodeBinding.getType();
        }
        if (typeBinding != null && (!type.isParameterizedType() || typeBinding.isArray())) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
        } else {
            calculatedTypeInfo = getTypeInfo(type, visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        return false;
    }

    @Override
    public boolean visit(IfStatement node) {
        return false;
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(InfixExpression node) {
        // For relational operations ==, !=, >, >=, <, <=
        // the result type is always boolean
        Operator operator = node.getOperator();
        if (operator == InfixExpression.Operator.EQUALS
                || operator == InfixExpression.Operator.NOT_EQUALS
                || operator == InfixExpression.Operator.GREATER
                || operator == InfixExpression.Operator.GREATER_EQUALS
                || operator == InfixExpression.Operator.LESS
                || operator == InfixExpression.Operator.LESS_EQUALS) {
            calculatedTypeInfo = new ScalarTypeInfo("boolean");
            return false;
        }
        Expression left = node.getLeftOperand();
        left.accept(this);
        TypeInfo leftTypeInfo = calculatedTypeInfo;
        Expression right = node.getRightOperand();
        right.accept(this);
        TypeInfo rightTypeInfo = calculatedTypeInfo;
        // For InfixExpression, either of the two operands should give us the correct
        // type info. However, in the case of String operations, it may differ.
        // In Java, we can add any other type (primitive, class objects, etc.) with a toString()
        // implemented with it. So, just for this exceptional case, we handle it accordingly.
        // If the two operand types mismatch and one of them is a String, we consider the Type of
        // InfixExpression to be String.
        if (rightTypeInfo == null) {
            calculatedTypeInfo = leftTypeInfo;
        } else if (leftTypeInfo != null && !rightTypeInfo.equals(leftTypeInfo)) {
            if (rightTypeInfo.toString().equals("String")
                    || leftTypeInfo.toString().equals("String")) {
                ScalarTypeInfo scalarTypeInfo = new ScalarTypeInfo("String");
                calculatedTypeInfo = scalarTypeInfo;
            }
        }
        return false;
    }

    @Override
    public boolean visit(Initializer node) {
        return false;
    }

    @Override
    public boolean visit(InstanceofExpression node) {

        return false;
    }

    @Override
    public boolean visit(IntersectionType node) {
        return false;
    }

    @Override
    public boolean visit(Javadoc node) {
        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        return false;
    }

    @Override
    public boolean visit(LambdaExpression node) {
        if (node.resolveTypeBinding() != null) {
            calculatedTypeInfo = getTypeInfo(node.resolveTypeBinding(), null, null, visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(LineComment node) {
        return false;
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        return false;
    }

    @Override
    public boolean visit(MemberRef node) {
        return false;
    }

    @Override
    public boolean visit(MemberValuePair node) {
        return false;
    }

    @Override
    public boolean visit(MethodRef node) {
        return false;
    }

    @Override
    public boolean visit(MethodRefParameter node) {
        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (methodCallingContextCache == null) {
            methodCallingContextCache = new NeoLRUCache<>(100);
        }
        // The calling-Context type cache is passed to reduce type calculations,
        // also we need the already calculated calling context type for
        // matching and getting actual type for symbolic and parametric type.
        int servicingMethodHashIndex =
                CallGraphUtility.getServicingMethodHashIndex(node, null, filePath, methodCallingContextCache);
        if (servicingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
            MethodIdentity identity = CallGraphDataStructures.getMatchingMethodIdentity(servicingMethodHashIndex);
            calculatedTypeInfo = identity.getReturnTypeInfo();
            // For symbolic type we need to search for the actual type
            // from the calling context declared type and found type
            Expression exp = node.getExpression();
            if (calculatedTypeInfo instanceof SymbolicTypeInfo && exp != null) {
                TypeInfo actualContainerType = TypeCalculator.getCallingContextType(exp, methodCallingContextCache);
                if (actualContainerType != null) {
                    TypeInfo declaredContainerType =
                            CallGraphUtility.getTypeInfoFromClassHash(actualContainerType.getName());
                    if (!actualContainerType.equals(declaredContainerType)) {
                        TypeInfo modifiedTypeInfo = TypeCalculator.getReplacementForSymbolicTypeByMatchingTypesFrom(
                                calculatedTypeInfo, actualContainerType, null, declaredContainerType);
                        if (modifiedTypeInfo != null) {
                            calculatedTypeInfo = modifiedTypeInfo;
                        }
                    }
                }
            }
        } else {
            // No servicing method is found, may be a library method
            // So, try to find the type info from json specs
            calculatedTypeInfo = getTypeInfoForLibraryMethod(node);
        }
        return false;
    }

    @Override
    public boolean visit(Modifier node) {
        return false;
    }

    @Override
    public boolean visit(ModuleDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(ModuleModifier node) {
        return false;
    }

    @Override
    public boolean visit(NameQualifiedType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        return false;
    }

    @Override
    public boolean visit(NullLiteral node) {
        calculatedTypeInfo = new ClassTypeInfo(Constants.NULL_OBJECT_TYPE);
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        calculatedTypeInfo = getTypeInfo(node.resolveTypeBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(OpensDirective node) {
        return false;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(ParameterizedType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        node.getExpression().accept(this);
        return false;
    }

    @Override
    public boolean visit(PostfixExpression node) {
        node.getOperand().accept(this);
        return false;
    }

    @Override
    public boolean visit(PrefixExpression node) {
        node.getOperand().accept(this);
        return false;
    }

    @Override
    public boolean visit(ProvidesDirective node) {
        return false;
    }

    @Override
    public boolean visit(PrimitiveType node) {
        if (node.getPrimitiveTypeCode().equals(PrimitiveType.VOID)) {
            calculatedTypeInfo = new ScalarTypeInfo("void");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.INT)) {
            calculatedTypeInfo = new ScalarTypeInfo("int");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.BOOLEAN)) {
            calculatedTypeInfo = new ScalarTypeInfo("boolean");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.BYTE)) {
            calculatedTypeInfo = new ScalarTypeInfo("byte");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.CHAR)) {
            calculatedTypeInfo = new ScalarTypeInfo("char");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.DOUBLE)) {
            calculatedTypeInfo = new ScalarTypeInfo("double");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.FLOAT)) {
            calculatedTypeInfo = new ScalarTypeInfo("float");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.LONG)) {
            calculatedTypeInfo = new ScalarTypeInfo("long");
        } else if (node.getPrimitiveTypeCode().equals(PrimitiveType.SHORT)) {
            calculatedTypeInfo = new ScalarTypeInfo("short");
        }
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        // We have to handle the array type case differently same as
        // in the FieldAccess.
        IBinding nodeBinding = node.resolveBinding();
        if (nodeBinding instanceof ITypeBinding) {
            calculatedTypeInfo = getTypeInfo((ITypeBinding) nodeBinding, null, null, visitorCalculatesSoftType);
        } else if (nodeBinding instanceof IVariableBinding) {
            ASTNode varDec = ASTNodeUtility.getDeclaringNode(nodeBinding);
            Type type = getTypeFromDeclarations(varDec);
            // If the declaring node is EnumConstantDeclaration,
            // We do not get any type object, for this type is null,
            // calculate type-info from type binding for this case.
            if (varDec != null && type != null && type.isParameterizedType()) {
                varDec.accept(this);
            } else {
                ITypeBinding typeBinding = ((IVariableBinding) nodeBinding).getType();
                calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
            }
        }
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(RequiresDirective node) {
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        return false;
    }

    @Override
    public boolean visit(SimpleName node) {
        // We have to handle the array type case differently same as
        // in the FieldAccess.
        IBinding nodeBinding = node.resolveBinding();
        if (nodeBinding == null) {
        	// For a static method call, the calling context may be a simple name
        	// But we do not get the binding for it (Arrays in the following case)
        	//    Arrays.copy(...)
        	// Get that info from matching with imports, etc.,
        	String typeFromImports = CallGraphUtility.getLibraryTypeQualifiedNameFromJSONData(node.getIdentifier(),
        			filePath);
			if (typeFromImports != null) {
				calculatedTypeInfo = new ClassTypeInfo(
						Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + typeFromImports);
			}
        } else if (nodeBinding instanceof ITypeBinding) {
            calculatedTypeInfo = getTypeInfo((ITypeBinding) nodeBinding, null, null, visitorCalculatesSoftType);
        } else if (nodeBinding instanceof IVariableBinding) {
            ASTNode varDec = ASTNodeUtility.getDeclaringNode(nodeBinding);
            ITypeBinding typeBinding = ((IVariableBinding) nodeBinding).getType();
            Type type = getTypeFromDeclarations(varDec);
            if (varDec != null && (type != null && type.isParameterizedType())) {
                varDec.accept(this);
            } else {
                if (typeBinding != null) {
                    calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
                } else {
                    calculatedTypeInfo = new ClassTypeInfo(Constants.LIB_TYPE_UNRESOLVED);
                }
            }
        }
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        return false;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        Type type = node.getType();
        ITypeBinding typeBinding = null;
        IVariableBinding nodeBinding = node.resolveBinding();
        if (nodeBinding != null) {
            typeBinding = nodeBinding.getType();
        }
        if (typeBinding != null && (!type.isParameterizedType() || typeBinding.isArray())) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
            if (node.isVarargs()) {
                if (calculatedTypeInfo instanceof ArrayTypeInfo) {
                    calculatedTypeInfo = new ArrayTypeInfo(
                            ((ArrayTypeInfo) calculatedTypeInfo).getDimension(),
                            ((ArrayTypeInfo) calculatedTypeInfo).getElementType(),
                            true);
                }
            }
        } else {
            calculatedTypeInfo = getTypeInfo(type, visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(StringLiteral node) {
        calculatedTypeInfo = new ScalarTypeInfo("String");
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        calculatedTypeInfo = new ScalarTypeInfo("void");
        return false;
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        IVariableBinding fieldBinding = node.resolveFieldBinding();
        // if superclass is from library, binding may be null
        if (fieldBinding != null) {
            ASTNode varDec = ASTNodeUtility.getDeclaringNode(fieldBinding);
            ITypeBinding fieldTypeBinding = fieldBinding.getType();
            if (varDec != null && !fieldTypeBinding.isArray()) {
                varDec.accept(this);
            } else {
                calculatedTypeInfo = getTypeInfo(fieldTypeBinding, null, null, visitorCalculatesSoftType);
            }
        }
        return false;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        int servicingMethodHashIndex = CallGraphUtility.getServicingMethodHashIndex(node, null, filePath, null);
        if (servicingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
            MethodIdentity identity = CallGraphDataStructures.getMatchingMethodIdentity(servicingMethodHashIndex);
            calculatedTypeInfo = identity.getReturnTypeInfo();
        }
        return false;
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        return false;
    }

    @Override
    public boolean visit(SwitchStatement node) {
        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        return false;
    }

    @Override
    public boolean visit(TagElement node) {
        return false;
    }

    @Override
    public boolean visit(TextElement node) {
        return false;
    }

    @Override
    public boolean visit(ThisExpression node) {
        // We do not get correct binding for ThisExpression used
        // inside the initializer block of anonymous class from JDT.
        // If a qualifier is present like 'Box.this', we can get the
        // type directly from the type binding of the qualifier 'Box'.
        // For other cases, get the type-info from the type binding of
        // the container class, enum, interface or anonymous class.
        Name qualifier = node.getQualifier();
        ITypeBinding typeBinding;
        if (qualifier != null) {
            typeBinding = qualifier.resolveTypeBinding();
        } else {
            typeBinding = CallGraphUtility.getContainerTypeBinding(node);
        }
        if (typeBinding != null) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
        return false;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        calculatedTypeInfo = getTypeInfo(node.resolveBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        calculatedTypeInfo = getTypeInfo(node);
        return false;
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        return false;
    }

    @Override
    public boolean visit(TypeParameter node) {
        calculatedTypeInfo = getTypeInfo(node.resolveBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(UnionType node) {
        calculatedTypeInfo = getTypeInfo(node.resolveBinding(), null, null, visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(UsesDirective node) {
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        // To handle the case where array is declared like,
        // int a[], String a[], etc., we use binding and for
        // other cases we use the direct Type because parameterized
        // types sometimes give us incorrect type info if taken via
        // type binding.
        ITypeBinding typeBinding = node.resolveTypeBinding();
        Type type = node.getType();
        if (typeBinding != null && ((type != null && !type.isParameterizedType()) || typeBinding.isArray())) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
        } else {
            calculatedTypeInfo = getTypeInfo(type, visitorCalculatesSoftType);
        }
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        calculatedTypeInfo = getTypeInfo(node.getType(), visitorCalculatesSoftType);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        // To handle the case where array is declared like,
        // int a[], String a[], etc., we use binding and for
        // other cases we use the direct Type because parameterized
        // types sometimes give us incorrect type info if taken via
        // type binding.
        ASTNode varDec = node.getParent();
        Type type = null;

        if (varDec instanceof VariableDeclarationExpression) {
            type = ((VariableDeclarationExpression) varDec).getType();
        } else if (varDec instanceof VariableDeclarationStatement) {
            type = ((VariableDeclarationStatement) varDec).getType();
        } else if (varDec instanceof FieldDeclaration) {
            type = ((FieldDeclaration) varDec).getType();
        }
        ITypeBinding typeBinding = null;
        IVariableBinding varBinding = node.resolveBinding();
        if (varBinding != null) {
            typeBinding = varBinding.getType();
        }
        if (typeBinding != null && ((type != null && !type.isParameterizedType()) || typeBinding.isArray())) {
            calculatedTypeInfo = getTypeInfo(typeBinding, null, null, visitorCalculatesSoftType);
        } else {
            if (type != null) {
                calculatedTypeInfo = getTypeInfo(type, visitorCalculatesSoftType);
            } else {
                calculatedTypeInfo = new ClassTypeInfo(Constants.LIB_TYPE_UNRESOLVED);
            }
        }
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        return false;
    }

    @Override
    public boolean visit(WildcardType node) {
        calculatedTypeInfo = getTypeInfo(node, visitorCalculatesSoftType);
        return false;
    }

    /**
     * Gets TypeInfo object for a type
     *
     * <p>This is the starting point of the recursive method (getAtomicTypeInfo) where the actual type will be created
     *
     * @param type the given type
     * @param calculateSoftType true if requires soft type info and we only calculate the type info based on the main
     *     class, false otherwise in which we get the proper type info in which the fields of a class are considered.
     * @return the TypeInfo of the given type
     */
    protected TypeInfo getTypeInfo(Type type, boolean calculateSoftType) {
        Pair<String, TypeInfo> typeInfoWithHash = getAtomicTypeInfo(type, null, null, true, calculateSoftType);
        if (typeInfoWithHash != null) {
            TypeInfo typeInfo = typeInfoWithHash.snd;
            return TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        }
        return null;
    }

    /**
     * Gets TypeInfo for a TypeBinding
     *
     * <p>This method additionally stores calculated type information in the call graph map
     *
     * <p>This is the starting point of the recursive method (getAtomicTypeInfo) where the actual type will be created
     *
     * @param typeBinding the binding of the type
     * @param typeBinding the binding of an object
     * @param containerTokenRange the token range for the container, if passed the class hash can be retrieved from the
     *     token range
     * @param containerHash container hash if already calculated, only calculated for
     * @param calculateSoftType true if requires soft type info and we only calculate the type info based on the main
     *     class, false otherwise in which we get the proper type info in which the fields of a class are considered.
     * @return the TypeInfo of the given binding
     */
    protected TypeInfo getTypeInfo(
            ITypeBinding typeBinding, TokenRange containerTokenRange, String containerHash, boolean calculateSoftType) {
        // Bound type of a type variable is calculated as a part
        // of type variable inside getAtomicTypeInfo method
        // So, Bound type should not reach here
        // set last argument to false
        Pair<String, TypeInfo> typeInfoWithHash =
                getAtomicTypeInfo(typeBinding, containerTokenRange, containerHash, true, calculateSoftType, false);
        if (typeInfoWithHash != null) {
            TypeInfo typeInfo = typeInfoWithHash.snd;
            return TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        }
        return null;
    }

    /**
     * For TypeLiteral like SomeClass.class, we are treating it like Class<SomeClass> being the correct type
     * info for our usage.
     *
     * @param typeLiteral a type literal/constant node
     * @return a parameterized type info like Class<T>
     */
    private TypeInfo getTypeInfo(TypeLiteral typeLiteral) {
        Type type = typeLiteral.getType();
        // For the type class, we only require the soft type,
        // No need for any more details
        TypeInfo typeInfo = getTypeInfo(type, true);

        ParameterizedTypeInfo parameterizedTypeInfo = new ParameterizedTypeInfo(
                Constants.LIB_TYPE + CallGraphUtility.CG_SEPARATOR + "java.lang.Class",
                1,
                List.of(typeInfo),
                false,
                Collections.emptyMap(),
                null,
                false);

        return TypeCalculator.putOrGetTypeInfoFromMemo(parameterizedTypeInfo);
    }

    /**
     * Returns the qualified name of an invoked method (typically for library methods without bindings).
     *
     * @param node the method invocation node
     * @return the qualified name of the invoked method
     */
    public String getQualifiedName(MethodInvocation node) {
        try {
            String targetClassName = null;
            String classQnameMaybe = null;
            if (node != null) {
                // If the expression is a simple name, it may be a static method call
                // In that case, the target class name is already there.
                // Note, this is a heuristic, it may not work in all cases
                // The heuristic does not work for most of the case,
                // which causes specinfo null
                // We already have the calling context type cache, try to retrive form there.
                TypeInfo callingContextType =
                        TypeCalculator.getCallingContextType(node.getExpression(), methodCallingContextCache);
                if (callingContextType != null) {
                    targetClassName = getActualTypeName(callingContextType);
                }
                String targetMethodName = node.getName().toString();
                int noOfParams = node.arguments().size();
                // We need package name, class name, method name and param count
                // to get FNDSpecInfo. We got class name from the expression
                // of the Method invocation. Method can can be get directly from
                // the invocation. To get the package name we will use some heuristic.
                // First look in the java.lang package as it needs not to be imported
                // If found, no need to look anymore
                FNDSpecInfo specInfo = null;
                if (targetClassName != null) {
                    specInfo = FNDSpecManager.getInfoFor("java.lang", targetClassName, targetMethodName, noOfParams);
                }
                if (specInfo == null) {
                    // Not found in the java.lang package
                    // Look for possible packages that are imported
                    CompilationUnit cu = ASTNodeUtility.findNearestAncestor(node, CompilationUnit.class);
                    @SuppressWarnings("unchecked")
                    List<ImportDeclaration> importsList = cu.imports();
                    for (ImportDeclaration imprt : importsList) {
                        String qualifiedClassName = imprt.getName().getFullyQualifiedName();
                        String packageName;
                        String className;
                        if (imprt.isOnDemand()) {
                            // For cases like java.io.*;
                            packageName = qualifiedClassName;
                            className = null;
                        } else {
                            int lastIndexOfDot = qualifiedClassName.lastIndexOf('.');
                            packageName = qualifiedClassName.substring(0, lastIndexOfDot);
                            className = qualifiedClassName.substring(lastIndexOfDot + 1);
                        }
                        // If a matching class name is found or there is a on demand import
                        // load the info for that package
                        if (className == null || className.equals(targetClassName)) {
                            specInfo = FNDSpecManager.getInfoFor(
                                    packageName, targetClassName, targetMethodName, noOfParams);
                            classQnameMaybe = qualifiedClassName;
                            if (specInfo != null) {
                                break;
                            }
                        } else if (Constants.DUMMY_TYPE_CLASS.equals(targetClassName)) {
                            // We don't get binding for external libraries in test cases
                            // So, we are creating `Dummy__OR__Class` in those cases
                            // As a result we are not getting any taint info from the json specs
                            // To over come this issue this shortcut is added
                            // Hopefully we will get binding in projects
                            specInfo = FNDSpecManager.getInfoFor(packageName, className, targetMethodName, noOfParams);
                            if (specInfo != null) {
                                break;
                            }
                        }
                    }
                }

                if (specInfo == null) {
                    // If any relevant package name has not been retrieved yet,
                    // look for possible package names for target class in java doc
                    Set<String> packages = C2PManager.getPackages(targetClassName);
                    for (String packageName : packages) {
                        specInfo =
                                FNDSpecManager.getInfoFor(packageName, targetClassName, targetMethodName, noOfParams);
                        if (specInfo != null) {
                            break;
                        }
                    }
                }

                if (specInfo == null) {
                    if (classQnameMaybe != null) {
                        return classQnameMaybe + "." + targetMethodName;
                    }
                    return null;
                } else {
                    return specInfo.getPackageName() + "." + specInfo.getDeclaringType() + "."
                            + specInfo.getMethodName();
                }
            }
        } catch (Exception e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Get type info for a library method call
     *
     * @param node the method invocation node
     * @return typeInfo found in the json spec
     */
    private TypeInfo getTypeInfoForLibraryMethod(MethodInvocation node) {
        try {
            String targetClassName = null;
            if (node != null) {
                // If the expression is a simple name, it may be a static method call
                // In that case, the target class name is already there.
                // Note, this is a heuristic, it may not work in all cases
                // The heuristic does not work for most of the case,
                // which causes specinfo null
                // We already have the calling context type cache, try to retrive form there.
                TypeInfo callingContextType =
                        TypeCalculator.getCallingContextType(node.getExpression(), methodCallingContextCache);
                if (callingContextType != null) {
                    targetClassName = getActualTypeName(callingContextType);
                }
                String targetMethodName = node.getName().toString();
                int noOfParams = node.arguments().size();
                // We need package name, class name, method name and param count
                // to get FNDSpecInfo. We got class name from the expression
                // of the Method invocation. Method can can be get directly from
                // the invocation. To get the package name we will use some heuristic.
                // First look in the java.lang package as it needs not to be imported
                // If found, no need to look anymore
                FNDSpecInfo specInfo = null;
                if (targetClassName != null) {
                    specInfo = FNDSpecManager.getInfoFor("java.lang", targetClassName, targetMethodName, noOfParams);
                }
                if (specInfo == null) {
                    // Not found in the java.lang package
                    // Look for possible packages that are imported
                    CompilationUnit cu = ASTNodeUtility.findNearestAncestor(node, CompilationUnit.class);
                    @SuppressWarnings("unchecked")
                    List<ImportDeclaration> importsList = cu.imports();
                    for (ImportDeclaration imprt : importsList) {
                        String qualifiedClassName = imprt.getName().getFullyQualifiedName();
                        String packageName;
                        String className;
                        if (imprt.isOnDemand()) {
                            // For cases like java.io.*;
                            packageName = qualifiedClassName;
                            className = null;
                        } else {
                            int lastIndexOfDot = qualifiedClassName.lastIndexOf('.');
                            packageName = qualifiedClassName.substring(0, lastIndexOfDot);
                            className = qualifiedClassName.substring(lastIndexOfDot + 1);
                        }
                        // If a matching class name is found or there is a on demand import
                        // load the info for that package
                        if (className == null || className.equals(targetClassName)) {
                            specInfo = FNDSpecManager.getInfoFor(
                                    packageName, targetClassName, targetMethodName, noOfParams);
                            if (specInfo != null) {
                                break;
                            }
                        } else if (Constants.DUMMY_TYPE_CLASS.equals(targetClassName)) {
                            // We don't get binding for external libraries in test cases
                            // So, we are creating `Dummy__OR__Class` in those cases
                            // As a result we are not getting any taint info from the json specs
                            // To over come this issue this shortcut is added
                            // Hopefully we will get binding in projects
                            specInfo = FNDSpecManager.getInfoFor(packageName, className, targetMethodName, noOfParams);
                            if (specInfo != null) {
                                break;
                            }
                        }
                    }
                }

                if (specInfo == null) {
                    // If any relevant package name has not been retrieved yet,
                    // look for possible package names for target class in java doc
                    Set<String> packages = C2PManager.getPackages(targetClassName);
                    for (String packageName : packages) {
                        specInfo =
                                FNDSpecManager.getInfoFor(packageName, targetClassName, targetMethodName, noOfParams);
                        if (specInfo != null) {
                            break;
                        }
                    }
                }

                if (specInfo == null) {
                    return null;
                } else {
                    // If the return type is a symbolic type (i.e. K, U, etc)
                    // and the calling context location is a parameterized type
                    // we need to infer the actual type of the return type from
                    //                    TypeInfo retTypeInfo =
                    // AntlrUtil.parseAndReturnTypeInfo(specInfo.getReturnType());
                    //                    if (retTypeInfo instanceof SymbolicTypeInfo || retTypeInfo instanceof
                    // ParameterizedTypeInfo) {
                    //                        List<ASTNode> args = node.arguments();
                    //                        List<TypeInfo> locFrameTypeInfos = new ArrayList<>(args.size() + 1);
                    //                        locFrameTypeInfos.add(callingContextType);
                    //                        for(int i=0; i<args.size(); i++) {
                    //                            locFrameTypeInfos.add(TypeCalculator.typeOf(args.get(i), false));
                    //                        }
                    //                        retTypeInfo = TypeCalculator.checkAndReplaceSymbolicType(retTypeInfo,
                    // specInfo, locFrameTypeInfos);
                    //                    }
                    //                    return retTypeInfo;
                }
            }
        } catch (Exception e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Gets the Type of the variable declaration node for type resolution decisions.
     *
     * @param node the variable/field declaration node
     * @return the Type object of the type of this variable/field
     */
    private Type getTypeFromDeclarations(ASTNode node) {
        Type type = null;
        if (node instanceof VariableDeclarationFragment) {
            ASTNode varDec = node.getParent();
            if (varDec instanceof VariableDeclarationExpression) {
                type = ((VariableDeclarationExpression) varDec).getType();
            } else if (varDec instanceof VariableDeclarationStatement) {
                type = ((VariableDeclarationStatement) varDec).getType();
            } else if (varDec instanceof FieldDeclaration) {
                type = ((FieldDeclaration) varDec).getType();
            }
        } else if (node instanceof VariableDeclarationExpression) {
            type = ((VariableDeclarationExpression) node).getType();
        } else if (node instanceof SingleVariableDeclaration) {
            type = ((SingleVariableDeclaration) node).getType();
        } else if (node instanceof VariableDeclarationStatement) {
            type = ((VariableDeclarationStatement) node).getType();
        } else if (node instanceof FieldDeclaration) {
            type = ((FieldDeclaration) node).getType();
        }
        return type;
    }

    /**
     * Resolves the actual type info by matching raw types with type parameters.
     *
     * @param rawType the raw field type
     * @param actualType actual type of the argument
     * @param typeParam the raw type parameter of a class
     * @return the resolved TypeInfo
     */
    private TypeInfo resolveActualTypeInfo(Type rawType, TypeInfo actualType, TypeParameter typeParam) {
        if (rawType.isParameterizedType()) {
            Pair<String, TypeInfo> argTypeInfoWithHash = getAtomicTypeInfo(rawType, null, null, false, true);
            return matchElementType((ParameterizedTypeInfo) argTypeInfoWithHash.snd, rawType, actualType, typeParam);
        } else {
            if (typeParam.getName().getIdentifier().equals(rawType.toString())) {
                return actualType;
            } else {
                return null;
            }
        }
    }

    /**
     * Resolves the actual type info for a previously created parameterized type field.
     *
     * @param paramTypeInfo the previously created ParameterizedTypeInfo of the field
     * @param rawType the raw field type
     * @param actualType actual type of the argument
     * @param typeParam the raw type parameter
     * @return the resolved TypeInfo
     */
    private TypeInfo resolveActualTypeInfo(
            ParameterizedTypeInfo paramTypeInfo, Type rawType, TypeInfo actualType, TypeParameter typeParam) {
        if (rawType.isParameterizedType()) {
            return matchElementType(paramTypeInfo, rawType, actualType, typeParam);
        } else {
            if (typeParam.getName().getIdentifier().equals(rawType.toString())) {
                return actualType;
            } else {
                return null;
            }
        }
    }

    /**
     * Helper method to resolve actual type info by matching element types.
     *
     * @param paramTypeInfo the previously created ParameterizedTypeInfo
     * @param rawType the raw field type
     * @param actualType actual type of the argument
     * @param typeParam the raw type parameter
     * @return the paramTypeInfo if found, null otherwise
     */
    private TypeInfo matchElementType(
            ParameterizedTypeInfo paramTypeInfo, Type rawType, TypeInfo actualType, TypeParameter typeParam) {
        boolean isFound = false;
        for (int i = 0; i < paramTypeInfo.getElementTypeSize(); i++) {
            if (paramTypeInfo
                    .getElementTypes()
                    .get(i)
                    .toString()
                    .equals(typeParam.getName().getIdentifier())) {
                isFound = true;
            } else if (paramTypeInfo.getElementTypes().get(i) instanceof ParameterizedTypeInfo) {
                Type elemType =
                        (Type) ((ParameterizedType) rawType).typeArguments().get(i);
                TypeInfo elemTypeInfo = resolveActualTypeInfo(elemType, actualType, typeParam);
                if (elemTypeInfo != null) {
                    isFound = true;
                }
            }
        }

        if (isFound) {
            return paramTypeInfo;
        } else {
            return null;
        }
    }

    /**
     * Gets TypeInfo of a type from Type AST node.
     *
     * @param type the Type AST node
     * @param containerTokenRange the token range for the container (can be null)
     * @param containerHash the container hash if already calculated (can be null)
     * @param isCalculatingContainerType flag for container type calculation
     * @param calculateSoftType true for soft type info, false for proper type info
     * @return pair containing hash and TypeInfo
     */
    private Pair<String, TypeInfo> getAtomicTypeInfo(
            Type type,
            TokenRange containerTokenRange,
            String containerHash,
            boolean isCalculatingContainerType,
            boolean calculateSoftType) {
        ITypeBinding typeBinding = type.resolveBinding();
        String hash = null;

        if (containerTokenRange != null) {
            // See if the provided token range or class hash can get the info
            hash = CallGraphDataStructures.getClassHashFromTokenRange(containerTokenRange);
            if (hash == null) {
                hash = containerHash;
            }
        }

        if (hash == null) {
            if (CallGraphUtility.isRecovered(typeBinding)) {
                hash = CallGraphUtility.getLibraryTypeHash(typeBinding, filePath);
            } else {
                // When binding is null, get information from the imports
                // associated with the file
                if (typeBinding != null) {
                    hash = CallGraphUtility.getClassHashFromTypeBinding(typeBinding, null, filePath);
                } else {
                    hash = Constants.LIB_TYPE
                            + CallGraphUtility.CG_SEPARATOR
                            + CallGraphUtility.getLibraryTypeQualifiedNameFromJSONData(type.toString(), filePath);
                }
            }
        }

        if (hash != null) {
            TypeInfo storedInfo = ((calculateSoftType || !isCalculatingContainerType)
                    ? CallGraphDataStructures.getSoftTypeInfoFromClassId(hash)
                    : CallGraphDataStructures.getProperTypeInfoFromClassId(hash));
            if (storedInfo != null) {
                return Pair.of(hash, storedInfo);
            }
        }
        TypeInfo typeInfo = null;

        if (type.isPrimitiveType()) {
            typeInfo = new ScalarTypeInfo(type.toString());
        } else if (type.isArrayType()) {
            Pair<String, TypeInfo> typeInfoWithHash =
                    getAtomicTypeInfo(((ArrayType) type).getElementType(), null, null, false, calculateSoftType);

            if (typeInfoWithHash != null && typeBinding != null) {
                typeInfo = new ArrayTypeInfo(typeBinding.getDimensions(), typeInfoWithHash.snd, false);
            }
        } else if (type.isParameterizedType() || (typeBinding != null && typeBinding.isRawType())) {
            // Either the type is parameterized as in Pair<Integer, Integer>
            // or it may be a raw type as in Pair<>. In the latter case
            // JDT does not identify as a parameterized type, hence the or part is needed
            //
            // If the type object is not parameterized (yet, definitely Raw type)
            // of a parameterized type, we take the TypeInfo from binding rather
            // than trying to get it from Type and not knowing which type we are actually
            // working with.
            // If a parameterized type is the container, then we calculate its fields if asked
            // If a parameterized type is a field, then we have a soft type for it
            if (type instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) type;
                TypeDeclaration typeDec = null;
                List<TypeInfo> elementTypes = new ArrayList<>();
                Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields = new HashMap<>();
                Map<Integer, String> typeArgsToFields = new HashMap<>();
                @SuppressWarnings("unchecked")
                List<Type> typeArgs = paramType.typeArguments();

                if (typeArgs.isEmpty()) {
                    // If the type args is empty
                    // That means it is a diamond operator (new ArrayList<>) case
                    // So, we need to infer the type arguments
                    List<TypeInfo> inferredType =
                            inferTypeArguments(paramType, containerTokenRange, containerHash, calculateSoftType);
                    // If the inferred type and the current type has different number of type
                    // parameters, then we will not used the inferred type.
                    // Example:
                    //      class Foo<U, V> implements Bar<U> { }
                    //  Class 'Foo' used in 'buzz' method:
                    //      Bar<U> buzz() { return new Foo<>();}
                    // For, 'new Foo<>()' the inferred type will be 'Bar<U>' whereas the type from
                    // binding is 'Foo<U, V>'. For cases like this, we will skip the type inference
                    // and use the type directly from type binding.
                    // If the element count in the inferred type and in the current type's binding
                    // is the same, then we will assume the inferred type to be valid and use that.
                    // Otherwise, we will use the element count from current type's binding and use
                    // 'java.lang.Object' as the elements of this parameterized type.
                    int typeElementCount = 0;
                    if (typeBinding != null) {
                        // First try to resolve type parameters if the binding is
                        // of a class or interface type binding
                        typeElementCount = typeBinding.getTypeParameters().length;
                        // If not resolved, then resolve the type arguments.
                        if (typeElementCount == 0) {
                            typeElementCount = typeBinding.getTypeArguments().length;
                        }
                        // If still unresolved, means JDT has failed to resolve the
                        // type parameters. We will try to get the actual type declaration node of
                        // this type binding and get the type parameters from that.
                        if (typeElementCount == 0) {
                            typeDec = (TypeDeclaration) ASTNodeUtility.getDeclaringNode(typeBinding);
                            if (typeDec != null) {
                                typeElementCount = typeDec.typeParameters().size();
                            }
                        }
                    }
                    if (typeElementCount != 0) {
                        // If inferred type and binding has same type params we will use the
                        // inferred type's elements.
                        if (inferredType.size() == typeElementCount) {
                            elementTypes.addAll(inferredType);
                        } else {
                            // Nothing to do. Use 'java.lang.Object' as the elements of this
                            // parameterized type.
                            TypeInfo objectClassTypeInfo = new ClassTypeInfo(Constants.JAVA_LANG_OBJECT);
                            for (int i = 0; i < typeElementCount; i++) {
                                elementTypes.add(objectClassTypeInfo);
                            }
                        }
                    } else {
                        // Either the type binding is null or, the resolved typed parameter list is
                        // empty.
                        // We will use the inferred type's elements.
                        elementTypes.addAll(inferredType);
                    }
                }

                // We are calculating type args for parameterized types
                // irrespective of soft, proper or container type
                // This is done to provide better type info for various
                // refactorings and to facilitate proper horizon expansion
                // and method resolution whenever necessary
                if (typeBinding != null && !typeBinding.isFromSource()) {
                    // Parameterized type is a library type
                    // In this case, for each of the param's,
                    // we will create a dummy field
                    // So, List<T> will have one type
                    // Map<K,V> will have two types
                    String dummy = Constants.dummyFieldPrefix;
                    int i = 1;
                    if (typeArgs.isEmpty()) {
                        // type args is empty
                        // Case for the <> operator
                        // Use the inferred type arguments
                        for (TypeInfo elementType : elementTypes) {
                            String fieldName = typeBinding.getName() + Constants.FIELDNAME_SEPARATOR + dummy + i;
                            fields.put(fieldName, Pair.of(Pair.of(null, null), elementType));
                            typeArgsToFields.put(i - 1, fieldName);
                            i++;
                        }
                    } else {
                        for (Type typeArg : typeArgs) {
                            Pair<String, TypeInfo> argTypeInfoWithHash =
                                    getAtomicTypeInfo(typeArg, null, null, false, true);
                            if (argTypeInfoWithHash != null) {
                                elementTypes.add(argTypeInfoWithHash.snd);
                                String fieldName =
                                        typeBinding.getName() + Constants.FIELDNAME_SEPARATOR + dummy + i;
                                fields.put(fieldName, Pair.of(Pair.of(null, null), argTypeInfoWithHash.snd));
                                typeArgsToFields.put(i - 1, fieldName);
                                i++;
                            }
                        }
                    }
                } else {
                    for (Type typeArg : typeArgs) {
                        Pair<String, TypeInfo> argTypeInfoWithHash =
                                getAtomicTypeInfo(typeArg, null, null, false, true);
                        if (argTypeInfoWithHash != null) {
                            elementTypes.add(argTypeInfoWithHash.snd);
                        }
                    }
                }

                if (!calculateSoftType && isCalculatingContainerType) {
                    Pair<Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>>, Map<Integer, String>> res = null;
                    if (typeBinding != null) {
                        res = getFieldsFromParameterizedType(hash, typeBinding, elementTypes, typeDec);
                    }

                    if (res != null) {
                        if (res.fst != null) {
                            fields.putAll(res.fst);
                        }
                        if (res.snd != null) {
                            typeArgsToFields.putAll(res.snd);
                        }
                    }
                }

                // If this class is an inner class, then set the isInner
                // of this TypeInfo to true
                if (typeBinding != null) {
                    typeInfo = new ParameterizedTypeInfo(
                            hash,
                            elementTypes.size(),
                            elementTypes,
                            typeBinding.isFromSource(),
                            fields,
                            typeArgsToFields,
                            typeBinding.isNested());
                } else {
                    typeInfo = new ParameterizedTypeInfo(
                            hash, elementTypes.size(), elementTypes, false, fields, typeArgsToFields, false);
                }
            } else {
                // Calculating type info for raw type,
                //    as in List
                // Just calculate the type in this case
                // For a raw type, we must be calculating soft type
                // A raw type is never at the top level
                if (typeBinding != null && typeBinding.isFromSource()) {
                    // If it is a raw type but the type comes from source,
                    // we will try to retrieve the fields
                    // The element type size passed is 0 since
                    // it is a raw type Foo
                    // and it has no elements
                    Pair<Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>>, Map<Integer, String>> res =
                            getFieldsFromParameterizedType(hash, typeBinding, Collections.emptyList(), null);
                    typeInfo = new ParameterizedTypeInfo(
                            hash,
                            0,
                            Collections.emptyList(),
                            typeBinding.isFromSource(),
                            res.fst,
                            Collections.emptyMap(),
                            typeBinding.isNested());
                } else {
                    // We are calculating type info of a raw type,
                    // As raw types don't have bounds, set the last
                    // argument to false
                    Pair<String, TypeInfo> typeInfoWithHash =
                            getAtomicTypeInfo(typeBinding, null, null, false, true, false);
                    if (typeInfoWithHash != null) {
                        typeInfo = typeInfoWithHash.snd;
                    }
                }
            }
        } else if (type.isWildcardType()) {
            // Populate bound type of this wild card type
            Type boundType = ((WildcardType) type).getBound();
            if (boundType == null) {
                typeInfo = new WildCardTypeInfo();
            } else {
                Pair<String, TypeInfo> boundTypeInfoPair = getAtomicTypeInfo(boundType, null, null, false, true);
                if (boundTypeInfoPair != null) {
                    typeInfo = new WildCardTypeInfo(boundTypeInfoPair.snd, ((WildcardType) type).isUpperBound());
                } else {
                    typeInfo = new WildCardTypeInfo();
                }
            }
        } else if (ScalarTypeInfo.Scalars.isBoxedScalar(type.toString())) {
            typeInfo = new ScalarTypeInfo(type.toString());
        } else if (typeBinding != null && typeBinding.isTypeVariable()) {
            // Populate bound type of this symbolic type
            // Symbolic type can not have lower bound
            // But it may have multiple upper bounds
            // In that case the upper bounds form an intersection type ( A & B...)
            ITypeBinding[] upperBounds = typeBinding.getTypeBounds();
            List<TypeInfo> bounds = new ArrayList<>();
            for (ITypeBinding boundBinding : upperBounds) {
                // We are calculating bound type for the type variable.
                // For bound types we don't need type arguments (if there is any)
                // so, set last argument to true so that type arguments are not
                // calculated for a parameterized type bound
                // This is done to resolve the recursive type calculation
                // in cases like `T extends Comparable<T>`
                // which was causing stack overflow as we were trying to
                // calculate type info for `T`.
                Pair<String, TypeInfo> boundTypeInfo = getAtomicTypeInfo(
                        boundBinding, containerTokenRange, containerHash, isCalculatingContainerType, true, true);
                if (boundTypeInfo != null) {
                    bounds.add(boundTypeInfo.snd);
                }
            }
            if (bounds.isEmpty()) {
                typeInfo = new SymbolicTypeInfo(typeBinding.getName());
            } else {
                typeInfo = new SymbolicTypeInfo(typeBinding.getName(), bounds);
            }
        } else if (typeBinding != null && typeBinding.isEnum()) {
            typeInfo = new EnumTypeInfo(hash);
        } else if (typeBinding != null) {
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields = null;
            if (!calculateSoftType && isCalculatingContainerType) {
                fields = getFieldsFromClassType(hash);
            }
            if (fields == null) {
                fields = Collections.emptyMap();
            }
            typeInfo = new ClassTypeInfo(hash, fields, typeBinding.isInterface(), typeBinding.isNested());
        }

        // Null value for typeInfo causes crash
        // Try to find the typeInfo from antlr
        // If still null, create an ClassTypeInfo of type Object.
        if (typeInfo == null) {
            //            typeInfo = AntlrUtil.parseAndReturnTypeInfo(type.toString());
        }
        if (typeInfo == null) {
            typeInfo = new ClassTypeInfo(Constants.JAVA_LANG_OBJECT);
        }
        return Pair.of(hash, TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo));
    }

    /**
     * Gets TypeInfo of a type from ITypeBinding.
     *
     * @param typeBinding the type binding for the type
     * @param containerTokenRange the token range for the container (can be null)
     * @param containerHash the container hash if already calculated (can be null)
     * @param isCalculatingContainerType flag for container type calculation
     * @param calculateSoftType true for soft type info, false for proper type info
     * @param calculatingBoundType true if calculating bound type for a type variable
     * @return pair containing hash and TypeInfo
     */
    private Pair<String, TypeInfo> getAtomicTypeInfo(
            ITypeBinding typeBinding,
            TokenRange containerTokenRange,
            String containerHash,
            boolean isCalculatingContainerType,
            boolean calculateSoftType,
            boolean calculatingBoundType) {
        if (typeBinding == null) {
            return null;
        }
        String hash = null;
        if (containerTokenRange != null) {
            // See if the provided token range or class hash can get the info
            hash = CallGraphDataStructures.getClassHashFromTokenRange(containerTokenRange);
            if (hash == null) {
                hash = containerHash;
            }
        }

        if (hash == null) {
            if (CallGraphUtility.isRecovered(typeBinding)) {
                hash = CallGraphUtility.getLibraryTypeHash(typeBinding, filePath);
            } else {
                hash = CallGraphUtility.getClassHashFromTypeBinding(typeBinding, null, filePath);
            }
        }

        if (hash != null) {
            TypeInfo storedInfo = ((calculateSoftType || !isCalculatingContainerType)
                    ? CallGraphDataStructures.getSoftTypeInfoFromClassId(hash)
                    : CallGraphDataStructures.getProperTypeInfoFromClassId(hash));
            if (storedInfo != null) {
                return Pair.of(hash, storedInfo);
            }
        }
        TypeInfo typeInfo = null;

        if (typeBinding.isPrimitive()) {
            // For a primitive type, there is no need to capture fields
            // So, soft and proper types are the same
            typeInfo = new ScalarTypeInfo(typeBinding.getName());
        } else if (typeBinding.isTypeVariable()) {
            // For a symbolic type, there is no need to capture fields
            // So, soft and proper types are the same
            // Populate bound type of this symbolic type
            // Symbolic type can not have lower bound
            // But it may have multiple upper bounds
            // In that case the upper bounds form an intersection type ( A & B...)
            ITypeBinding[] upperBounds = typeBinding.getTypeBounds();
            List<TypeInfo> bounds = new ArrayList<>();
            for (ITypeBinding boundBinding : upperBounds) {
                // We are calculating bound type for the type variable.
                // For bound types we don't need type arguments (if there is any)
                // so, set last argument to true so that type arguments are not
                // calculated for a parameterized type bound
                // This is done to resolve the recursive type calculation
                // in cases like `T extends Comparable<T>`
                // which was causing stack overflow as we were trying to
                // calculate type info for `T`.
                Pair<String, TypeInfo> boundTypeInfo = getAtomicTypeInfo(
                        boundBinding, containerTokenRange, containerHash, isCalculatingContainerType, true, true);
                if (boundTypeInfo != null) {
                    bounds.add(boundTypeInfo.snd);
                }
            }
            if (bounds.isEmpty()) {
                typeInfo = new SymbolicTypeInfo(typeBinding.getName());
            } else {
                typeInfo = new SymbolicTypeInfo(typeBinding.getName(), bounds);
            }
        } else if (typeBinding.isCapture()) {
            // Previously we did not get proper typeInfo for captured type binding.
            // For this we will get the type from the erasure.
            ITypeBinding typeErasure = typeBinding.getErasure();
            // We are calculating bound type for the capture.
            // For bound types we don't need type arguments (if there is any)
            // so, set last argument to true so that type arguments are not
            // calculated for a parameterized type bound
            // This is done to resolve the recursive type calculation
            // in cases where cyclic inheritence relation exist for type parameter
            // like `Comparable<T extends Comparable<T>>`
            // When we get Comparable<?>, as the erasure for the {capture# of ?}
            // we get again the Comparable<?> which was causing stack overflow.
            Pair<String, TypeInfo> typeInfoWithHash = getAtomicTypeInfo(
                    typeErasure, containerTokenRange, containerHash, isCalculatingContainerType, true, true);
            if (typeInfoWithHash != null) {
                typeInfo = typeInfoWithHash.snd;
            }
        } else if (typeBinding.isArray()) {
            // An array type can never be at the top level
            // The type of the array element is always a soft type
            // We are calculating type info for the element type of an array.
            // Array elements don't have bounds, so, set last argument to false
            Pair<String, TypeInfo> typeInfoWithHash =
                    getAtomicTypeInfo(typeBinding.getElementType(), null, null, false, calculateSoftType, false);

            if (typeInfoWithHash != null) {
                typeInfo = new ArrayTypeInfo(typeBinding.getDimensions(), typeInfoWithHash.snd, false);
            }
        } else if (typeBinding.isParameterizedType() || typeBinding.isGenericType() || typeBinding.isRawType()) {
            // If a parameterized type is the container, then we calculate its fields if asked
            // If a parameterized type is a field of that, then we have a soft type
            List<TypeInfo> elementTypes = new ArrayList<>();
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields = new HashMap<>();
            Map<Integer, String> typeArgsToFields = new HashMap<>();
            ITypeBinding[] typeArgs;
            if (typeBinding.isGenericType()) {
                typeArgs = typeBinding.getTypeParameters();
            } else {
                typeArgs = typeBinding.getTypeArguments();
            }

            // We don't need type arguments for type bounds of type variables
            if (!calculatingBoundType) {
                // We are calculating type args for parameterized types
                // irrespective of soft, proper or container type
                // This is done to provide better type info for various
                // refactorings and to facilitate proper horizon expansion
                // and method resolution whenever necessary
                if (!typeBinding.isFromSource()) {
                    // Parameterized type is a library type
                    // In this case, for each of the param's,
                    // we will create a dummy field
                    // So, List<T> will have one type
                    // Map<K,V> will have two types
                    String dummy = Constants.dummyFieldPrefix;
                    int i = 1;
                    for (ITypeBinding typeArg : typeArgs) {
                        // We are calculating type info for a type argument of a parameterized type here,
                        // so, we don't need to worry about bound type here
                        // Set the last argument to false.
                        // If the type argument is a type variable it will be
                        // handled in the getAtomicTypeInfo method
                        Pair<String, TypeInfo> argTypeInfoWithHash =
                                getAtomicTypeInfo(typeArg, null, null, false, calculateSoftType, false);
                        if (argTypeInfoWithHash != null) {
                            elementTypes.add(argTypeInfoWithHash.snd);
                            String fieldName = typeBinding.getName() + Constants.FIELDNAME_SEPARATOR + dummy + i;
                            fields.put(fieldName, Pair.of(Pair.of(null, null), argTypeInfoWithHash.snd));
                            typeArgsToFields.put(i - 1, fieldName);
                            i++;
                        }
                    }
                } else {
                    for (ITypeBinding typeArg : typeArgs) {
                        // We are calculating type info for a type argument of a parameterized type here,
                        // so, we don't need to worry about bound type here
                        // Set the last argument to false.
                        // If the type argument is a type variable it will be
                        // handled in the getAtomicTypeInfo method
                        Pair<String, TypeInfo> argTypeInfoWithHash =
                                getAtomicTypeInfo(typeArg, null, null, false, calculateSoftType, false);
                        if (argTypeInfoWithHash != null) {
                            elementTypes.add(argTypeInfoWithHash.snd);
                        }
                    }
                }
            }

            if (!calculateSoftType && isCalculatingContainerType) {
                Pair<Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>>, Map<Integer, String>> res =
                        getFieldsFromParameterizedType(hash, typeBinding, elementTypes, null);

                if (res != null) {
                    if (res.fst != null) {
                        fields.putAll(res.fst);
                    }
                    if (res.snd != null) {
                        typeArgsToFields.putAll(res.snd);
                    }
                }
            }

            // If this class is an inner class, then set the isInner
            // of this TypeInfo to true
            typeInfo = new ParameterizedTypeInfo(
                    hash,
                    elementTypes.size(),
                    elementTypes,
                    typeBinding.isFromSource(),
                    fields,
                    typeArgsToFields,
                    typeBinding.isNested());
        } else if (typeBinding.isWildcardType()) {
            // For a wild card type, there is no need to capture fields
            // So, soft and proper types are the same
            // Populate bound type of this wild card type
            ITypeBinding boundType = typeBinding.getBound();
            if (boundType == null) {
                typeInfo = new WildCardTypeInfo();
            } else {
                // Calculating bound type for wild card type, so, we don't need type info of
                // type arguments if the bound type is a parameterized type.
                // So, set last argument to true
                Pair<String, TypeInfo> boundTypeInfoPair = getAtomicTypeInfo(boundType, null, null, false, true, false);
                if (boundTypeInfoPair != null) {
                    typeInfo = new WildCardTypeInfo(boundTypeInfoPair.snd, typeBinding.isUpperbound());
                } else {
                    typeInfo = new WildCardTypeInfo();
                }
            }
        } else if (ScalarTypeInfo.Scalars.isBoxedScalar(typeBinding.getName())) {
            // For a boxed type, there is no need to capture fields
            // So, soft and proper types are the same
            typeInfo = new ScalarTypeInfo(typeBinding.getName());
        } else if (typeBinding.isEnum()) {
            typeInfo = new EnumTypeInfo(hash);
        } else {
            // If a class type is the container, then we calculate its fields if asked
            // If a class type is a field, then we have a soft type for it
            Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fields = null;
            if (!calculateSoftType && isCalculatingContainerType) {
                fields = getFieldsFromClassType(hash);
            }
            if (fields == null) {
                fields = Collections.emptyMap();
            }
            typeInfo = new ClassTypeInfo(hash, fields, typeBinding.isInterface(), typeBinding.isNested());
        }

        return Pair.of(hash, TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo));
    }

    /**
     * Finds all fields of a class with their token range, type info and declared class bit index.
     *
     * @param classHash the class's hash whose fields are sought
     * @return map of field names to field information tuples
     */
    private Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> getFieldsFromClassType(String classHash) {
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fieldsMap = new HashMap<>();
        // For a instance of type java.lang.Thread, we add a special field
        // which points to the location of the target runnable instance.
        if (classHash.equals(Constants.JAVA_LANG_THREAD)) {
            TypeInfo runnableType = new ClassTypeInfo(Constants.JAVA_LANG_RUNNABLE);
            Integer classIndex = CallGraphDataStructures.getClassHashToBitMap().get(classHash);
            fieldsMap.put(Constants.THREAD_RUNNABLE_FIELD, Pair.of(Pair.of(null, classIndex), runnableType));
            return fieldsMap;
        }
        if (CallGraphDataStructures.containsFieldsForContainer(classHash)) {
            List<TokenRange> fields = CallGraphDataStructures.getFieldsFromClassHash(classHash);

            // Find what kind of type is needed for this class's/enum's fields
            for (TokenRange fieldRange : fields) {
                FieldInfo fieldInfo = CallGraphDataStructures.getFieldInfoFromTokenRange(fieldRange);
                if (fieldInfo != null && fieldInfo.isStatic()) {
                    // We do not put static fields in a type
                    // They are handled differently
                    continue;
                }

                // Try to get the field info from cg
                // It helps reducing ast loading
                if (fieldInfo != null && fieldInfo.getFieldTypeInfo() != null) {
                    String declaringClasshash = fieldInfo.getContainerClassHash();
                    TypeInfo storedFieldType = fieldInfo.getFieldTypeInfo();
                    // Say we have a field that is a parameterized type in a parameterized class
                    //     public abstract class Y<T extends Enum<T>> {
                    //        protected final Map<T, String> configMap;
                    //     }
                    // Now the field's type will be stored as a parameter
                    // But now we make be looking for the field in a class that extends the parameterized class
                    // and resolves the parameter.
                    //     public class Z extends Y<TT>
                    // In this case, we can resolve the symbolic type (T extends Enum<T>)
                    // with a concrete type (TT).
                    // So we check if the conditions are met, we are calling from a type that is
                    // not the original declaring container type of the field and the
                    // field has symbolic parameters.
                    if (!classHash.equals(declaringClasshash)
                            && storedFieldType instanceof ParameterizedTypeInfo
                            && storedFieldType.needsReplacement()) {
                        TypeInfo modType = TypeCalculator.getReplacementForSymbolicTypeByMatchingTypesFrom(
                                storedFieldType,
                                CallGraphDataStructures.getSoftTypeInfoFromClassId(classHash),
                                declaringClasshash,
                                null);
                        if (modType != null) {
                            // The symbolic/wild card type is replaced. So use it.
                            storedFieldType = modType;
                        }
                    }
                    String fieldNameWithContainer = CallGraphUtility.getClassNameFromClassHash(declaringClasshash)
                            + Constants.FIELDNAME_SEPARATOR
                            + fieldInfo.getFieldName();
                    if (!fieldsMap.containsKey(fieldNameWithContainer)) {
                        fieldsMap.put(
                                fieldNameWithContainer,
                                Pair.of(
                                        Pair.of(
                                                fieldRange,
                                                CallGraphDataStructures.getBitIndexFromClassHash(declaringClasshash)),
                                        storedFieldType));
                    }
                } else {
                    SimpleName fieldName = ASTNodeUtility.getASTNodeFromTokenRange(fieldRange, SimpleName.class);
                    if (fieldName == null) {
                        // We do not handle field names being null
                        continue;
                    }
                    IVariableBinding field = (IVariableBinding) fieldName.resolveBinding();
                    FieldDeclaration fieldDec = ASTNodeUtility.findNearestAncestor(fieldName, FieldDeclaration.class);
                    if (field != null && fieldDec != null) {
                        ITypeBinding fieldTypeFromBinding = field.getType();
                        ITypeBinding declaringType = field.getDeclaringClass();
                        String declaringClassHash =
                                CallGraphUtility.getClassHashFromTypeBinding(declaringType, null, filePath);
                        int declaringTypeIndex = CallGraphDataStructures.getBitIndexFromClassHash(declaringClassHash);
                        Type fieldType = fieldDec.getType();

                        // In cases of parameterized library types, JDT occasionally finds the
                        // type without the parameter part and therefore considers it as a class type.
                        // So, for Map<String, String> it will find the binding to denote that the type is Map
                        // and that denotes a class type. To filter out this case, and correctly create types
                        // even in this situation, we are handling the case (that the type is a
                        // parameterized type but the binding says that is is not) separately.
                        if (fieldType.isParameterizedType() && !fieldTypeFromBinding.isParameterizedType()) {
                            Pair<String, TypeInfo> typeInfoAndHash =
                                    getAtomicTypeInfo(fieldType, null, null, false, true);
                            if (typeInfoAndHash != null) {
                                fieldsMap.put(
                                        declaringType.getName() + Constants.FIELDNAME_SEPARATOR + field.getName(),
                                        Pair.of(Pair.of(fieldRange, declaringTypeIndex), typeInfoAndHash.snd));
                                if (fieldInfo != null) {
                                    fieldInfo.setTypeInfo(typeInfoAndHash.snd);
                                }
                            }
                        } else {
                            // We are calculating type info for a field,
                            // As fields don't directly have bounds, set last argument to false.
                            // If the field's type is a type variable,
                            // it will be handled inside getAtomicTypeInfo method
                            Pair<String, TypeInfo> typeInfoAndHash =
                                    getAtomicTypeInfo(fieldTypeFromBinding, null, null, false, true, false);
                            if (typeInfoAndHash != null) {
                                fieldsMap.put(
                                        declaringType.getName() + Constants.FIELDNAME_SEPARATOR + field.getName(),
                                        Pair.of(Pair.of(fieldRange, declaringTypeIndex), typeInfoAndHash.snd));
                                if (fieldInfo != null) {
                                    fieldInfo.setTypeInfo(typeInfoAndHash.snd);
                                }
                            }
                        }
                    } else {
                        // when we can not resolve binding
                        // we will create typeInfo of field using Type
                        FieldDeclaration fd = ASTNodeUtility.findNearestAncestor(fieldName, FieldDeclaration.class);
                        Type fieldType = fd.getType();
                        TypeDeclaration declaringClass =
                                ASTNodeUtility.findNearestAncestor(fieldName, TypeDeclaration.class);
                        // Skip if binding is null
                        if (declaringClass.resolveBinding() == null) {
                            continue;
                        }
                        String declaringClassHash = CallGraphUtility.getClassHashFromTypeBinding(
                                declaringClass.resolveBinding(), null, filePath);
                        int declaringClassIndex = CallGraphDataStructures.getBitIndexFromClassHash(declaringClassHash);

                        TypeInfo calculatedFieldTypeInfo = null;
                        Pair<String, TypeInfo> typeInfoAndHash = getAtomicTypeInfo(fieldType, null, null, false, true);
                        if (typeInfoAndHash != null) {
                            calculatedFieldTypeInfo = typeInfoAndHash.snd;
                        }
                        if (calculatedFieldTypeInfo != null) {
                            fieldsMap.put(
                                    declaringClass.getName() + Constants.FIELDNAME_SEPARATOR + fieldName,
                                    Pair.of(Pair.of(fieldRange, declaringClassIndex), calculatedFieldTypeInfo));
                            if (fieldInfo != null) {
                                fieldInfo.setTypeInfo(calculatedFieldTypeInfo);
                            }
                        }
                    }
                }
            }
        }
        return fieldsMap;
    }

    /**
     * Finds fields for a parameterized type with resolved generic field types.
     *
     * @param classHash the hash of the parameterized type
     * @param typeBinding the binding of the parameterized type
     * @param elementTypes the list containing the type info of the parameterized type arguments
     * @param typeDec the type declaration node of current type binding
     * @return pair containing fields map and type arguments to fields mapping
     */
    private Pair<Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>>, Map<Integer, String>>
            getFieldsFromParameterizedType(
                    String classHash, ITypeBinding typeBinding, List<TypeInfo> elementTypes, TypeDeclaration typeDec) {
        Map<String, Pair<Pair<TokenRange, Integer>, TypeInfo>> fieldsMap = new HashMap<>();
        Map<Integer, String> typeArgsToFieldsMap = new HashMap<>();

        List<TokenRange> fields = CallGraphDataStructures.getFieldsFromClassHash(classHash);
        // If we have already calculated type declaration in this methods caller method then we'll
        // use that from passed argument instead of recalculating again.
        if (typeDec == null) {
            typeDec = (TypeDeclaration) ASTNodeUtility.getDeclaringNode(typeBinding);
        }
        if (typeDec != null) {
            FieldDeclaration[] fieldDecs = typeDec.getFields();
            // The type parameters are the parameters in the parameterized type declaration
            @SuppressWarnings("unchecked")
            List<TypeParameter> typeParams = typeDec.typeParameters();
            int i = 0;
            // We are matching the type parameters, say
            //    Pair <S, T>
            // with the fields that are of type S and type T (only one link per parameter)
            // and keeping that information in the typeArgsToFieldsMap structure
            for (TypeParameter typeParam : typeParams) {
                for (FieldDeclaration fieldDeclaration : fieldDecs) {
                    if (fieldDeclaration.getType().isParameterizedType()) {
                        @SuppressWarnings("unchecked")
                        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
                        for (VariableDeclarationFragment fragment : fragments) {
                            TypeInfo fieldTypeInfo = null;
                            String fieldName = typeDec.getName().getIdentifier()
                                    + Constants.FIELDNAME_SEPARATOR
                                    + fragment.getName().getIdentifier();
                            if (!elementTypes.isEmpty()) {
                                if (fieldsMap.get(fieldName) != null) {
                                    fieldTypeInfo = resolveActualTypeInfo(
                                            (ParameterizedTypeInfo) fieldsMap.get(fieldName).snd,
                                            fieldDeclaration.getType(),
                                            elementTypes.get(i),
                                            typeParam);
                                } else {
                                    fieldTypeInfo = resolveActualTypeInfo(
                                            fieldDeclaration.getType(), elementTypes.get(i), typeParam);
                                }
                            }

                            if (fieldTypeInfo != null) {
                                typeArgsToFieldsMap.put(i, fieldName);
                                // Skip if binding is null
                                if (typeDec.resolveBinding() != null) {
                                    String containerTypeHash = CallGraphUtility.getClassHashFromTypeBinding(
                                            typeDec.resolveBinding(), null, filePath);
                                    CompilationUnit cu =
                                            ASTNodeUtility.findNearestAncestor(typeDec, CompilationUnit.class);
                                    String fileName = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
                                    TokenRange fieldRange =
                                            new TokenRange(fragment.getStartPosition(), fragment.getLength(), fileName);
                                    fieldsMap.put(
                                            fieldName,
                                            Pair.of(
                                                    Pair.of(
                                                            fieldRange,
                                                            CallGraphDataStructures.getBitIndexFromClassHash(
                                                                    containerTypeHash)),
                                                    fieldTypeInfo));
                                }
                            }
                        }
                    } else {
                        if (typeParam
                                .getName()
                                .getIdentifier()
                                .equals(fieldDeclaration.getType().toString())) {
                            @SuppressWarnings("unchecked")
                            List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
                            for (VariableDeclarationFragment fragment : fragments) {
                                String fieldName = typeDec.getName().getIdentifier()
                                        + Constants.FIELDNAME_SEPARATOR
                                        + fragment.getName().getIdentifier();
                                typeArgsToFieldsMap.put(i, fieldName);
                                // Skip if binding is null
                                if (typeDec.resolveBinding() != null) {
                                    String containerClassHash = CallGraphUtility.getClassHashFromTypeBinding(
                                            typeDec.resolveBinding(), null, filePath);
                                    CompilationUnit cu =
                                            ASTNodeUtility.findNearestAncestor(typeDec, CompilationUnit.class);
                                    String fileName = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
                                    TokenRange fieldRange =
                                            new TokenRange(fragment.getStartPosition(), fragment.getLength(), fileName);
                                    // COMMENT ???
                                    if (elementTypes.isEmpty() || elementTypes.size() < i) {
                                        TypeInfo fieldTypeInfo = getTypeInfo(fieldDeclaration.getType(), true);
                                        fieldsMap.put(
                                                fieldName,
                                                Pair.of(
                                                        Pair.of(
                                                                fieldRange,
                                                                CallGraphDataStructures.getBitIndexFromClassHash(
                                                                        containerClassHash)),
                                                        fieldTypeInfo));
                                    } else {
                                        fieldsMap.put(
                                                fieldName,
                                                Pair.of(
                                                        Pair.of(
                                                                fieldRange,
                                                                CallGraphDataStructures.getBitIndexFromClassHash(
                                                                        containerClassHash)),
                                                        elementTypes.get(i)));
                                    }
                                }
                            }
                        }
                    }
                }
                i++;
            }

            if (fields != null) {
                for (TokenRange fieldRange : fields) {
                    FieldInfo fieldInfo = CallGraphDataStructures.getFieldInfoFromTokenRange(fieldRange);
                    // Only calculating for non-static variables
                    if (fieldInfo != null && fieldInfo.isStatic()) {
                        continue;
                    }
                    if (fieldInfo != null && fieldInfo.getFieldTypeInfo() != null) {
                        String declaringClasshash = fieldInfo.getContainerClassHash();
                        String fieldNameWithContainer = CallGraphUtility.getClassNameFromClassHash(declaringClasshash)
                                + Constants.FIELDNAME_SEPARATOR
                                + fieldInfo.getFieldName();

                        if (!fieldsMap.containsKey(fieldNameWithContainer)) {
                            fieldsMap.put(
                                    fieldNameWithContainer,
                                    Pair.of(
                                            Pair.of(
                                                    fieldRange,
                                                    CallGraphDataStructures.getBitIndexFromClassHash(
                                                            declaringClasshash)),
                                            fieldInfo.getFieldTypeInfo()));
                        }
                    } else {
                        // This path should not be visited that often anymore
                        // since the field info structure should be keeping most of the information
                        // Left here to cover corner cases.
                        SimpleName fieldName = ASTNodeUtility.getASTNodeFromTokenRange(fieldRange, SimpleName.class);
                        if (fieldName != null) {
                            IVariableBinding field = (IVariableBinding) fieldName.resolveBinding();
                            ITypeBinding fieldType = field.getType();
                            ITypeBinding declaringType = field.getDeclaringClass();
                            String declaringClasshash =
                                    CallGraphUtility.getClassHashFromTypeBinding(declaringType, null, filePath);
                            String fieldNameWithContainer =
                                    declaringType.getName() + Constants.FIELDNAME_SEPARATOR + field.getName();
                            if (!fieldsMap.containsKey(fieldNameWithContainer)) {
                                // We are calculating type info for a field,
                                // As fields don't directly have bounds, set last argument to false.
                                // If the field's type is a type variable,
                                // it will be handled inside getAtomicTypeInfo method
                                Pair<String, TypeInfo> typeInfoWithHash =
                                        getAtomicTypeInfo(fieldType, null, null, false, true, false);
                                if (typeInfoWithHash != null && typeInfoWithHash.snd != null) {
                                    fieldsMap.put(
                                            fieldNameWithContainer,
                                            Pair.of(
                                                    Pair.of(
                                                            fieldRange,
                                                            CallGraphDataStructures.getBitIndexFromClassHash(
                                                                    declaringClasshash)),
                                                    typeInfoWithHash.snd));
                                    if (fieldInfo != null) {
                                        fieldInfo.setTypeInfo(typeInfoWithHash.snd);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Pair.of(fieldsMap, typeArgsToFieldsMap);
    }

    /**
     * Infers the type arguments for ClassInstanceCreation with <> operator.
     *
     * @param type the type node with the <> operator
     * @param containerTokenRange the token range for the container (can be null)
     * @param containerHash the hash of the container if already calculated (can be null)
     * @param calculateSoftTypeInfo true for soft type info, false for proper type info
     * @return list of type info for the inferred type arguments, empty list if cannot be inferred
     */
    private List<TypeInfo> inferTypeArguments(
            Type type, TokenRange containerTokenRange, String containerHash, boolean calculateSoftTypeInfo) {
        TypeInfo inferredType = null;
        if (type.getParent() instanceof ClassInstanceCreation) {
            ASTNode parent = type.getParent();
            while (parent != null) {
                if (parent instanceof VariableDeclarationFragment) {
                    // For variable declaration fragment we can infer
                    // directly from that node
                    inferredType = TypeCalculator.typeOf(parent, calculateSoftTypeInfo);
                    break;
                } else if (parent instanceof Assignment) {
                    // For assignment we need to infer from the left hand side
                    inferredType =
                            TypeCalculator.typeOf(((Assignment) parent).getLeftHandSide(), calculateSoftTypeInfo);
                    break;
                } else if (parent instanceof MethodInvocation) {
                    // Inferring type arguments from parameter binding doesn't work
                    // As the binding only contains the type erasure
                    // Not the actual declared type.
                    // So, not doing any processing here
                    break;
                } else if (parent instanceof ReturnStatement) {
                    // For return statement, we need to infer from the method declaration
                    MethodDeclaration containerMethod =
                            ASTNodeUtility.findNearestAncestor(parent, MethodDeclaration.class);
                    if (containerMethod != null) {
                        Type returnType = containerMethod.getReturnType2();
                        inferredType = TypeCalculator.typeOf(returnType, filePath, calculateSoftTypeInfo);
                    }
                    break;
                } else if (parent instanceof Statement) {
                    break;
                }
                parent = parent.getParent();
            }
        }

        if (inferredType instanceof ParameterizedTypeInfo) {
            return ((ParameterizedTypeInfo) inferredType).getElementTypes();
        }
        return Collections.emptyList();
    }

    /**
     * Gets the type name from type info.
     *
     * @param typeInfo the type info for which type name is needed
     * @return the type's name
     */
    private String getActualTypeName(TypeInfo typeInfo) {
        if (typeInfo instanceof ScalarTypeInfo) {
            ScalarTypeInfo scalarType = (ScalarTypeInfo) typeInfo;
            if (scalarType.isBoxType()) {
                if (scalarType.getName().equals("int")) {
                    return "Integer";
                } else if (scalarType.getName().equals("float")) {
                    return "Float";
                } else if (scalarType.getName().equals("long")) {
                    return "Long";
                } else if (scalarType.getName().equals("double")) {
                    return "Double";
                } else if (scalarType.getName().equals("boolean")) {
                    return "Boolean";
                } else if (scalarType.getName().equals("void")) {
                    return "Void";
                } else if (scalarType.getName().equals("byte")) {
                    return "Byte";
                } else if (scalarType.getName().equals("char")) {
                    return "Character";
                } else if (scalarType.getName().equals("short")) {
                    return "Short";
                } else {
                    return scalarType.getName();
                }
            }
        }

        return CallGraphUtility.getLibraryNameForSpecInfo(typeInfo.getName());
    }
}
