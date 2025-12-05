// Issue 54
// Test for library instance method edges inside a nested class

class OuterName {
    static class Inner {
        void sanitize() {
            String cleaned = ("hello" + " world").replace(" ", "-");
        }
    }
}

public class Demo {
    public static void fooNested() {
        new OuterName.Inner().sanitize();
    }
}

/*$$$$$ Demo.fooNested(),
  2,
  OuterName.Inner.<init>(),
  OuterName.Inner#sanitize()
*/

/*$$$$$ OuterName.Inner#sanitize(),
  1,
  java.lang.String#replace(String@@@String)
*/
