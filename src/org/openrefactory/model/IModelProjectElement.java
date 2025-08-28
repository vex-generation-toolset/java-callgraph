/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

/**
 * Interface defining the contract for a project element in the model hierarchy.
 *
 * <p>This interface extends IModelElement to represent project-specific functionality.</p>
 */
public interface IModelProjectElement extends IModelElement {

    String getRelativePathTo(IModelElement element);
}
