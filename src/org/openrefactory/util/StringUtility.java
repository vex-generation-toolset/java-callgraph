/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Utility methods for JUnit/testing.
 * 
 * @author Jeff Overbey
 * 
 * @since 3.0
 */
public final class StringUtility
{
    private StringUtility() {;}
    
    /** @return true if Eclipse is being run via the JUnit Plug-in Test runner */
    public static boolean runningJUnitPluginTests()
    {
        String app = System.getProperty("eclipse.application"); //$NON-NLS-1$
        return app != null && app.toLowerCase().contains("junit"); //$NON-NLS-1$
    }
    
    public static long countLines(String s)
    {
        if (s.length() == 0) return 0L;

        long numLines = 1L;
        int lastIndex = 0;
        int nextIndex = s.indexOf('\n');
        while (nextIndex >= 0)
        {
            numLines++;
            lastIndex = nextIndex;
            if (lastIndex + 1 >= s.length())
                nextIndex = -1;
            else
                nextIndex = s.indexOf('\n', lastIndex + 1);
        }
        return numLines;
    }

    public static long countElementsExceptSpaceChars(String s) {
        String[] splits = s.split("\\s+");
        return splits.length;
    }

    public static String read(Reader in) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (int ch = in.read(); ch >= 0; ch = in.read())
        {
            sb.append((char)ch);
        }
        in.close();
        return sb.toString();
    }

    public static String read(InputStream in) throws IOException
    {
        return read(new InputStreamReader(in));
    }

    //    public static String read(IFile file) throws IOException, CoreException
    //    {
    //        return read(new BufferedReader(new InputStreamReader(file.getContents(true), file.getCharset())));
    //    }

    public static String read(File file) throws IOException
    {
        return read(new BufferedReader(new FileReader(file)));
    }

    //    public static String readOrReturnNull(IFile file)
    //    {
    //        try
    //        {
    //            return read(file);
    //        }
    //        catch (Exception e)
    //        {
    //            return null;
    //        }
    //    }

    /**
     * Returns the (0-based) offset of the character at the given (1-based) line and column.
     * @param targetLine the line number (the first line is line 1)
     * @param targetCol the column number (the first column is column 1)
     * @return the character offset (the first character is at offset 0)
     */
    public static int offsetOf(int targetLine, int targetCol, String string)
    {
        assert targetLine >= 1 && targetCol >= 1 && string != null;

        int line = 1;
        int col = 1;
        for (int offset = 0; offset < string.length(); offset++) {
            if (line == targetLine && col == targetCol) {
                return offset;
            }
            col++;
            if (string.charAt(offset) == '\n') {
                line++;
                col = 1;
            }
        }
        return -1;
    }

    /**
     * Returns the (0-based) offset of the first character on the given (1-based) line.
     * @param line the line number (the first line is line 1)
     * @return the character offset (the first character is at offset 0)
     */
    public static int offsetOfLine(int line, String string)
    {
        assert line >= 1 && string != null;

        if (line == 1) return 0;

        int precedingLF = -1;
        int curLine = 1;
        while (precedingLF >= 0 || curLine == 1)
        {
            precedingLF = string.indexOf('\n', precedingLF+1);
            curLine++;
            if (curLine == line) return precedingLF < 0 ? -1 : precedingLF+1;
        }

        return -1;
    }

    public static String stripNonASCIICharsAndCRs(String s)
    {
        return stripNonASCIIChars(s, true);
    }

    public static String stripNonASCIIChars(String s)
    {
        return stripNonASCIIChars(s, false);
    }

    protected static String stripNonASCIIChars(String s, boolean stripCRs)
    {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0, len = s.length(); i < len; i++)
        {
            char ch = s.charAt(i);
            if (stripCRs && i == '\r')
                continue;
            else if (i < 256)
                sb.append(ch);
            else
                sb.append('?');
        }
        return sb.toString();
    }
}
