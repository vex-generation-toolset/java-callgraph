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
 * Represents a method invocation in capslock format.
 * 
 * @param caller   the index of the caller function
 * @param callee   the index of the callee function
 * @param callSite the call site of the method call
 * 
 * @author Rifat Rubayatul Islam
 */
public record Call(Long caller, Long callee, Site callSite) {

    public JSONObject toJson() {
        JSONObject call = new JSONObject();
        call.put("caller", caller);
        call.put("callee", callee);
        call.put("callSite", callSite.toJson());
        return call;
    }
}
