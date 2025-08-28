/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.openrefactory.analysis.vpg.JavaVPG;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.model.IModelFileElement;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModelFileElement;
import org.openrefactory.util.StringUtility;

import junit.framework.TestCase;

/**
 * Base class for a test case that imports files into the runtime workspace
 * This class is based on org.eclipse.cdt.core.tests.BaseTestFramework.
 * 
 * @author aniefer
 * @author Jeff Overbey
 * @author Munawar Hafiz
 */
public abstract class WorkspaceTestCase extends TestCase
{
    /** Used to give each project a new name */
    protected static int n = 0;
    
    protected final JavaVPG vpg;
    
    static {
        ConfigurationManager.loadConfigForTest();
    }
    
    public WorkspaceTestCase()
    {
        super();
        this.vpg = null;
    }
    
    public WorkspaceTestCase(String name)
    {
        super(name);
        this.vpg = null;
    }

    public WorkspaceTestCase(String name, JavaVPG vpg)
    {
        super(name);
        this.vpg = vpg;
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        if (vpg != null)
        {
            vpg.releaseAllASTs();
        }
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        if (vpg != null)
        {
            // To speed things up a bit and conserve memory...
            vpg.releaseAllASTs();
        }
    }
    
    protected IFile importFile(String fileName, String contents) throws Exception
    {
        IModelFileElement fileElem = Model.getInstance().getFile(fileName);
        IFile file = null;
        if (fileElem instanceof EclipseModelFileElement) {
            ICompilationUnit cu = ((EclipseModelFileElement)fileElem).getCompilationUnit();
            file = (IFile)cu.getCorrespondingResource();
        }
        if (file == null) {
            return null;
        }
        InputStream stream = new ByteArrayInputStream(contents.getBytes());

        if (file.exists())
            file.setContents(stream, false, false, new NullProgressMonitor());
        else
            file.create(stream, false, new NullProgressMonitor());

        return file;
    }
    
    protected Map<String, IFile> importAllFiles(File directory, FilenameFilter filenameFilter) throws Exception
    {
        Map<String, IFile> filesImported = new TreeMap<String, IFile>();
        for (File file : directory.listFiles(filenameFilter))
        {
            if (file.isDirectory()) {
                filesImported.putAll(importAllFiles(file, filenameFilter));
            } else {
                String path = file.getAbsolutePath();
                IFile thisFile = importFile(path, StringUtility.read(file));
                filesImported.put(path, thisFile);
            }
        }
        return filesImported;
    }
    
    protected String readWorkspaceFile(String filename) throws IOException, CoreException {
        return StringUtility.read(Model.getInstance().getFile(filename).getContents());
    }

    protected IDocument createDocument(IFile file) throws IOException, CoreException {
        return new Document(readWorkspaceFile(file.getName()));
    }
}
