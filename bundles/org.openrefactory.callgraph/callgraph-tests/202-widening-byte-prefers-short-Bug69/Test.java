// Issue 69
// The narrowest-widening tie break is not char specific. A byte actual widens into both
// short and long, and short is the more specific applicable formal.

public class Z {
    static void h(short s) {
    }

    static void h(long l) {
    }

    void foo(byte b) {
        h(b);
    }
}

/*$$$$$ Z#foo(byte), 1,
        13,9, 1, Z.h(short)
*/
