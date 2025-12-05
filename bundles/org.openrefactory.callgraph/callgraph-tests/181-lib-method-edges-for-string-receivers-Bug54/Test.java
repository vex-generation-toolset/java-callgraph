// Issue 54
// Simple test for a custom varargs function

class Util {
    static String bar(String delimiter, String... parts) {
        return String.join(delimiter, parts);
    }
}

public class Demo {
    public static void foo() {
        String result = Util.bar("-", "alpha", "beta", "gamma");
        System.out.println(result);
    }
}

/*$$$$$ Demo.foo(),
  1,
  Util.bar(String@@@String[]...)
*/

/*$$$$$ Util.bar(String@@@String[]...),
  1,
  java.lang.String.join(String@@@String[])
*/
