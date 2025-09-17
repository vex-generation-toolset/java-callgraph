/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph.method;

/**
 * Denotes the kind of info stored in each position of the method identity bits.
 *
 * <p>This enum defines the various characteristics and modifiers that can be associated
 * with a method in the call graph analysis system. Each enum value represents a specific
 * bit position in a {@link java.util.BitSet} that stores method metadata.</p>
 *
 * <p>The bits are used to efficiently categorize methods for analysis purposes,
 * allowing quick identification of method characteristics without complex objec
 * comparisons. This is particularly useful during call graph construction and
 * method resolution.</p>
 *
 * <p>The bit positions are used in conjunction with the {@link MethodIdentity} class
 * to store and retrieve method characteristics using the ordinal values of these
 * enum constants.</p>
 *
 * @author Munawar Hafiz
 */
public enum MethodIdentityBits {

    /**
     * Indicates that the method has no implementation body.
     *
     * <p>This bit is set for methods that lack a concrete implementation, including:</p>
     * <ul>
     *   <li>Abstract methods in abstract classes</li>
     *   <li>Interface methods (prior to Java 8 default methods)</li>
     *   <li>Native methods</li>
     *   <li>Default constructors with no explicit body</li>
     * </ul>
     *
     * Also the frmaework methods, e.g., the Android Activity Lifecycle method
     * does not have a body.
     *
     * <p>Bodyless methods often require special handling during call graph analysis
     * as they may be overridden in subclasses or implemented by implementing classes.</p>
     */
    BODYLESS,

    /**
     * Indicates that the method is a constructor.
     *
     * <p>Constructor methods have special semantics in Java and are treated
     * differently during call graph analysis. They are responsible for objec
     * initialization and have specific calling patterns.</p>
     *
     * <p>This bit helps distinguish constructors from regular methods when
     * analyzing method calls and building the call graph.</p>
     */
    CONSTRUCTOR,

    /**
     * Indicates that the method is static.
     *
     * <p>Static methods belong to the class rather than instances and canno
     * be overridden. They are resolved at compile time and have differen
     * calling semantics compared to instance methods.</p>
     *
     * <p>This bit is important for determining method resolution strategy
     * and call graph construction.</p>
     */
    STATIC,

    /**
     * Indicates that the method is possibly polymorphic.
     *
     * <p>A method is considered possibly polymorphic if it could potentially
     * be overridden in subclasses, making it eligible for dynamic dispatch.
     * This includes:</p>
     * <ul>
     *   <li>Abstract methods without bodies</li>
     *   <li>Interface methods</li>
     *   <li>Non-final instance methods that participate in polymorphic call relationships</li>
     * </ul>
     *
     * <p>This bit is crucial for call graph analysis as polymorphic methods
     * require special handling during method resolution.</p>
     */
    POSSIBLY_POLYMORPHIC,

    /**
     * Indicates that the method is virtual.
     *
     * <p>There are special methods in frameworks that connect method calls
     * but they are not explicitly declared.</p>
     *
     * <p>This bit helps identify methods in frameworks.</p>
     */
    VIRTUAL,

    /**
     * Indicates that the method is a default method in an interface.
     *
     * <p>Default methods in interfaces (introduced in Java 8) provide a
     * default implementation that can be overridden by implementing classes.
     * They bridge the gap between abstract interfaces and concrete implementations.</p>
     *
     * <p>This bit is important for understanding method resolution in
     * interface hierarchies and call graph construction.</p>
     */
    DEFAULT,
}
