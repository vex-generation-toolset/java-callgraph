// Issue 27
// Overloaded method with and without varargs

public class Example7 {
    static void log(String format, Object... args) {
        System.out.printf(format + "%n", args);
    }

    public static void main2(String[] args) {
        Object[] arr = {"A", "B", "C"};
        log("fool", arr);
    }
}


/*$$$$$ Example7.main2(String[]), 
   1,
   Example7.log(String@@@Object[]...)
 */

/*!!!!! Example7.main2(String[]),  1, 285, 16 */
