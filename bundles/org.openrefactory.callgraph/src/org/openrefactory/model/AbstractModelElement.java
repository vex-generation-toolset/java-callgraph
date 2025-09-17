/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Abstract base class implementing the IModelElement interface.
 *
 * <p>This class provides default implementations for common model element operations
 * including printing, string representation, and equality comparison based on full paths.</p>
 */
public abstract class AbstractModelElement implements IModelElement {

    /**
     * Prints this model element to the specified output stream.
     *
     * @param out the output stream to print to
     */
    @Override
    public void printOn(PrintStream out) {
        out.print(getDescription());
        try {
            Iterable<? extends IModelElement> children = getChildren();
            if (children != null) {
                for (IModelElement child : children) {
                    if (child != null) {
                        out.print(System.lineSeparator());
                        out.print(indent(child.toString()));
                    }
                }
            }
        } catch (IOException e) {
            out.print(System.lineSeparator());
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            e.printStackTrace(new PrintWriter(bs));
            out.print(bs.toString());
        }
        out.flush();
    }

    @Override
    public String toString() {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        printOn(new PrintStream(bs));
        return bs.toString();
    }

    /**
     * Adds indentation to a string by prepending spaces to each line.
     *
     * @param string the string to indent
     * @return the indented string
     */
    private final String indent(String string) {
        return "    " + string.replace(System.lineSeparator(), System.lineSeparator() + "    ");
    }

    /**
     * Gets a description of this model element.
     *
     * @return a string description of the model element
     */
    protected abstract String getDescription();

    @Override
    public int hashCode() {
        return getFullPath().hashCode() * 7 + getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        else if (!this.getClass().equals(o.getClass())) return false;
        else return this.getFullPath().equals(((AbstractModelElement) o).getFullPath());
    }
}
