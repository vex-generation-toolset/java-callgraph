/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.AntlrUtil;
import org.openrefactory.util.test.TestSuiteFromFiles;

import junit.framework.Test;

/**
 * Checks that the parsing and pretty printing tasks are correct for a type 
 * 
 * @author Ridwanul Haque
 */
public class AntlrTypeTestSuite extends TestSuiteFromFiles {
    private static final String DIRECTORY = "antlr-type-tests";
    private static final String OR_INFIX = "__OR__";

    public static Test suite() throws Exception {
        return new AntlrTypeTestSuite();
    }

    public AntlrTypeTestSuite() throws Exception {
        super("Running antlr type grammar tests", DIRECTORY, ".txt");
    }

    private void createType(File file) throws Exception {
        CallGraphDataStructures.initialize();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String expected = null;
        TypeInfo foundTypeInfo = null;
        String foundTypeInfoString = null;
        String foundTree = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            // Annotation:
            // Lines starting with ## or blank lines are commented out
            // Lines starting with ***** are expected result line where the antlr tree is stringified
            // Other lines are input containing a type specification to parse
            // Only one test per file
            if (line.startsWith("##") || line.isBlank()) {
                continue;
            }
            if (line.startsWith("*****")) {
                expected = line.replace("*****", "");
            } else {
                foundTypeInfo = AntlrUtil.parseAndReturnTypeInfo(line);
                assertNotNull(foundTypeInfo);
                foundTypeInfoString = foundTypeInfo.toString();
                assertNotNull(foundTypeInfoString);
                foundTree = AntlrUtil.parseAndReturnParseTree(line);
            }
        }
        br.close();
        assertTrue(
            "In file, " + file + " expected " + expected + ", but found " + foundTree + OR_INFIX + foundTypeInfoString,
            expected != null && foundTree != null && foundTypeInfoString != null
                && expected.equals(foundTree + OR_INFIX + foundTypeInfoString));
    }

    @Override
    protected void test(File file) throws Exception {
        createType(file);
        assertEquals("For File: " + file + " the type is not matching", "", "");
    }
}
