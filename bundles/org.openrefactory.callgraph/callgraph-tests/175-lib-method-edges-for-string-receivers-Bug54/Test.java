// Issue 54
// Test for library instance method edges for String receivers

class Name {
    public static void printUpper() {
        String upper = "hello".toUpperCase();
    }
}

public class Demo {
    public static void foo() {
        Name.printUpper();
    }
}

/*$$$$$ Demo.foo(),
  1,
  Name.printUpper()
*/

/*$$$$$ Name.printUpper(),
  1,
  java.lang.String#toUpperCase()
*/
