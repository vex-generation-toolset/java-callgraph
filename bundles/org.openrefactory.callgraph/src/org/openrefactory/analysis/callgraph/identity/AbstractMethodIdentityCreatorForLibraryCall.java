/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.identity;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.type.typeinfo.ClassTypeInfo;
import org.openrefactory.analysis.type.typeinfo.ScalarTypeInfo;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.analysis.type.TypeCalculator;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.Constants;

/**
 * Issue 25
 *
 * Abstract class for creating method identity for method calls
 * This follows the template method pattern.
 * The abstract class contains the template method
 *
 * @author Munawar Hafiz
 */
public abstract class AbstractMethodIdentityCreatorForLibraryCall {
    
    /**
     * Issue 25
     * Creates and returns the method identity from a library method invocation.
     * This follows the template method pattern to calculate the method identity
     * from different kinds of method invocations.
     *
     * @param isStatic      true if the method is static. This information is probably
     *                      going to be false always since we can not know if the method is static
     *                      without binding and the binding is absent here. 
     * @param isConstructor true if the method is a constructor, false otherwise.
     *                      This information can be provided effectively from the caller
     *                      because a constructor can be identified from the ClassInstanceCreation
     *                      AST node. We do not need the binding for this.
     * @param filePath      the path of the file containing the call 
     * @return the method identity
     */
    public MethodIdentity createMethodIdentity(
        boolean isStatic,
        boolean isConstructor,
        String filePath)
    {
        List<ASTNode> actualParams = getArguments();
        int paramCount = actualParams.size();
        List<TypeInfo> actualParamTypes = new ArrayList<>(paramCount);
        if (paramCount > 0) {
            // Since we only have the method invocation, and no declaration
            // we calculate the method types from actual parameters
            // When we cannot find  type, we will use Class type object for that case
            for (ASTNode actualParam: actualParams) {
                TypeInfo actualParamType = TypeCalculator.typeOf(actualParam, true);
                if (actualParamType == null) {
                    actualParamType = new ClassTypeInfo(Constants.JAVA_LANG_OBJECT);
                }
                actualParamTypes.add(actualParamType);
            }
        }
        TypeInfo returnType = new ScalarTypeInfo("void");
        MethodIdentity methodIdentity = new MethodIdentity(getName(), returnType, actualParamTypes);
        if (isStatic) {
            methodIdentity.setStaticBit();
        } else {
            // We do a testing for a match with static
            // We are looking for library method calls that are static
            // because they have the form
            //
            //    import com.openrefactory.A;
            //    ...
            //        A.foo();
            //    ...
            ASTNode callingContext = getExpression();
            if (callingContext != null) {
                if (callingContext instanceof SimpleName s) {
                    String typeFromImports = CallGraphUtility.getLibraryTypeQualifiedNameFromJSONData(s.getIdentifier(),
                        filePath);
                    if (typeFromImports != null) {
                        if (typeFromImports.endsWith("." + s.getIdentifier().toString())) {
                            methodIdentity.setStaticBit();
                        }
                    }
                }
            }
        }
        if (isConstructor) {
            methodIdentity.setConstructorBit();
        }
        // This call is virtual meaning that there are no method declarations
        // to service this.
        methodIdentity.setVirtualMethodBit();
        return methodIdentity;
    }
    
    /**
     * Get the actual parameters
     *
     * @return a list of actual parameters
     */
    protected abstract List<ASTNode> getArguments();
    
    /**
     * Get the method/constructor name from the expression
     *
     * @return the name of the method/constructor
     */
    protected abstract String getName();
    
    /**
     * Relevant for Method invocation, returns the method call expression
     * For,    a.foo()
     * Returns the AST node for foo
     *
     * @return the expression
     */
    protected abstract ASTNode getExpression();
}
