/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.openrefactory.model.AbstractModelElement;
import org.openrefactory.model.IModel;
import org.openrefactory.model.IModelElement;
import org.openrefactory.model.IModelFileElement;

/**
 * Element to capture a file inside an eclipse project
 *
 * <p>This class represents an Eclipse file element in the model hierarchy, specifically
 * compilation units. It provides access to file contents, metadata, and language detection
 * for C and Java files.</p>
 *
 * @author Munawar Hafiz
 */
public class EclipseModelFileElement extends AbstractModelElement implements IModelFileElement {

    private final ICompilationUnit cu;
    private String canonicalPath;

    EclipseModelFileElement(ICompilationUnit cu) throws IOException {
        if (cu == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        this.cu = cu;
        try {
            IResource file = cu.getCorrespondingResource();
            this.canonicalPath = file.getRawLocation().toOSString();
        } catch (JavaModelException e) {
            this.canonicalPath = "";
        }
    }

    /**
     * Gets the name of this file element.
     *
     * @return the file name, or "EMPTY_FILE_NAME" if compilation unit is null
     */
    @Override
    public String getName() {
        if (cu == null) {
            return "EMPTY_FILE_NAME";
        } else {
            return cu.getElementName();
        }
    }

    /**
     * Gets the full canonical path of this file element.
     *
     * @return the canonical path, or "EMPTY_FILE_PATH" if path is null
     */
    @Override
    public String getFullPath() {
        if (canonicalPath == null) {
            return "EMPTY_FILE_PATH";
        } else {
            return canonicalPath;
        }
    }

    /**
     * Gets the underlying Eclipse compilation unit.
     *
     * @return the compilation unit
     */
    public ICompilationUnit getCompilationUnit() {
        return cu;
    }

    /**
     * Gets the children elements of this file.
     *
     * @return an empty collection (files have no children)
     * @throws IOException if an I/O error occurs while accessing children
     */
    @Override
    public Iterable<? extends IModelElement> getChildren() throws IOException {
        return Collections.emptyList();
    }

    /**
     * Compares this file element with another file element.
     *
     * @param that the file element to compare with
     * @return a negative integer, zero, or a positive integer as this path is less than, equal to, or greater than that path
     * @throws IllegalArgumentException if that is null or not the same class
     */
    @Override
    public int compareTo(IModelFileElement that) {
        if (that == null || !this.getClass().equals(that.getClass())) {
            throw new IllegalArgumentException();
        } else {
            return this.getFullPath().compareTo(that.getFullPath());
        }
    }

    /**
     * Gets the filename extension of this file.
     *
     * @return the file extension, or null if no extension exists or is a hidden file
     */
    @Override
    public String getFilenameExtension() {
        String name = getName();
        int indexOfLastPeriod = name.lastIndexOf('.');
        if (indexOfLastPeriod <= 0) {
            // Filename does not have an extension (e.g., /etc/passwd)
            // or is a Unix-style hidden file (e.g., /home/user/.bashrc)
            return null;
        } else {
            return name.substring(indexOfLastPeriod + 1);
        }
    }

    /**
     * Gets the last modification time of this file.
     *
     * @return the modification timestamp, or 0 if the time cannot be determined
     */
    @Override
    public long getModificationTime() {
        try {
            IResource file = cu.getCorrespondingResource();
            return file.getModificationStamp();
        } catch (JavaModelException e) {
            return 0;
        }
    }

    /**
     * Gets a reader for accessing the file contents.
     *
     * @return a reader containing the source code of the compilation unit
     * @throws IOException if an I/O error occurs while accessing the file
     */
    @Override
    public Reader getContents() throws IOException {
        Reader targetReader;
        try {
            targetReader = new StringReader(cu.getSource());
        } catch (JavaModelException e) {
            targetReader = new StringReader("");
        }
        return targetReader;
    }

    /**
     * Checks if this file matches the specified programming language.
     *
     * @param language the programming language identifier
     * @return true if the file matches the language, false otherwise
     */
    @Override
    public boolean matchesLanguage(String language) {
        if (IModel.C_LANGUAGE.equals(language)) {
            return getFilenameExtension() != null && getFilenameExtension().equals("c");
        } else if (IModel.JAVA_LANGUAGE.equals(language)) {
            return getFilenameExtension() != null && getFilenameExtension().equals("java");
        } else {
            return false;
        }
    }

    /**
     * Gets a description of this model element.
     *
     * @return the name of the element as its description
     */
    @Override
    protected String getDescription() {
        return getName();
    }

    @Override
    public int hashCode() {
        return getFullPath().hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        else if (that == null || !this.getClass().equals(that.getClass())) return false;
        else return this.getFullPath().equals(((EclipseModelFileElement) that).getFullPath());
    }
}
