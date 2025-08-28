/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.datastructure;

/**
 * A boolean class with three states - True, False, Maybe and their various operations
 *
 * @author Mohammad Rafid Ul Islam
 */
public enum TriBool {
    True,
    False,
    MayBe;

    public boolean isTrue() {
        return this == True;
    }

    public boolean isFalse() {
        return this == False;
    }

    public boolean isMayBe() {
        return this == MayBe;
    }

    /**
     * calculates logical not
     *
     * @return TriBool
     */
    public TriBool not() {
        if (isTrue()) {
            return False;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return True;
        }
    }

    /**
     * calculates bitwise not
     *
     * @return TriBool
     */
    public TriBool complement() {
        if (isTrue()) {
            return False;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return MayBe;
        }
    }

    /**
     * calculates logical and
     *
     * @return TriBool
     */
    public TriBool and(TriBool other) {
        if (isTrue()) {
            return other;
        } else if (isMayBe()) {
            return other.isFalse() ? False : MayBe;
        } else {
            return False;
        }
    }

    /**
     * calculates bitwise and
     *
     * @return TriBool
     */
    public TriBool bitwiseAnd(TriBool other) {
        if (isTrue()) {
            return True;
        } else if (isMayBe()) {
            return other.isTrue() ? True : MayBe;
        } else {
            return other.isFalse() ? MayBe : other;
        }
    }

    /**
     * calculates logical or
     *
     * @return TriBool
     */
    public TriBool or(TriBool other) {
        if (isTrue()) {
            return other.isTrue() ? True : MayBe;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return other.isFalse() ? False : MayBe;
        }
    }

    /**
     * calculates bitwise or
     *
     * @return TriBool
     */
    public TriBool bitwiseOr(TriBool other) {
        if (isTrue()) {
            return other;
        } else if (isMayBe()) {
            return other.isFalse() ? False : MayBe;
        } else {
            return False;
        }
    }

    /**
     * calculates bitwise xor
     *
     * @return TriBool
     */
    public TriBool xor(TriBool other) {
        if (isTrue()) {
            return other;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return other.isTrue() ? False : MayBe;
        }
    }

    /**
     * calculates bitwise left shift
     *
     * @return TriBool
     */
    public TriBool leftShift(TriBool other) {
        if (isTrue()) {
            return True;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return other.isTrue() ? False : MayBe;
        }
    }

    /**
     * calculates bitwise right shift
     *
     * @return TriBool
     */
    public TriBool rightShift(TriBool other) {
        if (isTrue()) {
            return True;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return other.isTrue() ? False : MayBe;
        }
    }

    /**
     * calculates arithmetic addition/subtraction
     *
     * @return TriBool
     */
    public TriBool addSub(TriBool other) {
        if (isTrue()) {
            return other;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return other.isTrue() ? False : MayBe;
        }
    }

    /**
     * calculates arithmetic multiplication
     *
     * @return TriBool
     */
    public TriBool mult(TriBool other) {
        if (isTrue() || other.isTrue()) {
            return True;
        } else if (isMayBe() || other.isMayBe()) {
            return MayBe;
        } else {
            return False;
        }
    }

    /**
     * calculates arithmetic division
     *
     * @return TriBool
     */
    public TriBool div(TriBool other) {
        if (isTrue()) {
            return other.isTrue() ? True : MayBe;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return MayBe;
        }
    }

    /**
     * calculates arithmetic increment/decrement
     *
     * @return TriBool
     */
    public TriBool incDec() {
        if (isTrue()) {
            return False;
        } else {
            return MayBe;
        }
    }

    /**
     * calculates unary plus
     *
     * @return TriBool
     */
    public TriBool unaryPlus() {
        if (isTrue()) {
            return True;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return False;
        }
    }

    /**
     * calculates unary plus
     *
     * @return TriBool
     */
    public TriBool unaryMinus() {
        if (isTrue()) {
            return True;
        } else if (isMayBe()) {
            return MayBe;
        } else {
            return False;
        }
    }

    /**
     * calculates result of comparison operators
     *
     * @return TriBool
     */
    public TriBool comparison(TriBool other, String op) {
        switch (op) {
            case "<":
                if (other.isTrue() || (this.isFalse() && other.isFalse())) return False;
                else return MayBe;
            case "<=":
                return (this.isFalse() && other.isFalse()) ? False : MayBe;
            case ">":
                if (other.isTrue() || (this.isFalse() && other.isFalse())) return False;
                else return MayBe;
            case ">=":
                return (this.isFalse() && other.isFalse()) ? False : MayBe;
            case "==":
                return other;
            case "!=":
                return other.isTrue() ? False : MayBe;
            default:
                return MayBe;
        }
    }

    /**
     * calculates result of comparison operators
     *
     * @return TriBool
     */
    public TriBool comparison(double other, String op) {
        switch (op) {
            case "<":
                if (other > 0) {
                    return MayBe;
                } else {
                    return False;
                }
            case "<=":
                if (other >= 0) {
                    return MayBe;
                } else {
                    return False;
                }
            case ">":
                if (other < 0) {
                    return MayBe;
                } else {
                    return False;
                }
            case ">=":
                if (other <= 0) {
                    return MayBe;
                } else {
                    return False;
                }
            case "==":
                if (other == 0) {
                    return True;
                } else {
                    return False;
                }
            case "!=":
                if (other == 0) {
                    return False;
                } else {
                    return True;
                }
            default:
                return MayBe;
        }
    }
}
