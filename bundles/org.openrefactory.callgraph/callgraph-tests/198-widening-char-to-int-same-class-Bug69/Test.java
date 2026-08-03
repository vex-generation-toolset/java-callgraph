// Issue 69
// A same class static call whose char arguments widen into int formals.
// JLS 5.1.2 makes this legal Java, but the numeric conversion gate in the matcher left char out,
// so allowedConversion was never consulted and the only candidate came back a hard mismatch.

public class Z {
    static boolean checkDate(char y0, char y1, int h, int m) {
        return y0 == y1 && h == m;
    }

    boolean foo(char y0, char y1, char h, char m) {
        return checkDate(y0, y1, h, m);
    }
}

/*$$$$$ Z#foo(char@@@char@@@char@@@char), 1,
        12,16, 1, Z.checkDate(char@@@char@@@int@@@int)
*/
