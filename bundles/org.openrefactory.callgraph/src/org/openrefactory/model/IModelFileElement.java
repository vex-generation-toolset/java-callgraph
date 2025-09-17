/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.IOException;
import java.io.Reader;

/**
 * Interface defining the contract for a file element in the model hierarchy.
 *
 * <p>This interface extends IModelElement to represent file-specific functionality,
 * including file extensions, modification times, content access, and language matching.
 * It also implements Comparable for file ordering.</p>
 */
public interface IModelFileElement extends IModelElement, Comparable<IModelFileElement> {
    
    /**
     * Gets the filename extension of this file element.
     *
     * @return the filename extension, or null if no extension exists
     */
    String getFilenameExtension();

    /**
     * Gets the last modification time of this file.
     *
     * @return the modification time as a long value
     */
    long getModificationTime();

    /**
     * Gets a reader for accessing the file contents.
     *
     * @return a reader for the file contents
     * @throws IOException if an I/O error occurs while accessing the file
     */
    Reader getContents() throws IOException;

    /**
     * Checks if this file matches the specified programming language.
     *
     * @param language the programming language to check against
     * @return true if the file matches the language, false otherwise
     */
    boolean matchesLanguage(String language);
}
