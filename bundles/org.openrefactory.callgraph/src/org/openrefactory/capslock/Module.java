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
 * Represents a module in capslock format.
 * 
 * @param name    the name of the module
 * @param version the version of the module
 * @param hash    the hash of the module
 * 
 * @author Rifat Rubayatul Islam
 */
public record Module(String name, String version, String hash) {
    
    public JSONObject toJson() {
        JSONObject module = new JSONObject();
        module.put("name", name);
        module.put("version", version);
        module.put("hash", hash);
        return module;
    }
}
