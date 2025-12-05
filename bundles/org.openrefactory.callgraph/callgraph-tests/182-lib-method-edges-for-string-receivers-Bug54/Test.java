// Issue 54
// Test for library instance method edges for String.isEmpty inside a conditional

public class Name {
    public static void checkEmpty() {
        String s = "";
        if (s.isEmpty()) {
            // no operation
        }
    }
}

/*$$$$$ Name.checkEmpty(),
  1,
  java.lang.String#isEmpty()
*/
