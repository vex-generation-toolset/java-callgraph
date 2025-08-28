/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended call graph that includes library functions/methods and uses qualified names.
 *
 * <p>The ExtendedCallGraph is a specialized version of the call graph that provides
 * additional functionality beyond the standard CallGraph class. Unlike the normal call
 * graph which focuses on source code analysis, this extended version includes library
 * functions and methods that may not be present in the analyzed source code.</p>
 * 
 * <p>Key characteristics of the ExtendedCallGraph:</p>
 * <ul>
 *   <li><strong>Library Inclusion:</strong> Incorporates library functions and methods
 *       that are referenced but not defined in the source code</li>
 *   <li><strong>Qualified Names:</strong> Uses fully qualified method names (e.g., 
 *       "com.example.Class.method") instead of method hashes for better readability</li>
 *   <li><strong>JSON Export:</strong> Primarily designed for serializing the call graph
 *       to JSON format for external analysis or visualization</li>
 *   <li><strong>Thread Safety:</strong> Uses ConcurrentHashMap for thread-safe operations
 *       during concurrent call graph construction</li>
 * </ul>
 * 
 * <p>This class is typically used in conjunction with the main CallGraph to provide
 * a comprehensive view of all method call relationships, including those that span
 * across library boundaries.</p>
 *
 * @see CallGraph
 * @see CallGraphDataStructures
 */
public class ExtendedCallGraph {

    /** 
     * The main data structure storing caller-to-callee relationships.
     * 
     * <p>This map maintains the core call graph structure where each key represents
     * a caller method (in qualified name format) and the corresponding value is a set
     * of callee methods that the caller can invoke.</p>
     * 
     * <p>The map uses:</p>
     * <ul>
     *   <li><strong>Keys:</strong> Fully qualified method names of caller methods
     *       (e.g., "com.example.MyClass.myMethod")</li>
     *   <li><strong>Values:</strong> Sets of fully qualified method names representing
     *       all possible callee methods that can be called by the caller</li>
     * </ul>
     * 
     * <p>This structure is thread-safe and supports concurrent access during call graph
     * construction, making it suitable for parallel processing scenarios.</p>
     */
    private final Map<String, Set<String>> callerToCalleeMap;

    public ExtendedCallGraph() {
        this.callerToCalleeMap = new ConcurrentHashMap<>();
    }

    public void addEdge(String caller, String callee) {
        this.callerToCalleeMap.computeIfAbsent(caller, k -> new HashSet<>()).add(callee);
    }

    public Map<String, Set<String>> getCallerToCalleeMap() {
        return callerToCalleeMap;
    }
}
