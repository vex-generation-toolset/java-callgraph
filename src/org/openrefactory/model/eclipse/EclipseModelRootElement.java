/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.IOException;
import java.util.Iterator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.openrefactory.model.AbstractModelElement;
import org.openrefactory.model.IModelProjectElement;
import org.openrefactory.model.IModelRootElement;

/**
 * Element to capture the workspace root of the eclipse model
 *
 * <p>This class represents the root element of an Eclipse workspace in the model hierarchy.
 * It provides access to Eclipse projects and handles the conversion between Eclipse
 * resources and model elements.</p>
 *
 * @author Munawar Hafiz
 */
public class EclipseModelRootElement extends AbstractModelElement implements IModelRootElement {

    private final IWorkspaceRoot root;
    private String canonicalPath;

    EclipseModelRootElement(IWorkspaceRoot root) throws IOException {
        if (root == null) {
            throw new IllegalArgumentException("the workspace root cannot be null");
        }
        this.root = root;
        this.canonicalPath = root.getFullPath().makeAbsolute().toOSString();
    }

    /**
     * Gets the name of this workspace root element.
     *
     * @return the workspace root name, or "EMPTY_ROOT_NAME" if root is null
     */
    @Override
    public String getName() {
        if (root == null) {
            return "EMPTY_ROOT_NAME";
        } else {
            return root.getName();
        }
    }

    /**
     * Gets the full canonical path of this workspace root element.
     *
     * @return the canonical path, or "EMPTY_ROOT_PATH" if path is null
     */
    @Override
    public String getFullPath() {
        if (canonicalPath == null) {
            return "EMPTY_ROOT_PATH";
        } else {
            return canonicalPath;
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

    /**
     * Gets the Eclipse projects as model project elements.
     *
     * @return an iterable collection of Eclipse model project elements
     * @throws IOException if an I/O error occurs while accessing children
     */
    @Override
    public Iterable<? extends IModelProjectElement> getChildren() throws IOException {
        // Return an empty iterator if the project is empty
        if (root == null || root.getProjects() == null) {
            return new Iterable<IModelProjectElement>() {

                @Override
                public Iterator<IModelProjectElement> iterator() {
                    return new Iterator<IModelProjectElement>() {
                        @Override
                        public boolean hasNext() {
                            // TODO Auto-generated method stub
                            return false;
                        }

                        @Override
                        public IModelProjectElement next() {
                            // TODO Auto-generated method stub
                            return null;
                        }
                    };
                }
            };
        }
        IJavaProject[] resources = new IJavaProject[root.getProjects().length];
        int index = 0;
        for (IProject project : root.getProjects()) {
            IJavaProject javaProject = JavaCore.create(project);
            resources[index++] = javaProject;
        }

        return new Iterable<IModelProjectElement>() {

            @Override
            public Iterator<IModelProjectElement> iterator() {
                return new Iterator<IModelProjectElement>() {
                    int nextIndex = 0;

                    @Override
                    public boolean hasNext() {
                        return nextIndex < resources.length;
                    }

                    @Override
                    public IModelProjectElement next() {
                        return adapt(resources[nextIndex++]);
                    }

                    /**
                     * Adapts an Eclipse Java project to a model project element.
                     *
                     * @param resource the Eclipse Java project to adapt
                     * @return the adapted model project element
                     * @throws IllegalStateException if the resource cannot be adapted
                     */
                    private IModelProjectElement adapt(IJavaProject resource) {
                        try {
                            if (resource.exists()) {
                                return new EclipseModelProjectElement(resource);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("INTERNAL ERROR: resource is not an file or folder");
                        }
                        throw new IllegalStateException("INTERNAL ERROR: resource is not an file or folder");
                    }
                    ;
                };
            }
        };
    }
}
