/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.model;

/**
 * Singleton class that manages the global model instance.
 *
 * <p>This class provides a centralized way to access and manage the current model
 * instance. It ensures that only one model is active at a time and handles
 * initialization of new models.</p>
 */
public final class Model {

    private static IModel instance = NullModel.getInstance();

    /**
     * Gets the current global model instance.
     *
     * @return the current model instance
     */
    public static IModel getInstance() {
        return instance;
    }
    
    private Model() {
    }

    /**
     * Sets the global model instance and initializes it.
     *
     * @param model the model to use, or null to use the null model
     */
    public static void useModel(IModel model) {
        instance = (model == null ? NullModel.getInstance() : model);
        instance.initialize();
    }    
}
