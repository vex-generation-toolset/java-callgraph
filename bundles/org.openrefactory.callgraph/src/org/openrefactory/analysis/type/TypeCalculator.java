/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
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
import org.openrefactory.util.datastructure.ObjectIntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Calculate TypeInfo representations of Java types from AST nodes.
 *
 * <p>This class provides methods to analyze AST nodes and determine their corresponding
 * TypeInfo objects, supporting both soft and proper type calculations.</p>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class TypeCalculator {

    // List to store all typeinfos.
    private static List<TypeInfo> typeInfoList = new ArrayList<>();
    // Mapping from a typeinfo object to a type info index
    private static Map<TypeInfo, Integer> typeInfoIndexMap = new HashMap<>();

    /**
     * Checks if a TypeInfo object exists in the global map and returns it, or adds it if not found.
     *
     * @param typeInfo the TypeInfo object to check or add
     * @return the existing TypeInfo object if found, or the new object if added
     */
    public static synchronized TypeInfo putOrGetTypeInfoFromMemo(TypeInfo typeInfo) {
        if (typeInfo == null) {
            return null;
        }
        Integer typeIndex = typeInfoIndexMap.get(typeInfo);
        if (typeIndex != null) {
            return typeInfoList.get(typeIndex);
        } else {
            typeInfoIndexMap.put(typeInfo, typeInfoList.size());
            typeInfoList.add(typeInfo);
            return typeInfo;
        }
    }

    /**
     * Returns the type index of a TypeInfo object, adding it to the map if not found.
     *
     * @param typeInfo the TypeInfo object to get index for
     * @return the type index of the TypeInfo object
     */
    public static synchronized int getTypeInfoIndex(TypeInfo typeInfo) {
        if (typeInfo == null) {
            return Constants.INVALID_TYPE_INDEX;
        }
        Integer typeIndex = typeInfoIndexMap.get(typeInfo);
        if (typeIndex == null) {
            typeIndex = typeInfoList.size();
            typeInfoList.add(typeInfo);
            typeInfoIndexMap.put(typeInfo, typeIndex);
        }
        return typeIndex;
    }

    /**
     * Returns the TypeInfo object mapped to the specified type index.
     *
     * @param index the type index
     * @return the TypeInfo object mapped to the index, or null if invalid
     */
    public static synchronized TypeInfo getTypeInfo(int index) {
        if (index < 0) {
            return null;
        } else {
            return typeInfoList.get(index);
        }
    }

    /**
     * Calculates the TypeInfo representation of the type indicated by an AST node.
     *
     * @param node the AST node to calculate the type for
     * @param calculateSoftType true for soft type info (main class only), false for proper type info (with fields)
     * @return the TypeInfo representation of the AST node's type
     */
    public static TypeInfo typeOf(ASTNode node, boolean calculateSoftType) {
        // Retrieve the source file path were the type is being used
        CompilationUnit cu = ASTNodeUtility.findNearestAncestor(node, CompilationUnit.class);
        String filePath = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
        TypeCalculatorVisitor typeVisitor = new TypeCalculatorVisitor(filePath, calculateSoftType);
        node.accept(typeVisitor);
        TypeInfo typeInfo = typeVisitor.getCalculatedTypeInfo();
        if (typeInfo == null) {
            // No type info is found, so check
            // if the node is in return statement
            // we can infer the type from the Return type
            // in the method Declaration
            ASTNode parent = node.getParent();
            while (parent != null) {
                if (parent instanceof ReturnStatement) {
                    MethodDeclaration declaringMethod =
                            ASTNodeUtility.findNearestAncestor(parent, MethodDeclaration.class);
                    Type type = declaringMethod.getReturnType2();
                    typeInfo = typeVisitor.getTypeInfo(type, calculateSoftType);
                    break;
                } else if (parent instanceof ParenthesizedExpression) {
                    parent = parent.getParent();
                } else {
                    break;
                }
            }
        }
        typeInfo = TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        return typeInfo;
    }

    /**
     * Calculates the TypeInfo for a calling context expression, using cache if available.
     *
     * @param contextExp the AST node representing the calling context
     * @param methodCallingContextCache cache for method calling context types
     * @return the TypeInfo for the calling context expression
     */
    public static TypeInfo getCallingContextType(
            ASTNode contextExp, NeoLRUCache<String, Pair<TypeInfo, TokenRange>> methodCallingContextCache) {
        if (methodCallingContextCache != null) {
            Pair<TypeInfo, TokenRange> cachedEntry = methodCallingContextCache.get(contextExp.toString());
            if (cachedEntry != null) {
                TokenRange localVarScope = cachedEntry.snd;
                // If the scope is null, the variable is accessible from anywhere
                // in the function body. Otherwise the cached entry is a local variable,
                // check if the context expression is inside its scope.
                if (localVarScope == null
                        || localVarScope.encompasses(ASTNodeUtility.getTokenRangeFromNode(contextExp))) {
                    return cachedEntry.fst;
                }
            }
        }
        // Retrieve the source file path were the type is being used
        CompilationUnit cu = ASTNodeUtility.findNearestAncestor(contextExp, CompilationUnit.class);
        String filePath = ASTNodeUtility.getFilePathFromCompilationUnit(cu);
        // Calculate type and update the cache, methodCallingContextCache is
        // passed to visitor to avoid redundant calculations.
        TypeCalculatorVisitor typeVisitor = new TypeCalculatorVisitor(filePath, methodCallingContextCache);
        contextExp.accept(typeVisitor);
        TypeInfo callingContextType = typeVisitor.getCalculatedTypeInfo();
        TokenRange localVarScope = ASTNodeUtility.getLocalVariableScope(contextExp);
        callingContextType = TypeCalculator.putOrGetTypeInfoFromMemo(callingContextType);
        if (methodCallingContextCache != null) {
            methodCallingContextCache.cache(contextExp.toString(), Pair.of(callingContextType, localVarScope));
        }
        return callingContextType;
    }

    /**
     * Gets the TypeInfo object for a Type AST node.
     *
     * @param type the Type AST node
     * @param filePath the file path where the type is used
     * @param calculateSoftType true for soft type info (main class only), false for proper type info (with fields)
     * @return the TypeInfo for the given type
     */
    public static TypeInfo typeOf(Type type, String filePath, boolean calculateSoftType) {
        TypeCalculatorVisitor typeVisitor = new TypeCalculatorVisitor(filePath, calculateSoftType);
        TypeInfo typeInfo = typeVisitor.getTypeInfo(type, calculateSoftType);
        typeInfo = TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        return typeInfo;
    }

    /**
     * Returns the qualified name of an invoked method (typically for library methods without bindings).
     *
     * @param invocation the method invocation node
     * @param filePath the file path
     * @param calculateSoftType flag for soft type calculation
     * @return the qualified name of the invoked method
     */
    public static String qualifiedNameOf(MethodInvocation invocation, String filePath, boolean calculateSoftType) {
        TypeCalculatorVisitor typeVisitor = new TypeCalculatorVisitor(filePath, calculateSoftType);
        String qualifiedName = typeVisitor.getQualifiedName(invocation);
        return qualifiedName;
    }

    /**
     * Calculates the TypeInfo representation of a type from its type binding.
     *
     * @param containerBinding the type binding for the type
     * @param filePath path to the source file where the type is used
     * @param containerTokenRange the token range for the container (can be null)
     * @param containerHash the container hash if already calculated (can be null)
     * @param calculateSoftType true for soft type info (main class only), false for proper type info (with fields)
     * @return the TypeInfo representation of the type binding
     */
    public static TypeInfo typeOf(
            ITypeBinding containerBinding,
            String filePath,
            TokenRange containerTokenRange,
            String containerHash,
            boolean calculateSoftType) {
        TypeCalculatorVisitor typeVisitor = null;
        if (containerTokenRange != null) {
            typeVisitor = new TypeCalculatorVisitor(containerTokenRange.getFileName(), calculateSoftType);
        } else {
            typeVisitor = new TypeCalculatorVisitor(filePath, calculateSoftType);
        }
        TypeInfo typeInfo =
                typeVisitor.getTypeInfo(containerBinding, containerTokenRange, containerHash, calculateSoftType);
        typeInfo = TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
        return typeInfo;
    }

    /**
     * Gets the class hash index from a TypeInfo object.
     *
     * @param typeInfo the TypeInfo object to analyze
     * @return the class hash index for the class, or invalid index if not found
     */
    public static int getClassHashIndex(TypeInfo typeInfo) {
        int index = Constants.INVALID_CLASS_HASH_INDEX;
        if (typeInfo != null) {
            if (typeInfo instanceof ArrayTypeInfo) {
                index = getClassHashIndex(((ArrayTypeInfo) typeInfo).getElementType());
            } else if (typeInfo instanceof ClassTypeInfo) {
                String hash = typeInfo.getName();
                index = CallGraphDataStructures.getBitIndexFromClassHash(hash);
            } else if (typeInfo instanceof EnumTypeInfo) {
                String hash = typeInfo.getName();
                index = CallGraphDataStructures.getBitIndexFromClassHash(hash);
            } else if (typeInfo instanceof ParameterizedTypeInfo) {
                String hash = ((ParameterizedTypeInfo) typeInfo).getName();
                index = CallGraphDataStructures.getBitIndexFromClassHash(hash);
            } else if (typeInfo instanceof ScalarTypeInfo) {
                // For String and boxed types, return index
                if (((ScalarTypeInfo) typeInfo).isClassType()) {
                    String hash = typeInfo.getName();
                    index = CallGraphDataStructures.getBitIndexFromClassHash(hash);
                }
            } else if (typeInfo instanceof SymbolicTypeInfo) {
                // Do nothing
            } else if (typeInfo instanceof WildCardTypeInfo) {
                // If there is an upper bound of type,
                // return that; otherwise ignore
                if (((WildCardTypeInfo) typeInfo).isUpperBound()) {
                    index = getClassHashIndex(((WildCardTypeInfo) typeInfo).getBoundType());
                }
            }
        }
        return index;
    }

    private static TypeInfo getTypeInfoFromTypeHash(String declaredTypeHash) {
        ASTNode containerNode = CallGraphUtility.getClassFromClassSignature(
                CallGraphDataStructures.getClassSignatureFromHash(declaredTypeHash));
        TokenRange containerTokenRange = CallGraphUtility.getTokeRangeFromClassHash(declaredTypeHash);
        if (containerNode != null && containerTokenRange != null) {
            ITypeBinding binding = null;
            if (containerNode instanceof TypeDeclaration) {
                binding = ((TypeDeclaration) containerNode).resolveBinding();
            } else if (containerNode instanceof AnonymousClassDeclaration) {
                binding = ((AnonymousClassDeclaration) containerNode).resolveBinding();
            }
            if (binding != null) {
                return TypeCalculator.typeOf(
                        binding, containerTokenRange.getFileName(), containerTokenRange, declaredTypeHash, true);
            }
        }
        return null;
    }

    /**
     * Matches actual type info with declared type info to find a replacement for a symbolic type.
     *
     * @param typeToChange the symbolic type seeking a replacement
     * @param actualTypeInfo the type info of the actual type to match against
     * @param declaredTypeHash the hash of the container for matching (can be null)
     * @param declaredTypeInfo the type info for the container (can be null)
     * @return a replacement symbolic type if found, null otherwise
     */
    public static TypeInfo getReplacementForSymbolicTypeByMatchingTypesFrom(
            TypeInfo typeToChange, TypeInfo actualTypeInfo, String declaredTypeHash, TypeInfo declaredTypeInfo) {
        // Get the actual
        String actualTypeHash = null;
        if (actualTypeInfo != null) {
            int classHashIndex = TypeCalculator.getClassHashIndex(actualTypeInfo);
            if (classHashIndex != Constants.INVALID_CLASS_HASH_INDEX) {
                actualTypeHash = CallGraphDataStructures.getClassHashFromBitIndex(classHashIndex);
            }
        }
        if (actualTypeHash != null) {
            if (declaredTypeInfo == null) {
                // Get the declared type info from the hash
                // Since the declared type info is from the declaration
                // it will have parameterized types which we are trying to match
                // In that case, we do not need to map to concete parameters
                if (declaredTypeHash != null) {
                    declaredTypeInfo = CallGraphDataStructures.getSoftTypeInfoFromClassId(declaredTypeHash);
                    // There is a chance that the declared type info is not found from the map
                    // We do not store in the soft map or proper map when
                    // the type is a parameterized type
                    // In that case, we have to actually calculate the type
                    if (declaredTypeInfo == null && !declaredTypeHash.startsWith(Constants.LIB_TYPE)) {
                        declaredTypeInfo = getTypeInfoFromTypeHash(declaredTypeHash);
                    }
                }
            }
            if (declaredTypeInfo != null) {
                List<TypeInfo> workList = new ArrayList<>(4);
                Set<TypeInfo> seenInfo = new HashSet<>(7);
                workList.add(actualTypeInfo);
                while (!workList.isEmpty()) {
                    TypeInfo temp = workList.remove(0);
                    if (!seenInfo.contains(temp)) {
                        seenInfo.add(temp);
                    }
                    if (temp instanceof ParameterizedTypeInfo) {
                        // Try to match otherwise move on
                        Map<TypeInfo, TypeInfo> capturedSymbolicTypes = new HashMap<>(3);
                        Map<ObjectIntPair<TypeInfo>, TypeInfo> capturedWildCardTypes = new HashMap<>(3);
                        Set<ObjectIntPair<TypeInfo>> matchedTraversalTypesDuringParsing = new HashSet<>(4);
                        Set<ObjectIntPair<TypeInfo>> matchedTraversalTypesDuringReplcement = new HashSet<>(4);
                        declaredTypeInfo.parseAndMapSymbols(
                                temp,
                                null,
                                matchedTraversalTypesDuringParsing,
                                capturedSymbolicTypes,
                                capturedWildCardTypes);
                        if (!capturedSymbolicTypes.isEmpty()) {
                            // Even though we have found a match
                            Pair<Boolean, TypeInfo> replacementResult = typeToChange.replaceSymbol(
                                    null,
                                    matchedTraversalTypesDuringReplcement,
                                    capturedSymbolicTypes,
                                    capturedWildCardTypes);
                            if (replacementResult != null && replacementResult.fst && replacementResult.snd != null) {
                                return replacementResult.snd;
                            }
                        }
                    }
                    // Could not match with the actual type info
                    // So, may match with some super class of the actual type info
                    // But for this we need the actual param types with concrete fields
                    // So, we resolve this from binding
                    int tempTypeHashIndex = TypeCalculator.getClassHashIndex(temp);
                    if (tempTypeHashIndex != Constants.INVALID_CLASS_HASH_INDEX) {
                        String tempTypeHash = CallGraphDataStructures.getClassHashFromBitIndex(tempTypeHashIndex);
                        if (tempTypeHash != null) {
                            String classSignature = CallGraphDataStructures.getClassSignatureFromHash(tempTypeHash);
                            if (classSignature == null || classSignature.contains(Constants.LIB_TYPE)) {
                                // If for some reason, we get a null type,
                                // or the class is actually a library type, then
                                // the matching stops.
                                continue;
                            }
                            ASTNode containerNode = CallGraphUtility.getClassFromClassSignature(classSignature);
                            ITypeBinding binding = null;
                            if (containerNode instanceof TypeDeclaration) {
                                binding = ((TypeDeclaration) containerNode).resolveBinding();
                            } else if (containerNode instanceof AnonymousClassDeclaration) {
                                binding = ((AnonymousClassDeclaration) containerNode).resolveBinding();
                            }
                            if (binding != null) {
                                // Get type of super interface
                                ITypeBinding[] interfaces = binding.getInterfaces();
                                if (interfaces != null) {
                                    for (ITypeBinding interfaceBinding : interfaces) {
                                        String interfaceHash = null;
                                        if (CallGraphUtility.isResolvableType(interfaceBinding)) {
                                            // The interface is from source code
                                            // The interface that is implemented is supposed to be a regular interface
                                            // So, we use the regular method for getting token range
                                            TokenRange interfaceTokenRange =
                                                    ASTNodeUtility.getTokenRangeOfBinding(interfaceBinding);
                                            interfaceHash = CallGraphDataStructures.getClassHashFromTokenRange(
                                                    interfaceTokenRange);
                                            if (interfaceHash == null) {
                                                String containerHash = CallGraphUtility.getClassHashFromTypeBinding(
                                                        binding,
                                                        interfaceTokenRange,
                                                        interfaceTokenRange.getFileName());
                                                // Ignoring the interface signature here
                                                // The only method to update hash to signature map is the process
                                                // container method
                                                // But we may assign the bit index now
                                                if (containerHash != null) {
                                                    interfaceHash = containerHash;
                                                }
                                            }
                                            if (interfaceHash != null) {
                                                TypeInfo interfaceType = TypeCalculator.typeOf(
                                                        interfaceBinding,
                                                        interfaceTokenRange.getFileName(),
                                                        interfaceTokenRange,
                                                        interfaceHash,
                                                        true);
                                                if (interfaceType != null) {
                                                    workList.add(interfaceType);
                                                }
                                            }
                                        }
                                    }
                                }
                                // Get type of super class
                                ITypeBinding superClassBinding = binding.getSuperclass();
                                String superClassHash = null;
                                if (CallGraphUtility.isResolvableType(superClassBinding)) {
                                    // The super class is from source code
                                    // The super class is supposed to be a regular class
                                    // So, we use the regular method for getting token range
                                    TokenRange superClassTokenRange =
                                            ASTNodeUtility.getTokenRangeOfBinding(superClassBinding);
                                    superClassHash =
                                            CallGraphDataStructures.getClassHashFromTokenRange(superClassTokenRange);
                                    if (superClassHash == null) {
                                        String containerHash = CallGraphUtility.getClassHashFromTypeBinding(
                                                binding, superClassTokenRange, superClassTokenRange.getFileName());
                                        // Ignoring the super class signature here
                                        // The only method to update hash to signature map is the process container
                                        // method
                                        if (containerHash != null) {
                                            superClassHash = containerHash;
                                        }
                                    }
                                    if (superClassHash != null) {
                                        TypeInfo superClassType = TypeCalculator.typeOf(
                                                superClassBinding,
                                                superClassTokenRange.getFileName(),
                                                superClassTokenRange,
                                                superClassHash,
                                                true);
                                        if (superClassType != null) {
                                            workList.add(superClassType);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
