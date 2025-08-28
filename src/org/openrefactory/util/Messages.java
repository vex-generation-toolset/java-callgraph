/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized strings.
 * 
 * @since 2.0
 */
public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "org.eclipse.rephraserengine.core.util.messages"; //$NON-NLS-1$

    public static String Spawner_ProcessExitedAbnormally;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
