/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface defining the contract for a model element in the project hierarchy.
 *
 * <p>This interface represents a basic element in the model structure that can have a name,
 * full path, children elements, and can be printed to an output stream.</p>
 */
public interface IModelElement {
    
    /**
     * Gets the name of this model element.
     *
     * @return the name of the model element
     */
    String getName();

    /**
     * Gets the full path of this model element.
     *
     * @return the full path of the model element
     */
    String getFullPath();

    /**
     * Gets the children elements of this model element.
     *
     * @return an iterable collection of child model elements
     * @throws IOException if an I/O error occurs while accessing children
     */
    Iterable<? extends IModelElement> getChildren() throws IOException;

    /**
     * Prints this model element to the specified output stream.
     *
     * @param out the output stream to print to
     */
    void printOn(PrintStream out);
}
