/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

/**
 * Null object implementation of the IModel interface.
 *
 * <p>This class provides a safe default implementation that returns empty collections
 * and null values when no actual model is configured. It serves as a placeholder
 * until a real model is set using Model#useModel.</p>
 */
public final class NullModel implements IModel {

    private static final String ERROR_MESSAGE =
            "(OpenRefactory is currently using a null model.  Use Model#useFileModel to configure a model.)";

    private static IModel instance = null;

    /**
     * Gets the singleton instance of NullModel.
     *
     * @return the NullModel instance
     */
    public static IModel getInstance() {
        if (instance == null) instance = new NullModel();
        return instance;
    }

    private NullModel() {}

    /**
     * Checks if this model represents a virtual file system.
     *
     * @return false (null model is not virtual)
     */
    @Override
    public boolean isVirtualFileModel() {
        return false;
    }

    /**
     * Initializes the model and its associated resources.
     */
    @Override
    public void initialize() {}

    /**
     * Deinitializes the model and releases associated resources.
     */
    @Override
    public void deinitialize() {}

    /**
     * Adds a listener to receive model change notifications.
     *
     * @param listener the listener to add (ignored in null model)
     */
    @Override
    public void addListener(IModelListener listener) {}

    /**
     * Removes a listener from receiving model change notifications.
     *
     * @param listener the listener to remove (ignored in null model)
     */
    @Override
    public void removeListener(IModelListener listener) {}

    /**
     * Gets the root element of the model hierarchy.
     *
     * @return a null model root element that returns empty collections
     */
    @Override
    public IModelRootElement getRoot() {
        return new IModelRootElement() {
            @Override
            public String getName() {
                return "";
            }

            @Override
            public String getFullPath() {
                return File.separator;
            }

            @Override
            public Iterable<? extends IModelProjectElement> getChildren() {
                return Collections.emptyList();
            }

            @Override
            public void printOn(PrintStream out) {
                out.println(ERROR_MESSAGE);
            }
        };
    }

    /**
     * Prints the model structure to the specified output stream.
     *
     * @param out the output stream to print to
     */
    @Override
    public void printOn(PrintStream out) {
        out.println(ERROR_MESSAGE);
    }

    @Override
    public String toString() {
        return ERROR_MESSAGE;
    }

    /**
     * Gets a file element by its path.
     *
     * @param path the file path
     * @return null (null model has no files)
     */
    @Override
    public IModelFileElement getFile(String path) {
        return null;
    }

    /**
     * Gets the project element that contains the specified path.
     *
     * @param path the path to find the project for
     * @return null (null model has no projects)
     */
    @Override
    public IModelProjectElement getProjectForPath(String path) {
        return null;
    }

    /**
     * Gets the project element that contains the specified model element.
     *
     * @param element the model element to find the project for
     * @return null (null model has no projects)
     */
    @Override
    public IModelProjectElement getProjectForElement(IModelElement element) {
        return null;
    }

    /**
     * Gets all files in the model.
     *
     * @return an empty collection (null model has no files)
     */
    @Override
    public Iterable<IModelFileElement> getAllFiles() {
        return Collections.emptyList();
    }

    /**
     * Gets all source files for a specific programming language.
     *
     * @param language the programming language identifier
     * @return an empty collection (null model has no files)
     */
    @Override
    public Iterable<IModelFileElement> getAllSourceFiles(String language) {
        return Collections.emptyList();
    }

    /**
     * Writes content to a file at the specified path.
     *
     * @param path the file path to write to
     * @param contents the content to write to the file
     * @throws IOException as writeFile is not supported by null model
     */
    @Override
    public void writeFile(String path, String contents) throws IOException {
        throw new IOException("writeFile not supported");
    }
}
