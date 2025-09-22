/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.progressreporter.IProgressReporter;

/**
 * Loads data from JSON file and populate in appropriate structures 
 * used to detect special root methods like android
 * life-cycle methods, event handlers .etc.
 *
 * @author Md. Rishadur Rahman
 */
public class SpecialRootSpecManager {
    private static String[] specFiles =
            new String[] {"special-root-methods-java.json", "special-root-methods-android.json"};

    // A map containing informations for library methods that are used
    // as callback like event listeners, android life-cycle methods .etc
    // The key of the map is the full qualified name of the class and
    // value is the set of target methods of that class.
    // For event handler methods specified in layout xml files,
    // we use the class hash of the container activity class
    // instead of full qualified class name as it is not from library.
    public static Map<String, Set<String>> specInfo;

	public static void loadSpecsFromJson(IProgressReporter progressReporter) {
		// If already loaded, ignore
		if (specInfo != null) {
			return;
		}
		specInfo = new HashMap<>();
		for (String specFileStr : specFiles) {
			File specFile = Path.of(ConfigurationManager.config.SUMMARIES, "configs", specFileStr).toFile();
			if (specFile.exists()) {
				try (Reader in = new BufferedReader(new FileReader(specFile))) {
					JSONArray specArray = new JSONArray(new JSONTokener(in));
					for (int i = 0; i < specArray.length(); i++) {
						JSONObject specInfoObject = specArray.getJSONObject(i);
						String qualifiedClassName = specInfoObject.getString("qualified_class_name");
						JSONArray methodsArray = specInfoObject.getJSONArray("method_names");
						Set<String> methods = new HashSet<>();
						if (methodsArray != null) {
							for (int j = 0; j < methodsArray.length(); j++) {
								methods.add(methodsArray.getString(j));
							}
						}
						specInfo.put(qualifiedClassName, methods);
					}
				} catch (Exception | Error e) {
					e.printStackTrace();
				}
			} else {
				progressReporter.showProgress(
						"Unable to load special root methods info, file does not exist: " + specFile.getAbsolutePath());
			}
		}
	}

	public static void addToSpecialRootSpec(String classHash, Set<String> methods) {
		if (specInfo == null) {
			return;
		}
		specInfo.put(classHash, methods);
	}

	public static void clearSpecialRootSpecs() {
		if (specInfo != null) {
			specInfo.clear();
			specInfo = null;
		}
	}

	public static boolean isSpecialRootMethod(String qualifiedClassName, String methodName) {
		if (specInfo == null) {
			return false;
		}
		Set<String> methods = specInfo.get(qualifiedClassName);
		return methods != null && methods.contains(methodName);
	}
}
