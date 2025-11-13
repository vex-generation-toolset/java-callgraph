/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.capslock;

import org.json.JSONObject;

/**
 * Issue 32
 * 
 * Represents a package in capslock format.
 * 
 * @param name              the simple name of the package
 * @param path              the qualified path of the package 
 * @param module            the index of the container module
 * @param isRoot            whether this is a root package
 * @param isStandardLibrary whether this package is in the standard library
 * 
 * @author Rifat Rubayatul Islam
 */
public record Package(String name, String path, Long module, Boolean isRoot, Boolean isStandardLibrary) {
    
    public JSONObject toJson() {
        JSONObject pkg = new JSONObject();
        pkg.put("name", name);
        pkg.put("path", path);
        pkg.put("module", module);
        pkg.put("isRoot", isRoot);
        pkg.put("isStandardLibrary", isStandardLibrary);
        return pkg;
    }
}
