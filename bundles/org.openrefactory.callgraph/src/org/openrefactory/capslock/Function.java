/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.capslock;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

/**
 * Issue 32
 * 
 * Represents a method in capslock format.
 * 
 * @param name              a human-readable fully-qualified name for the method
 * @param packageIndex      the index of the package
 * @param type              the container class/enum of the method
 * @param function          the simple name of the method
 * @param templateArguments the type arguments of the method call
 * @param parameterTypes    the parameters of the method
 * @param properties        the language specific special properties of a method
 * @param language          the language of the function
 * 
 * @author Rifat Rubayatul Islam
 */
public record Function(String name, Long packageIndex, String type, String function, List<String> templateArguments,
    List<String> parameterTypes, List<String> properties, String language) {

    public Function(String name, Long packageIndex, String type, String function, List<String> parameterTypes,
        String language) {
        this(name, packageIndex, type, function, Collections.emptyList(), parameterTypes, Collections.emptyList(),
            language);
    }

    public Function(String name, Long packageIndex, String type, String function, List<String> parameterTypes) {
        this(name, packageIndex, type, function, parameterTypes, "java");
    }

    public Function(String name, Long packageIndex, String type, String function, List<String> parameterTypes,
        List<String> properties) {
        this(name, packageIndex, type, function, Collections.emptyList(), parameterTypes, properties, "java");
    }

    public JSONObject toJson() {
        JSONObject func = new JSONObject();
        func.put("name", name).put("function", function).put("packageIndex", packageIndex).put("type", type)
            .put("templateArguments", templateArguments).put("parameterTypes", parameterTypes)
            .put("properties", properties).put("language", language);
        return func;
    }
}
