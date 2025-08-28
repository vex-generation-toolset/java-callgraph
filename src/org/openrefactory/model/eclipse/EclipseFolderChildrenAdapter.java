/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openrefactory.model.IModelElement;

/**
 * Adapter to create an iterator for model elements
 *
 * <p>This class provides an iterable interface for Eclipse Java elements, collecting
 * compilation units from package fragments and package fragment roots. It adapts
 * Eclipse compilation units to model elements for iteration.</p>
 *
 * @author Munawar Hafiz
 */
public class EclipseFolderChildrenAdapter implements Iterable<IModelElement> {
    private final List<ICompilationUnit> resources = new ArrayList<>();

    public EclipseFolderChildrenAdapter(final IPackageFragmentRoot container) throws IOException {
        collectAccessibleResources(container);
    }

    public EclipseFolderChildrenAdapter(final IPackageFragment container) throws IOException {
        collectAccessibleResources(container);
    }

    /**
     * Collects accessible compilation units from the specified Java element container.
     *
     * @param container the Java element container to collect resources from
     * @throws IOException if an I/O error occurs during collection
     */
    private void collectAccessibleResources(final IJavaElement container) throws IOException {
        List<IPackageFragment> pkgs = new ArrayList<>();
        try {
            if (container instanceof IPackageFragmentRoot) {
                if (((IPackageFragmentRoot) container).getChildren() != null) {
                    for (IJavaElement temp : ((IPackageFragmentRoot) container).getChildren()) {
                        if (temp instanceof IPackageFragment) {
                            pkgs.add((IPackageFragment) temp);
                        } else if (temp instanceof ICompilationUnit) {
                            if (temp.exists()) {
                                resources.add((ICompilationUnit) temp);
                            }
                        }
                    }
                }
            } else if (container instanceof IPackageFragment) {
                pkgs.add((IPackageFragment) container);
            }

            for (IPackageFragment pkg : pkgs) {
                if (pkg.getCompilationUnits() != null) {
                    for (ICompilationUnit icu : pkg.getCompilationUnits()) {
                        if (icu.exists()) {
                            resources.add(icu);
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            // Do nothing
        }
    }

    /**
     * Gets an iterator for the model elements.
     *
     * @return an iterator that provides access to the collected compilation units as model elements
     */
    @Override
    public Iterator<IModelElement> iterator() {
        return new Iterator<IModelElement>() {
            int nextIndex = 0;

            @Override
            public boolean hasNext() {
                return nextIndex < resources.size();
            }

            @Override
            public IModelElement next() {
                return adapt(resources.get(nextIndex++));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Adapts a compilation unit to a model element.
     *
     * @param resource the compilation unit to adapt
     * @return the adapted model element
     * @throws IllegalStateException if the resource cannot be adapted
     */
    private IModelElement adapt(ICompilationUnit resource) {
        IModelElement result = null;
        try {
            if (resource.exists()) {
                result = new EclipseModelFileElement(resource);
            }
        } catch (IOException e) {
        }
        if (result == null) {
            throw new IllegalStateException("INTERNAL ERROR: resource is not an file or folder");
        } else {
            return result;
        }
    }
}
