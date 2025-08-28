/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openrefactory.util.CallGraphUtility;

/**
 * A class representing the Call Graph.
 *
 * <p>The CallGraph class is a fundamental data structure that represents the relationships
 * between methods in a Java program. It maintains bidirectional mappings between caller
 * and callee methods, enabling efficient traversal and analysis of method call patterns.</p>
 * 
 * <p>The call graph is implemented using two concurrent maps:</p>
 * <ul>
 *   <li><strong>Caller-to-Callee mapping:</strong> Maps each method to the list of methods it calls</li>
 *   <li><strong>Callee-to-Caller mapping:</strong> Maps each method to the list of methods that call it</li>
 * </ul>
 * 
 * <p>This bidirectional structure enables efficient queries in both directions:
 * finding what methods a given method calls, and finding what methods call a given method.
 * The class uses thread-safe collections to support concurrent access during call graph
 * construction and analysis.</p>
 * 
 * <p>The call graph automatically filters out methods from test files and protobuf files
 * during edge addition, ensuring that only production code methods are included in the
 * analysis.</p>
 *
 * @author Mohammad Rafid Ul Islam
 */
public class CallGraph implements Serializable {

    /** Serialization version identifier */
    private static final long serialVersionUID = 1L;

    /** 
     * Caller to a list of callees map.
     * 
     * <p>This map maintains the forward relationship from caller methods to their callees.
     * Each key is a method hash representing a caller, and the value is a list of method
     * hashes representing the methods that are called by that caller.</p>
     * 
     * <p>The map is thread-safe using {@link ConcurrentHashMap} to support concurrent
     * access during call graph construction.</p>
     */
    private Map<String, List<String>> callerKeyCallGraphMap;
    
    /** 
     * Callee to a list of callers map.
     * 
     * <p>This map maintains the reverse relationship from callee methods to their callers.
     * Each key is a method hash representing a callee, and the value is a list of method
     * hashes representing the methods that call that callee.</p>
     * 
     * <p>The map is thread-safe using {@link ConcurrentHashMap} to support concurrent
     * access during call graph construction.</p>
     */
    private Map<String, List<String>> calleeKeyCallGraphMap;
    
    /** 
     * Number of edges in the call graph.
     * 
     * <p>This field tracks the total number of method call relationships (edges)
     * in the call graph. It's incremented each time a new edge is added and
     * reset to zero when the graph is deinitialized.</p>
     */
    private int size;

    public CallGraph() {
        size = 0;
        callerKeyCallGraphMap = new ConcurrentHashMap<>();
        calleeKeyCallGraphMap = new ConcurrentHashMap<>();
    }

    public void deinitialize() {
        size = 0;
        callerKeyCallGraphMap.clear();
        calleeKeyCallGraphMap.clear();
    }

    /**
     * Creates an edge between two methods in the call graph.
     *
     * <p>This method adds a directed edge from the caller method to the callee method,
     * representing that the caller invokes the callee. The edge is added to both
     * directional maps to maintain bidirectional relationships.</p>
     * 
     * <p>The method automatically filters out edges where the callee method is from:
     * <ul>
     *   <li>Test files (excluded files)</li>
     *   <li>Protobuf files (auto-generated files)</li>
     * </ul>
     * 
     * <p>This filtering ensures that only production code method calls are included
     * in the call graph analysis.</p>
     *
     * @param caller the hash of the caller method
     * @param callee the hash of the callee method
     * @see #addToCallerKeyCallGraphMap(String, String)
     * @see #addToCalleeKeyCallGraphMap(String, String)
     */
    public void addEdge(String caller, String callee) {
        // Don't add the edge if the callee method
        // is from test file as test files are excluded.
        String methodSignature = CallGraphDataStructures.getMethodSignatureFromHash(callee);
        if (methodSignature != null) {
            // For dummy methods, method signature is null
            // So, add a check
            String fileName = CallGraphUtility.getFileNameFromSignature(methodSignature);
            if (CallGraphDataStructures.isExcludedFile(fileName) || CallGraphDataStructures.isAProtoFile(fileName)) {
                return;
            }
        }
        addToCallerKeyCallGraphMap(caller, callee);
        addToCalleeKeyCallGraphMap(callee, caller);
        size++;
    }

    /**
     * Returns the list of callee methods for a given caller method.
     *
     * <p>This method retrieves all methods that are called by the specified caller method.
     * The result can optionally include or exclude virtual methods based on the client's needs.</p>
     * 
     * <p>Virtual methods are special wrapper methods that link method calls to actual
     * implementations. For creating dependency graphs, virtual methods are essential.
     * However, for other analysis purposes, clients may choose to filter them out.</p>
     * 
     * <p>When virtual methods are excluded, the method filters out the connecting
     * virtual method hash for Spring Bean calls, which is a special case in the
     * call graph analysis.</p>
     *
     * @param caller the method whose callees will be returned
     * @param includingVirtualMethod a boolean value indicating whether virtual methods
     *                               should be included in the results. Virtual wrapper
     *                               methods that link getBean calls to actual bean dummy
     *                               methods should only be considered in methods that are
     *                               before the structural analysis. For other cases, set
     *                               this to {@code false} to filter out connector virtual
     *                               methods from the results.
     * @return the list of callee method hashes for the given caller, or {@code null}
     *         if the caller has no callees
     * @see CallGraphUtility#connectingVirtualMethodHashForSpringBean
     */
    public List<String> getCalleeListOf(String caller, boolean includingVirtualMethod) {
        if (includingVirtualMethod) {
            return callerKeyCallGraphMap.get(caller);
        }
        List<String> callees = null;
        if (callerKeyCallGraphMap.get(caller) != null) {
            callees = new ArrayList<>();
            for (String callee : callerKeyCallGraphMap.get(caller)) {
                if (!callee.equals(CallGraphUtility.connectingVirtualMethodHashForSpringBean)) {
                    callees.add(callee);
                }
            }
        }
        return callees;
    }

    /**
     * Returns the list of caller methods for a given callee method.
     *
     * <p>This method retrieves all methods that call the specified callee method.
     * The result can optionally include or exclude virtual methods based on the client's needs.</p>
     * 
     * <p>Virtual methods are special wrapper methods that link method calls to actual
     * implementations. For creating dependency graphs, virtual methods are essential.
     * However, for other analysis purposes, clients may choose to filter them out.</p>
     * 
     * <p>When virtual methods are excluded, the method filters out the connecting
     * virtual method hash for Spring Bean calls, which is a special case in the
     * call graph analysis.</p>
     *
     * @param callee the method whose callers will be returned
     * @param includingVirtualMethod a boolean value indicating whether virtual methods
     *                               should be included in the results. Virtual wrapper
     *                               methods that link getBean calls to actual bean dummy
     *                               methods should only be considered in methods that are
     *                               before the structural analysis. For other cases, set
     *                               this to {@code false} to filter out connector virtual
     *                               methods from the results.
     * @return the list of caller method hashes for the given callee, or {@code null}
     *         if the callee has no callers
     * @see CallGraphUtility#connectingVirtualMethodHashForSpringBean
     */
    public List<String> getCallerListOf(String callee, boolean includingVirtualMethod) {
        if (includingVirtualMethod) {
            return calleeKeyCallGraphMap.get(callee);
        }
        List<String> callers = null;
        if (calleeKeyCallGraphMap.get(callee) != null) {
            callers = new ArrayList<>();
            for (String caller : calleeKeyCallGraphMap.get(callee)) {
                if (!caller.equals(CallGraphUtility.connectingVirtualMethodHashForSpringBean)) {
                    callers.add(caller);
                }
            }
        }
        return callers;
    }

    /**
     * Gets the number of edges in this call graph.
     *
     * @return the size of the graph
     */
    public int getSize() {
        return size;
    }

    public Map<String, List<String>> getCallerKeyCallGraphMap() {
        return callerKeyCallGraphMap;
    }

    public Map<String, List<String>> getCalleeKeyCallGraphMap() {
        return calleeKeyCallGraphMap;
    }

    /** 
     * Prints the callerKeyCallGraphMap
     */
    public void printCallGraph() {
        System.out.println(System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
                + "__________CallGraph__________" + System.lineSeparator());
        for (String caller : callerKeyCallGraphMap.keySet()) {
            System.out.println("caller: " + CallGraphDataStructures.getMethodSignatureFromHash(caller));
            for (String callee : callerKeyCallGraphMap.get(caller)) {
                System.out.println("\tcallee: " + CallGraphDataStructures.getMethodSignatureFromHash(callee));
            }
            System.out.println();
        }
        System.out.println();
    }
    

    /**
     * Adds a caller-callee relationship to the caller key call graph map.
     *
     * <p>This method maintains the forward relationship where each caller method
     * maps to a list of callee methods. It ensures that duplicate callees are
     * not added to the same caller's list.</p>
     * 
     * <p>The method uses thread-safe collections to support concurrent access
     * during call graph construction. If the caller doesn't exist in the map,
     * a new list is created to hold its callees.</p>
     *
     * @param caller the hash of the caller method
     * @param callee the hash of the callee method
     */
    private void addToCallerKeyCallGraphMap(String caller, String callee) {
        List<String> callees = null;
        if (callerKeyCallGraphMap.containsKey(caller)) {
            callees = callerKeyCallGraphMap.get(caller);
            if (!callees.contains(callee)) callees.add(callee);
        } else {
            callees = new CopyOnWriteArrayList<String>();
            if (!callees.contains(callee)) callees.add(callee);
        }
        callerKeyCallGraphMap.put(caller, callees);
    }

    /**
     * Adds a callee-caller relationship to the callee key call graph map.
     *
     * <p>This method maintains the reverse relationship where each callee method
     * maps to a list of caller methods. It ensures that duplicate callers are
     * not added to the same callee's list.</p>
     * 
     * <p>The method uses thread-safe collections to support concurrent access
     * during call graph construction. If the callee doesn't exist in the map,
     * a new list is created to hold its callers.</p>
     *
     * @param callee the hash of the callee method
     * @param caller the hash of the caller method
     */
    private void addToCalleeKeyCallGraphMap(String callee, String caller) {
        List<String> callers = null;
        if (calleeKeyCallGraphMap.containsKey(callee)) {
            callers = calleeKeyCallGraphMap.get(callee);
            if (!callers.contains(caller)) callers.add(caller);
        } else {
            callers = new CopyOnWriteArrayList<String>();
            if (!callers.contains(caller)) callers.add(caller);
        }
        calleeKeyCallGraphMap.put(callee, callers);
    }
}
