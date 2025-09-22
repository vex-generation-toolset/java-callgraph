/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.progressreporter;

public final class NullProgressReporter implements IProgressReporter {

    @Override
    public void showProgress(String message) {}
}
