/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.vpg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.openrefactory.model.IModel;
import org.openrefactory.model.IModelFileElement;
import org.openrefactory.model.Model;
import org.openrefactory.model.eclipse.EclipseModelFileElement;
import org.openrefactory.util.datastructure.NeoLRUCache;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.progressreporter.IProgressReporter;
import org.openrefactory.util.progressreporter.NullProgressReporter;

/**
 * Entry class that keeps track of Java compilation units
 *
 * <p>This class manages Java AST (Abstract Syntax Tree) parsing and caching for compilation units.
 * It provides a singleton pattern for accessing ASTs and manages memory through an LRU cache.</p>
 *
 * @author Munawar Hafiz
 */
public final class JavaVPG {

    /** The AST cache, which provides access to ASTs and determines which files' ASTs are in memory. */
    protected NeoLRUCache<String, CompilationUnit> transientASTs;

    private static volatile JavaVPG instance = null;

    /**
     * Gets the singleton instance of JavaVPG for the specified directory.
     *
     * @param directory the directory to store the CVPG database
     * @return the JavaVPG instance
     */
    public static JavaVPG getInstance(String directory) {
        if (instance == null) instance = new JavaVPG(directory);
        return instance;
    }

    /**
     * Gets the singleton instance of JavaVPG using the current directory.
     *
     * @return the JavaVPG instance
     */
    public static JavaVPG getInstance() {
        if (instance == null) {
            System.err.println("WARNING: Using current directory to store CVPG database");
            instance = new JavaVPG(".");
        }
        return instance;
    }

    private JavaVPG(String directory) {
        transientASTs = new NeoLRUCache<>(100);
    }

    /**
     * Acquires an AST for the given file which will be garbage collected after no pointers to any of its nodes remain.
     *
     * @param filename the name of the file to acquire AST for
     * @return the compilation unit AST, or null if file is virtual or parsing fails
     */
    public final CompilationUnit acquireAST(String filename) {
        if (isVirtualFile(filename)) return null;

        CompilationUnit ast = null;

        if (transientASTs.contains(filename)) {
            ast = transientASTs.get(filename);
        }

        if (ast == null) {
            ast = parse(filename);
            if (ast != null) {
                transientASTs.cache(filename, ast);
            }
        }
        return ast;
    }

    /**
     * Parse a Java file
     *
     * @param filename the target filename to parse
     * @return a compilation unit representing the root of the AST content, or null if parsing fails
     */
    @SuppressWarnings("deprecation")
	protected CompilationUnit parse(String filename) {
        IModelFileElement fileElement = Model.getInstance().getFile(filename);
        if (fileElement == null) return null;
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        if (fileElement instanceof EclipseModelFileElement) {
            // If we are using eclipse model, then use the ICompilationUnit
            parser.setSource(((EclipseModelFileElement) fileElement).getCompilationUnit());
        } else {
            // Get the source from the source file
            File file = new File(fileElement.getFullPath());
            // Must set unit name for files that are read directly from file system
            // and not through a compilation unit
            // If we do not set unit name explicitly, type resolution will not
            // work even if we set resolve bindings to true
            parser.setUnitName(fileElement.getName());
            parser.setEnvironment(null, null, null, true);
            try {
                parser.setSource(readFileToString(file).toCharArray());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        // Protection against potential failure in parsing
        try {
            ASTNode node = parser.createAST(null);
            if (node instanceof CompilationUnit) {
                return (CompilationUnit) node;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Ensures AST parsing for all Java files using a null progress reporter.
     */
    public void ensureASTParsing() {
        ensureASTParsing(new NullProgressReporter());
    }

    /**
     * Ensures AST parsing for all Java files with progress reporting.
     *
     * @param progressReporter the progress reporter to use for status updates
     */
    public void ensureASTParsing(IProgressReporter progressReporter) {
        Set<String> filesInModel = new HashSet<>();

        // Used for reporting progress
        // Count the total number of files
        int totalFileCount = 0;
		for (@SuppressWarnings("unused")
		IModelFileElement file : Model.getInstance().getAllSourceFiles(IModel.JAVA_LANGUAGE)) {
			totalFileCount++;
		}

        int processedFileCount = 0;
        for (IModelFileElement file : Model.getInstance().getAllSourceFiles(IModel.JAVA_LANGUAGE)) {
            final String fullPath = file.getFullPath();
            filesInModel.add(fullPath);
            processedFileCount++;
            progressReporter.showProgress(
                    String.format("Indexing %s, File %d of %d", file.getName(), processedFileCount, totalFileCount));
        }

        progressReporter.showProgress("Indexing complete");
    }

    /**
     * Releases the AST for the given file, regardless of whether it was acquired as a permanent or transient AST.
     *
     * @param filename the filename whose AST should be released
     */
    public void releaseAST(String filename) {
        transientASTs.remove(filename);
    }

    /**
     * Releases all ASTs, regardless of whether they were acquired as transient and permanent ASTs.
     */
    public void releaseAllASTs() {
        transientASTs.clear();
    }

    /**
     * Pretty print an AST
     *
     * @param cu the AST to print
     * @return the pretty-printed AST string
     */
    public String prettyPrintAST(CompilationUnit cu) {
        if (cu == null) {
            return "";
        }
        return "";
        //        StringBuilder builder = new StringBuilder();
        //        if (cu.getTokenRange().get() != null) {
        //            for (JavaToken t : cu.getTokenRange().get()) {
        //                builder.append(t.getText());
        //            }
        //        }
        //        return builder.toString();
    }

    /**
     * Separates cached and uncached files from a file list.
     *
     * @param fileList the list of all files to separate
     * @return a pair of lists where the first contains cached files and the second contains uncached files
     */
    public Pair<List<IModelFileElement>, List<IModelFileElement>> separateCachedAndUncachedFiles(
            List<IModelFileElement> fileList) {
        List<IModelFileElement> cached = new ArrayList<>();
        List<IModelFileElement> unchached = new ArrayList<>();
        for (IModelFileElement file : fileList) {
            if (transientASTs.contains(file.getFullPath())) {
                cached.add(file);
            } else {
                unchached.add(file);
            }
        }
        return Pair.of(cached, unchached);
    }

    /**
     * Read file content into a string
     *
     * @param file the file to read from
     * @return the content of the file in string format
     * @throws IOException if an I/O error occurs
     */
    private String readFileToString(File file) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            char[] buf = new char[10];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }

            return fileData.toString();
        } catch (IOException e) {
            throw e;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Files & Resource Filtering
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Checks if the given filename refers to a virtual file.
     *
     * @param filename the filename to check (non-null)
     * @return true if the filename refers to a virtual file, false otherwise
     */
    private boolean isVirtualFile(String filename) {
        return false;
    }
}
