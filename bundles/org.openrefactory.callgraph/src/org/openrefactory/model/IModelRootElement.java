/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

import java.io.IOException;

/**
 * Interface defining the contract for a root element in the model hierarchy.
 *
 * <p>This interface extends IModelElement to represent the root of the project structure.
 * It overrides the getChildren method to return project elements specifically.</p>
 */
public interface IModelRootElement extends IModelElement {
    
    /**
     * Gets the project elements that are direct children of this root element.
     *
     * @return an iterable collection of project elements
     * @throws IOException if an I/O error occurs while accessing children
     */
    @Override
    Iterable<? extends IModelProjectElement> getChildren() throws IOException;
}
