// Issue 69
// Call-graph test with varargs overload.
// Two log methods and call `log(a, b)` should pick `log(long, long)`

public class Z {
    static void log(long a, long b) {
    }

    static void log(int a, int... rest) {
    }

    void foo(int a, long b) {
        log(a, b);
    }
}

/*$$$$$ Z#foo(int@@@long), 1,
        13,9, 1, Z.log(long@@@long)
*/
