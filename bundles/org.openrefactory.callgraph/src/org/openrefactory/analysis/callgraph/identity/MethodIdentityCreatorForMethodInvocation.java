/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.identity;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * Issue 25
 *
 * Implements the templates used in the AbstractMethodIdentityCreatorForLibraryCall class
 * This is for processing the method invocation AST node
 *
 * @author Munawar Hafiz
 */
public class MethodIdentityCreatorForMethodInvocation extends AbstractMethodIdentityCreatorForLibraryCall {
    private MethodInvocation mi;
    
    public MethodIdentityCreatorForMethodInvocation(MethodInvocation m) {
        this.mi = m;
    }
    
    @SuppressWarnings("unchecked")
    protected List<ASTNode> getArguments() {
        return mi.arguments();
    }
    
    protected String getName() {
        return mi.getName().toString();
    }
    
    protected ASTNode getExpression() {
        return mi.getExpression();
    }
}
