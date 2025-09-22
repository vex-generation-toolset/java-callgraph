/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for calculating various types of hash values. Currently, only has API to calculate SHA1 hash
 *
 * @author Munawar Hafiz
 */
public class HashUtility {
    /** Private constructor to prevent instantiation */
    private HashUtility() {}

    /** Generate hash following SHA-1 algorithm */
    public static String generateSHA1(String message) {
        return generate(message, "SHA-1");
    }

    /**
     * Generate the hash value following the algorithm specified
     *
     * @param message The input for the hash generator
     * @param algorithm The algorithm to use.
     * @return The hash string
     */
    private static String generate(String message, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
            return toHexString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
        } catch (UnsupportedEncodingException e) {
        }
        return "";
    }

    /**
     * Converts a byte array to a hex string
     *
     * @param arrayBytes The input byte array
     * @return The hex string
     */
    private static String toHexString(byte[] arrayBytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arrayBytes.length; i++) {
            builder.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }
}
