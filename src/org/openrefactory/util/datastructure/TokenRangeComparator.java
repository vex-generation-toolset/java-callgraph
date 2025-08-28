/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.datastructure;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for TokenRange objects refactored into one place Comparison is done based on the pre-order
 * traversal counter set by structural analysis component
 *
 * @author Munawar Hafiz
 */
public class TokenRangeComparator implements Comparator<TokenRange>, Serializable {

    /** */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(TokenRange o1, TokenRange o2) {
        if (o1.getFileName().equals(o2.getFileName())) {
            if (o1.getOffset() < o2.getOffset()) {
                return -1;
            } else if (o1.getOffset() > o2.getOffset()) {
                return 1;
            } else {
                if (o1.getLength() < o2.getLength()) {
                    return -1;
                } else if (o1.getLength() > o2.getLength()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        } else {
            return o1.getFileName().compareTo(o2.getFileName());
        }
    }
}
