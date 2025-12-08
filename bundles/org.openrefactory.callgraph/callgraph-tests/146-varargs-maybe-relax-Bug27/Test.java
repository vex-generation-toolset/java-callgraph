// Issue 27
// Overloaded method with and without varargs

public class Example7 {
    static void log(String format, Object... args) {
        System.out.printf(format + "%n", args);
    }

    public static void main(String[] args) {
        log("Hello %s, you have %d new messages.", "Munawar", 3);
    }
}

/*$$$$$ Example7.main(String[]), 1,
   		10,9, 1, Example7.log(String@@@Object[]...)
*/
