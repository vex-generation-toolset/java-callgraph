/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.test;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized strings.
 */
public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "com.eclipse.rephraserengine.testing.junit3.messages"; //$NON-NLS-1$

    public static String GeneralTestSuiteFromFiles_DirectoryNotFound;

    public static String GeneralTestSuiteFromFiles_NoTestFilesFoundInDirectory;

    public static String GeneralTestSuiteFromFiles_SomeOptionalTestFilesAreNotPresent;

    public static String GeneralTestSuiteFromFiles_UnableToFindDirectory;

    public static String GeneralTestSuiteFromMarkers_FileCannotBeRead;

    public static String GeneralTestSuiteFromMarkers_FileNotFound;

    public static String GeneralTestSuiteFromMarkers_NoMarkersFound;

    public static String GeneralTestSuiteFromMarkers_SomeOptionalTestFilesAreNotPresent;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
