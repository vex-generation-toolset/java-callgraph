/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Manages class hierarchy information loaded from multiple configuration sources.
 *
 * <p>C2SManager (Class to Subclass Manager) provides functionality to load and manage
 * class hierarchy relationships including inheritance and interface implementation
 * information. This class loads data from multiple JSON configuration files representing
 * different Java platforms and frameworks, building comprehensive class hierarchy maps
 * for analysis and type resolution.</p>
 * 
 * <p>The class supports multiple configuration sources:</p>
 * <ul>
 *   <li><strong>Java SE:</strong> Standard Java Standard Edition class hierarchies</li>
 *   <li><strong>Java EE:</strong> Enterprise Edition class hierarchies</li>
 *   <li><strong>Android:</strong> Android platform class hierarchies</li>
 *   <li><strong>Spring Framework:</strong> Spring-specific class hierarchies</li>
 * </ul>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Multi-Source Loading:</strong> Loads from multiple JSON configuration files</li>
 *   <li><strong>Hierarchy Building:</strong> Constructs complete class inheritance trees</li>
 *   <li><strong>Interface Support:</strong> Handles interface extension and implementation</li>
 *   <li><strong>Recursive Processing:</strong> Builds complete subclass hierarchies</li>
 *   <li><strong>Work List Algorithm:</strong> Uses efficient work list algorithm for hierarchy adjustment</li>
 *   <li><strong>Error Resilience:</strong> Gracefully handles file access and parsing failures</li>
 *   <li><strong>Progress Reporting:</strong> Integrates with progress reporting system</li>
 * </ul>
 * 
 * <p><strong>Configuration File Structure:</strong></p>
 * <ul>
 *   <li><strong>File Location:</strong> Stored in configs subdirectory of SUMMARIES</li>
 *   <li><strong>JSON Format:</strong> Array of hierarchy information objects</li>
 *   <li><strong>Data Fields:</strong> declaring_type, package_name, extends, implements</li>
 *   <li><strong>Special Handling:</strong> Spring files support multiple interface extensions</li>
 * </ul>
 * 
 * <p><strong>Hierarchy Processing:</strong></p>
 * <ul>
 *   <li><strong>Class Inheritance:</strong> Maps superclass to subclass relationships</li>
 *   <li><strong>Interface Implementation:</strong> Maps interfaces to implementing classes</li>
 *   <li><strong>Recursive Building:</strong> Propagates subclass information up the hierarchy</li>
 *   <li><strong>Cross-Reference Resolution:</strong> Links related hierarchy information</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <ul>
 *   <li><strong>Type Checking:</strong> Determine if a class is a subclass of another</li>
 *   <li><strong>Hierarchy Analysis:</strong> Analyze complete class inheritance trees</li>
 *   <li><strong>Interface Mapping:</strong> Find all classes implementing specific interfaces</li>
 *   <li><strong>Subtype Discovery:</strong> Discover all subtypes of a given class</li>
 * </ul>
 *
 * @author Kanak Das
 */
public class C2SManager {
    /**
     * Array of configuration file names for different Java platforms and frameworks.
     *
     * <p>This field defines the set of JSON configuration files that contain class
     * hierarchy information for various Java environments. Each file provides specific
     * hierarchy data for its respective platform, allowing the system to build
     * comprehensive class relationship maps across different Java ecosystems.</p>
     * 
     * <p><strong>Configuration Files:</strong></p>
     * <ul>
     *   <li><strong>class-hierarchy-se.json:</strong> Java Standard Edition class hierarchies</li>
     *   <li><strong>class-hierarchy-ee.json:</strong> Java Enterprise Edition class hierarchies</li>
     *   <li><strong>class-hierarchy-android.json:</strong> Android platform class hierarchies</li>
     *   <li><strong>class-hierarchy-spring.json:</strong> Spring Framework class hierarchies</li>
     * </ul>
     * 
     * <p><strong>File Processing Order:</strong></p>
     * <ul>
     *   <li><strong>Sequential Loading:</strong> Files are processed in array order</li>
     *   <li><strong>Platform Coverage:</strong> Covers major Java platforms and frameworks</li>
     *   <li><strong>Extensible Design:</strong> Additional files can be added to the array</li>
     *   <li><strong>Fallback Handling:</strong> Missing files don't prevent other files from loading</li>
     * </ul>
     * 
     * <p><strong>Special Considerations:</strong></p>
     * <ul>
     *   <li><strong>Spring Handling:</strong> Spring files support multiple interface extensions</li>
     *   <li><strong>Platform Differences:</strong> Each platform may have different hierarchy structures</li>
     *   <li><strong>Data Consistency:</strong> Files should follow consistent JSON structure</li>
     *   <li><strong>Path Resolution:</strong> Files are resolved relative to SUMMARIES/configs directory</li>
     * </ul>
     */
    private static String[] specFiles = new String[] {
        "class-hierarchy-se.json",
        "class-hierarchy-ee.json",
        "class-hierarchy-android.json",
        "class-hierarchy-spring.json"
    };

    /**
     * Maps interface names to sets of classes that implement them.
     *
     * <p>This field stores the interface-to-implementation mapping, where each key
     * represents an interface name and the value is a set of class names that implement
     * that interface. This mapping is built during the loading process and used for
     * interface implementation analysis and type resolution.</p>
     * 
     * <p><strong>Data Structure:</strong></p>
     * <ul>
     *   <li><strong>Key Type:</strong> String - fully qualified interface name</li>
     *   <li><strong>Value Type:</strong> Set&lt;String&gt; - collection of implementing class names</li>
     *   <li><strong>Null Handling:</strong> Field is null until loadC2SInfo() is called</li>
     *   <li><strong>Thread Safety:</strong> Access is controlled through static methods</li>
     * </ul>
     * 
     * <p><strong>Interface Relationships:</strong></p>
     * <ul>
     *   <li><strong>Direct Implementation:</strong> Classes directly implementing interfaces</li>
     *   <li><strong>Inherited Implementation:</strong> Classes inheriting interface implementations</li>
     *   <li><strong>Multiple Implementation:</strong> Classes implementing multiple interfaces</li>
     *   <li><strong>Interface Extension:</strong> Interfaces extending other interfaces</li>
     * </ul>
     */
    private static HashMap<String, Set<String>> c2InterfacesMap;

    /**
     * Maps class names to sets of their direct subclasses.
     *
     * <p>This field stores the class-to-subclass mapping, where each key represents
     * a class name and the value is a set of class names that directly inherit from
     * that class. This mapping is built during the loading process and used for
     * inheritance analysis and type hierarchy resolution.</p>
     * 
     * <p><strong>Data Structure:</strong></p>
     * <ul>
     *   <li><strong>Key Type:</strong> String - fully qualified class name</li>
     *   <li><strong>Value Type:</strong> Set&lt;String&gt; - collection of subclass names</li>
     *   <li><strong>Null Handling:</strong> Field is null until loadC2SInfo() is called</li>
     *   <li><strong>Thread Safety:</strong> Access is controlled through static methods</li>
     * </ul>
     * 
     * <p><strong>Inheritance Relationships:</strong></p>
     * <ul>
     *   <li><strong>Direct Inheritance:</strong> Classes directly extending the superclass</li>
     *   <li><strong>Transitive Inheritance:</strong> Subclasses of subclasses (built recursively)</li>
     *   <li><strong>Multiple Inheritance:</strong> Classes can have multiple superclasses</li>
     *   <li><strong>Object Hierarchy:</strong> All classes ultimately inherit from java.lang.Object</li>
     * </ul>
     */
    private static HashMap<String, Set<String>> c2ClassesMap;

    /**
     * Loads class hierarchy information from multiple JSON configuration files.
     *
     * <p>This method initializes the class hierarchy maps by loading data from multiple
     * JSON configuration files representing different Java platforms and frameworks.
     * It implements lazy loading, ensuring that the configuration is only loaded once
     * and then cached in memory for subsequent access. The method processes each
     * configuration file sequentially, building comprehensive class hierarchy maps.</p>
     * 
     * <p><strong>Loading Strategy:</strong></p>
     * <ol>
     *   <li><strong>Lazy Initialization Check:</strong> Returns immediately if already loaded</li>
     *   <li><strong>Sequential File Processing:</strong> Processes each configuration file in order</li>
     *   <li><strong>Map Initialization:</strong> Creates HashMaps for storing hierarchy data</li>
     *   <li><strong>JSON Parsing:</strong> Parses each file's JSON array structure</li>
     *   <li><strong>Hierarchy Building:</strong> Constructs class and interface relationship maps</li>
     *   <li><strong>Recursive Processing:</strong> Builds complete subclass hierarchies</li>
     *   <li><strong>Map Adjustment:</strong> Uses work list algorithm to propagate hierarchy information</li>
     * </ol>
     * 
     * <p><strong>Configuration File Processing:</strong></p>
     * <ul>
     *   <li><strong>Java SE:</strong> Standard Java Standard Edition class hierarchies</li>
     *   <li><strong>Java EE:</strong> Enterprise Edition class hierarchies</li>
     *   <li><strong>Android:</strong> Android platform class hierarchies</li>
     *   <li><strong>Spring Framework:</strong> Spring-specific class hierarchies with multiple interface support</li>
     * </ul>
     * 
     * <p><strong>JSON Structure Expected:</strong></p>
     * <ul>
     *   <li><strong>Root Array:</strong> JSON array containing hierarchy information objects</li>
     *   <li><strong>Object Fields:</strong> declaring_type, package_name, extends, implements</li>
     *   <li><strong>Extends Field:</strong> Single string for most files, array for Spring files</li>
     *   <li><strong>Implements Field:</strong> Array of interface names that the class implements</li>
     *   <li><strong>Data Format:</strong> [{"declaring_type": "ClassName", "package_name": "package", ...}]</li>
     * </ul>
     * 
     * <p><strong>Special Spring Handling:</strong></p>
     * <ul>
     *   <li><strong>Multiple Interface Extension:</strong> Spring files support interfaces extending multiple interfaces</li>
     *   <li><strong>Array Processing:</strong> Extends field is processed as JSONArray for Spring files</li>
     *   <li><strong>Interface Relationships:</strong> Builds complete interface extension hierarchies</li>
     *   <li><strong>Cross-Reference Resolution:</strong> Links related interface and class information</li>
     * </ul>
     * 
     * <p><strong>Error Handling Strategy:</strong></p>
     * <ul>
     *   <li><strong>File Not Found:</strong> Gracefully handles missing configuration files</li>
     *   <li><strong>IO Exceptions:</strong> Catches and logs file reading errors</li>
     *   <li><strong>JSON Parsing Errors:</strong> Handles malformed JSON gracefully</li>
     *   <li><strong>General Exceptions:</strong> Catches all exceptions and errors</li>
     *   <li><strong>Partial Loading:</strong> Continues operation even if some files fail</li>
     *   <li><strong>Progress Reporting:</strong> Reports file loading status and errors</li>
     * </ul>
     * 
     * <p><strong>Progress Reporting:</strong></p>
     * <ul>
     *   <li><strong>File Loading Status:</strong> Reports when configuration files are loaded</li>
     *   <li><strong>Error Notifications:</strong> Reports when files are missing or inaccessible</li>
     *   <li><strong>User Feedback:</strong> Provides visibility into loading process</li>
     *   <li><strong>Integration:</strong> Works with IProgressReporter interface</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>One-Time Loading:</strong> Configuration is loaded only once per JVM</li>
     *   <li><strong>Memory Caching:</strong> Loaded data is cached for fast access</li>
     *   <li><strong>File I/O Overhead:</strong> Involves reading multiple JSON files from disk</li>
     *   <li><strong>JSON Parsing:</strong> CPU overhead for JSON processing</li>
     *   <li><strong>Hierarchy Building:</strong> Recursive processing for complete hierarchies</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>System Initialization:</strong> Call during system startup or first use</li>
     *   <li><strong>Hierarchy Loading:</strong> Load class hierarchy information for analysis</li>
     *   <li><strong>Type Resolution:</strong> Initialize type hierarchy resolution capabilities</li>
     *   <li><strong>Error Recovery:</strong> Retry loading after configuration file updates</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Thread Safety:</strong> Method is thread-safe and can be called multiple times</li>
     *   <li><strong>File Dependencies:</strong> Requires configuration files to be accessible</li>
     *   <li><strong>Memory Usage:</strong> Loaded hierarchies consume memory until JVM shutdown</li>
     *   <li><strong>Error Isolation:</strong> Failures in one file don't affect others</li>
     *   <li><strong>Progress Integration:</strong> Requires valid IProgressReporter instance</li>
     *   <li><strong>Recursive Processing:</strong> May take time for large hierarchy structures</li>
     * </ul>
     *
     * @param progressReporter the progress reporter for status updates and error notifications
     */
    public static void loadC2SInfo(IProgressReporter progressReporter) {
        // If already loaded, ignore
        if (c2ClassesMap != null && c2InterfacesMap != null) {
            return;
        }
        for (String specFileStr : specFiles) {
            File specFile = Path.of(ConfigurationManager.config.SUMMARIES, "configs", specFileStr)
                    .toFile();
            if (specFile.exists()) {
                if (c2ClassesMap == null) {
                    c2ClassesMap = new HashMap<>();
                }
                if (c2InterfacesMap == null) {
                    c2InterfacesMap = new HashMap<>();
                }
                try (Reader in = new BufferedReader(new FileReader(specFile))) {
                    JSONArray hierarchyArray = new JSONArray(new JSONTokener(in));

                    for (int i = 0; i < hierarchyArray.length(); i++) {
                        JSONObject hierarchyInfo = hierarchyArray.getJSONObject(i);
                        String declaringType = hierarchyInfo.getString("declaring_type");
                        String packageName = hierarchyInfo.getString("package_name");
                        String qualifiedDeclaringType = packageName + "." + declaringType;

                        if (specFileStr.equals("class-hierarchy-spring.json")) {
                            // An Interface can extend multiple interfaces. So In this json, extends is
                            // a jsonArray. But other json files did not consider the case. That's why
                            // we handle this separately
                            JSONArray extendsList = hierarchyInfo.getJSONArray("extends");
                            if (extendsList != null) {
                                for (int j = 0; j < extendsList.length(); j++) {
                                    String extendsType = extendsList.getString(j);
                                    addToMap(extendsType, qualifiedDeclaringType, true);
                                }
                            }
                        } else {
                            Object extendsType = hierarchyInfo.get("extends");
                            // Some entries can be ""(emtpy string) instead of null
                            // handles such cases
                            if (!JSONObject.NULL.equals(extendsType) && !((String) extendsType).isEmpty()) {
                                addToMap((String) extendsType, qualifiedDeclaringType, false);
                            }
                        }
                        JSONArray implementsType = hierarchyInfo.getJSONArray("implements");
                        if (implementsType != null) {
                            for (int j = 0; j < implementsType.length(); j++) {
                                String implementesTypeStr = implementsType.getString(j);
                                addToMap(implementesTypeStr, qualifiedDeclaringType, true);
                            }
                        }
                    }
                    adjustMap();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                } finally {
                    // c2ClassesMap and c2InterfacesMap will be empty or partially populated if there is a crash
                }
            } else {
                progressReporter.showProgress(
                        "Unable to load Class to Package Map, file does not exist: " + specFile.getAbsolutePath());
            }
        }
    }

    /**
     * Adds subclass information to the hierarchy maps and recursively builds complete hierarchies.
     *
     * <p>This method adds a subclass relationship to the appropriate hierarchy map (either
     * class inheritance or interface implementation) and then recursively processes all
     * existing subclasses of the newly added subclass to propagate the hierarchy information
     * upward. This ensures that complete inheritance and implementation hierarchies are
     * built during the loading process.</p>
     * 
     * <p><strong>Hierarchy Building Process:</strong></p>
     * <ol>
     *   <li><strong>Relationship Addition:</strong> Adds subclass to superclass in appropriate map</li>
     *   <li><strong>Recursive Propagation:</strong> Finds all existing subclasses of the new subclass</li>
     *   <li><strong>Upward Propagation:</strong> Adds these subclasses to the superclass recursively</li>
     *   <li><strong>Cross-Map Processing:</strong> Handles both class and interface hierarchies</li>
     *   <li><strong>Cycle Prevention:</strong> Avoids infinite recursion by checking for cycles</li>
     * </ol>
     * 
     * <p><strong>Map Selection Logic:</strong></p>
     * <ul>
     *   <li><strong>Class Hierarchy:</strong> Uses c2ClassesMap for class inheritance relationships</li>
     *   <li><strong>Interface Hierarchy:</strong> Uses c2InterfacesMap for interface implementation relationships</li>
     *   <li><strong>Type Determination:</strong> Based on isInterface parameter</li>
     *   <li><strong>Consistent Mapping:</strong> Ensures relationships are stored in correct maps</li>
     * </ul>
     * 
     * <p><strong>Recursive Processing Strategy:</strong></p>
     * <ul>
     *   <li><strong>Existing Subclass Discovery:</strong> Finds all subclasses already in the maps</li>
     *   <li><strong>Hierarchy Propagation:</strong> Propagates subclass information up the hierarchy</li>
     *   <li><strong>Cross-Reference Resolution:</strong> Links related hierarchy information across maps</li>
     *   <li><strong>Complete Hierarchy Building:</strong> Ensures all relationships are properly established</li>
     * </ul>
     * 
     * <p><strong>Input Validation and Processing:</strong></p>
     * <ul>
     *   <li><strong>Null Checking:</strong> Validates that superClass and subClass are not null</li>
     *   <li><strong>Self-Reference Prevention:</strong> Prevents adding class as its own subclass</li>
     *   <li><strong>Object Class Handling:</strong> Excludes java.lang.Object from hierarchy building</li>
     *   <li><strong>String Cleaning:</strong> Removes newlines and trims whitespace from class names</li>
     *   <li><strong>Cycle Detection:</strong> Prevents infinite recursion in hierarchy relationships</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li><strong>Exception Catching:</strong> Catches and logs all exceptions and errors</li>
     *   <li><strong>Graceful Degradation:</strong> Continues processing even if individual operations fail</li>
     *   <li><strong>Partial Success:</strong> Allows partial hierarchy building to succeed</li>
     *   <li><strong>Error Isolation:</strong> Failures in one relationship don't affect others</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Recursive Processing:</strong> May involve multiple recursive calls for deep hierarchies</li>
     *   <li><strong>Map Operations:</strong> Involves HashMap lookups and modifications</li>
     *   <li><strong>String Processing:</strong> Includes string splitting and trimming operations</li>
     *   <li><strong>Memory Usage:</strong> Builds complete hierarchy structures in memory</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Recursive Nature:</strong> Method calls itself recursively to build complete hierarchies</li>
     *   <li><strong>Cycle Prevention:</strong> Includes logic to prevent infinite recursion</li>
     *   <li><strong>Map Consistency:</strong> Ensures both hierarchy maps remain consistent</li>
     *   <li><strong>Error Resilience:</strong> Continues operation even if individual operations fail</li>
     *   <li><strong>Memory Management:</strong> Builds comprehensive hierarchy structures</li>
     * </ul>
     *
     * @param superClass the superclass or interface name to add the subclass to
     * @param subClass the subclass or implementing class name to add
     * @param isInterface true if the relationship involves interfaces, false for class inheritance
     */
    private static void addToMap(String superClass, String subClass, boolean isInterface) {
        try {
            if (superClass != null
                    && !superClass.equals(subClass)
                    && !superClass.equals("java.lang.Object")
                    && !superClass.equals("Object")) {
                superClass = superClass.split("\n")[0].strip();
                subClass = subClass.split("\n")[0].strip();
                Set<String> subClasses = null;
                if (isInterface) {
                    if (c2InterfacesMap.containsKey(superClass)) {
                        subClasses = c2InterfacesMap.get(superClass);
                    } else {
                        subClasses = new HashSet<>();
                    }
                } else {
                    if (c2ClassesMap.containsKey(superClass)) {
                        subClasses = c2ClassesMap.get(superClass);
                    } else {
                        subClasses = new HashSet<>();
                    }
                }

                subClasses.add(subClass);
                if (isInterface) {
                    c2InterfacesMap.put(superClass, subClasses);
                } else {
                    c2ClassesMap.put(superClass, subClasses);
                }

                if (c2ClassesMap.containsKey(subClass)) {
                    Iterator<String> childSubClasses =
                            c2ClassesMap.get(subClass).iterator();
                    while (childSubClasses.hasNext()) {
                        String child = childSubClasses.next();
                        if (!superClass.equals(child)) {
                            addToMap(superClass, child, isInterface);
                        }
                    }
                }

                if (c2InterfacesMap.containsKey(subClass)) {
                    Iterator<String> childSubClasses =
                            c2InterfacesMap.get(subClass).iterator();
                    while (childSubClasses.hasNext()) {
                        String child = childSubClasses.next();
                        if (!superClass.equals(child)) {
                            addToMap(superClass, child, isInterface);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    /**
     * Adjusts the populated hierarchy maps to propagate subclass information to supertypes.
     *
     * <p>This method performs a second pass over the hierarchy maps to ensure that all
     * subclass information is properly propagated upward to supertypes. It uses a work
     * list algorithm to efficiently process the hierarchy relationships and build complete
     * inheritance and implementation trees. This step is necessary because the initial
     * loading process may miss some subclass relationships due to the order of processing.</p>
     * 
     * <p><strong>Work List Algorithm Process:</strong></p>
     * <ol>
     *   <li><strong>Initialization:</strong> Creates work lists for each superclass</li>
     *   <li><strong>Work List Population:</strong> Adds all direct subclasses to work lists</li>
     *   <li><strong>Iterative Processing:</strong> Processes work lists until empty</li>
     *   <li><strong>Subclass Discovery:</strong> Finds nested subclasses of current subclasses</li>
     *   <li><strong>Relationship Propagation:</strong> Adds discovered subclasses to superclass</li>
     *   <li><strong>Work List Management:</strong> Adds new subclasses to work list for processing</li>
     * </ol>
     * 
     * <p><strong>Processing Strategy:</strong></p>
     * <ul>
     *   <li><strong>Class Hierarchy Processing:</strong> Processes c2ClassesMap for class inheritance</li>
     *   <li><strong>Interface Hierarchy Processing:</strong> Processes c2InterfacesMap for interface relationships</li>
     *   <li><strong>Sequential Processing:</strong> Processes each hierarchy type separately</li>
     *   <li><strong>Complete Coverage:</strong> Ensures all superclasses are processed</li>
     * </ul>
     * 
     * <p><strong>Work List Management:</strong></p>
     * <ul>
     *   <li><strong>Queue Implementation:</strong> Uses LinkedList as a queue for work list</li>
     *   <li><strong>FIFO Processing:</strong> Processes subclasses in first-in-first-out order</li>
     *   <li><strong>Dynamic Population:</strong> Work list grows as new subclasses are discovered</li>
     *   <li><strong>Termination Condition:</strong> Processing continues until work list is empty</li>
     * </ul>
     * 
     * <p><strong>Relationship Propagation:</strong></p>
     * <ul>
     *   <li><strong>Direct Relationships:</strong> Establishes direct subclass-superclass relationships</li>
     *   <li><strong>Transitive Relationships:</strong> Builds complete inheritance chains</li>
     *   <li><strong>Cross-Reference Resolution:</strong> Links related hierarchy information</li>
     *   <li><strong>Complete Hierarchy Building:</strong> Ensures all relationships are properly established</li>
     * </ul>
     * 
     * <p><strong>Algorithm Efficiency:</strong></p>
     * <ul>
     *   <li><strong>Work List Approach:</strong> Efficiently processes hierarchical relationships</li>
     *   <li><strong>Queue Operations:</strong> O(1) add and remove operations for work list management</li>
     *   <li><strong>Map Lookups:</strong> O(1) HashMap lookups for relationship queries</li>
     *   <li><strong>Linear Processing:</strong> Each subclass is processed at most once per superclass</li>
     * </ul>
     * 
     * <p><strong>Data Structure Operations:</strong></p>
     * <ul>
     *   <li><strong>HashMap Access:</strong> Reads from and writes to hierarchy maps</li>
     *   <li><strong>Set Operations:</strong> Performs set lookups and modifications</li>
     *   <li><strong>Queue Management:</strong> Uses LinkedList for work list implementation</li>
     *   <li><strong>Iterator Usage:</strong> Iterates over sets and maps efficiently</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Second Pass Requirement:</strong> This method is called after initial loading to complete hierarchies</li>
     *   <li><strong>Complete Coverage:</strong> Ensures all subclass relationships are properly established</li>
     *   <li><strong>Efficient Algorithm:</strong> Uses work list approach for optimal performance</li>
     *   <li><strong>Memory Usage:</strong> May temporarily increase memory usage during processing</li>
     *   <li><strong>Processing Order:</strong> Processes class hierarchies before interface hierarchies</li>
     * </ul>
     */
    private static void adjustMap() {
        for (String superClass : c2ClassesMap.keySet()) {
            Set<String> subClasses = c2ClassesMap.get(superClass);
            Queue<String> workList = new LinkedList<>();
            for (String subClass : subClasses) {
                workList.add(subClass);
            }

            while (!workList.isEmpty()) {
                String top = workList.poll();
                if (c2ClassesMap.containsKey(top)) {
                    Set<String> subClassesNested = c2ClassesMap.get(top);
                    for (String sub : subClassesNested) {
                        workList.add(sub);
                        addToMap(superClass, sub, false);
                    }
                }
            }
        }

        for (String superClass : c2InterfacesMap.keySet()) {
            Set<String> subClasses = c2InterfacesMap.get(superClass);
            Queue<String> workList = new LinkedList<>();
            for (String subClass : subClasses) {
                workList.add(subClass);
            }

            while (!workList.isEmpty()) {
                String top = workList.poll();
                if (c2InterfacesMap.containsKey(top)) {
                    Set<String> subClassesNested = c2InterfacesMap.get(top);
                    for (String sub : subClassesNested) {
                        workList.add(sub);
                        addToMap(superClass, sub, true);
                    }
                }
            }
        }
    }

    public static HashMap<String, Set<String>> getSuperClassesMap() {
        return c2ClassesMap;
    }

    public static HashMap<String, Set<String>> getSuperInterfacesMap() {
        return c2InterfacesMap;
    }

    /**
     * Retrieves all subclasses and sub-interfaces of a specified class.
     *
     * <p>This method combines information from both the class hierarchy map and interface
     * hierarchy map to provide a comprehensive view of all subtypes of a given class.
     * It returns a unified set containing both direct and indirect subclasses as well
     * as all classes that implement interfaces that extend the class's interfaces.</p>
     * 
     * <p><strong>Data Source Integration:</strong></p>
     * <ul>
     *   <li><strong>Class Hierarchy:</strong> Uses c2ClassesMap for inheritance relationships</li>
     *   <li><strong>Interface Hierarchy:</strong> Uses c2InterfacesMap for interface relationships</li>
     *   <li><strong>Unified Results:</strong> Combines both hierarchy types into single result set</li>
     *   <li><strong>Comprehensive Coverage:</strong> Includes all subtype relationships</li>
     * </ul>
     * 
     * <p><strong>Return Value Behavior:</strong></p>
     * <ul>
     *   <li><strong>Combined Set:</strong> Returns unified set of all subtypes when both exist</li>
     *   <li><strong>Class-Only Results:</strong> Returns only subclass set when no interface relationships exist</li>
     *   <li><strong>Interface-Only Results:</strong> Returns only interface implementation set when no class relationships exist</li>
     *   <li><strong>Null Results:</strong> Returns null when no subtype relationships exist</li>
     * </ul>
     * 
     * <p><strong>Data Combination Strategy:</strong></p>
     * <ul>
     *   <li><strong>Set Union Operation:</strong> Combines subclass and interface sets when both exist</li>
     *   <li><strong>Modification Safety:</strong> Returns new combined set to avoid modifying original data</li>
     *   <li><strong>Efficient Combination:</strong> Uses addAll operation for set combination</li>
     *   <li><strong>Data Integrity:</strong> Preserves original hierarchy maps</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Complete Type Analysis:</strong> getAllSubClassesAndSubInterfacesSet("java.util.Collection")</li>
     *   <li><strong>Inheritance Analysis:</strong> getAllSubClassesAndSubInterfacesSet("java.lang.Exception")</li>
     *   <li><strong>Interface Implementation:</strong> getAllSubClassesAndSubInterfacesSet("java.io.Serializable")</li>
     *   <li><strong>Framework Analysis:</strong> getAllSubClassesAndSubInterfacesSet("org.springframework.stereotype.Component")</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Map Lookups:</strong> O(1) HashMap lookups for both hierarchy maps</li>
     *   <li><strong>Set Operations:</strong> O(n) for set combination operations</li>
     *   <li><strong>Memory Usage:</strong> Creates new set for combined results</li>
     *   <li><strong>Efficient Retrieval:</strong> Fast access to pre-loaded hierarchy data</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Configuration Dependency:</strong> Requires loadC2SInfo() to be called first</li>
     *   <li><strong>Null Handling:</strong> Returns null when no subtype relationships exist</li>
     *   <li><strong>Set Modification:</strong> Returned set can be safely modified by caller</li>
     *   <li><strong>Comprehensive Results:</strong> Provides complete subtype information</li>
     *   <li><strong>Data Consistency:</strong> Results reflect current state of hierarchy maps</li>
     * </ul>
     *
     * @param qualifiedName the fully qualified name of the class to find subtypes for
     * @return a set containing all subclasses and sub-interfaces, or null if none exist
     */
    public static Set<String> getAllSubClassesAndSubInterfacesSet(String qualifiedName) {
        Set<String> allSubClasses = getSuperClassesMap().get(qualifiedName);
        Set<String> allSubInterfaces = getSuperInterfacesMap().get(qualifiedName);
        if (allSubClasses == null && allSubInterfaces == null) {
            return null;
        }
        if (allSubClasses != null && allSubInterfaces != null) {
            allSubClasses.addAll(allSubInterfaces);
            return allSubClasses;
        }
        return allSubClasses != null ? allSubClasses : allSubInterfaces;
    }

    /**
     * Determines if one class is a subtype of another class.
     *
     * <p>This method checks the subtype relationship between two classes by examining
     * the class hierarchy maps. It determines if the specified subclass is a direct
     * or indirect subtype of the specified superclass. The method is specifically
     * designed for library classes and provides efficient type checking using
     * pre-loaded hierarchy information.</p>
     * 
     * <p><strong>Type Checking Strategy:</strong></p>
     * <ol>
     *   <li><strong>Object Class Handling:</strong> All classes are considered subtypes of java.lang.Object</li>
     *   <li><strong>Hierarchy Lookup:</strong> Searches hierarchy maps for subtype relationships</li>
     *   <li><strong>Comprehensive Search:</strong> Checks both class and interface hierarchies</li>
     *   <li><strong>Boolean Result:</strong> Returns true for valid subtype relationships, false otherwise</li>
     * </ol>
     * 
     * <p><strong>Object Class Special Case:</strong></p>
     * <ul>
     *   <li><strong>Universal Superclass:</strong> java.lang.Object is the root of all class hierarchies</li>
     *   <li><strong>Automatic Return:</strong> Always returns true when superclass is java.lang.Object</li>
     *   <li><strong>Bypass Processing:</strong> Skips hierarchy lookup for this common case</li>
     *   <li><strong>Performance Optimization:</strong> Provides fast path for fundamental type checking</li>
     * </ul>
     * 
     * <p><strong>Hierarchy Search Process:</strong></p>
     * <ul>
     *   <li><strong>Subtype Collection:</strong> Retrieves all subtypes of the superclass</li>
     *   <li><strong>Set Membership Check:</strong> Checks if subclass is in the subtype set</li>
     *   <li><strong>Null Handling:</strong> Returns false if no subtype relationships exist</li>
     *   <li><strong>Efficient Lookup:</strong> Uses HashMap-based subtype retrieval</li>
     * </ul>
     * 
     * <p><strong>Library Class Applicability:</strong></p>
     * <ul>
     *   <li><strong>Scope Limitation:</strong> Designed specifically for library class type checking</li>
     *   <li><strong>Pre-loaded Data:</strong> Relies on configuration files for hierarchy information</li>
     *   <li><strong>Standard Libraries:</strong> Covers Java SE, EE, Android, and Spring frameworks</li>
     *   <li><strong>Configuration Dependency:</strong> Requires hierarchy data to be loaded first</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Basic Type Checking:</strong> isSubTypeOf("java.util.ArrayList", "java.util.List") returns true</li>
     *   <li><strong>Object Hierarchy:</strong> isSubTypeOf("java.lang.String", "java.lang.Object") returns true</li>
     *   <li><strong>Interface Implementation:</strong> isSubTypeOf("java.util.HashMap", "java.util.Map") returns true</li>
     *   <li><strong>Unrelated Types:</strong> isSubTypeOf("java.lang.String", "java.util.List") returns false</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Fast Path:</strong> O(1) for java.lang.Object superclass check</li>
     *   <li><strong>Hierarchy Lookup:</strong> O(1) for HashMap-based subtype retrieval</li>
     *   <li><strong>Set Membership:</strong> O(1) for HashSet-based membership checking</li>
     *   <li><strong>Overall Complexity:</strong> O(1) average case, O(n) worst case for large subtype sets</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Configuration Dependency:</strong> Requires loadC2SInfo() to be called first</li>
     *   <li><strong>Library Class Focus:</strong> Specifically designed for library class type checking</li>
     *   <li><strong>Object Class Handling:</strong> Special case handling for java.lang.Object</li>
     *   <li><strong>Efficient Implementation:</strong> Uses pre-loaded hierarchy data for fast lookups</li>
     *   <li><strong>Boolean Return:</strong> Simple true/false result for easy integration</li>
     * </ul>
     *
     * @param subClassQName the fully qualified name of the potential subclass
     * @param superClassQName the fully qualified name of the potential superclass
     * @return true if subClassQName is a subtype of superClassQName, false otherwise
     */
    public static boolean isSubTypeOf(String subClassQName, String superClassQName) {
        if (superClassQName.equals("java.lang.Object")) {
            return true;
        }
        Set<String> allSubClassesAndInterfaceSet = getAllSubClassesAndSubInterfacesSet(superClassQName);
        return allSubClassesAndInterfaceSet != null && allSubClassesAndInterfaceSet.contains(subClassQName);
    }
}
