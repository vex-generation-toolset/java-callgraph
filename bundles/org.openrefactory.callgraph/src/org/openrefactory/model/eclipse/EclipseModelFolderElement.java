/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openrefactory.model.AbstractModelElement;
import org.openrefactory.model.IModelElement;
import org.openrefactory.model.IModelFolderElement;

/**
 * Element to capture a folder inside an eclipse project A folder can be denoted by a package fragment root or
 * package fragment
 *
 * <p>This class represents an Eclipse folder element in the model hierarchy, which can be either
 * a package fragment root or a package fragment. It provides access to folder contents and
 * handles the conversion between Eclipse Java elements and model elements.</p>
 *
 * @author Munawar Hafiz
 */
public class EclipseModelFolderElement extends AbstractModelElement implements IModelFolderElement {

    private final IPackageFragmentRoot pkgRoot;
    private final IPackageFragment pkg;
    private String canonicalPath;

    EclipseModelFolderElement(IPackageFragmentRoot root) throws IOException {
        if (root == null) {
            throw new IllegalArgumentException("folder cannot be null");
        }
        this.pkgRoot = root;
        this.pkg = null;
        try {
            IResource file = pkgRoot.getCorrespondingResource();
            if (file != null) this.canonicalPath = file.getRawLocation().toOSString();
        } catch (JavaModelException e) {
            this.canonicalPath = "";
        }
    }

    EclipseModelFolderElement(IPackageFragment pkg) throws IOException {
        if (pkg == null) {
            throw new IllegalArgumentException("folder cannot be null");
        }
        this.pkgRoot = null;
        this.pkg = pkg;
        try {
            IResource file = pkg.getCorrespondingResource();
            this.canonicalPath = file.getRawLocation().toOSString();
        } catch (JavaModelException e) {
            this.canonicalPath = "";
        }
    }

    /**
     * Gets the name of this folder element.
     *
     * @return the folder name, or "EMPTY_FOLDER_NAME" if neither package root nor package is set
     */
    @Override
    public String getName() {
        if (pkgRoot != null) {
            return pkgRoot.getElementName();
        } else if (pkg != null) {
            return pkg.getElementName();
        } else {
            return "EMPTY_FOLDER_NAME";
        }
    }

    /**
     * Gets the full canonical path of this folder element.
     *
     * @return the canonical path, or "EMPTY_FOLDER_PATH" if path is null
     */
    @Override
    public String getFullPath() {
        if (canonicalPath == null) {
            return "EMPTY_FOLDER_PATH";
        } else {
            return canonicalPath;
        }
    }

    /**
     * Gets the underlying Eclipse Java element contained in this folder.
     *
     * @return the package fragment root, package fragment, or null if neither is set
     */
    public IJavaElement getContainedElement() {
        if (pkgRoot != null) {
            return pkgRoot;
        } else if (pkg != null) {
            return pkg;
        } else {
            return null;
        }
    }

    /**
     * Gets the children elements of this folder.
     *
     * @return an iterable collection of model elements representing folder contents
     * @throws IOException if an I/O error occurs while accessing children
     */
    @Override
    public Iterable<? extends IModelElement> getChildren() throws IOException {
        if (pkgRoot != null) {
            return new EclipseFolderChildrenAdapter(pkgRoot);
        } else {
            return new EclipseFolderChildrenAdapter(pkg);
        }
    }

    /**
     * Gets a description of this model element.
     *
     * @return the name of the element with a file separator suffix
     */
    @Override
    protected String getDescription() {
        return getName() + File.separator;
    }
}
