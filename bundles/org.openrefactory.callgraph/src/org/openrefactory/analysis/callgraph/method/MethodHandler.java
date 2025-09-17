/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.*;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;
import org.openrefactory.util.datastructure.Pair;

/**
 * This class is used to create the {@link MethodIdentity} of different kinds of method/constructor calls and
 * method declarations.
 *
 * @author Mohammad Rafid Ul Islam
 */
public class MethodHandler extends ASTVisitor {

    /** The method identity being constructed during AST traversal */
    private MethodIdentity methodIdentity;

    public MethodHandler() {
        methodIdentity = new MethodIdentity();
    }

    /**
     * Retrieves the constructed method identity.
     *
     * <p>This method returns the {@link MethodIdentity} object that was buil
     * during the AST traversal process.</p>
     *
     * @return the constructed method identity, or a new empty identity if no
     *         method-related nodes were visited
     */
    public MethodIdentity getMethodIdentity() {
        return methodIdentity;
    }

    /**
     * Calculates return type info from method binding
     *
     * <p>This method determines the return type of a method by examining its binding.
     * It handles various scenarios including constructors, methods with known identities,
     * and methods that need type calculation from their declaration.</p>
     *
     * @param binding the binding of the method to analyze
     * @param node the {@link MethodInvocation} or {@link SuperMethodInvocation} node,
     *             or {@code null} if not available
     * @return the type info of the return type, never {@code null}
     */
    private static TypeInfo getReturnTypeFromMethodBinding(IMethodBinding binding, ASTNode node) {
        if (binding.isConstructor() || binding.isDefaultConstructor()) {
            return new ScalarTypeInfo(Constants.VOID);
        }

        Pair<String, String> methodHashAndSign = CallGraphUtility.getHashCodeOfMethod(binding);
        if (methodHashAndSign != null && methodHashAndSign.fst != null) {
            MethodIdentity identity = CallGraphDataStructures.getMatchingMethodIdentity(methodHashAndSign.fst);
            if (identity != null) {
                return identity.getReturnTypeInfo();
            }
        }

        MethodDeclaration decl = (MethodDeclaration) ASTNodeUtility.getDeclaringNode(binding);
        if (decl != null) {
            // For a method's identity, use soft type
            return decl.getReturnType2() == null
                    ? new ScalarTypeInfo(Constants.VOID)
                    : TypeCalculator.typeOf(decl.getReturnType2(), true);
        } else {
            String filePath = null;
            if (node != null) {
                // From the node get the file path and send tha
                CompilationUnit cu = ASTNodeUtility.findNearestAncestor(node, CompilationUnit.class);
                if (cu != null) {
                    filePath = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
                }
            }
            // For a method's identity, use soft type
            // We should have sent file path as the second parameter
            // However, from the binding, we cannot calculate the file path
            // So the file path may be null if the node is null
            return TypeCalculator.typeOf(binding.getReturnType(), filePath, null, null, true);
        }
    }

    /**
     * Static utility method to process an AST node and extract method identity.
     *
     * <p>This method creates a new MethodHandler instance, traverses the given AST node,
     * and returns the constructed method identity. It's a convenience method for
     * one-time processing of AST nodes.</p>
     *
     * @param node the AST node to process for method identity extraction
     * @return the extracted method identity, never {@code null}
     */
    public static MethodIdentity process(ASTNode node) {
        MethodHandler visitor = new MethodHandler();
        node.accept(visitor);
        return visitor.getMethodIdentity();
    }

    // AST Visitor Methods - Most return false to avoid deep traversal
    // Only method-related nodes are processed to build the identity

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
        return false;
    }

    @Override
    public boolean visit(ArrayAccess node) {
        return false;
    }

    @Override
    public boolean visit(ArrayCreation node) {
        return false;
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        return false;
    }

    @Override
    public boolean visit(ArrayType node) {
        return false;
    }

    @Override
    public boolean visit(AssertStatement node) {
        return false;
    }

    @Override
    public boolean visit(Assignment node) {
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
        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        return false;
    }

    @Override
    public boolean visit(CastExpression node) {
        return false;
    }

    @Override
    public boolean visit(CatchClause node) {
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        return false;
    }

    /**
     * Visits a class instance creation node (constructor call).
     *
     * <p>This method processes constructor calls like <code>new ClassName()</code>.
     * It extracts the constructor name, processes arguments, and creates a method
     * identity with constructor-specific information.</p>
     *
     * <p>The method handles generic type parameters by stripping them from the
     * constructor name to ensure consistent identity matching.</p>
     *
     * @param node the class instance creation node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(ClassInstanceCreation node) {
        // new A()
        ScalarTypeInfo voidType = new ScalarTypeInfo(Constants.VOID);
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        String constructorNameAtInvocation = node.getType().toString();
        if (constructorNameAtInvocation.contains("<")) {
            // Get rid of the parameter from a call
            //    new Foo<T>(...)
            // and register the constructor to be Foo.
            constructorNameAtInvocation =
                    constructorNameAtInvocation.substring(0, constructorNameAtInvocation.indexOf("<"));
        }
        methodIdentity = new MethodIdentity(constructorNameAtInvocation, voidType, argParamTypeInfos);
        // No need to set any bits for a method invocation,
        // still setting the constructor info since at least we are sure about i
        methodIdentity.setConstructorBit();
        return false;
    }

    @Override
    public boolean visit(CompilationUnit node) {
        return false;
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        return false;
    }

    /**
     * Visits a constructor invocation node (this() call).
     *
     * <p>This method processes constructor chaining calls like <code>this()</code>.
     * It extracts the constructor name from the containing method declaration
     * and processes the arguments to build the method identity.</p>
     *
     * @param node the constructor invocation node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(ConstructorInvocation node) {
        // this()
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        // This call will be in a constructor and it will refer to another constructor
        // Get the containing constructor method name.
        // This will be the name of the constructor class
        MethodDeclaration consMethodDeclaration = ASTNodeUtility.findNearestAncestor(node, MethodDeclaration.class);
        methodIdentity = new MethodIdentity(
                consMethodDeclaration.getName().getIdentifier(),
                new ScalarTypeInfo(Constants.VOID),
                argParamTypeInfos);
        // No need to set any bits for a method invocation,
        // still setting the constructor info since at least we are sure about i
        methodIdentity.setConstructorBit();
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

    /**
     * Visits an enum constant declaration node.
     *
     * <p>This method processes enum constant declarations which implicitly call
     * the enum constructor. It creates a method identity for the constructor
     * call with the provided argument types.</p>
     *
     * <p>Enum constants can have arguments that are passed to the enum constructor,
     * and this method captures those arguments to build the constructor identity.</p>
     *
     * @param node the enum constant declaration node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(EnumConstantDeclaration node) {
        // Enum constant declarations implicitly calls the enum constructor.
        // So create a method identity for that with the provided argument types.
        ScalarTypeInfo voidType = new ScalarTypeInfo(Constants.VOID);
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        String constructorName = ((EnumDeclaration) node.getParent()).getName().getIdentifier();
        methodIdentity = new MethodIdentity(constructorName, voidType, argParamTypeInfos);
        // No need to set any bits for a method invocation,
        // still setting the constructor info since at least we are sure about i
        methodIdentity.setConstructorBit();
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
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
        return false;
    }

    @Override
    public boolean visit(FieldAccess node) {
        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
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

    /**
     * Visits a method declaration node.
     *
     * <p>This method processes method declarations to extract comprehensive
     * method identity information including:</p>
     * <ul>
     *   <li>Method name and return type</li>
     *   <li>Parameter types</li>
     *   <li>Method modifiers (static, constructor, abstract, etc.)</li>
     *   <li>Method characteristics (bodyless, polymorphic)</li>
     * </ul>
     *
     * <p>The method handles various edge cases including constructors withou
     * explicit return types, abstract methods, and interface methods.</p>
     *
     * @param node the method declaration node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(MethodDeclaration node) {
        // Sometimes, we don't get the return type from the MethodDeclaration.
        // It usually happens in case of the constructor declarations. So,
        // we make the return type `void` in such cases.
        // For a method's identity, use soft type
        TypeInfo returnTypeInfo = null;
        if (node.getReturnType2() != null) {
            returnTypeInfo = TypeCalculator.typeOf(node.getReturnType2(), true);
        }
        if (returnTypeInfo == null) {
            returnTypeInfo = new ScalarTypeInfo(Constants.VOID);
        }
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = node.parameters();
        for (SingleVariableDeclaration param : params) {
            // For a method's identity, use soft type
            TypeInfo paramTypeInfo = TypeCalculator.typeOf(param, true);
            argParamTypeInfos.add(paramTypeInfo);
        }
        methodIdentity = new MethodIdentity(node.getName().getIdentifier(), returnTypeInfo, argParamTypeInfos);
        int modifiers = node.getModifiers();
        // For a method declaration, we are creating the identity during CG processing
        // and then storing it for use later.
        // So, setting various bits here.
        if (node.isConstructor()) {
            methodIdentity.setConstructorBit();
        }
        if (Modifier.isStatic(modifiers)) {
            methodIdentity.setStaticBit();
        }
        if (Modifier.isDefault(modifiers)) {
            methodIdentity.setDefaultBit();
        }
        if (node.getBody() == null) {
            methodIdentity.setBodylessBit();
            // Possibly polymorphic bit is set in two phases
            // (1) Here in phase 3 when we encounter a method without a body
            if (!Modifier.isNative(modifiers) && !node.isConstructor()) {
                if (Modifier.isAbstract(modifiers)) {
                    methodIdentity.setPossiblyPolymorphicBit();
                } else {
                    IMethodBinding binding = node.resolveBinding();
                    if (binding != null) {
                        ITypeBinding containerClass = binding.getDeclaringClass();
                        if (containerClass != null) {
                            if (containerClass.isInterface()) {
                                methodIdentity.setPossiblyPolymorphicBit();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Visits a method invocation node.
     *
     * <p>This method processes method calls like <code>methodName()</code>.
     * It extracts the method name, determines the return type, and processes
     * arguments to build the method identity.</p>
     *
     * <p>The method handles cases where method bindings cannot be resolved
     * by providing fallback return types based on the context.</p>
     *
     * @param node the method invocation node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(MethodInvocation node) {
        TypeInfo returnTypeInfo = null;
        // If we cannot resolve the method binding, it may be a library method.
        // In such cases, we make the return type void if the method call is the only
        // expression in the statement. Like,
        //     foo();
        // However, for other cases like Assignments and other expressions, we make the
        // return type a `dummy class`.
        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null) {
            returnTypeInfo = getReturnTypeFromMethodBinding(binding, node);
        } else {
            if (node.getParent() instanceof ExpressionStatement) {
                returnTypeInfo = new ScalarTypeInfo("void");
            } else {
                returnTypeInfo = new ClassTypeInfo(Constants.DUMMY_TYPE_CLASS);
            }
        }
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        methodIdentity = new MethodIdentity(node.getName().getIdentifier(), returnTypeInfo, argParamTypeInfos);
        // No need to set any bits for a method invocation
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
        return false;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        return false;
    }

    @Override
    public boolean visit(NullLiteral node) {
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
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
        return false;
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        return false;
    }

    @Override
    public boolean visit(PostfixExpression node) {
        return false;
    }

    @Override
    public boolean visit(PrefixExpression node) {
        return false;
    }

    @Override
    public boolean visit(ProvidesDirective node) {
        return false;
    }

    @Override
    public boolean visit(PrimitiveType node) {
        return false;
    }

    @Override
    public boolean visit(QualifiedName node) {
        return false;
    }

    @Override
    public boolean visit(QualifiedType node) {
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
        return false;
    }

    @Override
    public boolean visit(SimpleType node) {
        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        return false;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(StringLiteral node) {
        return false;
    }

    /**
     * Visits a super constructor invocation node (super() call).
     *
     * <p>This method processes super constructor calls like <code>super()</code>.
     * It determines the constructor name by analyzing the class hierarchy and
     * processes the arguments to build the method identity.</p>
     *
     * <p>The method navigates through the class hierarchy to find the superclass
     * and uses its name as the constructor name.</p>
     *
     * @param node the super constructor invocation node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(SuperConstructorInvocation node) {
        // super()
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        // Get the containing class of the constructor call.
        // Get its super class. The constructor name will be the name of the class
        // without parameterized types if present.
        String servicingConstructorName = null;
        MethodDeclaration containerMethod = ASTNodeUtility.findNearestAncestor(node, MethodDeclaration.class);
        // Passing this as null, since this is only used in a very small number of cases.
        Pair<String, String> methodHashAndSig =
                CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(containerMethod, null);
        if (methodHashAndSig != null && methodHashAndSig.fst != null) {
            String methodHash = methodHashAndSig.fst;
            String containerClassHash = CallGraphUtility.getClassHashFromMethodHash(methodHash);
            if (containerClassHash != null) {
                String superClassHash = CallGraphDataStructures.getSuperClassOf(containerClassHash);
                if (superClassHash != null) {
                    // The constructor's name is the same as the class name
                    servicingConstructorName = CallGraphUtility.getClassNameFromClassHash(superClassHash);
                }
            }
        }
        methodIdentity = new MethodIdentity(
                servicingConstructorName, new ScalarTypeInfo(Constants.VOID), argParamTypeInfos);
        // No need to set any bits for a method invocation,
        // still setting the constructor info since at least we are sure about i
        methodIdentity.setConstructorBit();
        return false;
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        return false;
    }

    /**
     * Visits a super method invocation node (super.methodName() call).
     *
     * <p>This method processes super method calls like <code>super.methodName()</code>.
     * It extracts the method name, determines the return type, and processes
     * arguments to build the method identity.</p>
     *
     * <p>The method handles cases where method bindings cannot be resolved
     * by providing fallback return types based on the context.</p>
     *
     * @param node the super method invocation node to process
     * @return {@code false} to prevent deep traversal of child nodes
     */
    @Override
    public boolean visit(SuperMethodInvocation node) {
        // super.foo()
        TypeInfo returnTypeInfo = null;
        // If we cannot resolve the method binding, it may be a library method.
        // In such cases, we make the return type void if the method call is the only
        // expression in the statement. Like,
        // super.foo();
        // However, for other cases like Assignments and other expressions, we make the
        // return type a `dummy class`.
        IMethodBinding binding = node.resolveMethodBinding();
        if (binding != null) {
            returnTypeInfo = getReturnTypeFromMethodBinding(binding, node);
        } else {
            if (node.getParent() instanceof ExpressionStatement) {
                returnTypeInfo = new ScalarTypeInfo("void");
            } else {
                returnTypeInfo = new ClassTypeInfo(Constants.DUMMY_TYPE_CLASS);
            }
        }
        List<TypeInfo> argParamTypeInfos = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Expression> args = node.arguments();
        for (Expression arg : args) {
            // For a method's identity, use soft type
            TypeInfo argTypeInfo = TypeCalculator.typeOf(arg, true);
            argParamTypeInfos.add(argTypeInfo);
        }
        methodIdentity = new MethodIdentity(node.getName().getIdentifier(), returnTypeInfo, argParamTypeInfos);
        // No need to set any bits for a method invocation
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
        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        return false;
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        return false;
    }

    @Override
    public boolean visit(TypeParameter node) {
        return false;
    }

    @Override
    public boolean visit(UnionType node) {
        return false;
    }

    @Override
    public boolean visit(UsesDirective node) {
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {

        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        return false;
    }

    @Override
    public boolean visit(WildcardType node) {
        return false;
    }
}
