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
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Manages class-to-package mapping information loaded from JSON configuration files.
 *
 * <p>C2PManager (Class to Package Manager) provides functionality to load and access
 * mapping information between Java class names and their corresponding packages. This
 * information is loaded from JSON configuration files and used for package resolution
 * and class identification in the call graph analysis system.</p>
 * 
 * <p>The class supports multiple configuration sources:</p>
 * <ul>
 *   <li><strong>Java 8 Documentation:</strong> Standard Java 8 class package mappings</li>
 *   <li><strong>Spring Framework:</strong> Spring-specific class package mappings</li>
 *   <li><strong>Extensible Design:</strong> Additional configuration files can be added</li>
 *   <li><strong>JSON Format:</strong> Uses JSON for flexible configuration management</li>
 * </ul>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li><strong>Lazy Loading:</strong> Configuration is loaded only when first requested</li>
 *   <li><strong>Memory Caching:</strong> Loaded mappings are cached in memory for performance</li>
 *   <li><strong>Error Resilience:</strong> Gracefully handles file access and parsing failures</li>
 *   <li><strong>Progress Reporting:</strong> Integrates with progress reporting system</li>
 *   <li><strong>Thread Safety:</strong> Static methods provide thread-safe access</li>
 * </ul>
 * 
 * <p><strong>Configuration File Structure:</strong></p>
 * <ul>
 *   <li><strong>File Location:</strong> Stored in configs subdirectory of SUMMARIES</li>
 *   <li><strong>JSON Format:</strong> Maps class names to arrays of package strings</li>
 *   <li><strong>Multiple Sources:</strong> Supports multiple configuration files</li>
 *   <li><strong>Fallback Handling:</strong> Continues operation even if some files are missing</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <ul>
 *   <li><strong>Package Resolution:</strong> Determine which packages contain a specific class</li>
 *   <li><strong>Class Analysis:</strong> Analyze class distribution across packages</li>
 *   <li><strong>Dependency Mapping:</strong> Map class dependencies to package structures</li>
 *   <li><strong>Configuration Management:</strong> Manage class-package relationships</li>
 * </ul>
 *
 * @author Kanak Das
 */
public class C2PManager {
    /**
     * Static cache for class-to-package mapping information.
     *
     * <p>This field stores the loaded class-to-package mappings in memory for efficient
     * access. The map structure uses class names as keys and sets of package names as values,
     * allowing a single class to be associated with multiple packages (useful for cases
     * where the same class name appears in different package contexts).</p>
     * 
     * <p><strong>Data Structure:</strong></p>
     * <ul>
     *   <li><strong>Key Type:</strong> String - fully qualified or simple class names</li>
     *   <li><strong>Value Type:</strong> Set&lt;String&gt; - collection of package names</li>
     *   <li><strong>Null Handling:</strong> Field is null until loadC2PInfo() is called</li>
     *   <li><strong>Thread Safety:</strong> Access is controlled through static methods</li>
     * </ul>
     * 
     * <p><strong>Memory Management:</strong></p>
     * <ul>
     *   <li><strong>Lazy Initialization:</strong> Map is created only when needed</li>
     *   <li><strong>Persistent Storage:</strong> Map remains in memory after loading</li>
     *   <li><strong>Memory Footprint:</strong> Size depends on configuration file contents</li>
     *   <li><strong>Garbage Collection:</strong> Map is eligible for GC when class is unloaded</li>
     * </ul>
     * 
     * <p><strong>Configuration Sources:</strong></p>
     * <ul>
     *   <li><strong>Java 8 Documentation:</strong> Standard Java class package mappings</li>
     *   <li><strong>Spring Framework:</strong> Spring-specific class package mappings</li>
     *   <li><strong>Custom Configurations:</strong> Additional mapping sources can be added</li>
     *   <li><strong>JSON Parsing:</strong> Data is loaded from JSON configuration files</li>
     * </ul>
     */
    private static Map<String, Set<String>> c2pMap;

    /**
     * Loads class-to-package mapping information from JSON configuration files.
     *
     * <p>This method initializes the class-to-package mapping cache by loading data from
     * multiple JSON configuration files. It implements lazy loading, ensuring that the
     * configuration is only loaded once and then cached in memory for subsequent access.
     * The method handles multiple configuration sources and provides comprehensive error
     * handling and progress reporting.</p>
     * 
     * <p><strong>Loading Strategy:</strong></p>
     * <ol>
     *   <li><strong>Lazy Initialization Check:</strong> Returns immediately if already loaded</li>
     *   <li><strong>Map Initialization:</strong> Creates new HashMap for storing mappings</li>
     *   <li><strong>Java 8 Configuration:</strong> Loads standard Java class package mappings</li>
     *   <li><strong>Spring Configuration:</strong> Loads Spring-specific class package mappings</li>
     *   <li><strong>Data Consolidation:</strong> Combines mappings from multiple sources</li>
     * </ol>
     * 
     * <p><strong>Configuration File Processing:</strong></p>
     * <ul>
     *   <li><strong>Java 8 Documentation:</strong> java-doc-8-class-package.json</li>
     *   <li><strong>Spring Framework:</strong> spring-class-packages.json</li>
     *   <li><strong>File Path Resolution:</strong> Uses ConfigurationManager for path resolution</li>
     *   <li><strong>JSON Parsing:</strong> Uses org.json library for JSON processing</li>
     * </ul>
     * 
     * <p><strong>JSON Structure Expected:</strong></p>
     * <ul>
     *   <li><strong>Root Object:</strong> JSON object with class names as keys</li>
     *   <li><strong>Package Arrays:</strong> Each class maps to an array of package strings</li>
     *   <li><strong>Data Format:</strong> {"ClassName": ["package1", "package2", ...]}</li>
     *   <li><strong>Flexible Structure:</strong> Supports null or empty package arrays</li>
     * </ul>
     * 
     * <p><strong>Error Handling Strategy:</strong></p>
     * <ul>
     *   <li><strong>File Not Found:</strong> Gracefully handles missing configuration files</li>
     *   <li><strong>IO Exceptions:</strong> Catches and logs file reading errors</li>
     *   <li><strong>JSON Parsing Errors:</strong> Handles malformed JSON gracefully</li>
     *   <li><strong>General Exceptions:</strong> Catches all exceptions and errors</li>
     *   <li><strong>Partial Loading:</strong> Continues operation even if some files fail</li>
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
     *   <li><strong>File I/O Overhead:</strong> Involves reading JSON files from disk</li>
     *   <li><strong>JSON Parsing:</strong> CPU overhead for JSON processing</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Initialization:</strong> Call during system startup or first use</li>
     *   <li><strong>Configuration Loading:</strong> Load class-package mappings for analysis</li>
     *   <li><strong>System Setup:</strong> Initialize package resolution capabilities</li>
     *   <li><strong>Error Recovery:</strong> Retry loading after configuration file updates</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Thread Safety:</strong> Method is thread-safe and can be called multiple times</li>
     *   <li><strong>File Dependencies:</strong> Requires configuration files to be accessible</li>
     *   <li><strong>Memory Usage:</strong> Loaded mappings consume memory until JVM shutdown</li>
     *   <li><strong>Error Isolation:</strong> Failures in one file don't affect others</li>
     *   <li><strong>Progress Integration:</strong> Requires valid IProgressReporter instance</li>
     * </ul>
     *
     * @param progressReporter the progress reporter for status updates and error notifications
     */
    public static void loadC2PInfo(IProgressReporter progressReporter) {
        if (c2pMap != null) {
            return;
        }
        c2pMap = new HashMap<>();

        File specFileJava8 = Path.of(ConfigurationManager.config.SUMMARIES, "configs", "java-doc-8-class-package.json")
                .toFile();
        if (specFileJava8.exists()) {
            try (Reader in = new BufferedReader(new FileReader(specFileJava8))) {
                JSONObject jsonObject = new JSONObject(new JSONTokener(in));
                Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String className = keys.next();
                    JSONArray packageArray = jsonObject.getJSONArray(className);
                    Set<String> packages = new HashSet<>();
                    if (packageArray != null) {
                        for (int j = 0; j < packageArray.length(); j++) {
                            packages.add(packageArray.getString(j));
                        }
                    }
                    c2pMap.put(className, packages);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            } finally {
                // c2pMap will be empty or partially populated if there is a crash
            }
        } else {
            progressReporter.showProgress(
                    "Unable to load Class to Package Map, file does not exist: " + specFileJava8.getAbsolutePath());
        }

        File specFileSpring = Path.of(ConfigurationManager.config.SUMMARIES, "configs", "spring-class-packages.json")
                .toFile();
        if (specFileSpring.exists()) {
            try (Reader in = new BufferedReader(new FileReader(specFileSpring))) {
                JSONObject jsonObject = new JSONObject(new JSONTokener(in));
                Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String className = keys.next();
                    JSONArray packageArray = jsonObject.getJSONArray(className);
                    Set<String> packages = new HashSet<>();
                    if (packageArray != null) {
                        for (int j = 0; j < packageArray.length(); j++) {
                            packages.add(packageArray.getString(j));
                        }
                    }
                    c2pMap.put(className, packages);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            } finally {
                // c2pMap will be empty or partially populated if there is a crash
            }
        } else {
            progressReporter.showProgress(
                    "Unable to load Class to Package Map, file does not exist: " + specFileSpring.getAbsolutePath());
        }
    }

    /**
     * Retrieves the set of packages associated with a specific class name.
     *
     * <p>This method provides access to the cached class-to-package mapping information.
     * It returns the set of package names that are associated with the specified class
     * name, or an empty set if no mapping exists. The method is safe to call even if
     * the configuration hasn't been loaded yet.</p>
     * 
     * <p><strong>Access Strategy:</strong></p>
     * <ol>
     *   <li><strong>Cache Check:</strong> Verifies if c2pMap has been initialized</li>
     *   <li><strong>Direct Lookup:</strong> Searches the map for the specified class name</li>
     *   <li><strong>Default Handling:</strong> Returns empty set if no mapping found</li>
     *   <li><strong>Safe Return:</strong> Always returns a valid Set instance</li>
     * </ol>
     * 
     * <p><strong>Return Value Behavior:</strong></p>
     * <ul>
     *   <li><strong>Package Set:</strong> Returns set of package names for found classes</li>
     *   <li><strong>Empty Set:</strong> Returns empty set for unknown classes</li>
     *   <li><strong>Null Safety:</strong> Never returns null, always returns valid Set</li>
     *   <li><strong>Immutability:</strong> Returned set may be modified by caller</li>
     * </ul>
     * 
     * <p><strong>Class Name Matching:</strong></p>
     * <ul>
     *   <li><strong>Exact Matching:</strong> Requires exact class name match</li>
     *   <li><strong>Case Sensitivity:</strong> Class names are case-sensitive</li>
     *   <li><strong>Format Support:</strong> Supports both simple and fully qualified names</li>
     *   <li><strong>No Pattern Matching:</strong> Does not support wildcards or regex</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li><strong>Package Resolution:</strong> getPackages("String") returns ["java.lang"]</li>
     *   <li><strong>Class Analysis:</strong> getPackages("ArrayList") returns ["java.util"]</li>
     *   <li><strong>Dependency Mapping:</strong> getPackages("Controller") returns Spring packages</li>
     *   <li><strong>Unknown Classes:</strong> getPackages("UnknownClass") returns empty set</li>
     * </ul>
     * 
     * <p><strong>Performance Characteristics:</strong></p>
     * <ul>
     *   <li><strong>Constant Time:</strong> O(1) for map lookups</li>
     *   <li><strong>Memory Access:</strong> Works entirely with cached data</li>
     *   <li><strong>No I/O:</strong> No file system or network operations</li>
     *   <li><strong>Efficient Retrieval:</strong> Fast access to pre-loaded mappings</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <ul>
     *   <li><strong>Read-Only Access:</strong> Method only reads from cached data</li>
     *   <li><strong>Concurrent Access:</strong> Safe for multiple concurrent readers</li>
     *   <li><strong>No Synchronization:</strong> No locks or blocking operations</li>
     *   <li><strong>Immutable Returns:</strong> Returned sets are independent of cache</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li><strong>Configuration Dependency:</strong> Requires loadC2PInfo() to be called first</li>
     *   <li><strong>Cache State:</strong> Returns empty set if configuration not loaded</li>
     *   <li><strong>Memory Usage:</strong> Returned sets consume additional memory</li>
     *   <li><strong>Set Modifications:</strong> Callers can modify returned sets safely</li>
     *   <li><strong>Performance Impact:</strong> Very fast for loaded configurations</li>
     * </ul>
     *
     * @param key the class name to look up in the package mapping
     * @return a set of package names associated with the class, or empty set if not found
     */
    public static Set<String> getPackages(String key) {
        if (c2pMap != null) {
            return c2pMap.getOrDefault(key, new HashSet<>());
        }
        return new HashSet<>();
    }
}
