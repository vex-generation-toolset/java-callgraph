/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

/**
 * Various constants.
 *
 * @author Munawar Hafiz
 */
public class Constants {

    public static final String dummyFieldPrefix = "field__OR__";

    public static final String FIELDNAME_SEPARATOR = ":";

    public static final int INVALID_CLASS_HASH_INDEX = Integer.MIN_VALUE;

    public static final int INVALID_METHOD_HASH_INDEX = Integer.MIN_VALUE;

    // String place holder for Dummy class
    public static final String DUMMY_TYPE_CLASS = "Dummy__OR__Class";

    public static final int INVALID_TYPE_INDEX = -1;

    // Denotes a prefix of a type that is not in the source code
    public static final String LIB_TYPE = "LIB__OR__TYPE";

    // Special dummy root method considered as caller of all the callback
    // methods like android life-cycle methods, event listeners .etc
    public static final String SPECIAL_ROOT_HASH = "Special__OR__Root";

    // The special type name representation for the java.lang.Object class
    // It is created because we have analyze and manipulate the TypeConstraint that
    // have java.lang.Object differently than the other type constraints. Otherwise,
    // it may throw a StackOverflowError in the TypeCOnstraint operations.
    public static final String JAVA_LANG_OBJECT = "LIB__OR__TYPE::java.lang.Object";

    // The special type name representation for the java.lang.Enum class
    // It is the common base class for all enums in java
    public static final String JAVA_LANG_ENUM = "LIB__OR__TYPE::java.lang.Enum";

    // This is the type name for `null` literals TypeInfo
    public static final String NULL_OBJECT_TYPE = "LIB__OR__TYPE::null";

    // Type name for wild card type (?)
    public static final String WILDCARD_TYPE = "LIB__OR__TYPE::?";

    public static final String JAVA_LANG_CLASS = "LIB__OR__TYPE::java.lang.Class";

    // Sometimes, when the library files are not available, JDT cannot resolve the
    // Type binding and returns `null`. In such cases, to avoid getting a NullPointerException,
    // we shall use this special unresolved type to indicate such unresolved bindings.
    public static final String LIB_TYPE_UNRESOLVED = "LIB__OR__TYPE::UNRESOLVED";

    public static final String VOID = "void";

    public static final String JAVA_LANG_RUNNABLE = "LIB__OR__TYPE::java.lang.Runnable";
    public static final String JAVA_LANG_THREAD = "LIB__OR__TYPE::java.lang.Thread";
    // Special field for runnable target for a thread object
    public static final String THREAD_RUNNABLE_FIELD = "runnable__OR__target";
}
