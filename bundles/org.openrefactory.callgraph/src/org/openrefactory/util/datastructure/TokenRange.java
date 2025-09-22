/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.datastructure;

import java.io.File;
import java.io.Serializable;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;

/**
 * A class containing the starting offset and length of a node
 *
 * <p>Making this class immutable as a node's token range can not change
 *
 * @author Mohammad Rafid Ul Islam
 */
public final class TokenRange implements Serializable, Comparable<TokenRange> {
    /** */
    private static final long serialVersionUID = 1L;

    private final int offset;
    private final int length;
    private final boolean isVariable;
    private final String fileName;

    public TokenRange(int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.isVariable = false;
        this.fileName = null;
    }

    public TokenRange(int offset, int length, String fileName) {
        this.offset = offset;
        this.length = length;
        this.isVariable = false;
        this.fileName = fileName;
    }

    public TokenRange(int offset, int length, boolean isVariable, String fileName) {
        this.offset = offset;
        this.length = length;
        this.isVariable = isVariable;
        this.fileName = fileName;
    }

    public boolean isVariable() {
        return isVariable;
    }

    public String getFileName() {
        return fileName;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public boolean encompasses(TokenRange other) {
        if (!other.getFileName().equals(fileName)) {
            return false;
        }

        if (other.getOffset() < this.getOffset()) {
            return false;
        }

        if (other.getOffset() == this.getOffset()) {
            return other.getLength() < this.getLength();
        }

        if ((other.getOffset() + other.getLength()) > (offset + length)) {
            return false;
        }

        return true;
    }

    public boolean containsRange(TokenRange other) {
        return other.getOffset() >= offset && other.getOffset() + other.getLength() <= offset + length;
    }

    @Override
    public TokenRange clone() {
        return new TokenRange(this.offset, this.length, isVariable, fileName);
    }
    /** Included fileName in hash code calculation. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + length;
        result = prime * result + offset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TokenRange other = (TokenRange) obj;
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
            return false;
        }
        if (length != other.length) {
            return false;
        }
        if (offset != other.offset) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (fileName != null) {
            builder.append(fileName.substring(fileName.lastIndexOf(File.separator) + 1));
            builder.append(", ");
        }
        builder.append(offset);
        builder.append(", ");
        builder.append(length);
        return builder.toString();
    }

    /**
     * Returns token range of a variable's declaration
     *
     * @param iNode
     * @param isVariable whether this token range belongs to a variable
     * @return TokenRange of the variable declaration
     */
    public static TokenRange getTokenRange(ILocalVariable iNode, boolean isVariable) {
        int offset, length;
        if (iNode != null) {
            offset = iNode.getNameRange().getOffset();
            length = iNode.getNameRange().getLength();
            TokenRange token = null;
            try {
                IResource iResource = iNode.getResource();
                String fileName = iResource.getRawLocation().toOSString();
                token = new TokenRange(offset, length, isVariable, fileName);
            } catch (Exception e) {
                // Do nothing
            }
            return token;
        }
        return null;
    }

    /**
     * Returns token range of a field's declaration
     *
     * @param iNode
     * @param isVariable whether this token range belongs to a variable
     * @return TokenRange of the field declaration
     */
    public static TokenRange getTokenRange(IMember iNode, boolean isVariable) {
        int offset, length;
        if (iNode != null) {
            try {
                offset = iNode.getNameRange().getOffset();
                length = iNode.getNameRange().getLength();
                TokenRange token = null;
                try {
                    IResource iResource = iNode.getResource();
                    String fileName = iResource.getRawLocation().toOSString();
                    token = new TokenRange(offset, length, isVariable, fileName);
                } catch (Exception e) {
                    // Do nothing
                }
                return token;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public int compareTo(TokenRange o) {
        if (this.offset > o.getOffset()) {
            return 1;
        } else if (this.offset < o.getOffset()) {
            return -1;
        } else if (this.length > o.getLength()) {
            return 1;
        } else if (this.length < o.getLength()) {
            return -1;
        }
        return 0;
    }
}
