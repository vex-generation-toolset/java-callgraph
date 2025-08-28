/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.openrefactory.util.StringUtility;

import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.cli.ConfigurationManager;

import junit.framework.TestCase;

/**
 * Base class for a test case that runs tests on a bunch of files without invoking the 
 * runtime workspace.
 * <p>
 * This class is a modification of org.eclipse.rephraserengine.testing.junit3.WorkspaceTestCase
 * so that it runs in a headless manner
 * 
 * @author Munawar Hafiz
 * @author Ben Madany
 * 
 */
public class JUnitCommandLineTestCase extends TestCase {
    /** Used to give each project a new name */
    protected static int n = 0;
    
    static {
        ConfigurationManager.loadConfigForTest();
    }
    
    public JUnitCommandLineTestCase()
    {
        super();
    }

    public JUnitCommandLineTestCase(String name)
    {
        super(name);
    }
    
    /*
     * Method to create initial setup. Called by TestCase#runBare method of JUnit API
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    protected Map<String, File> importAllFiles(File directory, FilenameFilter filenameFilter) throws Exception
    {
        Map<String, File> filesImported = new TreeMap<String, File>();
        for (File file : directory.listFiles(filenameFilter))
        {
            filesImported.put(file.getName(), file);
        }
        return filesImported;
    }
    
    protected File importFile(String name, String contents) throws IOException {
        File file = File.createTempFile(name, "temp");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        out.write(contents.getBytes());
        out.close();
        return file;
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
    
    protected IDocument createDocument(File file) throws IOException, CoreException {
        return new Document(readWorkspaceFile(file.getAbsolutePath()));
    }
    
    protected String readWorkspaceFile(String filename) throws IOException, CoreException {
        FileInputStream in = new FileInputStream(filename);
        String contents = StringUtility.read(in);
        in.close();
        return contents;
    }
}
