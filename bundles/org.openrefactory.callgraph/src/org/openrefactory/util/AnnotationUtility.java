/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

/**
 * A utility class for extracting and manipulating Java annotations.
 *
 * <p>AnnotationUtil provides static methods to work with Java annotations in the
 * Abstract Syntax Tree (AST). It offers functionality to find annotations of specific
 * types and locate annotations that match particular criteria.</p>
 * 
 * <p>The class supports various AST node types:</p>
 * <ul>
 *   <li><strong>Body Declarations:</strong> Classes, methods, fields, constructors</li>
 *   <li><strong>Single Variable Declarations:</strong> Method parameters, local variables</li>
 *   <li><strong>Extended Modifiers:</strong> Annotations and other modifiers</li>
 * </ul>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li><strong>Annotation Discovery:</strong> Find all annotations on a given node</li>
 *   <li><strong>Type-Safe Extraction:</strong> Generic methods for type-specific annotation retrieval</li>
 *   <li><strong>Pattern Matching:</strong> Find annotations by type name</li>
 *   <li><strong>AST Integration:</strong> Works seamlessly with Eclipse JDT AST nodes</li>
 *   <li><strong>Utility Design:</strong> Static methods for easy access</li>
 * </ul>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class AnnotationUtility {

    /**
     * Finds the first annotation that matches the specified annotation type name.
     *
     * <p>This method searches through all annotations on a given AST node to find
     * the first one that matches the specified annotation type name. It's useful
     * for quickly locating specific annotations without processing the entire
     * annotation list.</p>
     * 
     * <p><strong>Matching Examples:</strong></p>
     * <ul>
     *   <li><strong>@Override annotation:</strong> annotationText = "Override"</li>
     *   <li><strong>@Deprecated annotation:</strong> annotationText = "Deprecated"</li>
     *   <li><strong>@SuppressWarnings annotation:</strong> annotationText = "SuppressWarnings"</li>
     *   <li><strong>@Entity annotation:</strong> annotationText = "Entity"</li>
     *   <li><strong>@NotNull annotation:</strong> annotationText = "NotNull"</li>
     * </ul>
     * 
     * <p><strong>Usage Scenarios:</strong></p>
     * <ul>
     *   <li><strong>Quick Annotation Check:</strong> Verify if a specific annotation exists</li>
     *   <li><strong>Single Annotation Retrieval:</strong> Get one instance of a particular annotation</li>
     *   <li><strong>Conditional Processing:</strong> Check for annotations before performing actions</li>
     *   <li><strong>Validation:</strong> Ensure required annotations are present</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Exact Matching:</strong> Uses exact string comparison for type names</li>
     *   <li><strong>Case Sensitivity:</strong> Annotation type names are case-sensitive</li>
     *   <li><strong>First Match Only:</strong> Returns only the first matching annotation</li>
     *   <strong>Null Return:</strong> Returns null if no matching annotation is found</li>
     *   <li><strong>Performance:</strong> Stops searching after finding the first match</li>
     * </ul>
     *
     * @param node the AST node to search for annotations
     * @param annotationText the exact annotation type name to search for (case-sensitive)
     * @return the first matching annotation found, or null if no match is found
     */
    public static Annotation findFirstMatchingAnnotation(ASTNode node, String annotationText) {
        List<Annotation> annotations = findAnnotationsOfType(node);
        Annotation annotation = null;
        for (Annotation ann : annotations) {
            if (ann.getTypeName().toString().equals(annotationText)) {
                annotation = ann;
                break;
            }
        }
        return annotation;
    }
    
    /**
     * Finds all annotations associated with the given AST node.
     *
     * <p>This method extracts all annotations from an AST node by examining its
     * extended modifiers. It supports both body declarations (classes, methods,
     * fields, constructors) and single variable declarations (method parameters,
     * local variables).</p>
     * 
     * <p>The method implements a comprehensive annotation discovery strategy:</p>
     * <ul>
     *   <li><strong>Node Type Detection:</strong> Identifies the type of AST node</li>
     *   <li><strong>Modifier Extraction:</strong> Retrieves extended modifiers from the node</li>
     *   <li><strong>Annotation Filtering:</strong> Filters modifiers to find only annotations</li>
     *   <li><strong>Type-Safe Return:</strong> Returns a generic list of annotations</li>
     * </ul>
     * 
     * <p><strong>Supported Node Types:</strong></p>
     * <ul>
     *   <li><strong>BodyDeclaration:</strong> Classes, interfaces, methods, fields, constructors</li>
     *   <li><strong>SingleVariableDeclaration:</strong> Method parameters, local variables</li>
     *   <li><strong>Other ASTNode types:</strong> Returns empty list (no modifiers)</li>
     * </ul>
     * 
     * <p><strong>Annotation Discovery Process:</strong></p>
     * <ol>
     *   <li>Check if the node supports modifiers</li>
     *   <li>Extract the list of extended modifiers</li>
     *   <li>Iterate through modifiers to find annotations</li>
     *   <li>Collect all discovered annotations</li>
     *   <li>Return the filtered annotation list</li>
     * </ol>
     * 
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li><strong>Method with @Override:</strong> Returns list containing @Override annotation</li>
     *   <li><strong>Field with @Deprecated:</strong> Returns list containing @Deprecated annotation</li>
     *   <li><strong>Class with @Entity:</strong> Returns list containing @Entity annotation</li>
     *   <li><strong>Parameter with @NotNull:</strong> Returns list containing @NotNull annotation</li>
     *   <li><strong>Node without annotations:</strong> Returns empty list</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Type Safety:</strong> Uses @SuppressWarnings("unchecked") for generic casting</li>
     *   <li><strong>Null Handling:</strong> Returns empty list for nodes without modifiers</li>
     *   <li><strong>Modifier Support:</strong> Only works with nodes that have modifiers</li>
     *   <li><strong>Annotation Filtering:</strong> Only returns actual annotations, not other modifiers</li>
     * </ul>
     *
     * @param <T> the type of annotation to return (must extend Annotation)
     * @param node the AST node to search for annotations
     * @return a list of all annotations found on the node, or empty list if none found
     */
    @SuppressWarnings("unchecked")
    private static <T extends Annotation> List<T> findAnnotationsOfType(ASTNode node) {
        List<Annotation> annotations = new ArrayList<>();

        List<IExtendedModifier> modifiers = null;
        if (node instanceof BodyDeclaration) {
            modifiers = ((BodyDeclaration) node).modifiers();
        } else if (node instanceof SingleVariableDeclaration) {
            modifiers = ((SingleVariableDeclaration) node).modifiers();
        }
        if (modifiers != null) {
            for (IExtendedModifier mod : modifiers) {
                if (mod.isAnnotation()) {
                    annotations.add((Annotation) mod);
                }
            }
        }
        return (List<T>) annotations;
    }
}
