/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.identity;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;

import org.openrefactory.util.Constants;
import org.openrefactory.util.CallGraphUtility;

/**
 * Issue 25
 *
 * Implements the templates used in the AbstractMethodIdentityCreatorForLibraryCall class
 * This is for processing the super constructor invocation AST node
 *
 * @author Munawar Hafiz
 */
public class MethodIdentityCreatorForSuperConstructorInvocation extends AbstractMethodIdentityCreatorForLibraryCall {
    private SuperConstructorInvocation sc;
    private String superclassHash;
    
    public MethodIdentityCreatorForSuperConstructorInvocation(SuperConstructorInvocation c, String superclassHash) {
        this.sc = c;
        this.superclassHash = superclassHash;
    }
    
    @SuppressWarnings("unchecked")
    protected List<ASTNode> getArguments() {
        return sc.arguments();
    }
    
    protected String getName() {
        if (superclassHash.startsWith(Constants.LIB_TYPE)) {
            String temp = new String(superclassHash);
            int angleBracketIndex = temp.indexOf("<");
            if (angleBracketIndex >= 0) {
                temp = temp.substring(0, angleBracketIndex);
            }
            int lastDotIndex = temp.lastIndexOf(".");
            if (lastDotIndex >= 0) {
                temp = temp.substring(lastDotIndex + 1);
            }
            return temp;
        } else {
            // Should never come here, because this is always invoked on library classes.
            return CallGraphUtility.getClassNameFromMethodSignature(superclassHash);
        }
    }
    
    protected ASTNode getExpression() {
        // This is not used, since the super class method is not static
        return sc.getExpression();
    }
}
