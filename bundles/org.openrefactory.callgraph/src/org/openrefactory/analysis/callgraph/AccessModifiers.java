/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Stores info of the field access modifiers in a BitSet and provides related utility functions.
 * 
 * <p>This class uses a 4-bit encoding scheme to efficiently represent Java access modifiers:</p>
 * <ul>
 *   <li><strong>Lower 2 bits (bits 0-1):</strong> Access level
 *     <ul>
 *       <li>00 - private</li>
 *       <li>01 - public</li>
 *       <li>10 - protected</li>
 *       <li>11 - default (package-private)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Upper 2 bits (bits 2-3):</strong> Additional modifiers
 *     <ul>
 *       <li>00 - regular</li>
 *       <li>01 - final</li>
 *       <li>10 - static</li>
 *       <li>11 - Not used</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <p>Modifiers can be combined by adding their constant values. For example:
 * <code>public static final = 1 + 8 + 4 = 13</code> (binary: 1101)</p>
 *
 * @author Md. Rishadur Rahman
 */
public class AccessModifiers {

    // 4 bits to represent modifiers
    // The lower two bits.
    //    00 - private
    //    01 - public
    //    10 - protected
    //    11 - default
    // The higher two bits.
    //    00 - regular
    //    01 - final
    //    10 - static
    //    11 - Not used
    // Constant values related to the modifiers
    // we can add them to get a combined modifier value
    // For example,
    //      public static final  =>  1 + 4 + 8  => 13  => (bitset 1101)
    
    /** Private access modifier (binary: 0000) */
    public static final int PRIVATE = 0;
    
    /** Public access modifier (binary: 0001) */
    public static final int PUBLIC = 1;
    
    /** Protected access modifier (binary: 0010) */
    public static final int PROTECTED = 2;
    
    /** Default (package-private) access modifier (binary: 0011) */
    public static final int DEFAULT = 3;
    
    /** Final modifier (binary: 0100) */
    public static final int FINAL = 4;
    
    /** Static modifier (binary: 1000) */
    public static final int STATIC = 8;

    // Access modifiers for fields
    // We do not need to clone them because all accesses are readonly
    private static final List<BitSet> modifiers = new ArrayList<BitSet>(16);
    
    // Generating an array of BitSets for all possible 16 values with 4 bit,
    // whenever we get a modifier value we will use it as index and get
    // the bit set at that index and use accordingly. So the client objects
    // will not need to store any bitset, they will just hold a byte value
    // and call the isStatic(), isFinal(), isPrivate() .etc  utility methods
    // with that byte value.
    static {
        for (int i = 0; i < 16; i++) {
            modifiers.add(BitSet.valueOf(new byte[] {(byte) i}));
        }
    }

    /**
     * Checks if the given modifier value represents a static field or method.
     * 
     * <p>The static modifier is encoded in bit 3 (the highest bit) of the 4-bit value.
     * A value is considered static if bit 3 is set to 1.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a static modifier, {@code false} otherwise
     */
    public static boolean isStatic(byte value) {
        // The static is packed such that it will set the
        // highest bit to be 1.
        return modifiers.get(value).get(3);
    }

    /**
     * Checks if the given modifier value represents a final field or method.
     * 
     * <p>The final modifier is encoded in bit 2 of the 4-bit value.
     * A value is considered final if bit 2 is set to 1.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a final modifier, {@code false} otherwise
     */
    public static boolean isFinal(byte value) {
        // The final is packed such that it will set the
        // 3rd bit to be 1.
        return modifiers.get(value).get(2);
    }

    /**
     * Checks if the given modifier value represents a public access level.
     * 
     * <p>The public modifier is encoded in the lowest two bits (bits 0-1) as "01".
     * A value is considered public if bit 0 is set to 1 and bit 1 is set to 0.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a public access level, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the value is outside the valid range (0-15)
     */
    public static boolean isPublic(byte value) {
        BitSet modifier = modifiers.get(value);
        // A public modifier will have 01 in the lowest two bits
        return modifier.get(0) && !modifier.get(1);
    }

    /**
     * Checks if the given modifier value represents a private access level.
     * 
     * <p>The private modifier is encoded in the lowest two bits (bits 0-1) as "00".
     * A value is considered private if both bit 0 and bit 1 are set to 0.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a private access level, {@code false} otherwise
     */
    public static boolean isPrivate(byte value) {
        BitSet modifier = modifiers.get(value);
        // A private modifier will have 00 in the lowest two bits
        return (!modifier.get(0) && !modifier.get(1));
    }

    /**
     * Checks if the given modifier value represents a protected access level.
     * 
     * <p>The protected modifier is encoded in the lowest two bits (bits 0-1) as "10".
     * A value is considered protected if bit 0 is set to 0 and bit 1 is set to 1.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a protected access level, {@code false} otherwise
     */
    public static boolean isProtected(byte value) {
        BitSet modifier = modifiers.get(value);
        // A protected modifier will have 10 in the lowest two bits
        return (!modifier.get(0) && modifier.get(1));
    }

    /**
     * Checks if the given modifier value represents a default (package-private) access level.
     * 
     * <p>The default modifier is encoded in the lowest two bits (bits 0-1) as "11".
     * A value is considered default if both bit 0 and bit 1 are set to 1.</p>
     * 
     * @param value the modifier value to check (0-15)
     * @return {@code true} if the value represents a default access level, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the value is outside the valid range (0-15)
     */
    public static boolean isDefault(byte value) {
        BitSet modifier = modifiers.get(value);
        // A default modifier will have 11 in the lowest two bits
        return (modifier.get(0) && modifier.get(1));
    }
}
