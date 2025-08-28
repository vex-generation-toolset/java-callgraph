/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;


import java.io.File;
import java.io.FilenameFilter;

/**
 * Constants for Java unit tests.
 *
 * @author Munawar Hafiz
 */
public final class JavaTestUtility {
    private JavaTestUtility() {
        ;
    }

    public static final FilenameFilter JAVA_FILENAME_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory() && !name.equalsIgnoreCase("CVS") && !name.equalsIgnoreCase(".svn")
                    || name.endsWith(".java");
        }
    };

    public static final String MARKER = "/*<<<<<";

    public static final String MARKER_END = "*/";
}
