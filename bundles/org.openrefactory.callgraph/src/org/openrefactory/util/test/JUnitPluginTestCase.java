/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.openrefactory.util.StringUtility;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModel;

import org.openrefactory.util.ASTNodeUtility;

/**
 * Base class for a test case that runs tests on a bunch of files and also uses the Eclipse 
 * runtime workspace.
 * <p>
 * This class is an extension of org.eclipse.rephraserengine.testing.junit3.WorkspaceTestCase
 * so that it loads the test project in a temporary workspace properly
 * 
 * @author Munawar Hafiz
 */
public class JUnitPluginTestCase extends WorkspaceTestCase {
    // Original file that contains a marker for the test
    protected final File originalFileContainingMarker;
    // Copied file to the virtual workspace. This becomes the main project file
    // And is accessed by the subclasses
    protected File file;
    protected final FilenameFilter filenameFilter;
    protected String description;
    // Maps absolute path of a file and the IFile handle
    protected Map<String, IFile> files;
    
    public JUnitPluginTestCase(File fileContainingMarker,
        FilenameFilter filenameFilter) throws Exception {
        this(fileContainingMarker, null, filenameFilter);
    }

    public JUnitPluginTestCase(File fileContainingMarker,
        org.openrefactory.analysis.vpg.JavaVPG vpg,
        FilenameFilter filenameFilter) throws Exception {
        super("test", vpg); //$NON-NLS-1$
        this.originalFileContainingMarker = fileContainingMarker;
        this.filenameFilter = filenameFilter;
    }
    
    /*
     * Method to create initial setup. Called by TestCase#runBare method of JUnit API
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        if (vpg != null)
        {
            vpg.releaseAllASTs();
        }
        
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        // Initialize work space and clear all ASTs
        IWorkspaceRoot root = workspace.getRoot();
        Model.getInstance().deinitialize();
        root.delete(true, true, null);
        // Copy all the files in the project directory into a temp directory 
        // and conduct the calculations there
        String rootPath = root.getRawLocation().toOSString();
        File rootFile = new File(rootPath);
        String projectPath = rootFile.getAbsolutePath() + File.separator + "TestProject";
        
        // Assume that the test project is under a directory whose name starts with a 0
        File temp = originalFileContainingMarker.getAbsoluteFile();
        temp = TestUtility.getTestContainerDirectory(temp);
        // Found the directory to test, now copy all files to the project in workspace 
        File absoluteFile = originalFileContainingMarker.getAbsoluteFile();
        file = TestUtility.copyFolder(temp, new File(projectPath + File.separator + absoluteFile.getName()), absoluteFile);
        
        this.description = file.getName();
        
		// Create Eclipse File System Model
		try {
			Model.useModel(new EclipseModel(new File(projectPath + File.separator + file.getName())));
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
        this.files = importFiles();
    }
    
    protected Map<String, IFile> importFiles() throws Exception
    {
        Map<String, IFile> result = importAllFiles(file.getParentFile().getAbsoluteFile(), filenameFilter);
        return result;
    }
    
    /**
     * Find an AST node of a type specified by the nodeType at the position identified
     * by the the line number, the offset for start position in that line, and the length of the token
     */
    protected <T extends ASTNode> T findNode(CompilationUnit ast, Class<T> nodeType, int targetLine, int targetCol,
        int targetLength) {
        int line = 1;
        int col = 1;
        String sourceCode = ast.toString();
        for (int offset = 0, totalLength = sourceCode.length(); offset < totalLength; offset++) {
            if (line == targetLine && col == targetCol) {
                return findNode(ast, nodeType, offset, targetLength);
            } else {
                if (sourceCode.charAt(offset) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }
        }
        return null;
    }
    
    /**
     * Find an AST node of a type specified by the nodeType at the position identified
     * by the offset from the start position denoting where the token starts, and the length of the token
     */
    protected <T extends ASTNode> T findNode(CompilationUnit ast, Class<T> nodeType, int offset, int length) {
        return ASTNodeUtility.findNodeForTestSuite(nodeType, ast, offset, length);
    }
    
    protected String readWorkspaceFile(String filename) throws IOException, CoreException {
        FileInputStream in = new FileInputStream(filename);
        String contents = StringUtility.read(in);
        in.close();
        return contents;
    }
}
