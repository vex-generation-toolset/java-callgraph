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
 * Represents a call site in capslock format.
 * 
 * @param directory         the directory of the file that contains the call site
 * @param filename          the name of the file that contains the call site
 * @param line              the line of the method call
 * @param column            the column of the method call
 * 
 * @author Rifat Rubayatul Islam
 */
public record Site(String directory, String filename, long line, long column) {

    public JSONObject toJson() {
        JSONObject callSite = new JSONObject();
        callSite.put("directory", directory);
        callSite.put("filename", filename);
        callSite.put("line", line);
        callSite.put("column", column);
        return callSite;
    }
}
