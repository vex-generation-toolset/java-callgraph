// Issue 54
// Test for library instance method edges for String receivers

class Name {
    public static void printUpperConcat() {
        String upper = ("he" + "llo").toUpperCase();
    }
}

public class Demo {
    public static void fooConcat() {
        Name.printUpperConcat();
    }
}

/*$$$$$ Demo.fooConcat(),
  1,
  Name.printUpperConcat()
*/

/*$$$$$ Name.printUpperConcat(),
  1,
  java.lang.String#toUpperCase()
*/
