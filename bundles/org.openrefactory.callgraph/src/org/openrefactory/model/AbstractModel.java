/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Abstract base class implementing the IModel interface.
 *
 * <p>This class provides default implementations for common model operations including
 * listener management, file iteration, and basic model functionality. It implements
 * a breadth-first traversal strategy for file discovery.</p>
 */
public abstract class AbstractModel implements IModel {

    private final List<IModelListener> listeners = new LinkedList<IModelListener>();

    /**
     * Adds a listener to receive model change notifications.
     *
     * @param listener the listener to add
     */
    @Override
    public void addListener(IModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from receiving model change notifications.
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(IModelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Checks if this model represents a virtual file system.
     *
     * @return false by default (concrete implementations may override)
     */
    @Override
    public boolean isVirtualFileModel() {
        return false;
    }

    /**
     * Initializes the model and its associated resources.
     */
    @Override
    public void initialize() {}

    /**
     * Deinitializes the model and releases associated resources.
     */
    @Override
    public void deinitialize() {}

    /**
     * Gets all files in the model.
     *
     * @return an iterable collection of all file elements
     * @throws Error if an IOException occurs during file iteration
     */
    @Override
    public Iterable<IModelFileElement> getAllFiles() {
        return getAllSourceFiles(null);
    }

    /**
     * Gets all source files for a specific programming language.
     *
     * @param language the programming language identifier, or null for all languages
     * @return an iterable collection of source files for the specified language
     * @throws Error if an IOException occurs during file iteration
     */
    @Override
    public Iterable<IModelFileElement> getAllSourceFiles(final String language) {
        return new Iterable<IModelFileElement>() {
            @Override
            public Iterator<IModelFileElement> iterator() {
                return new FileIterator(getRoot(), language);
            }
        };
    }

    /**
     * Writes content to a file at the specified path.
     *
     * @param path the file path to write to
     * @param contents the content to write to the file
     * @throws IOException if writeFile is not supported by this model
     */
    @Override
    public void writeFile(String path, String contents) throws IOException {
        throw new IOException("writeFile not supported");
    }

    /**
     * Iterator implementation for traversing file elements in the model.
     *
     * <p>This inner class implements a breadth-first traversal strategy to iterate
     * through all file elements in the model hierarchy, optionally filtered by language.</p>
     */
    protected static final class FileIterator implements Iterator<IModelFileElement> {
        private final Queue<IModelElement> worklist;
        private final HashSet<String> seenBefore;
        private final String language;

        /**
         * Creates a new FileIterator starting from the specified root element.
         *
         * @param root the root element to start iteration from
         * @param language the language filter, or null for all languages
         */
        public FileIterator(IModelElement root, String language) {
            this.worklist = new ArrayDeque<IModelElement>();
            this.seenBefore = new HashSet<String>();
            this.language = language;
            worklist.add(root);
            ensureHeadOfQueueIsFile();
        }

        /**
         * Ensures the head of the queue contains a file element by processing container elements.
         */
        private void ensureHeadOfQueueIsFile() {
            while (!worklist.isEmpty() && !(worklist.peek() instanceof IModelFileElement)) {
                IModelElement nextElt = worklist.remove();
                try {
                    for (IModelElement child : nextElt.getChildren()) {
                        if ((language == null || matchesLanguageOrIsContainer(child))
                                && !seenBefore.contains(child.getFullPath())) {
                            worklist.add(child);
                            seenBefore.add(child.getFullPath());
                        }
                    }
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }

        /**
         * Checks if a child element matches the specified language or is a container.
         *
         * @param child the child element to check
         * @return true if the child matches the language or is a container, false otherwise
         */
        private boolean matchesLanguageOrIsContainer(IModelElement child) {
            if (child instanceof IModelFileElement) return ((IModelFileElement) child).matchesLanguage(language);
            else return true;
        }

        /**
         * Checks if there are more file elements to iterate over.
         *
         * @return true if there are more elements, false otherwise
         */
        @Override
        public boolean hasNext() {
            return !worklist.isEmpty();
        }

        /**
         * Gets the next file element in the iteration.
         *
         * @return the next file element
         */
        @Override
        public IModelFileElement next() {
            assert !worklist.isEmpty() && worklist.peek() instanceof IModelFileElement;
            final IModelFileElement nextElt = (IModelFileElement) worklist.remove();
            ensureHeadOfQueueIsFile();
            return nextElt;
        }

        /**
         * Removes the last returned element from the iteration.
         *
         * @throws UnsupportedOperationException as removal is not supported
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Prints the model structure to the specified output stream.
     *
     * @param out the output stream to print to
     */
    @Override
    public void printOn(PrintStream out) {
        getRoot().printOn(out);
    }

    @Override
    public String toString() {
        try {
            return getRoot().toString();
        } catch (Exception e) {
            return "";
        }
    }
}
