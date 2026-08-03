// Issue 69
// Accepting char widening must not let a widened candidate displace an exact one.
// g(char) is an exact match so it short circuits as TriBool.
// True before g(int) is ever considered as a partial match.

public class Z {
    static void g(char c) {
    }

    static void g(int i) {
    }

    void foo(char c) {
        g(c);
    }
}

/*$$$$$ Z#foo(char), 1,
        14,9, 1, Z.g(char)
*/
