/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilities for test cases
 *
 * @author Munawar Hafiz
 */
public final class TestUtility {
    private TestUtility() {
        ;
    }

    /**
     * Copy contents in the source folder to the destination folder For the source file that matches the file with
     * marker, save the corresponding destination file as the file with marker. This is to ensure that we map the
     * correct file with marker in the temporary workspace
     *
     * @param source the source folder
     * @param destination the destination folder
     * @param originalFileWithMarker the original test file with marker
     */
    public static File copyFolder(File source, File destination, File originalFileWithMarker) {
        File copiedFileWithMarker = null;
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            for (String file : source.list()) {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                File temp = copyFolder(srcFile, destFile, originalFileWithMarker);
                if (temp != null) {
                    copiedFileWithMarker = temp;
                }
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);
                // Set the field that denotes the file with
                if (originalFileWithMarker.getAbsolutePath().equals(source.getAbsolutePath())) {
                    copiedFileWithMarker = destination;
                }

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (Exception e) {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return copiedFileWithMarker;
    }

    /**
     * Issue 964 From a test file, move to the test container directory
     *
     * @param file the starting file
     * @return the test container directory which will have a number or a letter followed by a number in the prefix. As
     *     in, 000, a00, A00, etc., as prefix but not aaa as a prefix
     */
    public static File getTestContainerDirectory(File file) {
        while (file != null && !(file.isDirectory() && startsWithDigitInFirstOrSecondPosition(file.getName()))) {
            file = file.getParentFile();
        }
        return file;
    }

    /**
     * Issue 964, 242 Since we have reached 1000 tests, we have started using a00, A00, etc., as test directories. We
     * will look for digit in the first or second position
     *
     * @param name the file/directory name
     * @return true if the name starts with a digit, false otherwise
     */
    private static boolean startsWithDigitInFirstOrSecondPosition(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() <= 1) {
            return false;
        }
        if (Character.isDigit(name.charAt(0))
                || (Character.isAlphabetic(name.charAt(0)) && Character.isDigit(name.charAt(1)))) {
            return true;
        }
        return false;
    }
}
