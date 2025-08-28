/*******************************************************************************
 * Copyright (c) 2011 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.openrefactory.util;

/**
 * Utility methods for JUnit/testing.
 * 
 * @author Jeff Overbey
 * 
 * @since 3.0
 */
public final class TestUtility
{
    private TestUtility() {;}
    
    /** @return true if Eclipse is being run via the JUnit Plug-in Test runner */
    public static boolean runningJUnitPluginTests()
    {
        String app = System.getProperty("eclipse.application"); //$NON-NLS-1$
        return app != null && app.toLowerCase().contains("junit"); //$NON-NLS-1$
    }
}
