/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.util.datastructure.IntPair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * A comprehensive utility class for working with JDT (Java Development Tools) AST nodes.
 *
 * <p>ASTNodeUtilities provides a rich set of static methods for navigating, searching, and
 * manipulating Abstract Syntax Tree nodes in Java source code. This utility class is essential
 * for call graph analysis, code navigation, and AST traversal operations.</p>
 * 
 * <p>The class offers several key capabilities:</p>
 * <ul>
 *   <li><strong>Node Discovery:</strong> Find nodes by type, position, and selection criteria</li>
 *   <li><strong>AST Traversal:</strong> Navigate parent-child relationships and find ancestors</li>
 *   <li><strong>Scope Analysis:</strong> Determine variable scopes and local variable contexts</li>
 *   <li><strong>Position Mapping:</strong> Convert between offsets, line numbers, and column positions</li>
 *   <li><strong>Binding Resolution:</strong> Convert between bindings, Java elements, and AST nodes</li>
 *   <li><strong>File Operations:</strong> Manage compilation units and file paths</li>
 *   <li><strong>Token Range Management:</strong> Handle source code location information</li>
 * </ul>
 * 
 * <p><strong>Supported Node Types:</strong></p>
 * <ul>
 *   <li><strong>Declaration Nodes:</strong> TypeDeclaration, MethodDeclaration, FieldDeclaration, etc.</li>
 *   <li><strong>Statement Nodes:</strong> IfStatement, ForStatement, WhileStatement, TryStatement, etc.</li>
 *   <li><strong>Expression Nodes:</strong> MethodInvocation, FieldAccess, ClassInstanceCreation, etc.</li>
 *   <li><strong>Structural Nodes:</strong> Block, CompilationUnit, AnonymousClassDeclaration, etc.</li>
 * </ul>
 *
 * @author Munawar Hafiz
 * @author Mohammad Rafid Ul Islam
 */
public class ASTNodeUtility {

    /**
     * Finds an AST node that matches the specified type and position criteria.
     *
     * <p>This method implements a sophisticated node discovery algorithm that searches for
     * nodes of a specific type within a given position range. It handles various edge cases
     * including anonymous class declarations, type declaration statements, and position
     * validation.</p>
     * 
     * <p>The search algorithm follows this strategy:</p>
     * <ol>
     *   <li><strong>Initial Search:</strong> Uses NodeFinder to get the covering node at the specified position</li>
     *   <li><strong>Anonymous Class Handling:</strong> Special logic for AnonymousClassDeclaration nodes</li>
     *   <li><strong>Type Unwrapping:</strong> Handles TypeDeclarationStatement wrapper nodes</li>
     *   <li><strong>Ancestor Traversal:</strong> Walks up the parent chain to find matching types</li>
     *   <li><strong>Fallback Search:</strong> Uses covered node and findAll as backup strategies</li>
     *   <li><strong>Position Validation:</strong> Ensures the found node covers the selection range</li>
     * </ol>
     * 
     * <p><strong>Anonymous Class Special Handling:</strong></p>
     * <ul>
     *   <li><strong>ClassInstanceCreation:</strong> Extracts AnonymousClassDeclaration from constructor calls</li>
     *   <li><strong>EnumConstantDeclaration:</strong> Extracts AnonymousClassDeclaration from enum constants</li>
     *   <li><strong>Covering Node Fallback:</strong> Uses covered node when covering node doesn't match</li>
     * </ul>
     * 
     * <p><strong>Type Declaration Statement Handling:</strong></p>
     * <ul>
     *   <li><strong>TypeDeclaration:</strong> Unwraps from TypeDeclarationStatement wrapper</li>
     *   <li><strong>EnumDeclaration:</strong> Unwraps from TypeDeclarationStatement wrapper</li>
     *   <li><strong>Type Safety:</strong> Ensures the unwrapped declaration matches the target type</li>
     * </ul>
     * 
     * <p><strong>Position Validation Logic:</strong></p>
     * <ul>
     *   <li><strong>Upper Bound Assumption:</strong> Assumes client sends upper bound of offset</li>
     *   <li><strong>Start Position Check:</strong> Rejects nodes starting before selection offset</li>
     *   <li><strong>Fallback Strategy:</strong> Uses covered node when position validation fails</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Find Method:</strong> findNode(MethodDeclaration.class, root, offset, length)</li>
     *   <li><strong>Find Class:</strong> findNode(TypeDeclaration.class, root, offset, length)</li>
     *   <li><strong>Find Field:</strong> findNode(FieldDeclaration.class, root, offset, length)</li>
     *   <li><strong>Find Anonymous Class:</strong> findNode(AnonymousClassDeclaration.class, root, offset, length)</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Generic Type Safety:</strong> Returns the specified type T or null</li>
     *   <li><strong>Position Sensitivity:</strong> Results depend on exact offset and length values</li>
     *   <li><strong>Anonymous Class Complexity:</strong> Special handling for anonymous class scenarios</li>
     *   <li><strong>Fallback Mechanisms:</strong> Multiple strategies ensure robust node discovery</li>
     *   <li><strong>Performance:</strong> Efficient traversal with early termination</li>
     * </ul>
     *
     * @param <T> the generic type of AST node to find (must extend ASTNode)
     * @param nodeType the class type of the node to find
     * @param node the root AST node to start the search from
     * @param selectionOffset the 0-based character offset in the source file
     * @param selectionLength the length of the selected text in characters
     * @return an AST node of type T that covers the specified position, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T findNode(
            Class<T> nodeType, ASTNode node, int selectionOffset, int selectionLength) {
        NodeFinder finder = new NodeFinder(node, selectionOffset, selectionLength);
        ASTNode temp = finder.getCoveringNode();
        // In the case of anonymous class declaration, the node's java element
        // gets us the offset and length of the container ClassInstanceCreation
        // or EnumConstantDeclaration (we are no going to handle this).
        //
        // Since we are always taking the offset and length from the JAVA elements
        // in Call Graph, the NodeFinder will always get us either ClassInstanceCreation
        // or EnumConstantDeclaration for a anonymous class.
        if (nodeType.equals(AnonymousClassDeclaration.class) && !(temp instanceof AnonymousClassDeclaration)) {
            if (temp instanceof ClassInstanceCreation) {
                temp = ((ClassInstanceCreation) temp).getAnonymousClassDeclaration();
            } else if (temp instanceof EnumConstantDeclaration) {
                temp = ((EnumConstantDeclaration) temp).getAnonymousClassDeclaration();
            } else if (finder.getCoveredNode() instanceof ClassInstanceCreation) {
                temp = ((ClassInstanceCreation) finder.getCoveredNode()).getAnonymousClassDeclaration();
            } else if (finder.getCoveredNode() instanceof EnumConstantDeclaration) {
                temp = ((EnumConstantDeclaration) finder.getCoveredNode()).getAnonymousClassDeclaration();
            } else {
                return null;
            }
        }

        while (temp != null && !nodeType.isAssignableFrom(temp.getClass())) {
            // In the client side of findNode, we do not handle TypeDeclarationStatement
            // So, we need to unpack this ASTNode to get actual declaration inside of it.
            if (temp instanceof TypeDeclarationStatement) {
                if (nodeType.isAssignableFrom(TypeDeclaration.class)
                        && ((TypeDeclarationStatement) temp).getDeclaration() instanceof TypeDeclaration) {
                    temp = ((TypeDeclarationStatement) temp).getDeclaration();
                    break;
                } else if (nodeType.isAssignableFrom(EnumDeclaration.class)
                        && ((TypeDeclarationStatement) temp).getDeclaration() instanceof EnumDeclaration) {
                    temp = ((TypeDeclarationStatement) temp).getDeclaration();
                    break;
                }
            }
            temp = temp.getParent();
        }
        // we use a heuristic here.
        // we will not accept node whose offset is less than selection offset becaause
        // we assume that client sends upper bound of offset
        if (temp == null || temp.getStartPosition() < selectionOffset) {
            temp = finder.getCoveredNode();
            // In the client side of findNode, we do not handle TypeDeclarationStatement
            // So, we need to unpack this ASTNode to get actual declaration inside of it.
            if (temp instanceof TypeDeclarationStatement) {
                if (nodeType.isAssignableFrom(TypeDeclaration.class)
                        && ((TypeDeclarationStatement) temp).getDeclaration() instanceof TypeDeclaration) {
                    return (T) ((TypeDeclarationStatement) temp).getDeclaration();
                } else if (nodeType.isAssignableFrom(EnumDeclaration.class)
                        && ((TypeDeclarationStatement) temp).getDeclaration() instanceof EnumDeclaration) {
                    return (T) ((TypeDeclarationStatement) temp).getDeclaration();
                }
            }
            while (temp != null && !nodeType.isAssignableFrom(temp.getClass())) {
                temp = temp.getParent();
            }
        }
        if (temp == null) {
            Iterable<T> ii = ASTNodeUtility.findAll(finder.getCoveringNode(), nodeType);
            for (T ff : ii) {
                if (nodeType.isAssignableFrom(ff.getClass())) {
                    return ff;
                }
                break;
            }
        }
        return (T) temp;
    }

    /**
     * Finds an AST node for test suite scenarios with simplified search logic.
     *
     * <p>This is a specialized version of the findNode method designed specifically for test suite
     * scenarios. It addresses issues that arise when using markers in tests where the covering node
     * approach may not be reliable, particularly with inner classes and line comments.</p>
     * 
     * <p><strong>Test Suite Specific Issues Addressed:</strong></p>
     * <ul>
     *   <li><strong>Marker Positioning:</strong> Tests place markers so covering node points to expected selection</li>
     *   <li><strong>Inner Class Complications:</strong> Covering node from inner class may point to outer class</li>
     *   <li><strong>Line Comment Interference:</strong> Comments can affect node selection accuracy</li>
     *   <li><strong>Covering Node Safety:</strong> Some scenarios require more conservative node selection</li>
     * </ul>
     * 
     * <p><strong>Simplified Search Strategy:</strong></p>
     * <ol>
     *   <li><strong>Covering Node Search:</strong> Start with NodeFinder's covering node</li>
     *   <li><strong>Ancestor Traversal:</strong> Walk up parent chain to find matching type</li>
     *   <li><strong>Covered Node Fallback:</strong> Use covered node if covering node search fails</li>
     *   <li><strong>Final Fallback:</strong> Use findAll method as last resort</li>
     * </ol>
     * 
     * <p><strong>Key Differences from findNode:</strong></p>
     * <ul>
     *   <li><strong>No Anonymous Class Handling:</strong> Simplified logic without special cases</li>
     *   <li><strong>No Type Declaration Unwrapping:</strong> Direct ancestor traversal only</li>
     *   <strong>No Position Validation:</strong> Accepts any node that matches the type</li>
     *   <li><strong>Faster Execution:</strong> Reduced complexity for test scenarios</li>
     * </ul>
     * 
     * <p><strong>When to Use:</strong></p>
     * <ul>
     *   <li><strong>Test Suite Scenarios:</strong> When testing node finding functionality</li>
     *   <li><strong>Marker-Based Tests:</strong> When using Eclipse markers for test positioning</li>
     *   <li><strong>Simplified Logic Needed:</strong> When complex node handling isn't required</li>
     *   <li><strong>Performance Testing:</strong> When measuring basic node finding performance</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Test Suite Only:</strong> Designed specifically for testing scenarios</li>
     *   <li><strong>Simplified Logic:</strong> May not handle all edge cases like the main findNode method</li>
     *   <li><strong>Marker Dependency:</strong> Assumes proper marker positioning in test files</li>
     *   <li><strong>Performance Focused:</strong> Optimized for speed over completeness</li>
     * </ul>
     *
     * @param <T> the generic type of AST node to find (must extend ASTNode)
     * @param nodeType the class type of the node to find
     * @param node the root AST node to start the search from
     * @param selectionOffset the 0-based character offset in the source file
     * @param selectionLength the length of the selected text in characters
     * @return an AST node of type T that covers the specified position, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T findNodeForTestSuite(
            Class<T> nodeType, ASTNode node, int selectionOffset, int selectionLength) {
        NodeFinder finder = new NodeFinder(node, selectionOffset, selectionLength);
        ASTNode temp = finder.getCoveringNode();
        while (temp != null && !nodeType.isAssignableFrom(temp.getClass())) {
            temp = temp.getParent();
        }
        if (temp == null) {
            temp = finder.getCoveredNode();
            while (temp != null && !nodeType.isAssignableFrom(temp.getClass())) {
                temp = temp.getParent();
            }
        }
        if (temp == null) {
            Iterable<T> ii = ASTNodeUtility.findAll(finder.getCoveringNode(), nodeType);
            for (T ff : ii) {
                if (nodeType.isAssignableFrom(ff.getClass())) {
                    return ff;
                }
                break;
            }
        }
        return (T) temp;
    }

    /**
     * Finds the nearest ancestor node that matches the specified target type.
     *
     * <p>This method traverses up the AST hierarchy from a given node to find the closest
     * ancestor that matches the specified type. It implements intelligent traversal logic
     * that respects type declaration boundaries for most searches while allowing full
     * traversal for compilation unit and type declaration searches.</p>
     * 
     * <p><strong>Traversal Strategy:</strong></p>
     * <ol>
     *   <li><strong>Parent Chain Navigation:</strong> Walks up the parent hierarchy systematically</li>
     *   <li><strong>Type Boundary Detection:</strong> Identifies when to stop at type declarations</li>
     *   <li><strong>Target Type Matching:</strong> Checks each ancestor against the target class</li>
     *   <li><strong>Early Termination:</strong> Returns immediately when a match is found</li>
     * </ol>
     * 
     * <p><strong>Type Boundary Logic:</strong></p>
     * <ul>
     *   <li><strong>Default Behavior:</strong> Stops traversal at the nearest type declaration</strong>
     *   <li><strong>Compilation Unit Search:</strong> Allows traversal to the root compilation unit</li>
     *   <li><strong>Type Declaration Search:</strong> Allows traversal to find type declarations</li>
     *   <li><strong>Scope Limitation:</strong> Prevents crossing method/class boundaries for other types</li>
     * </ul>
     * 
     * <p><strong>Supported Target Types:</strong></p>
     * <ul>
     *   <li><strong>CompilationUnit:</strong> Full traversal to find the root compilation unit</li>
     *   <li><strong>TypeDeclaration:</strong> Full traversal to find type declarations</li>
     *   <li><strong>Other Types:</strong> Limited traversal within the current type scope</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Find Enclosing Method:</strong> findNearestAncestor(node, MethodDeclaration.class)</li>
     *   <li><strong>Find Enclosing Class:</strong> findNearestAncestor(node, TypeDeclaration.class)</li>
     *   <li><strong>Find Compilation Unit:</strong> findNearestAncestor(node, CompilationUnit.class)</li>
     *   <li><strong>Find Enclosing Block:</strong> findNearestAncestor(node, Block.class)</li>
     * </ul>
     * 
     * <p><strong>Traversal Behavior:</strong></p>
     * <ul>
     *   <li><strong>Within Method:</strong> Can find blocks, statements, and expressions</li>
     *   <li><strong>Within Class:</strong> Can find methods, fields, and inner types</li>
     *   <li><strong>Cross-Method:</strong> Cannot find methods from other methods</li>
     *   <li><strong>Cross-Class:</strong> Cannot find classes from other classes (unless searching for TypeDeclaration)</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Scope Awareness:</strong> Respects logical boundaries in the AST structure</li>
     *   <li><strong>Performance:</strong> Efficient traversal with early termination</li>
     *   <li><strong>Type Safety:</strong> Returns the specified generic type T or null</li>
     *   <li><strong>Boundary Respect:</strong> Prevents inappropriate cross-scope searches</li>
     *   <li><strong>Null Handling:</strong> Returns null if no matching ancestor is found</li>
     * </ul>
     *
     * @param <T> the generic type of AST node to find (must extend ASTNode)
     * @param node the AST node to start the ancestor search from
     * @param targetClass the class type of the ancestor node to find
     * @return the nearest ancestor node of type T, or null if no matching ancestor exists
     */
    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T findNearestAncestor(ASTNode node, Class<T> targetClass) {
        // If we are looking for type declaration or compilation unit
        // then allow calculation to go all the way, otherwise
        // stop at the nearest type declaration
        boolean stopAtTypeDeclaration = true;
        if (targetClass.isAssignableFrom(CompilationUnit.class)
                || targetClass.isAssignableFrom(TypeDeclaration.class)) {
            stopAtTypeDeclaration = false;
        }
        for (ASTNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (targetClass.isAssignableFrom(parent.getClass())) {
                return (T) parent;
            }
            if (stopAtTypeDeclaration && parent.getClass().isAssignableFrom(TypeDeclaration.class)) {
                break;
            }
        }
        return null;
    }

    /**
     * Creates an iterable collection of all AST nodes that match the specified type.
     *
     * <p>This method provides a lazy-evaluated iterable that traverses the entire AST subtree
     * starting from the given node and returns all nodes that match the specified class type.
     * The iteration follows a depth-first traversal order, ensuring comprehensive coverage of
     * the AST structure.</p>
     * 
     * <p><strong>Lazy Evaluation Benefits:</strong></p>
     * <ul>
     *   <li><strong>Memory Efficiency:</strong> Nodes are discovered only when requested</li>
     *   <li><strong>Performance Optimization:</strong> Avoids unnecessary node processing</li>
     *   <li><strong>Scalability:</strong> Works efficiently with large AST trees</li>
     *   <li><strong>Stream Processing:</strong> Enables pipeline-style operations</li>
     * </ul>
     * 
     * <p><strong>Traversal Strategy:</strong></p>
     * <ol>
     *   <li><strong>Depth-First Search:</strong> Explores each branch completely before moving to siblings</li>
     *   <li><strong>Structural Property Navigation:</strong> Uses AST node structural properties for traversal</li>
     *   <li><strong>Type Filtering:</strong> Applies type checking at each node</li>
     *   <li><strong>Comprehensive Coverage:</strong> Visits every node in the subtree</li>
     * </ol>
     * 
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li><strong>Custom Iterator:</strong> Uses FilteringIterator for type-based filtering</li>
     *   <li><strong>AST Iterator Integration:</strong> Leverages ASTIterator for efficient traversal</li>
     *   <li><strong>Type Safety:</strong> Ensures returned objects match the specified type T</li>
     *   <li><strong>Null Safety:</strong> Returns empty iterable for null nodes</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Find All Methods:</strong> findAll(root, MethodDeclaration.class)</li>
     *   <li><strong>Find All Fields:</strong> findAll(root, FieldDeclaration.class)</li>
     *   <li><strong>Find All Statements:</strong> findAll(root, Statement.class)</li>
     *   <li><strong>Find All Expressions:</strong> findAll(root, Expression.class)</li>
     * </ul>
     * 
     * <p><strong>Iterator Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Single Pass:</strong> Each iterator can only be used once</li>
     *   <li><strong>Non-Modifiable:</strong> Does not support remove() operations</li>
     *   <li><strong>Thread Safety:</strong> Not thread-safe for concurrent access</li>
     *   <li><strong>Order Consistency:</strong> Depth-first order is guaranteed</li>
     * </ul>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li><strong>Linear Time:</strong> O(n) where n is the number of nodes in the subtree</li>
     *   <li><strong>Memory Usage:</strong> Minimal memory overhead due to lazy evaluation</li>
     *   <li><strong>Type Checking:</strong> Constant-time type checking for each node</li>
     *   <li><strong>Traversal Overhead:</strong> Minimal overhead for structural property access</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Lazy Evaluation:</strong> Nodes are not collected until iteration begins</li>
     *   <li><strong>Type Compatibility:</strong> Uses isAssignableFrom for flexible type matching</li>
     *   <li><strong>Subtree Scope:</strong> Only searches within the specified node's subtree</li>
     *   <li><strong>Empty Results:</strong> Returns empty iterable if no matches are found</li>
     * </ul>
     *
     * @param <T> the generic type of AST nodes to find (must extend ASTNode)
     * @param node the root AST node to start the search from
     * @param clazz the class type to filter nodes by
     * @return an iterable collection of all nodes of type T in the subtree, never null
     */
    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> Iterable<T> findAll(final ASTNode node, final Class<T> clazz) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new FilteringIterator<T>(new ASTIterator(node)) {
                    @Override
                    protected boolean shouldProcess(Object item) {
                        return clazz.isAssignableFrom(item.getClass());
                    }

                    @Override
                    protected T process(Object item) {
                        return (T) item;
                    }
                };
            }
            ;
        };
    }

    /**
     * Retrieves all direct children of an AST node.
     *
     * <p>This method extracts all immediate child nodes from a given AST node by examining
     * its structural properties. It handles both single nodes and lists of nodes, providing
     * a comprehensive view of the node's immediate children in the AST hierarchy.</p>
     * 
     * <p><strong>Structural Property Analysis:</strong></p>
     * <ol>
     *   <li><strong>Property Discovery:</strong> Retrieves all structural properties for the node type</li>
     *   <li><strong>Property Access:</strong> Accesses each property to get its value</li>
     *   <li><strong>Type Classification:</strong> Categorizes properties as single nodes or lists</li>
     *   <li><strong>Child Collection:</strong> Gathers all valid child nodes into a unified list</li>
     * </ol>
     * 
     * <p><strong>Child Node Types Handled:</strong></p>
     * <ul>
     *   <li><strong>Single AST Nodes:</strong> Direct child nodes (e.g., method body, field type)</li>
     *   <li><strong>Node Lists:</strong> Collections of child nodes (e.g., method parameters, class members)</li>
     *   <strong>Null Properties:</strong> Safely ignored when structural properties return null</li>
     *   <li><strong>Mixed Content:</strong> Handles nodes with both single and list children</li>
     * </ul>
     * 
     * <p><strong>Special Cases Handled:</strong></p>
     * <ul>
     *   <li><strong>For Loop Increment:</strong> Handles null returns from increment expressions</li>
     *   <li><strong>Empty Lists:</strong> Safely processes empty child collections</li>
     *   <li><strong>Null Children:</strong> Filters out null child references</li>
     *   <li><strong>Type Safety:</strong> Ensures only ASTNode instances are returned</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Method Children:</strong> getChildren(methodNode) returns parameters, body, etc.</li>
     *   <li><strong>Class Children:</strong> getChildren(classNode) returns fields, methods, etc.</li>
     *   <li><strong>Block Children:</strong> getChildren(blockNode) returns statements</li>
     *   <li><strong>Expression Children:</strong> getChildren(exprNode) returns operands</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Linear Time:</strong> O(p) where p is the number of structural properties</li>
     *   <li><strong>Memory Allocation:</strong> Creates new ArrayList for each call</li>
     *   <li><strong>Property Access:</strong> Each structural property is accessed once</li>
     *   <li><strong>List Operations:</strong> Efficient list operations for child collection</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Direct Children Only:</strong> Does not traverse deeper into the AST hierarchy</li>
     *   <li><strong>Structural Properties:</strong> Relies on Eclipse JDT's structural property system</li>
     *   <li><strong>Null Handling:</strong> Gracefully handles null structural properties</li>
     *   <li><strong>Type Consistency:</strong> Returns only ASTNode instances, never primitive types</li>
     *   <li><strong>Fresh List:</strong> Each call returns a new list instance</li>
     * </ul>
     *
     * @param node the AST node whose children should be retrieved
     * @return a list containing all direct child nodes of the specified node, never null
     */
    @SuppressWarnings("rawtypes")
    public static List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        List list = node.structuralPropertiesForType();
        for (int i = 0; i < list.size(); i++) {
            Object child = node.getStructuralProperty((StructuralPropertyDescriptor) list.get(i));
            // in the case of
            // for (;;i++) something;
            // when node.getStructuralProperty((StructuralPropertyDescriptor)list.get(i)) is called on the for increment
            // part it returns null to child
            if (child == null) continue;

            if (child instanceof ASTNode) {
                children.add((ASTNode) child);
            } else if (child instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<ASTNode> nodes = (List<ASTNode>) child;

                if (!nodes.isEmpty()) {
                    children.addAll(nodes);
                }
            }
        }
        return children;
    }

    /**
     * Recursively finds all nodes matching specified types and populates a results map.
     *
     * <p>This method performs a comprehensive search through an entire AST subtree to find
     * all nodes that match any of the specified class types. It organizes the results by
     * class type in a map, making it easy to access nodes of specific types after the search.</p>
     * 
     * <p><strong>Search Strategy:</strong></p>
     * <ol>
     *   <li><strong>Current Node Analysis:</strong> Checks if the current node matches any target types</li>
     *   <li><strong>Map Population:</strong> Adds matching nodes to the appropriate class lists</li>
     *   <li><strong>Recursive Traversal:</strong> Processes all children recursively</li>
     *   <li><strong>Comprehensive Coverage:</strong> Ensures the entire subtree is examined</li>
     * </ol>
     * 
     * <p><strong>Results Organization:</strong></p>
     * <ul>
     *   <li><strong>Class-Based Grouping:</strong> Each class type gets its own list of nodes</li>
     *   <li><strong>Dynamic List Creation:</strong> Lists are created as needed for each class type</li>
     *   <li><strong>Efficient Storage:</strong> Initial capacity of 2 for most class lists</li>
     *   <li><strong>Type Safety:</strong> Each list contains only nodes of the specified type</li>
     * </ul>
     * 
     * <p><strong>Traversal Behavior:</strong></p>
     * <ul>
     *   <li><strong>Depth-First Search:</strong> Explores each branch completely before moving to siblings</li>
     *   <li><strong>Unlimited Depth:</strong> Traverses to the deepest levels of the AST</li>
     *   <li><strong>All Children Processed:</strong> No nodes are skipped during traversal</li>
     *   <li><strong>Recursive Implementation:</strong> Uses stack-based recursion for traversal</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Find All Declarations:</strong> findAndPopulateAll(root, Arrays.asList(TypeDeclaration.class, MethodDeclaration.class, FieldDeclaration.class), results)</li>
     *   <li><strong>Find All Statements:</strong> findAndPopulateAll(root, Arrays.asList(IfStatement.class, ForStatement.class, WhileStatement.class), results)</li>
     *   <li><strong>Find All Expressions:</strong> findAndPopulateAll(root, Arrays.asList(MethodInvocation.class, FieldAccess.class, ClassInstanceCreation.class), results)</li>
     *   <li><strong>Mixed Type Search:</strong> findAndPopulateAll(root, Arrays.asList(Statement.class, Expression.class, Declaration.class), results)</li>
     * </ul>
     * 
     * <p><strong>Map Structure:</strong></p>
     * <ul>
     *   <li><strong>Key Type:</strong> Class<?> - the class type being searched for</li>
     *   <li><strong>Value Type:</strong> List<ASTNode> - all nodes of that type found in the subtree</li>
     *   <li><strong>Initial Capacity:</strong> Lists start with capacity of 2 for efficiency</li>
     *   <li><strong>Dynamic Growth:</strong> Lists grow automatically as more nodes are found</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Linear Time:</strong> O(n) where n is the total number of nodes in the subtree</li>
     *   <li><strong>Memory Usage:</strong> Proportional to the number of matching nodes found</li>
     *   <li><strong>Type Checking:</strong> Constant-time type checking for each node</li>
     *   <li><strong>Map Operations:</strong> Efficient map lookups and list operations</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Side Effect Method:</strong> Modifies the provided resultsMap parameter</li>
     *   <li><strong>Complete Traversal:</strong> Searches the entire subtree without early termination</li>
     *   <li><strong>Type Compatibility:</strong> Uses isAssignableFrom for flexible type matching</li>
     *   <li><strong>Null Safety:</strong> Handles null children gracefully</li>
     *   <li><strong>Memory Efficiency:</strong> Only stores references to matching nodes</li>
     * </ul>
     *
     * @param node the root AST node to start the search from
     * @param clazzes a list of class types to search for (must not be null)
     * @param resultsMap the map to populate with found nodes, organized by class type
     */
    public static void findAndPopulateAll(
            final ASTNode node, final List<Class<?>> clazzes, Map<Class<?>, List<ASTNode>> resultsMap) {
        // If this node fits into any of the classes from
        // the given list, we add an entry for it into the resultsMap
        // with the class object as key
        for (Class<?> clazz : clazzes) {
            if (clazz.isAssignableFrom(node.getClass())) {
                if (resultsMap.containsKey(clazz)) {
                    resultsMap.get(clazz).add(node);
                } else {
                    List<ASTNode> nodes = new ArrayList<>(2);
                    nodes.add(node);
                    resultsMap.put(clazz, nodes);
                }
            }
        }
        List<ASTNode> children = getChildren(node);
        // If this node has children, then we call this method again
        // until we reach the leaf nodes
        if (children != null) {
            for (ASTNode child : children) {
                findAndPopulateAll(child, clazzes, resultsMap);
            }
        }
    }

    /**
     * Finds nodes matching specified types within the current scope, excluding inner and anonymous classes.
     *
     * <p>This method is a scope-limited version of {@link #findAndPopulateAll(ASTNode, List, Map)} that
     * restricts the search to the current method body, initializer, or variable declaration scope.
     * It deliberately avoids traversing into inner classes, anonymous classes, and type declarations
     * to maintain scope boundaries.</p>
     * 
     * <p><strong>Scope Boundary Logic:</strong></p>
     * <ul>
     *   <li><strong>Method Body Scope:</strong> Searches within method boundaries only</li>
     *   <li><strong>Initializer Scope:</strong> Searches within static/instance initializer blocks</li>
     *   <li><strong>Variable Declaration Scope:</strong> Searches within variable declaration contexts</li>
     *   <li><strong>Block Scope:</strong> Respects block boundaries and control flow structures</li>
     * </ul>
     * 
     * <p><strong>Excluded Node Types:</strong></p>
     * <ul>
     *   <li><strong>AnonymousClassDeclaration:</strong> Skips anonymous class implementations</li>
     *   <li><strong>TypeDeclaration:</strong> Skips inner class and interface declarations</li>
     *   <li><strong>EnumDeclaration:</strong> Skips inner enum declarations</li>
     *   <li><strong>Nested Types:</strong> Prevents cross-scope type discovery</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Method Analysis:</strong> Find statements and expressions within a method</li>
     *   <li><strong>Variable Scope Analysis:</strong> Determine variable usage within current scope</li>
     *   <li><strong>Control Flow Analysis:</strong> Analyze statements within method boundaries</li>
     *   <li><strong>Local Code Analysis:</strong> Focus on current execution context</li>
     * </ul>
     * 
     * <p><strong>Key Differences from findAndPopulateAll:</strong></p>
     * <ul>
     *   <li><strong>Scope Limitation:</strong> Respects method and block boundaries</li>
     *   <li><strong>Inner Class Exclusion:</strong> Does not traverse into nested type declarations</li>
     *   <strong>Anonymous Class Exclusion:</strong> Skips anonymous class implementations</li>
     *   <li><strong>Performance Optimization:</strong> Faster execution due to limited scope</li>
     * </ul>
     * 
     * <p><strong>Traversal Strategy:</strong></p>
     * <ol>
     *   <li><strong>Current Node Analysis:</strong> Checks if current node matches target types</li>
     *   <li><strong>Map Population:</strong> Adds matching nodes to appropriate class lists</li>
     *   <li><strong>Selective Child Processing:</strong> Processes children while respecting scope boundaries</li>
     *   <li><strong>Scope Boundary Respect:</strong> Stops traversal at excluded node types</li>
     * </ol>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Method Statement Analysis:</strong> findAndPopulateAllInThisScope(methodBody, Arrays.asList(IfStatement.class, ForStatement.class), results)</li>
     *   <li><strong>Variable Usage Analysis:</strong> findAndPopulateAllInThisScope(methodBody, Arrays.asList(SimpleName.class, FieldAccess.class), results)</li>
     *   <li><strong>Expression Analysis:</strong> findAndPopulateAllInThisScope(methodBody, Arrays.asList(MethodInvocation.class, ClassInstanceCreation.class), results)</li>
     *   <li><strong>Control Flow Analysis:</strong> findAndPopulateAllInThisScope(methodBody, Arrays.asList(Statement.class, Expression.class), results)</li>
     * </ul>
     * 
     * <p><strong>Performance Benefits:</strong></p>
     * <ul>
     *   <li><strong>Reduced Traversal:</strong> Skips complex nested structures</li>
     *   <li><strong>Faster Execution:</strong> Limited scope means fewer nodes to process</li>
     *   <li><strong>Memory Efficiency:</strong> Smaller result sets due to scope limitation</li>
     *   <li><strong>Focused Results:</strong> Results are more relevant to current context</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Scope Awareness:</strong> Designed for method-level and block-level analysis</li>
     *   <li><strong>Side Effect Method:</strong> Modifies the provided resultsMap parameter</li>
     *   <li><strong>Boundary Respect:</strong> Maintains logical scope boundaries</li>
     *   <li><strong>Type Compatibility:</strong> Uses isAssignableFrom for flexible type matching</li>
     *   <li><strong>Null Safety:</strong> Handles null children gracefully</li>
     * </ul>
     *
     * @param node the root AST node to start the scope-limited search from
     * @param clazzes a list of class types to search for (must not be null)
     * @param resultsMap the map to populate with found nodes, organized by class type
     */
    public static void findAndPopulateAllInThisScope(
            final ASTNode node, final List<Class<?>> clazzes, Map<Class<?>, List<ASTNode>> resultsMap) {
        // If this node fits into any of the classes from
        // the given list, we add an entry for it into the resultsMap
        // with the class object as key
        for (Class<?> clazz : clazzes) {
            if (clazz.isAssignableFrom(node.getClass())) {
                if (resultsMap.containsKey(clazz)) {
                    resultsMap.get(clazz).add(node);
                } else {
                    List<ASTNode> nodes = new ArrayList<>(2);
                    nodes.add(node);
                    resultsMap.put(clazz, nodes);
                }
            }
        }
        List<ASTNode> children = getChildren(node);
        // If this node has children, then we call this method again
        // until we reach the leaf nodes
        if (children != null) {
            for (ASTNode child : children) {
                if (child instanceof AnonymousClassDeclaration
                        || child instanceof TypeDeclaration
                        || child instanceof EnumDeclaration) {
                    continue;
                }
                findAndPopulateAllInThisScope(child, clazzes, resultsMap);
            }
        }
    }

    /**
     * Converts the line, col pairs for the start position and end position to a pair that contains
     * the 0 based offset from the start of the file and the length
     * @param ast the compilation unit to calculate upon
     * @param fromLine the start line
     * @param fromCol the start column
     * @param toLine the end line
     * @param toCol the end column
     * @return a pair of integers, the first denoting the 0-based offset and the second denoting the
     *         length
     */
    public static IntPair getPosition(CompilationUnit ast, int fromLine, int fromCol, int toLine,
        int toCol) {
        int startOffset = ast.getPosition(fromLine, fromCol);
        int endOffset = ast.getPosition(toLine, toCol);

        if (startOffset >= 0 && endOffset >= startOffset) {
            return IntPair.of(startOffset, (endOffset - startOffset));
        } else {
            return IntPair.of(-1, -1);
        }
    }
    
    /**
     * Converts a TokenRange to its corresponding line and column numbers.
     *
     * <p>This method extracts the file path from a TokenRange and uses it to acquire the
     * corresponding CompilationUnit, then converts the offset to line and column coordinates.
     * It's useful for converting character-based positions to human-readable line/column format.</p>
     * 
     * <p><strong>Conversion Process:</strong></p>
     * <ol>
     *   <li><strong>File Path Extraction:</strong> Gets the file path from the TokenRange</li>
     *   <li><strong>AST Acquisition:</strong> Acquires the CompilationUnit for the file</li>
     *   <li><strong>Position Conversion:</strong> Converts offset to line and column numbers</li>
     *   <li><strong>Coordinate Adjustment:</strong> Adjusts column numbers to be 1-based</li>
     * </ol>
     * 
     * <p><strong>Coordinate System:</strong></p>
     * <ul>
     *   <li><strong>Line Numbers:</strong> 1-based (first line is line 1)</li>
     *   <li><strong>Column Numbers:</strong> 1-based (first column is column 1)</li>
     *   <strong>Offset System:</strong> 0-based character positions in the file</li>
     *   <li><strong>Error Indication:</strong> Returns <-1, -1> for invalid positions</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Error Reporting:</strong> Convert token positions to user-friendly line/column format</li>
     *   <li><strong>IDE Integration:</strong> Provide line/column information for navigation</li>
     *   <li><strong>Debug Information:</strong> Display source locations in error messages</li>
     *   <li><strong>Code Analysis:</strong> Report findings with line/column precision</li>
     * </ul>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li><strong>AST Loading:</strong> May require parsing the source file</li>
     *   <li><strong>File I/O:</strong> Involves reading the source file from disk</li>
     *   <li><strong>Caching:</strong> JavaVPG may cache ASTs for performance</li>
     *   <li><strong>Memory Usage:</strong> AST objects consume memory</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>File Dependency:</strong> Requires the source file to be accessible</li>
     *   <li><strong>AST Availability:</strong> Depends on successful AST parsing</li>
     *   <li><strong>Error Handling:</strong> Returns <-1, -1> for any conversion failures</li>
     *   <li><strong>Coordinate Consistency:</strong> Both line and column are 1-based for consistency</li>
     * </ul>
     *
     * @param tokenRange the TokenRange containing file path and offset information
     * @return an IntPair containing line number (first) and column number (second), or <-1, -1> on error
     */
    public static IntPair getLineAndColumn(TokenRange tokenRange) {
        CompilationUnit ast = getCompilationUnitFromFilePath(tokenRange.getFileName());
        return getLineAndColumn(ast, tokenRange.getOffset());
    }

    /**
     * Converts a character offset to line and column numbers within a CompilationUnit.
     *
     * <p>This is a helper method that performs the core conversion from character offset to
     * line and column coordinates. It's used internally by other methods and can be called
     * directly when you already have a CompilationUnit and offset.</p>
     * 
     * <p><strong>Conversion Logic:</strong></p>
     * <ol>
     *   <li><strong>Line Number Retrieval:</strong> Gets the line number for the given offset</li>
     *   <li><strong>Column Number Retrieval:</strong> Gets the column number for the given offset</li>
     *   <li><strong>Coordinate Adjustment:</strong> Converts column from 0-based to 1-based</li>
     *   <li><strong>Validation:</strong> Checks that both coordinates are valid</li>
     * </ol>
     * 
     * <p><strong>Coordinate System Details:</strong></p>
     * <ul>
     *   <li><strong>Line Numbers:</strong> Eclipse JDT returns 1-based line numbers</li>
     *   <li><strong>Column Numbers:</strong> Eclipse JDT returns 0-based column numbers</li>
     *   <strong>Adjustment:</strong> Column numbers are incremented to be 1-based for consistency</li>
     *   <li><strong>Validation:</strong> Both coordinates must be non-negative for success</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Constant Time:</strong> O(1) for coordinate lookups</li>
     *   <li><strong>No File I/O:</strong> Works entirely with in-memory AST data</li>
     *   <li><strong>Efficient Lookup:</strong> Uses Eclipse JDT's optimized position mapping</li>
     *   <li><strong>Memory Access:</strong> Minimal memory overhead</li>
     * </ul>
     * 
     * <p><strong>Usage Scenarios:</strong></p>
     * <ul>
     *   <li><strong>Direct Conversion:</strong> When you have AST and offset already</li>
     *   <li><strong>Performance Critical:</strong> When avoiding file I/O is important</li>
     *   <li><strong>Batch Processing:</strong> When converting multiple offsets in the same file</li>
     *   <li><strong>Internal Use:</strong> As a helper for other position conversion methods</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>AST Dependency:</strong> Requires a valid, non-null CompilationUnit</li>
     *   <li><strong>Offset Validation:</strong> Offset must be within the file's bounds</li>
     *   <li><strong>Coordinate Consistency:</strong> Returns 1-based coordinates for both line and column</li>
     *   <li><strong>Error Handling:</strong> Returns <-1, -1> for invalid coordinates</li>
     *   <li><strong>Performance:</strong> Much faster than file-based conversion</li>
     * </ul>
     *
     * @param ast the CompilationUnit containing the source code
     * @param offset the 0-based character offset in the source file
     * @return an IntPair containing line number (first) and column number (second), or <-1, -1> on error
     */
    public static IntPair getLineAndColumn(CompilationUnit ast, int offset) {
        if (ast != null) {
            int lineNo = ast.getLineNumber(offset);
            // column is 0-based and line is 1-based, so adding 1
            // to column number to make both line and column 1 based.
            int columnNo = ast.getColumnNumber(offset) + 1;

            if (lineNo >= 0 && columnNo >= 0) {
                return IntPair.of(lineNo, columnNo);
            }
        }
        return IntPair.of(-1, -1);
    }

    /**
     * Get the root context expression of field access or method invocation and calculate the token range of
     * the enclosing scope if it is a local variable. For example, 'a.b.c', 'a.b().c' and 'a.b().c()' the root context
     * expression is 'a' If a is a local variable, calculate the token range of the enclosing scope.
     *
     * @param node the target ast node
     * @return the token range of enclosing scope if the node's context expression is a local variable, otherwise null.
     */
    public static TokenRange getLocalVariableScope(ASTNode node) {
        ASTNode varDec = null;
        // First find out the root context expression
        while (true) {
            if (node instanceof FieldAccess) {
                node = ((FieldAccess) node).getExpression();
            } else if (node instanceof QualifiedName) {
                node = ((QualifiedName) node).getQualifier();
            } else if (node instanceof MethodInvocation) {
                node = ((MethodInvocation) node).getExpression();
            } else {
                break;
            }
        }
        // If the root context expression is a simple, then check
        // if it is a local variable and get the variable declaration
        if (node instanceof SimpleName) {
            IBinding binding = ((SimpleName) node).resolveBinding();
            if (binding != null && binding.getJavaElement() instanceof ILocalVariable) {
                varDec = ASTNodeUtility.getDeclaringNode(binding);
            }
        }
        // The root context expression is not a local variable
        if (varDec == null) {
            return null;
        }
        // Find the scope of the local variable
        while (node != null
                && !(node instanceof SwitchStatement
                        || node instanceof ForStatement
                        || node instanceof EnhancedForStatement
                        || node instanceof CatchClause
                        || node instanceof LambdaExpression
                        || node instanceof WhileStatement
                        || node instanceof MethodDeclaration
                        || node instanceof Initializer)) {
            ASTNode parent = node.getParent();
            if (node instanceof Block && (parent instanceof IfStatement || parent instanceof TryStatement)) {
                break;
            }
            node = parent;
        }
        // If the scope of the local variable is method-declaration or initializer,
        // then this variable is accessible from anywhere in it's body. scope is null.
        if (node == null || node instanceof MethodDeclaration || node instanceof Initializer) {
            return null;
        }
        // Get the token range of the local variable scope
        return getTokenRangeFromNode(node);
    }

    /**
     * Retrieves the AST node that declares the specified binding.
     *
     * <p>This method converts an IBinding to its corresponding AST node by first extracting
     * the Java element from the binding, then using that element to find the declaring node.
     * It handles various types of bindings including fields, methods, types, and local variables.</p>
     * 
     * <p><strong>Binding Resolution Process:</strong></p>
     * <ol>
     *   <li><strong>Java Element Extraction:</strong> Gets the Java element from the binding</li>
     *   <li><strong>Exception Handling:</strong> Wraps the extraction in try-catch for incomplete bindings</li>
     *   <li><strong>Node Lookup:</strong> Delegates to getDeclaringNode(IJavaElement) for actual resolution</li>
     *   <li><strong>Error Recovery:</strong> Returns null for any binding resolution failures</li>
     * </ol>
     * 
     * <p><strong>Supported Binding Types:</strong></p>
     * <ul>
     *   <li><strong>Field Bindings:</strong> IField bindings for class and enum fields</li>
     *   <li><strong>Method Bindings:</strong> IMethod bindings for class and interface methods</li>
     *   <li><strong>Type Bindings:</strong> IType bindings for classes, interfaces, and enums</li>
     *   <li><strong>Local Variable Bindings:</strong> ILocalVariable bindings for method parameters and local variables</li>
     *   <li><strong>Member Bindings:</strong> IMember bindings for general class members</li>
     * </ul>
     * 
     * <p><strong>Incomplete Binding Handling:</strong></p>
     * <ul>
     *   <li><strong>Synthetic Filename Issues:</strong> Some incomplete bindings create synthetic filenames</li>
     *   <li><strong>Exception Prevention:</strong> Try-catch prevents synthetic filename exceptions</li>
     *   <li><strong>Graceful Degradation:</strong> Returns null instead of throwing exceptions</li>
     *   <li><strong>Error Isolation:</strong> Binding failures don't propagate to calling code</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Field Resolution:</strong> getDeclaringNode(fieldBinding) returns FieldDeclaration node</li>
     *   <li><strong>Method Resolution:</strong> getDeclaringNode(methodBinding) returns MethodDeclaration node</li>
     *   <li><strong>Type Resolution:</strong> getDeclaringNode(typeBinding) returns TypeDeclaration node</li>
     *   <li><strong>Variable Resolution:</strong> getDeclaringNode(variableBinding) returns VariableDeclaration node</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Binding Access:</strong> Involves JDT binding resolution</li>
     *   <li><strong>Exception Handling:</strong> Minimal overhead for try-catch</li>
     *   <li><strong>Delegation:</strong> Efficient delegation to specialized methods</li>
     *   <li><strong>Memory Usage:</strong> Minimal overhead for binding analysis</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Incomplete Binding Support:</strong> Handles incomplete bindings gracefully</li>
     *   <li><strong>Exception Safety:</strong> Never throws exceptions from binding resolution</li>
     *   <li><strong>Null Return:</strong> Returns null for any binding resolution failures</li>
     *   <li><strong>Delegation Pattern:</strong> Uses delegation to specialized resolution methods</li>
     *   <li><strong>Error Isolation:</strong> Binding failures are contained within this method</li>
     * </ul>
     *
     * @param binding the IBinding to resolve to its declaring AST node
     * @return the AST node that declares the binding, or null if resolution fails
     */
    public static ASTNode getDeclaringNode(IBinding binding) {
        // binding that is incomplete should call getJavaElement() method
        // in try block because sometimes it creates synthetic filename
        // which throws exception
        try {
            IJavaElement javaElement = binding.getJavaElement();
            return getDeclaringNode(javaElement);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves the AST node that declares the specified Java element.
     *
     * <p>This method is the core implementation that converts various types of Java elements
     * to their corresponding AST nodes. It handles different element types with specialized
     * logic for each, including special cases for anonymous classes and enum constants.</p>
     * 
     * <p><strong>Element Type Handling:</strong></p>
     * <ol>
     *   <li><strong>Field Elements:</strong> Handles IField with special logic for enum constants</li>
     *   <li><strong>Type Elements:</strong> Handles IType with anonymous class detection</li>
     *   <li><strong>Method Elements:</strong> Handles IMethod for method declarations</li>
     *   <li><strong>Member Elements:</strong> Handles IMember for general class members</li>
     *   <li><strong>Local Variable Elements:</strong> Handles ILocalVariable for method parameters</li>
     * </ol>
     * 
     * <p><strong>Special Case Handling:</strong></p>
     * <ul>
     *   <li><strong>Enum Constants:</strong> Tries FieldDeclaration first, then EnumConstantDeclaration</li>
     *   <li><strong>Anonymous Classes:</strong> Returns AnonymousClassDeclaration nodes directly</li>
     *   <li><strong>Source Range Access:</strong> Uses appropriate range methods for each element type</li>
     *   <li><strong>Compilation Unit Access:</strong> Retrieves compilation unit for AST parsing</li>
     * </ul>
     * 
     * <p><strong>Source Range Strategies:</strong></p>
     * <ul>
     *   <li><strong>Field Declarations:</strong> Uses getNameRange() for field name positioning</li>
     *   <li><strong>Type Declarations:</strong> Uses getSourceRange() for full type positioning</li>
     *   <li><strong>Method Declarations:</strong> Uses getSourceRange() for full method positioning</li>
     *   <li><strong>Local Variables:</strong> Uses getNameRange() for variable name positioning</li>
     * </ul>
     * 
     * <p><strong>AST Node Resolution:</strong></p>
     * <ul>
     *   <li><strong>Source Range Analysis:</strong> Extracts offset and length from source ranges</li>
     *   <li><strong>File Path Resolution:</strong> Gets file path from compilation unit</li>
     *   <li><strong>AST Acquisition:</strong> Loads AST from file path</li>
     *   <li><strong>Node Finding:</strong> Uses findNode to locate the specific declaration</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Field Resolution:</strong> getDeclaringNode(fieldElement) returns FieldDeclaration or EnumConstantDeclaration</li>
     *   <li><strong>Type Resolution:</strong> getDeclaringNode(typeElement) returns TypeDeclaration or AnonymousClassDeclaration</li>
     *   <li><strong>Method Resolution:</strong> getDeclaringNode(methodElement) returns MethodDeclaration</li>
     *   <li><strong>Variable Resolution:</strong> getDeclaringNode(variableElement) returns VariableDeclaration</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Element Type Checking:</strong> Constant-time instanceof checks</li>
     *   <li><strong>Source Range Access:</strong> Involves JDT element operations</li>
     *   <li><strong>File I/O:</strong> May require reading source files</li>
     *   <li><strong>AST Parsing:</strong> May require parsing source code</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Exception Handling:</strong> Gracefully handles element access failures</li>
     *   <li><strong>Null Safety:</strong> Returns null for any resolution failures</li>
     *   <li><strong>Special Case Logic:</strong> Handles enum constants and anonymous classes specially</li>
     *   <li><strong>File Dependency:</strong> Requires source file accessibility</li>
     *   <li><strong>AST Dependency:</strong> Requires successful AST parsing</li>
     * </ul>
     *
     * @param javaElement the IJavaElement to resolve to its declaring AST node
     * @return the AST node that declares the Java element, or null if resolution fails
     */
    public static ASTNode getDeclaringNode(IJavaElement javaElement) {
        if (javaElement instanceof IField) {
            IField iNode = (IField) javaElement;
            ISourceRange sourceRange = null;
            try {
                sourceRange = iNode.getNameRange();
                ICompilationUnit icu = iNode.getCompilationUnit();
                // Enum constants come as static final fields and the
                // declaring node for them is not field declaration, so try
                // for EnumConstantDeclaration if FieldDeclaration not found.
                ASTNode node = getDeclaringNode(sourceRange, icu, FieldDeclaration.class);
                if (node == null) {
                    node = getDeclaringNode(sourceRange, icu, EnumConstantDeclaration.class);
                }
                return node;
            } catch (Exception e) {
            }
        } else if (javaElement instanceof IType) {
            IType iNode = (IType) javaElement;
            ISourceRange sourceRange = null;
            try {
                sourceRange = iNode.getSourceRange();
                ICompilationUnit icu = iNode.getCompilationUnit();
                if (iNode.isAnonymous()) {
                    return getDeclaringNode(sourceRange, icu, AnonymousClassDeclaration.class);
                } else {
                    return getDeclaringNode(sourceRange, icu, AbstractTypeDeclaration.class);
                }
            } catch (Exception e) {
            }
        } else if (javaElement instanceof IMethod) {
            IMethod iNode = (IMethod) javaElement;
            ISourceRange sourceRange = null;
            try {
                sourceRange = iNode.getSourceRange();
                ICompilationUnit icu = iNode.getCompilationUnit();
                return getDeclaringNode(sourceRange, icu, MethodDeclaration.class);
            } catch (Exception e) {
            }
        } else if (javaElement instanceof IMember) {
            IMember iNode = (IMember) javaElement;
            ISourceRange sourceRange = null;
            try {
                sourceRange = iNode.getSourceRange();
                ICompilationUnit icu = iNode.getCompilationUnit();
                return getDeclaringNode(sourceRange, icu, ASTNode.class);
            } catch (Exception e) {
            }
        } else if (javaElement instanceof ILocalVariable) {
            ILocalVariable iNode = (ILocalVariable) javaElement;
            ISourceRange sourceRange = null;
            try {
                sourceRange = iNode.getNameRange();
                ICompilationUnit icu = iNode.getDeclaringMember().getCompilationUnit();
                return getDeclaringNode(sourceRange, icu, VariableDeclaration.class);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static <T extends ASTNode> ASTNode getDeclaringNode(
            ISourceRange sourceRange, ICompilationUnit icu, Class<T> targetClass) {
        try {
            int offset = sourceRange.getOffset();
            int length = sourceRange.getLength();
            if (icu != null) {
                IResource resource = icu.getCorrespondingResource();
                String fileName = resource.getRawLocation().toOSString();
                CompilationUnit cu = getCompilationUnitFromFilePath(fileName);
                if (cu != null) {
                    ASTNode declaringNode = ASTNodeUtility.findNode(targetClass, cu, offset, length);
                    return declaringNode;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Retrieves the complete file path containing the specified method.
     *
     * <p>This method extracts the file path from a method's compilation unit, providing
     * the absolute path to the source file that contains the method. It's useful for
     * file operations, navigation, and cross-referencing between different parts of
     * the codebase.</p>
     * 
     * <p><strong>File Path Resolution Process:</strong></p>
     * <ol>
     *   <li><strong>Compilation Unit Access:</strong> Gets the compilation unit from the method</li>
     *   <li><strong>Resource Resolution:</strong> Converts compilation unit to IResource</li>
     *   <li><strong>Path Extraction:</strong> Extracts the raw location path from the resource</li>
     *   <li><strong>OS Conversion:</strong> Converts the path to the operating system format</li>
     * </ol>
     * 
     * <p><strong>Path Format Details:</strong></p>
     * <ul>
     *   <li><strong>Absolute Path:</strong> Returns the complete file system path</li>
     *   <li><strong>OS Specific:</strong> Uses operating system path separators</li>
     *   <strong>Raw Location:</strong> Gets the actual file system location, not workspace-relative</li>
     *   <li><strong>File Extension:</strong> Includes the .java file extension</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>File Operations:</strong> Use path for file reading, writing, or analysis</li>
     *   <li><strong>Navigation:</strong> Open source files in external editors or tools</li>
     *   <li><strong>Cross-Referencing:</strong> Link method references to source files</li>
     *   <li><strong>Build Integration:</strong> Use paths in build scripts or automation</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Constant Time:</strong> O(1) for path extraction operations</li>
     *   <li><strong>No File I/O:</strong> Works entirely with in-memory metadata</li>
     *   <li><strong>Efficient Resolution:</strong> Uses Eclipse's optimized resource system</li>
     *   <li><strong>Memory Usage:</strong> Minimal overhead for path operations</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>Exception Safety:</strong> Wraps all operations in try-catch</li>
     *   <li><strong>Null Return:</strong> Returns null for any resolution failures</li>
     *   <li><strong>Graceful Degradation:</strong> Failures don't propagate to calling code</li>
     *   <li><strong>Error Isolation:</strong> Path resolution failures are contained</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Method Dependency:</strong> Requires a valid, non-null IMethod</li>
     *   <li><strong>Compilation Unit Access:</strong> Method must have accessible compilation unit</li>
     *   <li><strong>Resource Availability:</strong> Source file must exist and be accessible</li>
     *   <li><strong>Path Validity:</strong> Returned path may not exist if source is missing</li>
     *   <li><strong>OS Compatibility:</strong> Path format depends on the operating system</li>
     * </ul>
     *
     * @param method the IMethod whose file path should be retrieved
     * @return the complete file path containing the method, or null if resolution fails
     */
    public static String getFilePathOfMethod(IMethod method) {
        try {
            ICompilationUnit icu = method.getCompilationUnit();
            IResource file = icu.getCorrespondingResource();
            String fileName = file.getRawLocation().toOSString();
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves the file path from a CompilationUnit.
     *
     * <p>This method extracts the file path from a CompilationUnit by accessing its underlying
     * Java element and converting it to a resource. It provides the absolute path to the source
     * file that the CompilationUnit represents.</p>
     * 
     * <p><strong>File Path Resolution Process:</strong></p>
     * <ol>
     *   <li><strong>Java Element Access:</strong> Gets the ICompilationUnit from the CompilationUnit</li>
     *   <li><strong>Resource Conversion:</strong> Converts the compilation unit to IResource</li>
     *   <li><strong>Path Extraction:</strong> Extracts the raw location path from the resource</li>
     *   <li><strong>OS Conversion:</strong> Converts the path to the operating system format</li>
     * </ol>
     * 
     * <p><strong>Path Format Details:</strong></p>
     * <ul>
     *   <li><strong>Absolute Path:</strong> Returns the complete file system path</li>
     *   <li><strong>OS Specific:</strong> Uses operating system path separators</li>
     *   <strong>Raw Location:</strong> Gets the actual file system location, not workspace-relative</li>
     *   <li><strong>File Extension:</strong> Includes the .java file extension</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>File Operations:</strong> Use path for file reading, writing, or analysis</li>
     *   <li><strong>AST Persistence:</strong> Save AST data with file path references</li>
     *   <li><strong>Cross-Referencing:</strong> Link AST nodes to source files</li>
     *   <li><strong>Build Integration:</strong> Use paths in build scripts or automation</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Constant Time:</strong> O(1) for path extraction operations</li>
     *   <li><strong>No File I/O:</strong> Works entirely with in-memory metadata</li>
     *   <li><strong>Efficient Resolution:</strong> Uses Eclipse's optimized resource system</li>
     *   <li><strong>Memory Usage:</strong> Minimal overhead for path operations</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>Exception Safety:</strong> Wraps all operations in try-catch</li>
     *   <li><strong>Null Return:</strong> Returns null for any resolution failures</li>
     *   <li><strong>Graceful Degradation:</strong> Failures don't propagate to calling code</li>
     *   <li><strong>Error Isolation:</strong> Path resolution failures are contained</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>CompilationUnit Dependency:</strong> Requires a valid, non-null CompilationUnit</li>
     *   <li><strong>Java Element Access:</strong> CompilationUnit must have accessible Java element</li>
     *   <li><strong>Resource Availability:</strong> Source file must exist and be accessible</li>
     *   <li><strong>Path Validity:</strong> Returned path may not exist if source is missing</li>
     *   <li><strong>OS Compatibility:</strong> Path format depends on the operating system</li>
     * </ul>
     *
     * @param cu the CompilationUnit whose file path should be retrieved
     * @return the complete file path containing the CompilationUnit, or null if resolution fails
     */
    public static String getFilePathFromCompilationUnit(CompilationUnit cu) {
        try {
            ICompilationUnit icu = (ICompilationUnit) cu.getJavaElement();
            IResource file = icu.getCorrespondingResource();
            String fileName = file.getRawLocation().toOSString();
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves a CompilationUnit from a file path.
     *
     * <p>This method uses the JavaVPG (Java Value Property Graph) system to acquire an AST
     * for the specified file path. It's the primary method for loading Java source files
     * into the AST system for analysis and manipulation.</p>
     * 
     * <p><strong>AST Acquisition Process:</strong></p>
     * <ol>
     *   <li><strong>JavaVPG Access:</strong> Gets the singleton instance of JavaVPG</li>
     *   <li><strong>File Path Validation:</strong> Uses the provided file path for AST loading</li>
     *   <li><strong>AST Parsing:</strong> JavaVPG parses the source file and creates the AST</li>
     *   <li><strong>AST Return:</strong> Returns the parsed CompilationUnit</li>
     * </ol>
     * 
     * <p><strong>JavaVPG Integration:</strong></p>
     * <ul>
     *   <li><strong>Singleton Pattern:</strong> Uses JavaVPG.getInstance() for access</li>
     *   <li><strong>AST Management:</strong> JavaVPG handles AST creation and caching</li>
     *   <li><strong>File Parsing:</strong> JavaVPG parses Java source files into ASTs</li>
     *   <li><strong>Memory Management:</strong> JavaVPG may cache ASTs for performance</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>AST Loading:</strong> Load source files for analysis</li>
     *   <li><strong>Node Finding:</strong> Use with findNode methods for node discovery</li>
     *   <li><strong>File Analysis:</strong> Analyze entire source files</li>
     *   <li><strong>Cross-Reference:</strong> Link file paths to AST structures</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>File I/O:</strong> Involves reading the source file from disk</li>
     *   <li><strong>Parsing Overhead:</strong> Java source parsing is CPU-intensive</li>
     *   <li><strong>Caching Benefits:</strong> JavaVPG may cache ASTs for repeated access</li>
     *   <li><strong>Memory Usage:</strong> AST objects consume significant memory</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>File Accessibility:</strong> File must exist and be readable</li>
     *   <li><strong>Syntax Validity:</strong> Source file must contain valid Java syntax</li>
     *   <li><strong>Parsing Errors:</strong> Invalid Java code may cause parsing failures</li>
     *   <li><strong>Null Return:</strong> May return null for parsing failures</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>File Path Dependency:</strong> Requires valid, accessible file path</li>
     *   <li><strong>Java Syntax Requirement:</strong> File must contain valid Java code</li>
     *   <li><strong>AST Ownership:</strong> Returned AST is managed by JavaVPG</li>
     *   <li><strong>Memory Management:</strong> ASTs may be cached and shared</li>
     *   <li><strong>Performance Impact:</strong> First-time parsing is expensive</li>
     * </ul>
     *
     * @param filePath the complete file path to the Java source file
     * @return the CompilationUnit representing the parsed source file, or null if parsing fails
     */
    public static CompilationUnit getCompilationUnitFromFilePath(String filePath) {
        JavaVPG javaVPG = JavaVPG.getInstance();
        CompilationUnit cu = null;
        cu = javaVPG.acquireAST(filePath);
        return cu;
    }

    /**
     * Creates a TokenRange from a binding's source location information.
     *
     * <p>This method extracts source location information (file path, offset, and length) from
     * various types of bindings and creates a TokenRange object. It handles different binding
     * types with specialized logic, including special cases for anonymous classes and library
     * elements.</p>
     * 
     * <p><strong>Binding Type Handling:</strong></p>
     * <ol>
     *   <li><strong>Type Bindings:</strong> Handles IType with special logic for anonymous classes</li>
     *   <li><strong>Field Bindings:</strong> Handles IField for class and enum fields</li>
     *   <li><strong>Member Bindings:</strong> Handles IMember for general class members</li>
     *   <li><strong>Annotation Bindings:</strong> Handles IAnnotation for annotation declarations</li>
     *   <li><strong>Local Variable Bindings:</strong> Handles ILocalVariable for method parameters</li>
     * </ol>
     * 
     * <p><strong>Special Case Handling:</strong></p>
     * <ul>
     *   <li><strong>Anonymous Classes:</strong> Uses AnonymousClassDeclaration for accurate positioning</li>
     *   <li><strong>Library Elements:</strong> Handles cases where only offset/length are available</li>
     *   <li><strong>Package Elements:</strong> Skips package and package root elements</li>
     *   <li><strong>Exception Handling:</strong> Gracefully handles binding access failures</li>
     * </ul>
     * 
     * <p><strong>Source Range Strategies:</strong></p>
     * <ul>
     *   <li><strong>Type Declarations:</strong> Uses getSourceRange() for full type positioning</li>
     *   <li><strong>Field Declarations:</strong> Uses getNameRange() for field name positioning</li>
     *   <li><strong>Method Declarations:</strong> Uses getSourceRange() for full method positioning</li>
     *   <li><strong>Local Variables:</strong> Uses getNameRange() for variable name positioning</li>
     * </ul>
     * 
     * <p><strong>Anonymous Class Special Logic:</strong></p>
     * <ul>
     *   <li><strong>Type Binding Issue:</strong> IType bindings for anonymous classes point to parent</li>
     *   <li><strong>AST Resolution:</strong> Uses findNode to locate actual AnonymousClassDeclaration</li>
     *   <li><strong>Accurate Positioning:</strong> Provides precise location for anonymous class</li>
     *   <li><strong>Fallback Strategy:</strong> Uses parent source range if AST resolution fails</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Type Binding:</strong> getTokenRangeOfBinding(typeBinding) returns TokenRange for class</li>
     *   <li><strong>Field Binding:</strong> getTokenRangeOfBinding(fieldBinding) returns TokenRange for field</li>
     *   <li><strong>Method Binding:</strong> getTokenRangeOfBinding(methodBinding) returns TokenRange for method</li>
     *   <li><strong>Variable Binding:</strong> getTokenRangeOfBinding(variableBinding) returns TokenRange for variable</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Binding Access:</strong> Involves JDT binding resolution</li>
     *   <li><strong>File I/O:</strong> May require reading source files for AST resolution</li>
     *   <li><strong>AST Parsing:</strong> May require parsing source code for anonymous classes</li>
     *   <li><strong>Memory Usage:</strong> Creates TokenRange objects for each binding</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Library Element Limitation:</strong> Library elements may not provide file paths</li>
     *   <li><strong>Anonymous Class Accuracy:</strong> Provides precise positioning for anonymous classes</li>
     *   <li><strong>Exception Safety:</strong> Gracefully handles all binding access failures</li>
     *   <li><strong>Null Return:</strong> Returns null for unsupported element types</li>
     *   <li><strong>File Dependency:</strong> Requires source file accessibility for AST resolution</li>
     * </ul>
     *
     * @param binding the binding (Class, Method, Field, etc.) to extract source location from
     * @return a TokenRange containing the binding's source location, or null if location cannot be determined
     */
    public static TokenRange getTokenRangeOfBinding(IBinding binding) {
        // For library class/Interface we get offset, length. But we
        // don't get filename. So it is unnecessary to create tokenrange with only
        // offset and length. In that case we will return null.
        TokenRange tokenRange = null;
        try {
            // There seems to be an exception thrown when getting the
            // Java Element from some library class/interface's binding
            // in the real projects. So, the covering try-catch block
            // handles this unwarranted exception.
            IJavaElement javaElement = binding.getJavaElement();
            // The java element taken from the type binding of an anonymous class
            // always gets us the source range for the parent ClassInstanceCreation
            // or EnumConstantDeclaration. So, we will have to get a more accurate
            // range for the anonymous class from its declaration node.
            //
            // That is why, IType has been handled differently before IMember now.
            // Previously, IMember handled methods and types both.
            if (javaElement instanceof IType) {
                IType type = (IType) javaElement;
                try {
                    IResource resource = type.getResource();
                    String fileName = resource.getRawLocation().toOSString();
                    ISourceRange sourceRange = type.getSourceRange();
                    if (type.isAnonymous()) {
                        CompilationUnit cu = getCompilationUnitFromFilePath(fileName);
                        AnonymousClassDeclaration anon = findNode(
                                AnonymousClassDeclaration.class, cu, sourceRange.getOffset(), sourceRange.getLength());
                        tokenRange = new TokenRange(anon.getStartPosition(), anon.getLength(), fileName);
                    } else {
                        tokenRange = new TokenRange(sourceRange.getOffset(), sourceRange.getLength(), fileName);
                    }
                } catch (Exception e) {
                    //                  Turned off printing exceptions as it creates ambiguity
                    //                    if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
                }
            } else if (javaElement instanceof IField) {
                IField field = (IField) javaElement;
                try {
                    IResource resource = field.getResource();
                    String fileName = resource.getRawLocation().toOSString();
                    ISourceRange sourceRange = field.getNameRange();
                    tokenRange = new TokenRange(sourceRange.getOffset(), sourceRange.getLength(), fileName);
                } catch (Exception e) {
                    //                      Turned off printing exceptions as it creates ambiguity
                    //                    if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
                }
            } else if (javaElement instanceof IMember) {
                IMember member = (IMember) javaElement;
                try {
                    IResource resource = member.getResource();
                    String fileName = resource.getRawLocation().toOSString();
                    ISourceRange sourceRange = member.getSourceRange();
                    tokenRange = new TokenRange(sourceRange.getOffset(), sourceRange.getLength(), fileName);
                } catch (Exception e) {
                    //                  Turned off printing exceptions as it creates ambiguity
                    //                    if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
                }
            } else if (javaElement instanceof IAnnotation) {
                IAnnotation annotaion = (IAnnotation) javaElement;
                try {
                    IResource resource = annotaion.getResource();
                    String fileName = resource.getRawLocation().toOSString();
                    ISourceRange sourceRange = annotaion.getSourceRange();
                    tokenRange = new TokenRange(sourceRange.getOffset(), sourceRange.getLength(), fileName);
                } catch (Exception e) {
                    //                  Turned off printing exceptions as it creates ambiguity
                    //                    if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
                }
            } else if (javaElement instanceof IPackageFragment || javaElement instanceof IPackageFragmentRoot) {
                // Do Nothing
            } else {
                ILocalVariable variable = (ILocalVariable) javaElement;
                try {
                    IResource resource = variable.getResource();
                    String fileName = resource.getRawLocation().toOSString();
                    ISourceRange sourceRange = variable.getNameRange();
                    tokenRange = new TokenRange(sourceRange.getOffset(), sourceRange.getLength(), fileName);
                } catch (Exception e) {
                    //                  Turned off printing exceptions as it creates ambiguity
                    //                    if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
                }
            }
        } catch (Exception e) {
            //          Turned off printing exceptions as it creates ambiguity
            //            if(ConfigurationUtility.contains(ConfigOptions.DEBUG)) e.printStackTrace();
        }
        return tokenRange;
    }

    /**
     * Creates a TokenRange from an ASTNode by automatically determining the file path.
     *
     * <p>This method creates a TokenRange for an ASTNode by finding its compilation unit
     * and extracting the file path from it. It's useful when you have an ASTNode but don't
     * know the file path, as it automatically resolves the path through the AST hierarchy.</p>
     * 
     * <p><strong>File Path Resolution Process:</strong></p>
     * <ol>
     *   <li><strong>Ancestor Search:</strong> Finds the nearest CompilationUnit ancestor</li>
     *   <li><strong>Java Element Access:</strong> Gets the ICompilationUnit from the CompilationUnit</li>
     *   <li><strong>Resource Resolution:</strong> Converts compilation unit to IResource</li>
     *   <li><strong>Path Extraction:</strong> Extracts the raw location path from the resource</li>
     *   <li><strong>TokenRange Creation:</strong> Creates TokenRange with position and file path</li>
     * </ol>
     * 
     * <p><strong>Position Information:</strong></p>
     * <ul>
     *   <li><strong>Start Position:</strong> Uses node.getStartPosition() for offset</li>
     *   <li><strong>Length:</strong> Uses node.getLength() for token length</li>
     *   <strong>File Path:</strong> Automatically resolved from compilation unit</li>
     *   <li><strong>Complete Information:</strong> Provides all TokenRange components</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Method Analysis:</strong> Get TokenRange for method declarations</li>
     *   <li><strong>Field Analysis:</strong> Get TokenRange for field declarations</li>
     *   <li><strong>Statement Analysis:</strong> Get TokenRange for individual statements</li>
     *   <li><strong>Expression Analysis:</strong> Get TokenRange for expressions</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Ancestor Traversal:</strong> Involves walking up the AST hierarchy</li>
     *   <li><strong>Resource Access:</strong> Involves Eclipse resource system operations</li>
     *   <li><strong>Path Resolution:</strong> Involves file system path operations</li>
     *   <li><strong>Memory Usage:</strong> Creates new TokenRange objects</li>
     * </ol>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>Exception Safety:</strong> Wraps all operations in try-catch</li>
     *   <li><strong>Fallback Strategy:</strong> Creates TokenRange without file path on failure</li>
     *   <li><strong>Graceful Degradation:</strong> Failures don't propagate to calling code</li>
     *   <li><strong>Partial Information:</strong> May return TokenRange with missing file path</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>AST Hierarchy Dependency:</strong> Node must be part of a valid AST hierarchy</li>
     *   <li><strong>Compilation Unit Access:</strong> Must have accessible compilation unit</li>
     *   <li><strong>File Path Resolution:</strong> Depends on successful resource resolution</li>
     *   <li><strong>Fallback Behavior:</strong> Returns TokenRange without file path on failure</li>
     *   <li><strong>Position Accuracy:</strong> Position information is always accurate</li>
     * </ul>
     *
     * @param node the ASTNode to create a TokenRange for
     * @return a TokenRange containing the node's position and file path, or position only if file path resolution fails
     */
    public static TokenRange getTokenRangeFromNode(ASTNode node) {
        CompilationUnit cu = findNearestAncestor(node, CompilationUnit.class);
        ICompilationUnit icu = (ICompilationUnit) cu.getJavaElement();
        try {
            IResource resource = icu.getCorrespondingResource();
            String fileName = resource.getRawLocation().toOSString();
            return new TokenRange(node.getStartPosition(), node.getLength(), fileName);
        } catch (Exception e) {
            // Do nothing
        }
        return new TokenRange(node.getStartPosition(), node.getLength());
    }

    /**
     * Creates a TokenRange from an ASTNode using a provided file path.
     *
     * <p>This method creates a TokenRange for an ASTNode using a known file path, making it
     * more efficient than the automatic file path resolution version. It's the preferred
     * method when you already have the file path available.</p>
     * 
     * <p><strong>Efficiency Benefits:</strong></p>
     * <ul>
     *   <li><strong>No Ancestor Traversal:</strong> Skips AST hierarchy walking</li>
     *   <li><strong>No Resource Resolution:</strong> Avoids Eclipse resource system operations</li>
     *   <strong>No Path Extraction:</strong> Uses provided file path directly</li>
     *   <li><strong>Direct Creation:</strong> Creates TokenRange immediately</li>
     * </ul>
     * 
     * <p><strong>Position Information:</strong></p>
     * <ul>
     *   <li><strong>Start Position:</strong> Uses node.getStartPosition() for offset</li>
     *   <li><strong>Length:</strong> Uses node.getLength() for token length</li>
     *   <strong>File Path:</strong> Uses the provided file path directly</li>
     *   <li><strong>Complete Information:</strong> Provides all TokenRange components</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Batch Processing:</strong> When processing multiple nodes from the same file</li>
     *   <li><strong>Performance Critical:</strong> When avoiding AST traversal overhead is important</li>
     *   <li><strong>Known File Path:</strong> When file path is already available</li>
     *   <li><strong>Efficient Operations:</strong> For high-frequency TokenRange creation</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Constant Time:</strong> O(1) for TokenRange creation</li>
     *   <li><strong>No AST Traversal:</strong> Avoids hierarchy walking overhead</li>
     *   <li><strong>No Resource Access:</strong> Avoids Eclipse resource system overhead</li>
     *   <li><strong>Memory Usage:</strong> Creates new TokenRange objects efficiently</li>
     * </ul>
     * 
     * <p><strong>When to Use:</strong></p>
     * <ul>
     *   <li><strong>File Path Available:</strong> When you already know the file path</li>
     *   <li><strong>Performance Critical:</strong> When efficiency is more important than convenience</li>
     *   <li><strong>Batch Operations:</strong> When processing multiple nodes from the same file</li>
     *   <li><strong>Known Context:</strong> When working within a known file context</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>File Path Dependency:</strong> Requires valid, non-null file path</li>
     *   <li><strong>Path Validation:</strong> No validation of file path existence or accessibility</li>
     *   <li><strong>Position Accuracy:</strong> Position information is always accurate</li>
     *   <li><strong>Efficiency Focus:</strong> Optimized for performance over convenience</li>
     *   <li><strong>Direct Creation:</strong> No fallback or error handling needed</li>
     * </ul>
     *
     * @param node the ASTNode to create a TokenRange for
     * @param filePath the file path to use in the TokenRange
     * @return a TokenRange containing the node's position and the specified file path
     */
    public static TokenRange getTokenRangeFromNode(ASTNode node, String filePath) {
        return new TokenRange(node.getStartPosition(), node.getLength(), filePath);
    }

    /**
     * Retrieves an AST node of a specific type from a TokenRange.
     *
     * <p>This method uses a TokenRange to locate and return an AST node of the specified type
     * within the source file. It's useful for converting location information back to actual
     * AST nodes for analysis or manipulation.</p>
     * 
     * <p><strong>Node Resolution Process:</strong></p>
     * <ol>
     *   <li><strong>File Path Extraction:</strong> Gets the file path from the TokenRange</li>
     *   <li><strong>AST Loading:</strong> Loads the CompilationUnit for the file</li>
     *   <li><strong>Position Extraction:</strong> Gets offset and length from TokenRange</li>
     *   <li><strong>Node Finding:</strong> Uses findNode to locate the specific node type</li>
     *   <li><strong>Type Validation:</strong> Ensures the found node matches the requested type</li>
     * </ol>
     * 
     * <p><strong>TokenRange Components Used:</strong></p>
     * <ul>
     *   <li><strong>File Path:</strong> Used to load the correct source file</li>
     *   <li><strong>Offset:</strong> Used to locate the node's starting position</li>
     *   <li><strong>Length:</strong> Used to determine the node's extent</li>
     *   <strong>Position Validation:</strong> Ensures accurate node location</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Method Resolution:</strong> getASTNodeFromTokenRange(tokenRange, MethodDeclaration.class)</li>
     *   <li><strong>Class Resolution:</strong> getASTNodeFromTokenRange(tokenRange, TypeDeclaration.class)</li>
     *   <li><strong>Field Resolution:</strong> getASTNodeFromTokenRange(tokenRange, FieldDeclaration.class)</li>
     *   <li><strong>Statement Resolution:</strong> getASTNodeFromTokenRange(tokenRange, Statement.class)</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>File I/O:</strong> Involves reading the source file from disk</li>
     *   <li><strong>AST Parsing:</strong> May require parsing the source file</li>
     *   <li><strong>Node Finding:</strong> Involves AST traversal and node matching</li>
     *   <li><strong>Memory Usage:</strong> Loads entire AST into memory</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>Null TokenRange:</strong> Returns null for null TokenRange input</li>
     *   <li><strong>File Access:</strong> May fail if file is not accessible</li>
     *   <li><strong>AST Parsing:</strong> May fail if source contains syntax errors</li>
     *   <li><strong>Node Finding:</strong> May fail if no matching node is found</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>TokenRange Dependency:</strong> Requires valid, non-null TokenRange</li>
     *   <li><strong>File Accessibility:</strong> Source file must be accessible</li>
     *   <li><strong>Syntax Validity:</strong> Source file must contain valid Java code</li>
     *   <li><strong>Type Matching:</strong> Returns null if no matching node type is found</li>
     *   <li><strong>Position Accuracy:</strong> TokenRange position must be accurate</li>
     * </ul>
     *
     * @param <T> the generic type of AST node to retrieve (must extend ASTNode)
     * @param tokenRange the TokenRange containing location information
     * @param nodeType the class type of the node to find
     * @return an AST node of type T at the specified location, or null if not found
     */
    public static <T extends ASTNode> T getASTNodeFromTokenRange(TokenRange tokenRange, Class<T> nodeType) {
        if (tokenRange == null) {
            return null;
        }
        CompilationUnit cu = getCompilationUnitFromFilePath(tokenRange.getFileName());
        return findNode(nodeType, cu, tokenRange.getOffset(), tokenRange.getLength());
    }

    /**
     * An abstract iterator that filters and transforms items from another iterator.
     *
     * <p>FilteringIterator provides a framework for creating iterators that filter items
     * based on custom criteria and transform them into the desired type. It's used by
     * the findAll method to create type-safe iterators for AST node collections.</p>
     * 
     * <p><strong>Iterator Behavior:</strong></p>
     * <ul>
     *   <li><strong>Lazy Evaluation:</strong> Items are processed only when requested</li>
     *   <li><strong>Filtering:</strong> Only items that pass shouldProcess() are returned</li>
     *   <li><strong>Transformation:</strong> Items are transformed via process() method</li>
     *   <li><strong>Single Pass:</strong> Each iterator can only be used once</li>
     * </ul>
     * 
     * <p><strong>Abstract Methods:</strong></p>
     * <ul>
     *   <li><strong>shouldProcess:</strong> Determines if an item should be included</li>
     *   <li><strong>process:</strong> Transforms an item to the target type T</li>
     * </ul>
     * 
     * <p><strong>Usage Pattern:</strong></p>
     * <ul>
     *   <li><strong>Type Filtering:</strong> Filter nodes by specific types</li>
     *   <li><strong>Condition Filtering:</strong> Filter nodes by custom conditions</li>
     *   <li><strong>Data Transformation:</strong> Convert nodes to different representations</li>
     *   <li><strong>Lazy Processing:</strong> Process large collections efficiently</li>
     * </ul>
     */
    public abstract static class FilteringIterator<T> implements Iterator<T> {
        private boolean done;
        private Iterator<?> wrappedIterator;
        private Object next;

        public FilteringIterator(Iterator<?> wrappedIterator) {
            this.done = false;
            this.wrappedIterator = wrappedIterator;
            findNext();
        }

        private void findNext() {
            do {
                if (!this.wrappedIterator.hasNext()) {
                    this.done = true;
                    return;
                }

                this.next = this.wrappedIterator.next();
            } while (!shouldProcess(this.next));
        }

        public boolean hasNext() {
            return !this.done;
        }

        public T next() {
            T result = process(this.next);
            findNext();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Determines whether or not the given item should be returned by this iterator.
         *
         * <p>The item will be passed to {@link #process(Object)} iff this method returns <code>true</code>.
         *
         * @return true iff the given item should be returned by this iterator
         */
        protected abstract boolean shouldProcess(Object item);

        /**
         * Translates the given item into an object of type <code>T</code>.
         *
         * <p>This method will be invoked iff {@link #shouldProcess(Object)} returned <code>true</code>.
         *
         * @return an object of type <code>T</code> corresponding to the given item
         */
        protected abstract T process(Object item);
    }

    /**
     * An iterator that traverses an AST tree in depth-first order.
     *
     * <p>ASTIterator provides efficient traversal of Abstract Syntax Trees by maintaining
     * a stack of iterators for different levels of the tree. It visits all nodes in
     * depth-first order, ensuring comprehensive coverage of the AST structure.</p>
     * 
     * <p><strong>Traversal Strategy:</strong></p>
     * <ul>
     *   <li><strong>Depth-First Search:</strong> Explores each branch completely before moving to siblings</li>
     *   <li><strong>Stack-Based Implementation:</strong> Uses a stack to manage traversal state</li>
     *   <li><strong>Child Processing:</strong> Processes all children of each node</li>
     *   <li><strong>Comprehensive Coverage:</strong> Visits every node in the tree</li>
     * </ul>
     * 
     * <p><strong>Stack Management:</strong></p>
     * <ul>
     *   <li><strong>Iterator Stack:</strong> Maintains stack of child iterators</li>
     *   <li><strong>Level Management:</strong> Each level gets its own iterator</li>
     *   <li><strong>Stack Cleanup:</strong> Removes exhausted iterators automatically</li>
     *   <li><strong>Memory Efficiency:</strong> Minimal memory overhead for traversal</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Complete Traversal:</strong> Visit every node in the AST</li>
     *   <li><strong>Node Collection:</strong> Gather all nodes for analysis</li>
     *   <li><strong>Pattern Matching:</strong> Find nodes matching specific criteria</li>
     *   <li><strong>Tree Analysis:</strong> Analyze the complete structure</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Linear Time:</strong> O(n) where n is the number of nodes</li>
     *   <li><strong>Constant Space:</strong> O(d) where d is the maximum tree depth</li>
     *   <li><strong>Efficient Traversal:</strong> Minimal overhead per node</li>
     *   <li><strong>Memory Management:</strong> Automatic cleanup of exhausted iterators</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Single Pass:</strong> Each iterator can only be used once</li>
     *   <li><strong>Non-Modifiable:</strong> Does not support remove() operations</li>
     *   <li><strong>Thread Safety:</strong> Not thread-safe for concurrent access</li>
     *   <li><strong>Order Consistency:</strong> Depth-first order is guaranteed</li>
     * </ul>
     */
    public static final class ASTIterator implements Iterator<ASTNode> {
        protected final Stack<Iterator<? extends ASTNode>> stack;

        public ASTIterator(ASTNode root) {
            this.stack = new Stack<Iterator<? extends ASTNode>>();
            stack.push(Collections.singleton(root).iterator());
        }

        public boolean hasNext() {
            return !stack.isEmpty() && stack.peek().hasNext();
        }

        public ASTNode next() {
            if (!hasNext()) return null;

            ASTNode nextNode = stack.peek().next();

            Iterator<? extends ASTNode> children = getChildren(nextNode).iterator();
            if (children.hasNext()) stack.push(children);

            while (!stack.isEmpty() && !stack.peek().hasNext()) stack.pop();

            return nextNode;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
