/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openrefactory.model.AbstractModelElement;
import org.openrefactory.model.IModelElement;
import org.openrefactory.model.IModelProjectElement;

/**
 * Element to capture a project inside the eclipse workspace
 *
 * <p>This class represents an Eclipse Java project in the model hierarchy.
 * It provides access to project resources, package fragments, and compilation units,
 * handling the conversion between Eclipse Java elements and model elements.</p>
 *
 * @author Munawar Hafiz
 */
public class EclipseModelProjectElement extends AbstractModelElement implements IModelProjectElement {

    private final IJavaProject project;
    private String canonicalPath;

    EclipseModelProjectElement(IJavaProject project) throws IOException {
        if (project == null) {
            throw new IllegalArgumentException("project cannot be null");
        }
        this.project = project;
        try {
            IResource file = project.getCorrespondingResource();
            this.canonicalPath = file.getRawLocation().toOSString();
        } catch (JavaModelException e) {
            this.canonicalPath = "";
        }
    }

    /**
     * Gets the name of this project element.
     *
     * @return the project name, or "EMPTY_PROJECT_NAME" if project is null
     */
    @Override
    public String getName() {
        if (project == null) {
            return "EMPTY_PROJECT_NAME";
        } else {
            return project.getElementName();
        }
    }

    /**
     * Gets the full canonical path of this project element.
     *
     * @return the canonical path, or "EMPTY_PROJECT_PATH" if path is null
     */
    @Override
    public String getFullPath() {
        if (canonicalPath == null) {
            return "EMPTY_PROJECT_PATH";
        } else {
            return canonicalPath;
        }
    }

    /**
     * Gets the underlying Eclipse Java project.
     *
     * @return the Eclipse Java project
     */
    public IJavaProject getProject() {
        return project;
    }

    /**
     * Gets the children elements of this project.
     *
     * @return an iterable collection of model elements representing project contents
     * @throws IOException if an I/O error occurs while accessing children
     */
    @Override
    public Iterable<? extends IModelElement> getChildren() throws IOException {
        try {
            if (project != null) {
                Set<IJavaElement> children = new HashSet<>();
                if (project.getPackageFragmentRoots() != null && project.getPackageFragmentRoots().length > 0) {
                    for (IPackageFragmentRoot pkgFragmentRoot : project.getPackageFragmentRoots()) {
                    	pkgFragmentRoot.open(null);
                        if (!pkgFragmentRoot.getElementName().equals(IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH)) {
                            children.add(pkgFragmentRoot);
                        } else {
                            // Distinguish a default package fragment root in which a java file is in the top level
                            // project directory
                            if (pkgFragmentRoot.getChildren() != null && pkgFragmentRoot.getChildren().length > 0) {
                                for (IJavaElement elemInsidePkgFragRoot : pkgFragmentRoot.getChildren()) {
                                    if (elemInsidePkgFragRoot instanceof ICompilationUnit) {
                                        ((ICompilationUnit) elemInsidePkgFragRoot).open(null);
                                        children.add(elemInsidePkgFragRoot);
                                    } else if (elemInsidePkgFragRoot instanceof IPackageFragment) {
                                        IPackageFragment pkgFragment = (IPackageFragment) elemInsidePkgFragRoot;
                                        pkgFragment.open(null);
                                        if (!pkgFragment
                                                .getElementName()
                                                .equals(IPackageFragment.DEFAULT_PACKAGE_NAME)) {
                                            children.add(pkgFragment);
                                        } else {
                                            // Distinguish a default package fragment in which a java file is in the top
                                            // level project directory
                                            if (pkgFragment.getChildren() != null
                                                    && pkgFragment.getChildren().length > 0) {
                                                for (IJavaElement elemInsidePkgFragment : pkgFragment.getChildren()) {
                                                    if (elemInsidePkgFragment instanceof ICompilationUnit) {
                                                        ((ICompilationUnit) elemInsidePkgFragment).open(null);
                                                        children.add(elemInsidePkgFragment);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (project.getChildren() != null && project.getChildren().length > 0) {
                    for (IJavaElement child : project.getChildren()) {
                        if (child instanceof ICompilationUnit) {
                            children.add(child);
                        }
                    }
                }
                if (!children.isEmpty()) {
                    return new FileOrFolderElementIterable(children);
                }
            }
        } catch (JavaModelException e1) {
            e1.printStackTrace();
        }
        // Return an empty iterator as the last resort
        Set<IJavaElement> emptyList = new HashSet<>();
        return new FileOrFolderElementIterable(emptyList);
    }

    /**
     * Finds the relative path from this project directory to the specified element.
     *
     * @param element the element to find the relative path to
     * @return the relative path, or null if the element is not in this project
     */
    @Override
    public String getRelativePathTo(IModelElement element) {
        if (getProjectForElement(element) == this) {
            String elementPath = element.getFullPath();
            String projectPath = this.getFullPath();
            if (elementPath.startsWith(projectPath + File.separator)) {
                return elementPath.substring(projectPath.length() + File.separator.length());
            } else if (elementPath.startsWith(projectPath)) {
                return elementPath.substring(projectPath.length());
            } else {
                return elementPath;
            }
        } else {
            return null;
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
     * Determines which project contains the specified element.
     *
     * @param element the element to find the project for
     * @return the project element containing the element, or null if not found
     */
    private EclipseModelProjectElement getProjectForElement(IModelElement element) {
        IJavaElement temp = null;
        if (element instanceof EclipseModelFileElement) {
            temp = ((EclipseModelFileElement) element).getCompilationUnit();
        } else if (element instanceof EclipseModelFolderElement) {
            temp = ((EclipseModelFolderElement) element).getContainedElement();
        }
        while (temp != null) {
            temp = temp.getParent();
            if (temp instanceof IJavaProject) {
                try {
                    return new EclipseModelProjectElement(project);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Inner class that creates an iterable for model elements
     *
     * @author Munawar Hafiz
     */
    public class FileOrFolderElementIterable implements Iterable<IModelElement> {
        private List<IJavaElement> units;

        /**
         * Creates a new iterable for the specified Java elements.
         *
         * @param units the set of Java elements to iterate over
         */
        public FileOrFolderElementIterable(Set<IJavaElement> units) {
            this.units = new ArrayList<>(units.size());
            for (IJavaElement unit : units) {
                this.units.add(unit);
            }
        }

        /**
         * Gets an iterator for the model elements.
         *
         * @return an iterator that adapts Java elements to model elements
         */
        @Override
        public Iterator<IModelElement> iterator() {
            return new Iterator<IModelElement>() {
                int nextIndex = 0;

                @Override
                public boolean hasNext() {
                    return nextIndex < units.size();
                }

                @Override
                public IModelElement next() {
                    return adapt(units.get(nextIndex++));
                }

                /**
                 * Adapts a Java element to a model element.
                 *
                 * @param resource the Java element to adapt
                 * @return the adapted model element
                 * @throws IllegalStateException if the resource cannot be adapted
                 */
                private IModelElement adapt(IJavaElement resource) {
                    try {
                        if (resource.exists()) {
                            if (resource instanceof ICompilationUnit) {
                                return new EclipseModelFileElement((ICompilationUnit) resource);
                            } else if (resource instanceof IPackageFragmentRoot) {
                                return new EclipseModelFolderElement((IPackageFragmentRoot) resource);
                            } else if (resource instanceof IPackageFragment) {
                                return new EclipseModelFolderElement((IPackageFragment) resource);
                            }
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("INTERNAL ERROR: resource is not an file or folder");
                    }
                    throw new IllegalStateException("INTERNAL ERROR: resource is not an file or folder");
                }
                ;
            };
        }
    }
}
