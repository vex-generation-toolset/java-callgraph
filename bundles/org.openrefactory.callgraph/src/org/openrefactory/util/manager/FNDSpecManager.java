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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * <p>Loads data from JSON file and populate in appropriate structures used in FND fixer.
 *
 * @author Kanak Das
 */
public class FNDSpecManager {
    private static String[] specFiles =
            new String[] {"java-doc-8-refined.json", "java-doc-android.json", "java-doc-spring.json"};

    // Stores FND spec information as a mapping from a package name
    // to a map that is mapping from a class name to map that is
    // a mapping from method name to a set of FNDSpecInfo with that name.
    // The FNDSpecInfo class is a value object that stores the actual info
    public static Map<String, Map<String, Map<String, Set<FNDSpecInfo>>>> fndInfoMap;

    public static void loadFNDSpecInfo(IProgressReporter progressReporter) {
        // If already loaded, ignore
        if (fndInfoMap != null) {
            return;
        }
        for (String specFileStr : specFiles) {
            File specFile = Path.of(ConfigurationManager.config.SUMMARIES, "configs", specFileStr)
                    .toFile();
            if (specFile.exists()) {
                if (fndInfoMap == null) {
                    fndInfoMap = new HashMap<>();
                }
                try (Reader in = new BufferedReader(new FileReader(specFile))) {
                    JSONArray methodsArray = new JSONArray(new JSONTokener(in));

                    for (int i = 0; i < methodsArray.length(); i++) {
                        JSONObject methodInfoObject = methodsArray.getJSONObject(i);
                        String declaringType = methodInfoObject.getString("declaring_type");
                        String methodName = methodInfoObject.getString("method_name");
                        String returnType = methodInfoObject.getString("return_type");
                        JSONArray modifiersArray = methodInfoObject.getJSONArray("modifiers");
                        List<String> modifiers = new ArrayList<>();
                        if (modifiersArray != null) {
                            for (int j = 0; j < modifiersArray.length(); j++) {
                                modifiers.add(modifiersArray.getString(j));
                            }
                        }
                        String packageName = methodInfoObject.getString("package_name");
                        int paramCount = methodInfoObject.getInt("param_count");
                        // We will no longer generate fix for library method
                        // that may return null.
                        // So, Forcefully setting canReturnNull to false
                        // instead of loading from json.
                        // In this way, we can do it with minimum changes in code
                        boolean canReturnNull = false;
                        boolean returnsPrimitive = methodInfoObject.getBoolean("returnsPrimitive");
                        String sinceVersion = methodInfoObject.getString("since_version");

                        JSONArray paramsArray = methodInfoObject.getJSONArray("params");
                        List<String> params = new ArrayList<>();
                        if (paramsArray != null) {
                            for (int j = 0; j < paramsArray.length(); j++) {
                                params.add(paramsArray.getString(j));
                            }
                        }

                        // Two new pieces of information were added to the JSON spec file
                        // 1. exceptions: a list of exceptions thrown by current method
                        // 2. declaring_type_params: parameterized type of declaring type
                        JSONArray exceptionsArray = methodInfoObject.getJSONArray("exceptions");
                        List<String> exceptions = new ArrayList<>();
                        if (exceptionsArray != null) {
                            for (int j = 0; j < exceptionsArray.length(); j++) {
                                exceptions.add(exceptionsArray.getString(j));
                            }
                        }
                        String declaringTypeParam = methodInfoObject.getString("declaring_type_params");

                        // Adds a new field 'bounded_type_parameters' to JSON spec file
                        String boundedTypeParam = methodInfoObject.getString("bounded_type_parameters");

                        FNDSpecInfo info = new FNDSpecInfo(
                                declaringType,
                                methodName,
                                returnType,
                                modifiers,
                                packageName,
                                paramCount,
                                canReturnNull,
                                returnsPrimitive,
                                sinceVersion,
                                params,
                                exceptions,
                                declaringTypeParam,
                                boundedTypeParam);
                        addToFndInfoMap(info);
                    }
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
                        "Unable to load FND Map, file does not exist: " + specFile.getAbsolutePath());
            }
        }
    }

    /**
     * Insets {@link FNDSpecInfo} object which contains information about methods and types, to 'fndInfoMap'.
     *
     * @param info the spec info to be added
     * @author Sanjay Malakar
     */
    public static void addToFndInfoMap(FNDSpecInfo info) {
        Map<String, Map<String, Set<FNDSpecInfo>>> classToFndInfoMap = null;
        Map<String, Set<FNDSpecInfo>> methodToFndInfoMap = null;
        Set<FNDSpecInfo> specInfoSet = null;
        if (fndInfoMap.containsKey(info.getPackageName())) {
            classToFndInfoMap = fndInfoMap.get(info.getPackageName());
            if (classToFndInfoMap.containsKey(info.getDeclaringType())) {
                methodToFndInfoMap = classToFndInfoMap.get(info.getDeclaringType());
                if (methodToFndInfoMap.containsKey(info.getMethodName())) {
                    specInfoSet = methodToFndInfoMap.get(info.getMethodName());
                } else {
                    specInfoSet = new HashSet<>();
                }
            } else {
                methodToFndInfoMap = new HashMap<>();
                specInfoSet = new HashSet<>();
            }
        } else {
            classToFndInfoMap = new HashMap<>();
            methodToFndInfoMap = new HashMap<>();
            specInfoSet = new HashSet<>();
        }
        specInfoSet.add(info);
        methodToFndInfoMap.put(info.getMethodName(), specInfoSet);
        classToFndInfoMap.put(info.getDeclaringType(), methodToFndInfoMap);
        fndInfoMap.put(info.getPackageName(), classToFndInfoMap);
    }

    /**
     * Returns {@link FNDSpecInfo} object which contains information about methods and types. It checks if
     * 'fndInfoMap' has a information for the passed package name, class name and method name. If there is any then it
     * returns the first element whose parameter count matches with passed parameter count. If no info is available in
     * the map then it returns <code>null</code>.
     *
     * @param packageName the package name in which the method resides
     * @param className the class name in which the method resides
     * @param methodName the name of the method
     * @param noOfParams the number of parameters of the method
     * @return the {@link FNDSpecInfo} if 'fndInfoMap' has info, <code>null</code> otherwise.
     * @author Sanjay Malakar
     */
    public static FNDSpecInfo getInfoFor(String packageName, String className, String methodName, int noOfParams) {
        if (fndInfoMap.containsKey(packageName)) {
            Map<String, Map<String, Set<FNDSpecInfo>>> classToFndInfoMap = fndInfoMap.get(packageName);
            if (classToFndInfoMap.containsKey(className)) {
                Map<String, Set<FNDSpecInfo>> methodToFndInfoMap = classToFndInfoMap.get(className);
                if (methodToFndInfoMap.containsKey(methodName)) {
                    Set<FNDSpecInfo> specInfoSet = methodToFndInfoMap.get(methodName);
                    for (FNDSpecInfo info : specInfoSet) {
                        if (info.getParamCount() == noOfParams) {
                            return info;
                        }
                    }
                }
            }
        }
        return null;
    }
}
