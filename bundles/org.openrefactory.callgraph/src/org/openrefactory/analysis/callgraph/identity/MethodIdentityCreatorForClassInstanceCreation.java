/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.identity;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

/**
 * Issue 25
 *
 * Implements the templates used in the AbstractMethodIdentityCreatorForLibraryCall class
 * This is for processing the class instance creation AST node
 *
 * @author Munawar Hafiz
 */
public class MethodIdentityCreatorForClassInstanceCreation extends AbstractMethodIdentityCreatorForLibraryCall {
    private ClassInstanceCreation cic;
    
    public MethodIdentityCreatorForClassInstanceCreation(ClassInstanceCreation c) {
        this.cic = c;
    }
    
    @SuppressWarnings("unchecked")
    protected List<ASTNode> getArguments() {
        return cic.arguments();
    }
    
    protected String getName() {
        Type type = cic.getType();
        if (type instanceof SimpleType) {
            return cic.getType().toString();
        } else if (type instanceof ParameterizedType) {
            String typeStr = type.toString();
            // Remove the < from the parameterized type
            int angleBracketIndex = typeStr.indexOf("<");
            if (angleBracketIndex >= 0) {
                typeStr = typeStr.substring(0, angleBracketIndex);
            }
            // Remove the . from the parameterized type for nested classes
            int dotIndex = typeStr.indexOf(".");
            if (dotIndex >= 0) {
                typeStr = typeStr.substring(dotIndex + 1);
            }
            return typeStr;
        } else {
            return cic.getType().toString();
        }
        
    }
    
    protected ASTNode getExpression() {
        // This is not used in the code right now.
        return cic.getType();
    }
}
