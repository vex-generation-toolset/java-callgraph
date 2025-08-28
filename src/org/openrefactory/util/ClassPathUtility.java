/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Container class for the utility methods to generate the .classpath file for the project to analyze with icr.
 *
 * @author Rishadur Rahman
 */
public class ClassPathUtility {

    /**
     * Generate the .classpath file needed to analyze a project with icr
     *
     * @param rootPathStr the path of the root directory of the project being analyzed. return true if successful, false
     *     otherwise
     */
    public static boolean createClassPath(String rootPathStr) {
        Path rootPath = Paths.get(rootPathStr);
        try {
            Set<String> targetFiles = DirectoryScanner.scan(rootPath);
            writeToXmlFile(rootPath, targetFiles);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Generate the actual class path file
     *
     * @param rootPath the path of the project root directory where the file will be generated.
     * @param targetFiles the set of paths of the .java and .jar files to add to class path.
     */
    private static void writeToXmlFile(Path rootPath, Set<String> targetFiles)
            throws TransformerException, ParserConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        // root element
        Element rootElement = doc.createElement("classpath");
        doc.appendChild(rootElement);
        // Add the elements for the target file paths
        for (String path : targetFiles) {
            String kind = path.endsWith(".jar") ? "lib" : "src";
            Element entry = doc.createElement("classpathentry");
            entry.setAttribute("kind", kind);
            entry.setAttribute("path", path);
            rootElement.appendChild(entry);
        }
        // Add the special jre container entry
        Element entry = doc.createElement("classpathentry");
        entry.setAttribute("kind", "con");
        entry.setAttribute("path", "org.eclipse.jdt.launching.JRE_CONTAINER");
        rootElement.appendChild(entry);
        // Generate the actual class path file
        File output = rootPath.resolve(".classpath").toFile();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(output));
    }

    /** Visitor class to collect and populate the paths of all the .java and .jar files. */
    private static class DirectoryScanner extends SimpleFileVisitor<Path> {
        private final Path rootDirPath;
        private final Set<String> targetFiles;

        private DirectoryScanner(Path rootDirPath) {
            this.rootDirPath = rootDirPath;
            this.targetFiles = new HashSet<>();
        }

        public static Set<String> scan(Path rootPath) throws IOException {
            DirectoryScanner scanner = new DirectoryScanner(rootPath);
            Files.walkFileTree(rootPath, scanner);
            return scanner.targetFiles;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(".jar")) {
                targetFiles.add(file.toAbsolutePath().toString());
            } else if (fileName.endsWith(".java")
                    && !fileName.equals("module-info.java")
                    && !fileName.equals("package-info.java")) {
                String srcFolder = getSrcFolderPath(file);
                if (srcFolder != null) {
                    targetFiles.add(srcFolder);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        /**
         * For .java files, we add the relative path of the folder where the package begins.
         *
         * <pre>
         *  For example,
         *      relative file path: 'project/src/com/example/Demo.java'
         *      package: 'com.example'
         *  Excluding the filename and package parts,
         *      output: 'project/src'
         * </pre>
         *
         * @param javaFilePath the path of the java file
         * @return the relative path of the folder where the package begins
         */
        private String getSrcFolderPath(Path javaFilePath) throws IOException {
            File javaFile = javaFilePath.toFile();
            if (!javaFile.canRead()) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
                String pkgStmt;
                while ((pkgStmt = reader.readLine()) != null && !pkgStmt.startsWith("package")) {
                    // Read and skip until we get the package statement
                }
                if (pkgStmt == null) {
                    // All the lines are read but no package statement is found, skip.
                    return null;
                }
                // First get the package name from the package statement.
                // The 'length()-1' is to skip the semicolon.
                String pkgName = pkgStmt.substring("package".length(), pkgStmt.length() - 1)
                        .trim();
                // Get the folder path from the package name
                String pkgElemPath = pkgName.replace('.', File.separatorChar);
                // Get the relative path of the container folder
                String folderPath =
                        rootDirPath.relativize(javaFilePath.getParent()).toString();
                // By conventions, the path from the package name should match the actual path,
                // we will skip the directories from the package elements for this case.
                if (folderPath.endsWith(pkgElemPath)) {
                    return folderPath.substring(0, folderPath.length() - pkgElemPath.length());
                } else {
                    // The path from package doesn't match the path of actual container folder.
                    return folderPath + File.separator;
                }
            }
        }
    }
}
