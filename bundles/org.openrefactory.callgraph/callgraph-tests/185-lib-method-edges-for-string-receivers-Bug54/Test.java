// Issue 54
// Test for library instance method edges in String.format

class Name {
    public static void formatVarArgs() {
        String result = String.format("User-%s-%d", "Alice", 42);
    }
}

public class Demo {
    public static void fooFormatVarArgs() {
        Name.formatVarArgs();
    }
}

/*$$$$$ Demo.fooFormatVarArgs(),
  1,
  Name.formatVarArgs()
*/

/*$$$$$ Name.formatVarArgs(),
  1,
  java.lang.String.format(String@@@String@@@int)
*/
