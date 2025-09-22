/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.openrefactory.model.AbstractModel;
import org.openrefactory.model.IModelElement;
import org.openrefactory.model.IModelFileElement;
import org.openrefactory.model.IModelProjectElement;
import org.openrefactory.model.IModelRootElement;

/**
 * Eclipse-specific implementation of the model interface.
 *
 * <p>This class provides Eclipse workspace integration, managing Java projects,
 * compilation units, and file caching. It handles project discovery, Java nature
 * configuration, and file path resolution within the Eclipse environment.</p>
 */
public final class EclipseModel extends AbstractModel {

    private IWorkspaceRoot workspaceRoot = null;
    private final List<EclipseModelProjectElement> javaProjects = new ArrayList<>();
    private IProject[] projects = null;
    // Cache the last 30 files so that the compilation unit can be retrieved
    // from the absolute path of the file name
    private Map<String, EclipseModelFileElement> fileCache = new HashMap<>();

    public EclipseModel(File projectRoot) throws CoreException {
        if (projectRoot == null) throw new IllegalArgumentException("projectRoot cannot be null");

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        if (workspace == null) throw new IllegalArgumentException("Eclipse workspace cannot be null");

        this.workspaceRoot = workspace.getRoot();

        if (workspaceRoot == null) throw new IllegalArgumentException("Eclipse workspace root cannot be null");

        boolean foundDotProject = false;
        try {
            IPath projectDotProjectFile =
                    new Path(projectRoot.getAbsolutePath().toString() + File.separator + ".project");
            IProject project = null;
            // If the .project file exists use that, otherwise create a new project file
            if (new File(projectRoot.getAbsolutePath().toString() + File.separator + ".project").exists()) {
                foundDotProject = true;
                IProjectDescription projectDescription = workspace.loadProjectDescription(projectDotProjectFile);
                project = workspaceRoot.getProject(projectDescription.getName());
            } else {
                project = workspaceRoot.getProject(projectRoot.getName());
            }
            JavaCapabilityConfigurationPage.createProject(project, projectRoot.toURI(), null);
            projects = new IProject[1];
            projects[0] = project;
        } catch (CoreException e) {
            e.printStackTrace();
        }

        // If we did not discover a directory with .project file and
        // we could not add it to projects, then there is no point in progressing.
        if (projects == null || projects.length == 0) {
            return;
        }

        for (IProject project : projects) {
            //            project.open(null);

            // set the Java nature
            if (!foundDotProject) {
                IProjectDescription description = project.getDescription();
                description.setNatureIds(new String[] {JavaCore.NATURE_ID});

                // create the project
                project.setDescription(description, null);
            }
            IJavaProject javaProject = JavaCore.create(project);
            if (javaProject.exists()) {
                if (!javaProject.isOpen()) {
                    //                    javaProject.open(null);
                }
                EclipseModelProjectElement proj;
                try {
                    proj = new EclipseModelProjectElement(javaProject);
                    if (projectRoot.getAbsolutePath().startsWith(proj.getFullPath())) {
                        javaProjects.add(proj);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Closes items in the Eclipse model so that they can be removed safely later.
     */
    @Override
    public void deinitialize() {

        for (EclipseModelProjectElement proj : javaProjects) {
            IJavaProject javaProject = proj.getProject();
            if (javaProject.isOpen()) {
                try {
                    javaProject.close();
                } catch (JavaModelException e) {
                    e.printStackTrace();
                }
            }
        }

        if (projects == null) return;

        for (IProject iproj : projects) {
            if (iproj.isOpen()) {
                try {
                    iproj.close(null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets the root element of the Eclipse workspace.
     *
     * @return the Eclipse model root element, or null if creation fails
     */
    @Override
    public IModelRootElement getRoot() {
        try {
            return new EclipseModelRootElement(workspaceRoot);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a file element by its path.
     *
     * @param path the file path to search for
     * @return the file element if found, null otherwise
     */
    @Override
    public IModelFileElement getFile(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            // For a relative path, we may have multiple projects,
            // here we will check if we can construct path from any one of them
            // and return the first one that matches
            EclipseModelProjectElement candidateProject = null;
            String candidatePath = null;
            String relativePath = null;
            if (file.isAbsolute()) {
                for (EclipseModelProjectElement temp : javaProjects) {
                    if (path.startsWith(temp.getFullPath())) {
                        candidateProject = temp;
                        relativePath = path.substring(temp.getFullPath().length() + 1);
                        break;
                    }
                }
                candidatePath = path;
            } else {
                for (EclipseModelProjectElement temp : javaProjects) {
                    file = new File(temp.getFullPath(), path);
                    if (file.exists() && file.isFile()) {
                        candidateProject = temp;
                        candidatePath = file.getAbsolutePath();
                        break;
                    }
                }
                relativePath = path;
            }
            if (candidateProject != null && candidatePath != null && relativePath != null) {
                if (fileCache.containsKey(candidatePath)) {
                    return fileCache.get(candidatePath);
                } else {
                    List<IModelElement> workList = new ArrayList<>();
                    Set<IModelElement> seenList = new HashSet<>();
                    workList.add(candidateProject);
                    while (!workList.isEmpty()) {
                        IModelElement temp = workList.remove(0);
                        if (!seenList.contains(temp)) {
                            seenList.add(temp);
                            try {
                                for (IModelElement child : temp.getChildren()) {
                                    if ((child instanceof IModelFileElement)
                                            && child.getFullPath().equals(candidatePath)) {
                                        return (IModelFileElement) child;
                                    }
                                    workList.add(child);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets all source files for a specific programming language.
     *
     * @param language the programming language identifier
     * @return an iterable collection of source files, or empty iterator if multiple projects exist
     */
    @Override
    public Iterable<IModelFileElement> getAllSourceFiles(final String language) {
        // Analysis done on only one project at a time
        // Make sure that there is only one project and start analysis from there
        if (javaProjects != null && javaProjects.size() == 1) {
            return new Iterable<IModelFileElement>() {
                @Override
                public Iterator<IModelFileElement> iterator() {
                    return new FileIterator(javaProjects.get(0), language);
                }
            };
        } else {
            // Return an empty iterator
            return new Iterable<IModelFileElement>() {

                @Override
                public Iterator<IModelFileElement> iterator() {
                    return new Iterator<IModelFileElement>() {

                        @Override
                        public boolean hasNext() {
                            return false;
                        }

                        @Override
                        public IModelFileElement next() {
                            return null;
                        }
                    };
                }
            };
        }
    }

    /**
     * Gets the project element that contains the specified path.
     *
     * @param path the path to find the project for
     * @return null (delegated to EclipseModelProjectElement)
     */
    @Override
    public IModelProjectElement getProjectForPath(String path) {
        return null;
    }

    /**
     * Gets the project element that contains the specified model element.
     *
     * @param element the model element to find the project for
     * @return null (delegated to EclipseModelProjectElement)
     */
    @Override
    public IModelProjectElement getProjectForElement(IModelElement element) {
        return null;
    }
}
