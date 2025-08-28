/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.openrefactory.analysis.callgraph.AccessModifiers;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.method.MethodHandler;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.callgraph.method.MethodMatchFinderUtil;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.analysis.type.typeinfo.ArrayTypeInfo;
import org.openrefactory.analysis.type.typeinfo.SymbolicTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.datastructure.IntPair;
import org.openrefactory.util.datastructure.NeoLRUCache;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;
import org.openrefactory.util.manager.C2PManager;

/**
 * Utility class providing various helper methods for Call Graph analysis and processing.
 *
 * <p>This class contains utility methods for working with call graphs, method signatures,
 * class hierarchies, and AST nodes. It provides functionality for method resolution,
 * type checking, hash generation, and various call graph operations.</p>
 *
 * @author Mohammad Rafid Ul Islam
 * @author Rifat Rubayatul Islam
 */
@SuppressWarnings("restriction")
public class CallGraphUtility {
    public final String ROOT_FUNCTION_SEPARATOR = "|||";
    public static final String CG_SEPARATOR = "::";
    public static final String CG_ANON_NAME_SEPARATOR = "$";
    public static final String CG_LAMBDA_STATUS_SEPARATOR = "#";
    public static final String CG_STATIC = "static";
    public static final String CG_NONSTATIC = "nonstatic";
    public static final String CG_INIT = "init";
    public static final String CG_INIT_STATIC = "init_static";
    public static final String CG_ANONYMOUS_TYPE = "ANON__OR__TYPE";
    public static final String CG_LAMBDA_METHOD = "lambda";

    // Any file under a directory listed in this list will not
    // be scanned. So, no fix or warning will be generated for
    // those files.
    private static final Set<String> EXCLUDED_DIRS = Set.of("example", "examples", "test", "tests", "testsuite");

    // By default we will not store perflow node summary unless we need it in PKMRefactoring.
    // But before running testsuite we will make it true, to test perflow node.
    public static boolean keepPerflowNode = false;

    // Creating two constants to denote dummy methods
    // The first one has a substring of the second and a
    // series of others (to come in future) and check if a method
    // is a dummy method or not. The second one denotes that
    // it is a spring specific dummy method.
    public static final String CG_DUMMY_METHOD = "DUMMY";
    public static final String CG_SPRING_BEAN_DUMMY_METHOD = "DUMMY__SPRING__BEAN";

    // We are creating a virtual connecting method, whose job is to play a role as a bridge
    // between all Beanfactory::getBean method to all Dummy method
    //
    // Suppose we have 3 beans: b1,b2,b3.
    // name1, name2 are two expressions that resolve to string values but the value cannot be found
    // before we do pointer analysis. So, we cannot link the getBean call with the appropriate bean
    // in the framework analysis stage.
    //
    // In call graph analysis and framework analysis we do not have any information about variable
    // name1. So to create dependency, we are using a virtual method as a bridge
    //
    // We need a virtual method as a bridge because we also need to pass the
    // actual parameters from the getBean call if there are any.
    //
    // Suppose "b1" is assigned to variable name1. So we need the composed result
    // of dummy method for "b1" while composing "x.getBean(name1)"
    // This will happen only if there is any dependency from
    // x.getBean(name1) to Dummy method for bean "b1"
    //
    //
    // x.getBean(name1) ---|                         |----Dummy method for bean "b1"
    //                     |----connecting method----|----Dummy method for bean "b2"
    // y.getBean(name2)----|                         |----Dummy method for bean "b3"
    public static String connectingVirtualMethodHashForSpringBean = "Virtual_OR_Type:Spring_Bean";

    public static final String CG_CLASS_ANON = "anonymous";
    public static final String CG_CLASS_INTERFACE = "interface";
    public static final String CG_CLASS_CLASS = "class";
    public static final String CG_CLASS_ENUM = "enum";
    // Separate abstract class from regular class
    public static final String CG_CLASS_ABSTRACT = "abstract";

    // Various kinds of annotations detected at call graph stage
    public static final String ANNOTATION_THREADSAFE = "ThreadSafe";

    // Class signature indices
    public static final int CG_FILENAME_INDEX = 0;
    public static final int CG_CLASS_OFFSET = 1;
    public static final int CG_CLASS_LENTGH = 2;
    public static final int CG_CLASS_NAME = 3;
    public static final int CG_CLASS_TYPE = 4;
    // Method signature indices
    public static final int CG_METHOD_NAME = 1;
    public static final int CG_METHOD_OFFSET = 2;
    public static final int CG_METHOD_LENGTH = 3;
    public static final int CG_METHOD_CLASS_OFFSET = 4;
    public static final int CG_METHOD_CLASS_LENGTH = 5;
    public static final int CG_METHOD_CLASS_NAME = 6;
    public static final int CG_METHOD_CLASS_TYPE = 7;
    public static final int CG_METHOD_STATUS = 8;

    public static final String PARAM_TYPE_NAME_INDICATOR = "<";

    /**
     * Retrieves all methods that call the specified method.
     *
     * @param method the method to find callers for
     * @return set of methods that call the specified method
     */
    public static Set<IMethod> getCallers(IMethod method) {
        CallHierarchy callHierarchy = CallHierarchy.getDefault();
        IMember[] members = {method};
        MethodWrapper[] methodWrappers = callHierarchy.getCallerRoots(members);
        Set<IMethod> callers = new HashSet<>();
        if (methodWrappers == null) return callers;
        for (MethodWrapper methodWrapper : methodWrappers) {
            try {
                MethodWrapper[] calls = methodWrapper.getCalls(new NullProgressMonitor());
                Set<IMethod> iMethods = getIMethodsFromMethodWrappers(calls);
                callers.addAll(iMethods);
            } catch (Exception e) {
                // Turned off printing exceptions
                // e.printStackTrace();
            }
        }
        return callers;
    }

    private static Set<IMethod> getIMethodsFromMethodWrappers(MethodWrapper[] methodWrappers) {
        Set<IMethod> iMethods = new HashSet<>();
        for (MethodWrapper methodWrapper : methodWrappers) {
            IMethod iMthod = getIMethodFromMethodWrapper(methodWrapper);
            if (iMthod != null) {
                iMethods.add(iMthod);
            }
        }
        return iMethods;
    }

    private static IMethod getIMethodFromMethodWrapper(MethodWrapper methodWrapper) {
        try {
            IMember iMethod = methodWrapper.getMember();
            if (iMethod.getElementType() == IJavaElement.METHOD) {
                return (IMethod) methodWrapper.getMember();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a superclass is reachable from a subclass in the inheritance hierarchy.
     *
     * @param subClassHash the hash of the subclass
     * @param superClassHash the hash of the superclass to check
     * @return true if the superclass is reachable from the subclass, false otherwise
     */
    public static boolean isReachableSuperClass(String subClassHash, String superClassHash) {
        boolean isReachableSuper = false;
        while (CallGraphDataStructures.getSuperClassOf(subClassHash) != null) {
            if (CallGraphDataStructures.getSuperClassOf(subClassHash).equals(superClassHash)) {
                isReachableSuper = true;
                break;
            } else {
                subClassHash = CallGraphDataStructures.getSuperClassOf(subClassHash);
            }
        }
        return isReachableSuper;
    }

    /**
     * Determines if a type cast represents downcasting.
     *
     * @param castingType the hash of the target type for casting
     * @param originalType the hash of the originally declared type
     * @return true if casting to a subclass, false if casting to same class or superclass
     */
    public static boolean isDownCasting(String castingType, String originalType) {
        if (originalType.equals("Object") || originalType.equals(Constants.JAVA_LANG_OBJECT)) {
            // If we are casting a class that is originally the Object type
            // happens often that we cast an object type
            // Then the casting is a down casting
            return true;
        }
        Set<String> subClassHashes = CallGraphDataStructures.getAllSubClass(originalType);
        // If there is no subclass then create a class with itself as the content
        if (subClassHashes == null) {
            subClassHashes = new HashSet<>(1);
            subClassHashes.add(originalType);
        }
        // If the casting type is a super class or the same class
        // return false, else return true
        return subClassHashes.contains(castingType);
    }

    /**
     * Determines if a type cast represents downcasting using TypeInfo objects.
     *
     * @param castingType the target type for casting
     * @param originalType the originally declared type
     * @return true if casting to a subclass, false if casting to same class or superclass
     */
    public static boolean isDownCasting(TypeInfo castingType, TypeInfo originalType) {
        // Casting on any symbolic type
        // as in T a
        // with ((Foo) T)
        // is down casting. This is true even for objects
        if (originalType instanceof SymbolicTypeInfo) {
            if (!(castingType instanceof SymbolicTypeInfo)) {
                return true;
            }
        }
        return isDownCasting(castingType.getName(), originalType.getName());
    }

    /**
     * Checks if a method exists in the specified class with an implemented body.
     *
     * @param classHash the hash of the class to check
     * @param methodName the name of the method to find
     * @return true if the method exists with a body, false otherwise
     */
    public static boolean isMethodExistInThisClass(String classHash, String methodName) {
        if (!classHash.startsWith(Constants.LIB_TYPE)
                && CallGraphDataStructures.getClassSignatureFromHash(classHash) != null) {
            if (CallGraphDataStructures.getClassToMethodsMap().get(classHash) != null) {
                return CallGraphDataStructures.getClassToMethodsMap().containsKey(methodName);
            }
        }
        return false;
    }

    /**
     * Checks if a method exists in the specified class or any of its reachable superclasses.
     *
     * @param classHash the hash of the class to check
     * @param methodName the name of the method to find
     * @return true if the method exists in the class or superclasses, false otherwise
     */
    public static boolean isMethodExist(String classHash, String methodName) {
        if (isMethodExistInThisClass(classHash, methodName)) {
            return true;
        }
        while (CallGraphDataStructures.getSuperClassOf(classHash) != null) {
            classHash = CallGraphDataStructures.getSuperClassOf(classHash);
            if (isMethodExistInThisClass(classHash, methodName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a list of MethodDeclaration nodes for methods that call the specified method.
     *
     * @param method the method declaration to find callers for
     * @return list of MethodDeclaration nodes for caller methods
     */
    public static List<MethodDeclaration> getCallerNodesOf(MethodDeclaration method) {
        // Currently this method is not used by anyone
        // So, leaving the last param as null
        // When we find some upstream callers, we will try to calculate the
        // file path from it.
        Pair<String, String> callee = CallGraphUtility.getHashCodeAndSignatureOfADeclaredMethod(method, null);
        List<String> callerHashes = CallGraphDataStructures.getCallGraph().getCallerListOf(callee.fst, false);
        List<MethodDeclaration> callers = new ArrayList<>();
        if (callerHashes != null) {
            for (String hash : callerHashes) {
                String signature = CallGraphDataStructures.getMethodSignatureFromHash(hash);
                MethodDeclaration caller = CallGraphUtility.getMethodFromMethodSignature(signature);
                callers.add(caller);
            }
        }
        return callers;
    }

    /**
     * Finds the container class, enum, or interface declaration for a field or method node.
     *
     * @param fieldOrMethod the AST node representing a field or method
     * @return the container class/enum/interface declaration node
     */
    public static ASTNode getContainerClassOrEnumOfFieldOrMethod(ASTNode fieldOrMethod) {
        ASTNode containerClassOrEnum = fieldOrMethod.getParent();
        // For annotation type declaration, containerClassOrEnum gets null
        // but does not find any TypeDeclaration, AnonymousClassDeclaration or EnumDeclaration
        // Restrict parent searching if AnnotationTypeDeclaration is found
        // Add null checking for safety.
        while (containerClassOrEnum != null
                && !((containerClassOrEnum instanceof TypeDeclaration)
                        || (containerClassOrEnum instanceof EnumDeclaration)
                        || (containerClassOrEnum instanceof AnonymousClassDeclaration)
                        || (containerClassOrEnum instanceof AnnotationTypeDeclaration))) {
            containerClassOrEnum = containerClassOrEnum.getParent();
        }
        return containerClassOrEnum;
    }

    /**
     * Retrieves the type binding from a container AST node (class, anonymous class, or enum).
     *
     * @param node the AST node representing a container declaration
     * @return the type binding for the container
     */
    public static ITypeBinding getContainerTypeBinding(ASTNode node) {
        ASTNode containerNode;
        if (node instanceof TypeDeclaration
                || node instanceof AnonymousClassDeclaration
                || node instanceof EnumDeclaration) {
            containerNode = node;
        } else {
            containerNode = getContainerClassOrEnumOfFieldOrMethod(node);
        }
        if (containerNode != null) {
            if (containerNode instanceof TypeDeclaration) {
                return ((TypeDeclaration) containerNode).resolveBinding();
            } else if (containerNode instanceof AnonymousClassDeclaration) {
                return ((AnonymousClassDeclaration) containerNode).resolveBinding();
            } else if (containerNode instanceof EnumDeclaration) {
                return ((EnumDeclaration) containerNode).resolveBinding();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieves the class hash of the class containing the specified method.
     *
     * @param methodHash the hash of the method
     * @return the hash of the containing class
     */
    public static String getClassHashFromMethodHash(String methodHash) {
        String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(methodHash);
        return getClassHashFromMethodSignature(methodSignature);
    }

    /**
     * Retrieves the class hash of the class containing the specified method.
     *
     * @param methodSignature the signature of the method
     * @return the hash of the containing class
     */
    public static String getClassHashFromMethodSignature(String methodSignature) {
        String[] splits = methodSignature.split(CG_SEPARATOR);
        StringBuilder classSignBuilder = new StringBuilder();
        classSignBuilder
                .append(splits[CG_FILENAME_INDEX])
                .append(CG_SEPARATOR)
                .append(splits[CG_METHOD_CLASS_OFFSET])
                .append(CG_SEPARATOR)
                .append(splits[CG_METHOD_CLASS_LENGTH])
                .append(CG_SEPARATOR)
                .append(splits[CG_METHOD_CLASS_NAME])
                .append(CG_SEPARATOR)
                .append(splits[CG_METHOD_CLASS_TYPE]);
        return HashUtility.generateSHA1(classSignBuilder.toString());
    }

    /**
     * Retrieves the location of a method node in the file.
     *
     * @param method the IMethod object
     * @param astNodeForMethod the AST node for the method (can be null)
     * @return the method location as a string in format "offset::length"
     */
    public static String getMethodLocation(IMethod method, ASTNode astNodeForMethod) {
        int startPosition;
        int length;
        try {
            if (astNodeForMethod == null) {
                ISourceRange range = method.getSourceRange();
                startPosition = range.getOffset();
                length = range.getLength();
            } else {
                ISourceRange range = method.getSourceRange();

                if ((astNodeForMethod.getStartPosition() >= range.getOffset())
                        && ((astNodeForMethod.getStartPosition() + astNodeForMethod.getLength())
                                <= (range.getOffset() + range.getLength()))) {
                    startPosition = range.getOffset();
                    length = range.getLength();
                } else {
                    startPosition = astNodeForMethod.getStartPosition();
                    length = astNodeForMethod.getLength();
                }
            }
            return startPosition + CG_SEPARATOR + length;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Checks if the specified method is a constructor.
     *
     * @param methodHash the hash of the method to check
     * @return true if the method is a constructor, false otherwise
     */
    public static boolean isConstructor(String methodHash) {
        String signature = CallGraphDataStructures.getMethodSignatureFromHash(methodHash);
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_METHOD_NAME].equals(splits[CG_METHOD_CLASS_NAME]);
    }

    /**
     * Returns whether the hash of the method indicates a default constructor
     * If the method signature has `init` in its last token, then it is a
     * default constructor.
     * 
     * @param hash of a method
     * @return true, if it is a defult constructor. Otherwise, false.
     */
    public static boolean isDefaultConstructor(String hash) {
        String sign = CallGraphDataStructures.getMethodSignatureFromHash(hash);
        String[] splits = sign.split(CG_SEPARATOR);
        return splits[CG_METHOD_STATUS].equals(CG_INIT);
    }
    
    /**
     * Returns whether the hash of the method indicates a static constructor
     * If the method signature has `init_static` in its last token, then it is a
     * static constructor.
     *
     * @param hash of a method
     * @return true, if it is a defult constructor. Otherwise, false.
     */
    public static boolean isStaticConstructor(String hash) {
        String sign = CallGraphDataStructures.getMethodSignatureFromHash(hash);
        String[] splits = sign.split(CG_SEPARATOR);
        return splits[CG_METHOD_STATUS].equals(CG_INIT_STATIC);
    }
    
    /**
     * Retrieves a MethodDeclaration node from a method signature.
     *
     * @param signature the method signature string
     * @return the MethodDeclaration node for the specified signature
     */
    public static MethodDeclaration getMethodFromMethodSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        CompilationUnit cu = ASTNodeUtility.getCompilationUnitFromFilePath(splits[CG_FILENAME_INDEX]);
        int startPos = Integer.parseInt(splits[CG_METHOD_OFFSET]);
        int length = Integer.parseInt(splits[CG_METHOD_LENGTH]);
        MethodDeclaration methodDeclaration = ASTNodeUtility.findNode(MethodDeclaration.class, cu, startPos, length);
        return methodDeclaration;
    }

    /**
     * Retrieves a MethodDeclaration node from a method hash.
     *
     * @param hash the hash of the method
     * @return the MethodDeclaration node for the specified hash
     */
    public static MethodDeclaration getMethodFromMethodHash(String hash) {
        String signature = CallGraphDataStructures.getMethodSignatureFromHash(hash);
        return getMethodFromMethodSignature(signature);
    }

    /**
     * Extracts the filename from a method or class signature.
     *
     * @param signature the method or class signature
     * @return the filename from the signature
     */
    public static String getFileNameFromSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_FILENAME_INDEX];
    }

    /**
     * Extracts the method name from a method signature.
     *
     * @param signature the method signature
     * @return the method name from the signature
     */
    public static String getMethodNameFromSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        if (isDummy(signature)) {
            // For dummy method we use different method status to differentiate with other methods.
            // Hence, we add the method status part in method name.
            return splits[CG_METHOD_CLASS_NAME]
                    + CG_SEPARATOR
                    + splits[CG_METHOD_NAME]
                    + CG_SEPARATOR
                    + splits[CG_METHOD_STATUS];
        }
        return splits[CG_METHOD_CLASS_NAME] + CG_SEPARATOR + splits[CG_METHOD_NAME];
    }

    /**
     * Extracts just the method name from a method signature without metadata.
     *
     * @param signature the method signature
     * @return the method name without metadata
     */
    public static String getMethodNameWithoutMetaInfo(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_METHOD_NAME];
    }

    /**
     * Extracts the class name from a method signature.
     *
     * @param signature the method signature
     * @return the class name from the signature
     */
    public static String getClassNameFromMethodSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_METHOD_CLASS_NAME];
    }

    /**
     * Extracts the offset and length of a class node from a signature.
     *
     * @param signature the method or class signature
     * @param isClass true if the signature is a class signature, false if method signature
     * @return pair containing the offset and length of the class node
     */
    public static IntPair getClassLocationFromSignature(String signature, boolean isClass) {
        String[] splits = signature.split(CG_SEPARATOR);
        int offset, length;
        if (isClass) {
            offset = Integer.parseInt(splits[CG_CLASS_OFFSET]);
            length = Integer.parseInt(splits[CG_CLASS_LENTGH]);
        } else {
            offset = Integer.parseInt(splits[CG_METHOD_CLASS_OFFSET]);
            length = Integer.parseInt(splits[CG_METHOD_CLASS_LENGTH]);
        }
        return IntPair.of(offset, length);
    }

    /**
     * Extracts the offset and length of a method node from a method signature.
     *
     * @param signature the method signature
     * @return pair containing the offset and length of the method node
     */
    public static IntPair getMethodLocationFromSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        int offset, length;
        offset = Integer.parseInt(splits[CG_METHOD_OFFSET]);
        length = Integer.parseInt(splits[CG_METHOD_LENGTH]);
        return IntPair.of(offset, length);
    }

    /**
     * Retrieves an AST node from a class signature.
     *
     * @param signature the class signature string
     * @return the AST node representing the class, enum, or anonymous class
     */
    public static ASTNode getClassFromClassSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        String fileName = splits[CG_FILENAME_INDEX];
        CompilationUnit cu = ASTNodeUtility.getCompilationUnitFromFilePath(fileName);
        int offset = Integer.parseInt(splits[CG_CLASS_OFFSET]);
        int length = Integer.parseInt(splits[CG_CLASS_LENTGH]);
        ASTNode typeDeclaration = null;
        if (splits[CG_CLASS_TYPE].equals(CG_CLASS_CLASS)
                || splits[CG_CLASS_TYPE].equals(CG_CLASS_INTERFACE)
                || splits[CG_CLASS_TYPE].equals(CG_CLASS_ABSTRACT)) {
            typeDeclaration = ASTNodeUtility.findNode(TypeDeclaration.class, cu, offset, length);
        } else if (splits[CG_CLASS_TYPE].equals(CG_CLASS_ENUM)) {
            typeDeclaration = ASTNodeUtility.findNode(EnumDeclaration.class, cu, offset, length);
        } else if (splits[CG_CLASS_TYPE].equals(CG_CLASS_ANON)) {
            typeDeclaration = ASTNodeUtility.findNode(AnonymousClassDeclaration.class, cu, offset, length);
        }
        return typeDeclaration;
    }

    /**
     * Retrieves the class type from a class hash.
     *
     * @param hash the class hash
     * @return the class type as a Class object
     */
    public static Class<? extends ASTNode> getClassTypeFromHash(String hash) {
        String signature = CallGraphDataStructures.getClassSignatureFromHash(hash);
        return getClassTypeFromSignature(signature);
    }

    /**
     * Retrieves the class type from a class signature.
     *
     * @param signature the class signature
     * @return the class type as a Class object
     */
    public static Class<? extends ASTNode> getClassTypeFromSignature(String signature) {
        if (signature != null) {
            String[] splits = signature.split(CG_SEPARATOR);
            if (splits[CG_CLASS_TYPE].equals(CG_CLASS_CLASS)
                    || splits[CG_CLASS_TYPE].equals(CG_CLASS_INTERFACE)
                    || splits[CG_CLASS_TYPE].equals(CG_CLASS_ABSTRACT)) {
                return TypeDeclaration.class;
            } else if (splits[CG_CLASS_TYPE].equals(CG_CLASS_ENUM)) {
                return EnumDeclaration.class;
            } else if (splits[CG_CLASS_TYPE].equals(CG_CLASS_ANON)) {
                return AnonymousClassDeclaration.class;
            }
        }
        return null;
    }

    /**
     * Extracts the class name from a class signature.
     *
     * @param signature the class signature
     * @return the class name from the signature
     */
    public static String getClassNameFromClassSignature(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_CLASS_NAME];
    }

    /**
     * Retrieves the class name from a class hash.
     *
     * @param hash the class hash
     * @return the class name
     */
    public static String getClassNameFromClassHash(String hash) {
        String signature = CallGraphDataStructures.getClassSignatureFromHash(hash);
        if (signature != null) {
            return CallGraphUtility.getClassNameFromClassSignature(signature);
        } else {
            return CallGraphUtility.getLibraryName(hash);
        }
    }

    /**
     * Retrieves the class hash from a container declaration AST node.
     *
     * @param containerDeclaration the AST node representing a type, enum, or anonymous class declaration
     * @param filePath the file path where the declaration occurs (can be null for type declarations)
     * @return the class hash for the container declaration, or null if not found
     */
    public static String getClassHashFromContainerDeclaration(ASTNode containerDeclaration, String filePath) {
        TokenRange declarationTokenRange = null;
        ITypeBinding typeBinding = null;
        String classHash = null;
        if (containerDeclaration instanceof AbstractTypeDeclaration) {
            typeBinding = ((AbstractTypeDeclaration) containerDeclaration).resolveBinding();
            declarationTokenRange = ASTNodeUtility.getTokenRangeOfBinding(typeBinding);
        } else if (containerDeclaration instanceof AnonymousClassDeclaration) {
            // For anonymous class declarations, we expect that the file name be passed.
            // But for some, there had been no file paths presented from outside.
            // This is covered by the additional null check when we need the path.
            declarationTokenRange = new TokenRange(
                    containerDeclaration.getStartPosition(),
                    containerDeclaration.getLength(),
                    filePath != null
                            ? filePath
                            : ASTNodeUtility.getFilePathFromCompilationUnit(
                                    ASTNodeUtility.findNearestAncestor(containerDeclaration, CompilationUnit.class)));
        }
        // At this point, the declaration token range must be found if the class is coming from
        // a type declaration or an anonymous declaration. If for some reason, we passed a different AST node
        // from the client, the token range will nto be calculated. The null check covers that case.
        if (declarationTokenRange != null) {
            classHash = CallGraphDataStructures.getClassHashFromTokenRange(declarationTokenRange);
            if (classHash != null) {
                return classHash;
            }
        }
        // We will come here if the token range based hash retrieval did not work before,
        // now we are calculating the class hash from binding
        if (containerDeclaration instanceof TypeDeclaration) {
            // binding already calculated above
        } else if (containerDeclaration instanceof AnonymousClassDeclaration) {
            typeBinding = ((AnonymousClassDeclaration) containerDeclaration).resolveBinding();
        }
        if (typeBinding != null) {
            return getClassHashFromTypeBinding(typeBinding, declarationTokenRange, filePath);
        }
        return null;
    }

    /**
     * Retrieves the class hash from a type binding.
     *
     * @param binding the type binding for the class
     * @param tokenRange the token range of the type declaration (can be null)
     * @param filepath the file path for resolving library types (can be null)
     * @return the class hash for the type binding
     */
    public static String getClassHashFromTypeBinding(ITypeBinding binding, TokenRange tokenRange, String filepath) {
        if (isResolvableType(binding)) {
            String bindingHash = CallGraphDataStructures.getBindingHash(binding, tokenRange);
            String classHashFromStoredInfo = CallGraphDataStructures.getClassHashFromBindingHash(bindingHash);
            if (classHashFromStoredInfo != null) {
                // Phase 2, OPTI 1
                // Getting class hash from stored info
                return classHashFromStoredInfo;
            } else {
                String fst = getClassHashAndSignatureFromSourceTypeBinding(binding, tokenRange).fst;
                // For the same type, multiple entries may be made. This is
                // because JDT returns different binding when a resolve binding is
                // called from an AST node vs a declaring class binding is being returned from the method binding
                // So, we may end up having multiple binding hashes with the same class hash
                // This is fine since we never use the reverse map.
                if (bindingHash != null) {
                    CallGraphDataStructures.addToBindingHashToClassHashMap(bindingHash, fst);
                }
                return fst;
            }
        } else if (binding != null && binding.isWildcardType()) {
            // Fixed hash for wildcard type
            return Constants.WILDCARD_TYPE;
        } else if (binding != null && binding.isTypeVariable()) {
            // Use the name of the type for symbolic types
            return binding.getName();
        } else {
            // We can handle the library types and the source types similarly since
            // the library types also have their own unique hash/signature now.
            String libraryTypeHash = getLibraryTypeHash(binding, filepath);
            return libraryTypeHash;
        }
    }

    /**
     * Retrieves the class hash and signature from a source type binding.
     *
     * @param binding the type binding for the class (must not be null)
     * @param range the token range for the type declaration (can be null)
     * @return pair containing the class hash and signature
     */
    public static Pair<String, String> getClassHashAndSignatureFromSourceTypeBinding(
            ITypeBinding binding, TokenRange range) {
        if (range == null) {
            range = ASTNodeUtility.getTokenRangeOfBinding(binding);
        }
        if (range.getFileName() == null) {
            return null;
        }
        String className = null;
        String typeType = null;
        if (binding.isAnonymous()) {
            // Class name is calculated from interfaces or super class of the anon declaration
            // Previously was here, now moved to this API method.
            String containerNameFromBinding = getProperClassNameForHashFromBinding(binding);
            className = CG_ANONYMOUS_TYPE + CG_ANON_NAME_SEPARATOR + containerNameFromBinding;
            typeType = CG_CLASS_ANON;
        } else {
            className = getProperClassNameForHashFromBinding(binding);
            if (binding.isInterface()) {
                typeType = CG_CLASS_INTERFACE;
            } else if (binding.isEnum()) {
                typeType = CG_CLASS_ENUM;
            } else if (Modifier.isAbstract(binding.getModifiers())) {
                // Separate abstract class from regular class
                typeType = CG_CLASS_ABSTRACT;
            } else {
                typeType = CG_CLASS_CLASS;
            }
        }
        StringBuilder signatureBuilder = new StringBuilder();
        signatureBuilder
                .append(range.getFileName())
                .append(CG_SEPARATOR)
                .append(range.getOffset())
                .append(CG_SEPARATOR)
                .append(range.getLength())
                .append(CG_SEPARATOR)
                .append(className)
                .append(CG_SEPARATOR)
                .append(typeType);
        return Pair.of(HashUtility.generateSHA1(signatureBuilder.toString()), signatureBuilder.toString());
    }

    /**
     * Checks if a type binding is recovered (not from source and recovered).
     *
     * @param binding the type binding to check
     * @return true if the binding is recovered, false otherwise
     */
    public static boolean isRecovered(ITypeBinding binding) {
        if (binding != null && binding.isRecovered() && !binding.isFromSource()) {
            return true;
        }
        return false;
    }

    /**
     * Retrieves the access modifier bitset for a field.
     *
     * @param fieldBinding the binding for the field
     * @param containerBinding the binding for the container of the field
     * @return the encoded bitset representing the field's access modifiers
     */
    public static byte getFieldAccessModifier(IBinding fieldBinding, ITypeBinding containerBinding) {
        int value = 0;
        if (containerBinding != null && containerBinding.isInterface()) {
            // All fields of an interface are public static final
            value = AccessModifiers.PUBLIC + AccessModifiers.STATIC + AccessModifiers.FINAL;
        } else {
            int modifiers = fieldBinding.getModifiers();
            if (Modifier.isFinal(modifiers)) {
                value += AccessModifiers.FINAL;
            }
            if (Modifier.isStatic(modifiers)) {
                value += AccessModifiers.STATIC;
            }
            if (Modifier.isPrivate(modifiers)) {
                value += AccessModifiers.PRIVATE;
            } else if (Modifier.isPublic(modifiers)) {
                value += AccessModifiers.PUBLIC;
            } else if (Modifier.isProtected(modifiers)) {
                value += AccessModifiers.PROTECTED;
            } else {
                value += AccessModifiers.DEFAULT;
            }
        }
        return (byte) value;
    }

    /**
     * Get class name for binding
     *
     * @param binding the binding provided
     * @return the class name
     */
    private static String getProperClassNameForHashFromBinding(ITypeBinding binding) {
        String className = null;

        if (binding.isAnonymous()) {
            // Get info from interfaces or super class information
            // The anonymous binding code was distributed at different places before
            // Now everything is here
            if (binding.getInterfaces().length != 0) {
                ITypeBinding[] interfaces = binding.getInterfaces();
                // Defensive check: Not likely because interface is different from this
                // Prevents infinite loop
                if (!interfaces[0].equals(binding)) {
                    className = getProperClassNameForHashFromBinding(interfaces[0]);
                }
            }
            // We will check for super class only if interfaces check returns null
            if (className == null) {
                if (binding.getSuperclass() != null) {
                    // Defensive check: Not likely because super class is different from this
                    // Prevents infinite loop
                    if (!binding.getSuperclass().equals(binding)) {
                        className = getProperClassNameForHashFromBinding(binding.getSuperclass());
                    }
                }
            }
        }
        if (className == null) {
            className = binding.getName();
        }
        if (binding.isParameterizedType()) {
            int index = className.indexOf(PARAM_TYPE_NAME_INDICATOR);
            if (index > -1) {
                className = className.substring(0, index);
            }
        }
        return className;
    }

    /**
     * Retrieves the class hash from a Type AST node.
     *
     * @param type the Type AST node representing a class
     * @return the class hash for the type
     */
    public static String getClassHashFromType(Type type) {
        ITypeBinding binding = type.resolveBinding();
        if (binding != null) {
            CompilationUnit cu = ASTNodeUtility.findNearestAncestor(type, CompilationUnit.class);
            String filePath = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
            if (CallGraphUtility.isRecovered(binding)) {
                return CallGraphUtility.getLibraryTypeHash(binding, filePath);
            } else {
                return CallGraphUtility.getClassHashFromTypeBinding(binding, null, filePath);
            }
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(Constants.LIB_TYPE).append(CG_SEPARATOR).append(type.toString());
            return builder.toString();
        }
    }

    /**
     * Retrieves the TypeInfo object for a given class hash.
     *
     * @param hash the class hash
     * @return the TypeInfo object for the class, or null if not found
     */
    public static TypeInfo getTypeInfoFromClassHash(String hash) {
        String sign = CallGraphDataStructures.getClassSignatureFromHash(hash);
        if (sign != null) {
            ASTNode node = CallGraphUtility.getClassFromClassSignature(sign);
            if (node != null) {
                return TypeCalculator.typeOf(node, false);
            }
        }
        return null;
    }

    /**
     * Generates the SHA1 hash of a complete method signature.
     *
     * @param method the method to hash
     * @param declaration the method declaration AST node (can be null)
     * @return pair containing the method's SHA1 hash and signature
     */
    public static Pair<String, String> getHashCodeOfMethod(IMethod method, ASTNode declaration) {
        String fileName = ASTNodeUtility.getFilePathOfMethod(method);
        String signature = getMethodLocation(method, declaration);
        if (signature == null || fileName == null) {
            return null;
        }
        IType type = method.getDeclaringType();
        ISourceRange source;
        try {
            String staticOrNonStatic = CG_NONSTATIC;
            if (JdtFlags.isStatic(method)) {
                staticOrNonStatic = CG_STATIC;
            }
            source = type.getSourceRange();
            if (method.isLambdaMethod()) {
                staticOrNonStatic += CG_LAMBDA_STATUS_SEPARATOR + CG_LAMBDA_METHOD;
            }
            StringBuilder typeSignatureBuilder;
            if (type.isAnonymous()) {
                CompilationUnit cu = ASTNodeUtility.getCompilationUnitFromFilePath(fileName);
                AnonymousClassDeclaration anonDec = ASTNodeUtility.findNode(
                        AnonymousClassDeclaration.class, cu, source.getOffset(), source.getLength());
                // if Anonymous inner class stated in enum declaration
                // we will get null from findNode.
                if (anonDec == null) {
                    return null;
                }
                ITypeBinding binding = anonDec.resolveBinding();
                if (binding == null) {
                    return null;
                }
                String className = CallGraphUtility.getProperClassNameForHashFromBinding(binding);
                typeSignatureBuilder = new StringBuilder();
                typeSignatureBuilder
                        .append(anonDec.getStartPosition())
                        .append(CG_SEPARATOR)
                        .append(anonDec.getLength());
                typeSignatureBuilder
                        .append(CG_SEPARATOR)
                        .append(CG_ANONYMOUS_TYPE)
                        .append(CG_ANON_NAME_SEPARATOR)
                        .append(className)
                        .append(CG_SEPARATOR)
                        .append(CG_CLASS_ANON);
            } else {
                typeSignatureBuilder = new StringBuilder();
                typeSignatureBuilder
                        .append(source.getOffset())
                        .append(CG_SEPARATOR)
                        .append(source.getLength())
                        .append(CG_SEPARATOR)
                        .append(type.getElementName())
                        .append(CG_SEPARATOR);
                if (type.isInterface()) {
                    typeSignatureBuilder.append(CG_CLASS_INTERFACE);
                } else if (type.isEnum()) {
                    typeSignatureBuilder.append(CG_CLASS_ENUM);
                } else {
                    CompilationUnit cu = ASTNodeUtility.getCompilationUnitFromFilePath(fileName);
                    TypeDeclaration parent = ASTNodeUtility.findNode(
                            TypeDeclaration.class, cu, source.getOffset(), source.getLength());
                    if (Modifier.isAbstract(parent.getModifiers())) {
                        // Separate abstract class from regular class
                        typeSignatureBuilder.append(CG_CLASS_ABSTRACT);
                    } else {
                        typeSignatureBuilder.append(CG_CLASS_CLASS);
                    }
                }
            }
            StringBuilder fullMethodSignatureBuilder = new StringBuilder();
            fullMethodSignatureBuilder
                    .append(fileName)
                    .append(CG_SEPARATOR)
                    .append(method.getElementName())
                    .append(CG_SEPARATOR)
                    .append(signature)
                    .append(CG_SEPARATOR)
                    .append(typeSignatureBuilder.toString())
                    .append(CG_SEPARATOR)
                    .append(staticOrNonStatic);
            return new Pair<String, String>(
                    HashUtility.generateSHA1(fullMethodSignatureBuilder.toString()),
                    fullMethodSignatureBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates the SHA1 hash of a method signature from a method binding.
     *
     * @param binding the method binding to hash
     * @return pair containing the method's SHA1 hash and signature
     */
    public static Pair<String, String> getHashCodeOfMethod(IMethodBinding binding) {
        if (binding == null) {
            return null;
        } else {
            if (binding.isDefaultConstructor()) {
                return createDefaultConstructor(binding.getDeclaringClass(), null, false);
            } else {
                IMethod iMethod = getIMethodFromBinding(binding);
                return getHashCodeOfMethod(iMethod, null);
            }
        }
    }

    /**
     * Checks if a method hash represents a static method.
     *
     * @param hash the method hash to check
     * @return true if the method is static, false otherwise
     */
    public static boolean isStaticMethod(String hash) {
        String signature = CallGraphDataStructures.getMethodSignatureFromHash(hash);
        String[] splits = signature.split(CG_SEPARATOR);
        if (splits[CG_METHOD_STATUS].startsWith(CG_STATIC)) return true;
        else return false;
    }

    /**
     * Checks if a method hash index represents a static method.
     *
     * @param methodHashIndex the method hash index to check
     * @return true if the method is static, false otherwise
     */
    public static boolean isStaticMethod(int methodHashIndex) {
        if (methodHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
            return false;
        } else {
            return isStaticMethod(CallGraphDataStructures.getMethodHashFromIndex(methodHashIndex));
        }
    }

    /**
     * Generates the SHA1 hash and signature for a declared method.
     *
     * @param declaration the method declaration to hash
     * @param filePath the file path where the method is declared (can be null)
     * @return pair containing the method's SHA1 hash and signature
     */
    public static Pair<String, String> getHashCodeAndSignatureOfADeclaredMethod(
            MethodDeclaration declaration, String filePath) {
        // Simplex:
        // Try to see if the information can be calculated from the method declaration token range
        TokenRange methodDeclarationTokenRange = null;
        CompilationUnit compilationUnit = null;
        if (filePath != null) {
            methodDeclarationTokenRange =
                    new TokenRange(declaration.getStartPosition(), declaration.getLength(), filePath);
            String methodHash =
                    CallGraphDataStructures.getMethodHashFromMethodDeclarationTokenRange(methodDeclarationTokenRange);
            if (methodHash != null) {
                String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(methodHash);
                return Pair.of(methodHash, methodSignature);
            }
        } else {
            compilationUnit = ASTNodeUtility.findNearestAncestor(declaration, CompilationUnit.class);
            filePath = ASTNodeUtility.getFilePathFromCompilationUnit(compilationUnit);
            methodDeclarationTokenRange =
                    new TokenRange(declaration.getStartPosition(), declaration.getLength(), filePath);
        }
        // Complex:
        // Proceed with the regular calculation
        IMethodBinding methodBinding = declaration.resolveBinding();
        ITypeBinding containerClassBinding = null;
        IMethod iMethod = null;
        if (methodBinding == null) {
            if (compilationUnit == null) {
                compilationUnit = ASTNodeUtility.findNearestAncestor(declaration, CompilationUnit.class);
            }
            ICompilationUnit cu = (ICompilationUnit) compilationUnit.getJavaElement();
            IJavaElement javaElement = null;
            try {
                javaElement = cu.getElementAt(declaration.getStartPosition());
            } catch (JavaModelException e) {
                // Not doing anything since we cannot do
                // any mitigation if JDT fails
            }
            if (javaElement instanceof IMethod) {
                iMethod = (IMethod) javaElement;
                TypeDeclaration typeDeclaration =
                        ASTNodeUtility.findNearestAncestor(declaration, TypeDeclaration.class);
                containerClassBinding = typeDeclaration.resolveBinding();
            }
        } else {
            containerClassBinding = methodBinding.getDeclaringClass();
            iMethod = getIMethodFromBinding(methodBinding);
        }
        if (iMethod != null
                && !iMethod.isLambdaMethod()
                && containerClassBinding != null
                && (containerClassBinding.isClass()
                        || containerClassBinding.isInterface()
                        || containerClassBinding.isEnum())) {
            boolean processMethod = false;
            if (declaration.getBody() == null) {
                // For methods marked abstract or methods with empty bodies that are in an
                // interface, still process them
                if (methodBinding != null && (Modifier.isAbstract(methodBinding.getModifiers()))
                        || containerClassBinding.isInterface()) {
                    processMethod = true;
                }
            } else {
                processMethod = true;
            }
            if (processMethod) {
                Pair<String, String> hashCodeOfMethod = getHashCodeOfMethod(iMethod, declaration);
                if (hashCodeOfMethod != null && hashCodeOfMethod.fst != null) {
                    CallGraphDataStructures.addToMethodDeclarationTokenRangeToMethodHashMap(
                            methodDeclarationTokenRange, hashCodeOfMethod.fst);
                }
                return hashCodeOfMethod;
            }
        }
        return null;
    }

    /**
     * Retrieves the hash index of the method that services a method invocation.
     *
     * @param invocation the AST node for the method invocation
     * @param callerHash the hash of the calling method (can be null)
     * @param filePath the file path where the invocation occurs (can be null)
     * @param methodCallingContextCache cache for method calling context types
     * @return the hash index of the servicing method, or invalid index if not found
     */
    public static int getServicingMethodHashIndex(
            ASTNode invocation,
            String callerHash,
            String filePath,
            NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache) {
        TokenRange methodInvocationTokenRange = null;
        if (filePath != null) {
            methodInvocationTokenRange =
                    new TokenRange(invocation.getStartPosition(), invocation.getLength(), filePath);
            int servicingMethodHashIndex =
                    CallGraphDataStructures.getServicingMethodIndexFor(methodInvocationTokenRange);
            if (servicingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
                return servicingMethodHashIndex;
            }
        }
        String callingContextDeclaredTypeHash = null;
        if (invocation instanceof MethodInvocation) {
            callingContextDeclaredTypeHash = getCallingContextDeclaredTypeHashOfMethodInvocation(
                    (MethodInvocation) invocation, callerHash, methodCallingContextCache);
            if (callingContextDeclaredTypeHash != null) {
                // In this case, we will have to get the servicing method
                return MethodMatchFinderUtil.getHashOfServicingMethod(callingContextDeclaredTypeHash, invocation);
            }
        } else if (invocation instanceof SuperMethodInvocation) {
            return getServicingMethodHashIndexFromSuperMethodInvocation((SuperMethodInvocation) invocation, callerHash);
        } else if (invocation instanceof ClassInstanceCreation) {
            return getServicingMethodHashIndexFromClassInstanceCreation((ClassInstanceCreation) invocation, null);
        }
        return Constants.INVALID_METHOD_HASH_INDEX;
    }

    /**
     * Calculate the servicing method hash index of a super method invocation.
     *
     * @param invocation the super method invocation expression
     * @param callerMethodHash hash of the caller method. This is used to calculate the calling context type when the
     *     expression is null.
     * @return the hash index of the servicing method. Returns invalid method hash index if hash not available
     */
    private static int getServicingMethodHashIndexFromSuperMethodInvocation(
            SuperMethodInvocation invocation, String callerMethodHash) {
        String callingContextDeclaredTypeHash = null;
        int servicingCalleeMethodIndex = Constants.INVALID_METHOD_HASH_INDEX;
        if (invocation.getQualifier() != null) {
            // A.super.foo() etc.,
            // This occurs when we have say multiple interfaces
            // A and B implemented, both have foo and we want to
            // qualify one foo
            TypeInfo callingContextDeclaredtype = TypeCalculator.typeOf(invocation.getQualifier(), false);
            if (callingContextDeclaredtype != null) {
                callingContextDeclaredTypeHash = callingContextDeclaredtype.getName();
            }
            if (callingContextDeclaredTypeHash != null) {
                servicingCalleeMethodIndex =
                        MethodMatchFinderUtil.getHashOfServicingMethod(callingContextDeclaredTypeHash, invocation);
            }
        } else {
            // super.foo()
            // In this case, traverse the super class and super interfaces for implementation
            String callerClassHash = getContainerHashFromInvocation(invocation, callerMethodHash);
            if (callerClassHash != null) {
                servicingCalleeMethodIndex = MethodMatchFinderUtil.lookupBestMatchingMethodInSuper(
                        callerClassHash, invocation, new HashSet<>());
            }
        }
        return servicingCalleeMethodIndex;
    }

    /**
     * Calculate the servicing method hash index of a constructor call.
     *
     * @param classInstanceCreation the class instance creation call
     * @param filePath the file path of the place where the declaration is. Passed as null most of the time, only passed
     *     as a valid value for anonumous class declaration because in those cases the file will contain the actual
     *     anonymous class declaration.
     * @return the hash index of the servicing constructor. Returns invalid method hash index if hash not available
     */
    private static int getServicingMethodHashIndexFromClassInstanceCreation(
            ClassInstanceCreation classInstanceCreation, String filePath) {
        MethodIdentity identity = MethodHandler.process(classInstanceCreation);
        int consHashIndex = Constants.INVALID_METHOD_HASH_INDEX;
        if (classInstanceCreation.getAnonymousClassDeclaration() != null) {
            AnonymousClassDeclaration anonClass = classInstanceCreation.getAnonymousClassDeclaration();
            // The file path is the same file since the anon declaration happens in the same file
            if (filePath == null) {
                CompilationUnit cu = ASTNodeUtility.findNearestAncestor(classInstanceCreation, CompilationUnit.class);
                filePath = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
            }
            // For
            //     new Polygon("ss", "yy") {
            //           public String getVal() {
            //               return y;
            //           }
            //        };
            // The anon class hash is the inner class, the super class of that is Polygon, which is the instantiated
            // class
            // In this case, we will have a call to the default constructor of the inner class, which will in turn call
            // the Polygon(String, String) constructor of the Polygon class (which is the super class of the anonymous
            // class).
            // The servicing method for the constructor call will be the constructor in the super class.
            // We just return it now. At the client, we create the link from the
            // default constructor of the anonymous inner class to the servicing constructor.
            String anonClassHash = CallGraphUtility.getClassHashFromContainerDeclaration(anonClass, filePath);
            if (anonClassHash != null) {
                String superOfAnonClass = CallGraphDataStructures.getSuperClassOf(anonClassHash);
                if (superOfAnonClass != null && !CallGraphUtility.isInterface(superOfAnonClass)) {
                    // For the servicing constructor with some param, refer to the constructor in the super class
                    // We do not need to check an interface since an interface does not
                    // have any constructor in the first place.
                    // Also, we are going to the super class, because anonymous inner class can not have a constructor
                    // So, the only place the parameterized constructor may come from is the super class.
                    if (identity.getArgParamTypeInfos().size() == 0) {
                        // If the parameter size for the constructor is 0, we may have to call the default
                        // constructor of the super class when there are no zero param constructors available.
                        consHashIndex = MethodMatchFinderUtil.getBestMatchedMethodServicingInvocation(
                                superOfAnonClass, identity);
                        if (consHashIndex == Constants.INVALID_METHOD_HASH_INDEX) {
                            String consHash = CallGraphDataStructures.getDefaultConstructorFor(superOfAnonClass);
                            if (consHash != null) {
                                consHashIndex = CallGraphDataStructures.getMethodIndexFromHash(consHash);
                            }
                        }
                    } else {
                        String instantiatedClassHash = CallGraphDataStructures.getSuperClassOf(anonClassHash);
                        if (instantiatedClassHash != null) {
                            consHashIndex = MethodMatchFinderUtil.getBestMatchedMethodServicingInvocation(
                                    instantiatedClassHash, identity);
                        }
                    }
                }
            }
        } else {
            ITypeBinding instantiatedClassBinding = classInstanceCreation.resolveTypeBinding();
            // We are instantiating an actual class
            // For now, no file path is being sent (the value is null)
            String classHash = CallGraphUtility.getClassHashFromTypeBinding(instantiatedClassBinding, null, filePath);
            if (classHash != null) {
                if (identity.getArgParamTypeInfos().size() == 0) {
                    String consHash = MethodMatchFinderUtil.getMatchingConstructorBasedOnParamCount(classHash, 0);
                    if (consHash == null) {
                        // No zero param constructor declared, so get the default constructor
                        consHash = CallGraphDataStructures.getDefaultConstructorFor(classHash);
                    }
                    if (consHash != null) {
                        consHashIndex = CallGraphDataStructures.getMethodIndexFromHash(consHash);
                    }
                } else {
                    consHashIndex = MethodMatchFinderUtil.getBestMatchedMethodServicingInvocation(classHash, identity);
                }
            }
        }
        return consHashIndex;
    }

    /**
     * Calculates the calling context type hash from a method invocation.
     *
     * @param invocation the method invocation expression
     * @param callerMethodHash the hash of the caller method
     * @param methodCallingContextCache cache for method calling context types
     * @return the hash of the calling context type, or null if not available
     */
    public static String getCallingContextDeclaredTypeHashOfMethodInvocation(
            MethodInvocation invocation,
            String callerMethodHash,
            NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache) {
        ASTNode callingContextExpressionOfInvocation = invocation.getExpression();
        String callingContextDeclaredTypeHash = null;
        if (callingContextExpressionOfInvocation != null) {
            TypeInfo callingContextDeclaredtype = TypeCalculator.getCallingContextType(
                    callingContextExpressionOfInvocation, methodCallingContextCache);
            // Skip if the calling context type is Array
            if (callingContextDeclaredtype != null && !(callingContextDeclaredtype instanceof ArrayTypeInfo)) {
                callingContextDeclaredTypeHash = callingContextDeclaredtype.getName();
            }
        } else {
            // No calling context expression, so calling context type is this
            callingContextDeclaredTypeHash = getContainerHashFromInvocation(invocation, callerMethodHash);
        }
        return callingContextDeclaredTypeHash;
    }

    /**
     * From an AST node, we want to find the container class's hash.
     *
     * <p>A use case for this is a method invocation that does not have a calling context expression, e.g., foo() In
     * this case, we want to find the class in which the method call has been made. This is later used to find the
     * servicing method hash index.
     *
     * <p>Since there may be anonymous classes, we traverse up the AST tree and pick the first type declaration or
     * anonymous type declaration. Note, we could have calculated this from the method declaration. But for the method
     * declaration, we have to calculate the file path to get the method declaration's hash. On the other hand, this can
     * be calculated easily using type binding hash that we saved earlier.
     *
     * @param node any AST node for which we request a container information. Could be a method invocation, a method
     *     declaration, a super method invocation. For each case, we are looking for the container class hash.
     * @param callerMethodHash hash of the caller method. This is used to calculate the container hash if available.
     * @return the container class hash, or null if it cannot be calculated.
     */
    private static String getContainerHashFromInvocation(ASTNode node, String callerMethodHash) {
        if (callerMethodHash != null) {
            return CallGraphUtility.getClassHashFromMethodHash(callerMethodHash);
        } else {
            ASTNode temp = node;
            while (temp != null) {
                // We do not need to pass the file path since this will not be fetched from the library.
                // Instead this is a hash of a real class in code.
                String containerHash = getClassHashFromContainerDeclaration(temp, null);
                if (containerHash != null) {
                    return containerHash;
                }
                temp = temp.getParent();
            }
        }
        return null;
    }

    /**
     * Retrieves the declaration AST node and hash code from a method invocation.
     *
     * @param method the method invocation node
     * @param filePath the file path containing the invocation
     * @return pair containing the method declaration and hash code
     */
    public static Pair<MethodDeclaration, String> getDeclarationAndHashCodeOfMethod(
            MethodInvocation method, String filePath) {
        int servicingMethodHashIndex = CallGraphUtility.getServicingMethodHashIndex(method, null, filePath, null);
        if (servicingMethodHashIndex != Constants.INVALID_METHOD_HASH_INDEX) {
            String servicingMethodHash = CallGraphDataStructures.getMethodHashFromIndex(servicingMethodHashIndex);
            MethodDeclaration decl = CallGraphUtility.getMethodFromMethodHash(servicingMethodHash);
            return Pair.of(decl, servicingMethodHash);
        } else {
            return null;
        }
    }

    /**
     * Retrieves the IMethod from a method binding.
     *
     * @param binding the method binding
     * @return the IMethod for the binding, or null if not found
     */
    public static IMethod getIMethodFromBinding(IBinding binding) {
        if (binding != null && binding.getJavaElement() instanceof IMethod) {
            return (IMethod) binding.getJavaElement();
        }
        return null;
    }

    /**
     * Checks if a type binding is resolvable (from source code with valid Java element).
     *
     * @param binding the type binding to check
     * @return true if the binding is resolvable, false otherwise
     */
    public static boolean isResolvableType(ITypeBinding binding) {
        if (binding == null) {
            return false;
        }
        boolean isResolved = false;
        if (binding.isArray()) {
            ITypeBinding elementType = binding.getElementType();
            isResolved = (elementType.isClass() || elementType.isInterface() || binding.isEnum())
                    && elementType.isFromSource()
                    && (elementType.getJavaElement() instanceof IType);
        } else {
            // 'binding.getJavaElement()' was causing an internal NPE. So added a try-catch to
            // suppress the exception.
            try {
                isResolved = (binding.isClass() || binding.isInterface() || binding.isEnum())
                        && binding.isFromSource()
                        && (binding.getJavaElement() instanceof IType);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        return isResolved;
    }

    /**
     * Checks if a type binding represents a library type.
     *
     * @param binding the type binding to check
     * @return true if the binding is a library type, false otherwise
     */
    public static boolean isLibraryType(ITypeBinding binding) {
        return binding != null && (binding.isClass() || binding.isInterface()) && !binding.isFromSource();
    }

    /**
     * Generates a unique type signature for a library type.
     *
     * @param binding the type binding for the library type
     * @param filePath the source file path where the type is used
     * @return the library type signature string
     */
    public static String getLibraryTypeHash(ITypeBinding binding, String filePath) {
        if (binding == null) {
            return null;
        }
        // Specially handle java.lang.Object
        if (binding.getQualifiedName().equals("java.lang.Object")
                || binding.getQualifiedName().equals("Object")) {
            return Constants.JAVA_LANG_OBJECT;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(Constants.LIB_TYPE).append(CG_SEPARATOR);

        // If binding is not from source and recovered type, try to utilize
        // spec jsons to retrieve the full qualified name.
        // Also utilize CallGraphDataStructures to retrieve package names
        // available from imports in the source file. This is performed
        // in getLibraryTypeQualifiedName(binding.getName(), filePath);
        // Retrieve type name in same fashion for library type as for recovered type
        if (isRecovered(binding) || isLibraryType(binding)) {
            // We only need A from a type name A<B>
            // Separately handle these cases
            String nameFromBinding = null;
            // For inner class like 'View.OnClickListener' getName()
            // gives us the OnClickListener which does not match with
            // any import or class-to-package info, so we get wrong hash.
            // First get the fully qualified name, the apply hurestic like
            // package name is all lower-case and first character of class name
            // is upper-case. So split by '.' and join the class names
            // 'android.view.View.OnClickListener' ---> 'View.OnClickListener'
            // 'android.app.Activity'              ---> 'Activity'
            String fulQualifiedName = binding.getErasure().getQualifiedName();
            String[] parts = fulQualifiedName.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (Character.isUpperCase(parts[i].charAt(0))) {
                    sb.append(parts[i]);
                    sb.append(".");
                }
            }
            sb.append(parts[parts.length - 1]);
            nameFromBinding = sb.toString();
            // Retrieves qualified name from c2p and import information.
            // Use the simple name of the type to retrieve qualified name.
            // If a qualified name is returned from the method, use that one.
            // Otherwise use JDT getQualified method to retrieve qualified name.
            String retrievedName = getLibraryTypeQualifiedNameFromJSONData(nameFromBinding, filePath);
            // Getting qualified name may give inaccurate name adding up package name from file
            builder.append(retrievedName);
            return builder.toString();
        }

        // Just take the name if the qualified name is not available
        // Happens for test cases
        builder.append(binding.getErasure().getName());
        return builder.toString();
    }

    /**
     * Retrieves the fully qualified name for a type using C2P map and file imports.
     *
     * @param typeName the type name to get qualified name for
     * @param filePath the file path containing the type
     * @return the fully qualified name if found, simple name otherwise
     */
    public static String getLibraryTypeQualifiedNameFromJSONData(String typeName, String filePath) {
        Set<String> packageNames = C2PManager.getPackages(typeName);
        Set<String> imports = null;
        if (filePath != null) {
            if (CallGraphDataStructures.getFilePathToImportsMap().containsKey(filePath)) {
                imports = CallGraphDataStructures.getFilePathToImportsMap().get(filePath);
            }
        }
        // For inner class like 'View.OnClickListener', no import ends with typeName,
        // rather the outer class View is imported as 'android.view.View'
        String OuterMostType = typeName.contains(".") ? typeName.substring(0, typeName.indexOf(".")) : typeName;
        // There can be two scenarios, either we have information for this particular typeName in
        // our C2P map, or we don't have. If we have multiple possible package names for the
        // typeName, we need to decide which one to choose among them. For that, we retrieve
        // all imports from the source file for this typeName.

        // Look at the imports first
        // Match type name accurately instead of a mere contains check
        if (imports != null) {
            // If any imported package name contains the typeName,
            // a.b.Type, we return this name as qualified name
            for (String importString : imports) {
                if (importString.endsWith("." + OuterMostType)) {
                    String temp = importString.substring(0, importString.length() - OuterMostType.length());
                    if (temp.length() > 0) {
                        if (temp.endsWith(".")) {
                            return temp + typeName;
                        }
                    } else {
                        return typeName;
                    }
                }
            }
        }
        // If we find only one package name in C2P map, we can construct a qualified name using that
        // package name and return. But if there are multiple options, we need to match them with
        // available imported package names for the source file to decide upon one.
        if (packageNames != null && !packageNames.isEmpty()) {
            Iterator<String> packageNamesIt = packageNames.iterator();
            if (packageNames.size() == 1) {
                return packageNamesIt.next() + "." + typeName;
            }

            if (imports != null) {
                while (packageNamesIt.hasNext()) {
                    String packageName = packageNamesIt.next();
                    // E.g., One possibility can be java.util.Map
                    // Another can be, java.util.*
                    // We construct both of them and find in the source file import set.
                    String qualifiedName = packageName + "." + typeName;
                    String allName = packageName + ".*";
                    // Check default import package java.lang.
                    if (packageName.equals("java.lang")
                            || imports.contains(qualifiedName)
                            || imports.contains(allName)) {
                        return qualifiedName;
                    }
                }
            } else if (packageNames.contains("java.lang")) {
                return "java.lang." + typeName;
            }
        }
        // Reaches here when,
        //  a) no C2P information retrieved. none of the source file imports include the type name.
        //  b) C2P information retrieved. But we have multiple package names against this type name.
        //     None of the source file imports match with any of the C2P package names we have.
        // Use the simple type name in this case.
        return typeName;
    }

    /**
     * Extracts the name or qualified name from a library type hash.
     *
     * @param type the library type hash
     * @return the name or qualified name of the type
     */
    public static String getLibraryName(String type) {
        String[] splits = type.split(CG_SEPARATOR);
        return splits[splits.length - 1];
    }

    /**
     * Extracts only the library name from a library type signature.
     *
     * @param type the library type signature
     * @return the library name without the package name
     */
    public static String getLibraryNameForSpecInfo(String type) {
        String[] splits = type.split(CG_SEPARATOR);
        String libType = splits[splits.length - 1];
        if (libType.contains(".")) {
            int index = libType.lastIndexOf(".");
            libType = libType.substring(index + 1);
        }
        return libType;
    }

    /**
     * Creates the SHA1 hash and signature for a default or static constructor.
     *
     * @param typeBinding the class whose constructor is needed
     * @param containerTokenRange the token range for the container (can be null)
     * @param isStatic true for static constructor, false for default constructor
     * @return pair containing the constructor's SHA1 hash and signature
     */
    public static Pair<String, String> createDefaultConstructor(
            ITypeBinding typeBinding, TokenRange containerTokenRange, boolean isStatic) {
        try {
            if (containerTokenRange != null) {
                String containerHash = CallGraphDataStructures.getClassHashFromTokenRange(containerTokenRange);
                if (containerHash != null) {
                    // If there is a default or static constructor already, we can use that
                    String defaultConstructorHash = isStatic
                            ? CallGraphDataStructures.getStaticConstructorFor(containerHash)
                            : CallGraphDataStructures.getDefaultConstructorFor(containerHash);
                    if (defaultConstructorHash != null) {
                        return Pair.of(
                                defaultConstructorHash,
                                CallGraphDataStructures.getMethodSignatureFromHash(defaultConstructorHash));
                    }
                    // Otherwise we will proceed to calculate one
                }
            }
            // Get the default constructor if already created
            int classOffset;
            int classLength;
            String fileName = null;
            String typeName = null;
            String typeType = null;

            if (typeBinding.isAnonymous()) {
                if (containerTokenRange == null) {
                    // For Anonymous object inside another Anonymous object
                    // previously we got outer range in place of the inner one.
                    // Find the containerTokenRange from typeBinding
                    containerTokenRange = ASTNodeUtility.getTokenRangeOfBinding(typeBinding);
                }
                // Add null checking if containerTokenRange is still null
                if (containerTokenRange == null) {
                    return null;
                }
                classOffset = containerTokenRange.getOffset();
                classLength = containerTokenRange.getLength();
                fileName = containerTokenRange.getFileName();
                // Class name is calculated from interfaces or super class of the anon declaration
                // Previously was here, now moved to this API method.
                String containerNameFromBinding = getProperClassNameForHashFromBinding(typeBinding);
                typeName = CG_ANONYMOUS_TYPE + CG_ANON_NAME_SEPARATOR + containerNameFromBinding;
                typeType = CG_CLASS_ANON;
            } else {
                // For a binding of type declaration, the token range is not needed to calculate the binding hash
                String bindingHash = CallGraphDataStructures.getBindingHash(typeBinding, null);
                String hashFromStoredInfo = CallGraphDataStructures.getClassHashFromBindingHash(bindingHash);
                if (hashFromStoredInfo != null && containerTokenRange != null) {
                    // Phase 2, OPTI 2
                    // Get directly from stored info
                    typeName = getClassNameFromClassHash(hashFromStoredInfo);
                    classOffset = containerTokenRange.getOffset();
                    classLength = containerTokenRange.getLength();
                    fileName = containerTokenRange.getFileName();
                } else {
                    if (containerTokenRange == null) {
                        IMember typeElement = (IMember) typeBinding.getJavaElement();
                        ISourceRange classSourceRange = typeElement.getSourceRange();
                        classOffset = classSourceRange.getOffset();
                        classLength = classSourceRange.getLength();
                        IResource resource = typeElement.getCompilationUnit().getCorrespondingResource();
                        fileName = resource.getRawLocation().toOSString();
                    } else {
                        classOffset = containerTokenRange.getOffset();
                        classLength = containerTokenRange.getLength();
                        fileName = containerTokenRange.getFileName();
                    }
                    typeName = getProperClassNameForHashFromBinding(typeBinding);
                }
                if (typeBinding.isInterface()) {
                    typeType = CG_CLASS_INTERFACE;
                } else if (typeBinding.isEnum()) {
                    typeType = CG_CLASS_ENUM;
                } else if (Modifier.isAbstract(typeBinding.getModifiers())) {
                    // Separate abstract class from regular class
                    typeType = CG_CLASS_ABSTRACT;
                } else {
                    typeType = CG_CLASS_CLASS;
                }
            }
            StringBuilder signatureBuilder = new StringBuilder();
            signatureBuilder
                    .append(fileName)
                    .append(CG_SEPARATOR)
                    .append(typeName)
                    .append(CG_SEPARATOR)
                    .append("0")
                    .append(CG_SEPARATOR)
                    .append("0")
                    .append(CG_SEPARATOR)
                    .append(classOffset)
                    .append(CG_SEPARATOR)
                    .append(classLength)
                    .append(CG_SEPARATOR)
                    .append(typeName)
                    .append(CG_SEPARATOR)
                    .append(typeType)
                    .append(CG_SEPARATOR)
                    .append(isStatic ? CG_INIT_STATIC : CG_INIT);
            String generatedSHA1HashForDefaultCons = HashUtility.generateSHA1(signatureBuilder.toString());
            return Pair.of(generatedSHA1HashForDefaultCons, signatureBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if a method signature represents a dummy method.
     *
     * @param signature the method signature to check
     * @return true if the method is a dummy method, false otherwise
     */
    public static boolean isDummy(String signature) {
        String[] splits = signature.split(CG_SEPARATOR);
        return splits[CG_METHOD_STATUS].startsWith(CG_DUMMY_METHOD);
    }

    /**
     * Retrieves the TokenRange for a class from its hash.
     *
     * @param classHash the class hash
     * @return the TokenRange for the class, or null if not found
     */
    public static TokenRange getTokeRangeFromClassHash(String classHash) {
        String signature = CallGraphDataStructures.getClassSignatureFromHash(classHash);
        if (signature == null) {
            // For library class,  signature may null
            return null;
        }
        IntPair location = getClassLocationFromSignature(signature, true);
        String fileName = getFileNameFromSignature(signature);
        return new TokenRange(location.fst, location.snd, fileName);
    }

    /**
     * Checks if a class hash represents an interface.
     *
     * @param classHash the class hash to check
     * @return true if the class is an interface, false otherwise
     */
    public static boolean isInterface(String classHash) {
        String signature = CallGraphDataStructures.getClassSignatureFromHash(classHash);
        if (signature != null) {
            String[] splits = signature.split(CG_SEPARATOR);
            return splits[CG_CLASS_TYPE].equals(CG_CLASS_INTERFACE);
        }
        return false;
    }

    /**
     * Retrieves the file path from a class hash.
     *
     * @param classHash the class hash
     * @return the full file path, or null if unable to resolve
     */
    public static String getFileNameFromHash(String classHash) {
        String signature = CallGraphDataStructures.getClassSignatureFromHash(classHash);
        if (signature != null) {
            return getFileNameFromSignature(signature);
        }
        return null;
    }

    /**
     * Checks if a file path is under a test or example directory.
     *
     * @param filePath the file path to check
     * @return true if the path contains test or example directories, false otherwise
     */
    public static boolean isUnderTestOrExampleDirectory(String filePath) {
        for (String dir :
                filePath.substring(0, filePath.lastIndexOf(File.separator)).split(File.separator)) {
            String dirName = dir.toLowerCase();
            if (EXCLUDED_DIRS.contains(dirName)) {
                return true;
            }
        }
        return false;
    }
}
