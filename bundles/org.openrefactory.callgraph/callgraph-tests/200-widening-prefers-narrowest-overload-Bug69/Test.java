// Issue 69
// Two candidates both accept a char by widening. Java picks the most specific applicable
// formal, so the callee must be f(int) and not f(long). Before the tie break in isABetterMatch,
// NUMERIC_TYPE_AUTOCOVERT had no opinion and whichever candidate the method map yielded first won.

public class Z {
    static void f(int x) {
    }

    static void f(long x) {
    }

    void foo(char c) {
        f(c);
    }
}

/*$$$$$ Z#foo(char), 1,
        14,9, 1, Z.f(int)
*/
