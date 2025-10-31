/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.callgraph.identity.AbstractMethodIdentityCreatorForLibraryCall;
import org.openrefactory.analysis.callgraph.identity.MethodIdentityCreatorForClassInstanceCreation;
import org.openrefactory.analysis.callgraph.identity.MethodIdentityCreatorForMethodInvocation;
import org.openrefactory.analysis.callgraph.identity.MethodIdentityCreatorForSuperConstructorInvocation;
import org.openrefactory.analysis.callgraph.identity.MethodIdentityCreatorForSuperMethodInvocation;
import org.openrefactory.util.CallGraphUtility;

/**
 * Contains the utility methods for ExtendedCallGraph.
 *
 * @author Munawar Hafiz
 */
public class ExtendedCallGraphUtils {
    
    /**
     * Issue 25
     * Add extended call graph entry for a library method call.
     * The call may be of various kinds, e.g.,
     *   (a) instance method invocation
     *   (b) class method invocation
     *   (c) constructors
     *
     * @param node                           the AST node for call to process
     * @param callerMethodHash               the hash of the container method of the method invocation 
     * @param invocationTokenRange           the invocation token range 
     * @param callingContextDeclaredTypeHash the calling context declared type hash
     * @param filePath                       the file path where the invocation happens
     */
    public static void addEntryForLibraryMethodInvocation(
        ASTNode node, 
        String callerMethodHash,
        TokenRange invocationTokenRange, 
        String callingContextDeclaredTypeHash, 
        String filePath)
    {
        Map<MethodIdentity, Pair<String, String>> calculatedData = new HashMap<>(1);
        MethodIdentity identity = null;
        Pair<String, String> methodInfoPair = null;
        if (node instanceof MethodInvocation m) {
            AbstractMethodIdentityCreatorForLibraryCall amc = new MethodIdentityCreatorForMethodInvocation(m);
            identity = amc.createMethodIdentity(false, true, invocationTokenRange.getFileName());
            methodInfoPair = CallGraphUtility.getHashCodeAndSignatureOfLibraryMethod(identity,
                callingContextDeclaredTypeHash, invocationTokenRange);
            calculatedData.put(identity, methodInfoPair);
        } else if (node instanceof ClassInstanceCreation c) {
            AbstractMethodIdentityCreatorForLibraryCall amc = new MethodIdentityCreatorForClassInstanceCreation(c);
            identity = amc.createMethodIdentity(false, true, invocationTokenRange.getFileName());
            methodInfoPair = CallGraphUtility.getHashCodeAndSignatureOfLibraryMethod(identity,
                callingContextDeclaredTypeHash, invocationTokenRange);
            calculatedData.put(identity, methodInfoPair);
            // If the constructor has zero params, create an additional entry for the default constructor
            if (identity.getArgParamTypeInfos().isEmpty()) {
                identity = new MethodIdentity("<init>", identity.getReturnTypeInfo(), identity.getArgParamTypeInfos());
                // Even though the constructor is not static
                // we set the static bit here so that the method construction
                // appears to have .<init>
                identity.setStaticBit();
                identity.setVirtualMethodBit();
                identity.setConstructorBit();
                methodInfoPair = CallGraphUtility.getHashCodeAndSignatureOfLibraryMethod(identity,
                    callingContextDeclaredTypeHash, invocationTokenRange);
                calculatedData.put(identity, methodInfoPair);
            }
        } else if (node instanceof SuperMethodInvocation sm) {
            AbstractMethodIdentityCreatorForLibraryCall amc = new MethodIdentityCreatorForSuperMethodInvocation(sm);
            identity = amc.createMethodIdentity(false, false, invocationTokenRange.getFileName());
            methodInfoPair = CallGraphUtility.getHashCodeAndSignatureOfLibraryMethod(identity,
                callingContextDeclaredTypeHash, invocationTokenRange);
            calculatedData.put(identity, methodInfoPair);
        } else if (node instanceof SuperConstructorInvocation sc) {
            // The caller hash passed here is in fact the super class hash
            AbstractMethodIdentityCreatorForLibraryCall amc = new MethodIdentityCreatorForSuperConstructorInvocation(sc,
                callingContextDeclaredTypeHash);
            identity = amc.createMethodIdentity(false, true, invocationTokenRange.getFileName());
            methodInfoPair = CallGraphUtility.getHashCodeAndSignatureOfLibraryMethod(identity,
                callingContextDeclaredTypeHash, invocationTokenRange);
            calculatedData.put(identity, methodInfoPair);
        }
        for (Entry<MethodIdentity, Pair<String, String>> entry: calculatedData.entrySet()) {
            int index = CallGraphDataStructures
                .getMethodHashIndexAndPotentiallyUpdateOtherInitialStructures(entry.getValue().fst,
                    entry.getValue().snd);
            CallGraphDataStructures.addMethodIdentity(index, entry.getKey());
            CallGraphDataStructures.getExtendedCallGraph().addEdge(callerMethodHash,
                entry.getValue().fst, invocationTokenRange);
        }
    }
}
