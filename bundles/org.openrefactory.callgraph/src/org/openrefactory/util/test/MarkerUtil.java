/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;

import java.util.LinkedList;

import org.eclipse.jface.text.BadLocationException;

/**
 * Utility methods for
 * 
 * @author Jeff Overbey
 * 
 * @since 3.0
 */
public class MarkerUtil
{
    private MarkerUtil() {;}

    public static SelectionFourTuple determineSelection(String markerText) throws BadLocationException
    {
        return determineSelection(parseMarker(markerText));
    }
    
    public static LinkedList<String> parseMarker(String markerText)
    {
        LinkedList<String> result = new LinkedList<String>();
        for (String field : markerText.split(",")) //$NON-NLS-1$
            result.add(field.trim());
        return result;
    }

    public static SelectionFourTuple determineSelection(LinkedList<String> markerFields) throws BadLocationException
    {
        if (markerFields.size() < 2) throw new IllegalArgumentException();
        
        int fromLine = Integer.parseInt(markerFields.removeFirst());
        int fromCol = Integer.parseInt(markerFields.removeFirst());
        int toLine = fromLine;
        int toCol = fromCol;
        if (markerFields.size() >= 2 && isInteger(markerFields.get(0)) && isInteger(markerFields.get(1)))
        {
            toLine = Integer.parseInt(markerFields.removeFirst());
            toCol = Integer.parseInt(markerFields.removeFirst());
        }
        
        return new SelectionFourTuple(fromLine, fromCol, toLine, toCol);
    }

    /**
     * @return true iff {@link Integer#parseInt(String)} can successfully parse the given
     *         string can be parsed as an integer
     */
    private static boolean isInteger(String string)
    {
        try
        {
            Integer.parseInt(string);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
