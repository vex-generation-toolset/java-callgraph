// Issue 69
// Call-graph test with varargs overload.
// `log(a, b, c)` should choose the fixed `log(long, long, long)` and not the varargs overload.

public class Z {
    static void log(long a, long b, long c) {
    }

    static void log(int a, int b, int... rest) {
    }

    void foo(int a, int b, long c) {
        log(a, b, c);
    }
}

/*$$$$$ Z#foo(int@@@int@@@long), 1,
        13,9, 1, Z.log(long@@@long@@@long)
*/
