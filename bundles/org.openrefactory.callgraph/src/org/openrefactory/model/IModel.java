/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface defining the contract for a model that manages project structure and file elements.
 *
 * <p>This interface provides methods for managing project models, file operations, and model lifecycle.
 * It supports multiple programming languages and provides access to project hierarchy elements.</p>
 */
public interface IModel {

    /** Constant representing the C programming language. */
    public static final String C_LANGUAGE = "C";

    /** Constant representing the Java programming language. */
    public static final String JAVA_LANGUAGE = "JAVA";

    /**
     * Checks if this model represents a virtual file system.
     *
     * @return true if the model is virtual, false otherwise
     */
    boolean isVirtualFileModel();

    /**
     * Initializes the model and its associated resources.
     */
    void initialize();

    /**
     * Deinitializes the model and releases associated resources.
     */
    void deinitialize();

    /**
     * Adds a listener to receive model change notifications.
     *
     * @param listener the listener to add
     */
    void addListener(IModelListener listener);

    /**
     * Removes a listener from receiving model change notifications.
     *
     * @param listener the listener to remove
     */
    void removeListener(IModelListener listener);

    /**
     * Gets the root element of the model hierarchy.
     *
     * @return the root element of the model
     */
    IModelRootElement getRoot();

    /**
     * Gets a file element by its path.
     *
     * @param path the file path
     * @return the file element, or null if not found
     */
    IModelFileElement getFile(String path);

    /**
     * Gets the project element that contains the specified path.
     *
     * @param path the path to find the project for
     * @return the project element containing the path, or null if not found
     */
    IModelProjectElement getProjectForPath(String path);

    /**
     * Gets the project element that contains the specified model element.
     *
     * @param element the model element to find the project for
     * @return the project element containing the element, or null if not found
     */
    IModelProjectElement getProjectForElement(IModelElement element);

    /**
     * Gets all files in the model.
     *
     * @return an iterable collection of all file elements
     */
    Iterable<IModelFileElement> getAllFiles();

    /**
     * Gets all source files for a specific programming language.
     *
     * @param language the programming language identifier
     * @return an iterable collection of source files for the specified language
     */
    Iterable<IModelFileElement> getAllSourceFiles(String language);

    /**
     * Writes content to a file at the specified path.
     *
     * @param path the file path to write to
     * @param contents the content to write to the file
     * @throws IOException if an I/O error occurs during writing
     */
    void writeFile(String path, String contents) throws IOException;

    /**
     * Prints the model structure to the specified output stream.
     *
     * @param out the output stream to print to
     */
    void printOn(PrintStream out);
}
