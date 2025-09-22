/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.progressreporter;

import org.openrefactory.util.logger.ILogger;

public class PrefixLoggerProgressReporter implements IProgressReporter {
    private String prefix;
    private ILogger logger;

    public PrefixLoggerProgressReporter(String prefix, ILogger logger) {
        this.prefix = prefix;
        this.logger = logger;
    }

    @Override
    public void showProgress(String message) {
        String prefixedMessage = prefix + ": " + message;
        logger.log(prefixedMessage);
    }
}
