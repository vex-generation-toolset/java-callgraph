/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.identity;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

/**
 * Issue 25
 *
 * Implements the templates used in the AbstractMethodIdentityCreatorForLibraryCall class
 * This is for processing the super method invocation AST node
 *
 * @author Munawar Hafiz
 */
public class MethodIdentityCreatorForSuperMethodInvocation extends AbstractMethodIdentityCreatorForLibraryCall {
    private SuperMethodInvocation sm;
    
    public MethodIdentityCreatorForSuperMethodInvocation(SuperMethodInvocation m) {
        this.sm = m;
    }
    
    @SuppressWarnings("unchecked")
    protected List<ASTNode> getArguments() {
        return sm.arguments();
    }
    
    protected String getName() {
        return sm.getName().toString();
    }
    
    protected ASTNode getExpression() {
        return sm.getName();
    }
}
