// Issue 54
// Test for library instance method edges for String receivers

class Name {
    public static void printUpperVar() {
        String s = "hello";
        String upper = s.toUpperCase();
    }
}

public class Demo {
    public static void fooVar() {
        Name.printUpperVar();
    }
}

/*$$$$$ Demo.fooVar(), 1,
  		13, 9, 1, Name.printUpperVar()
*/

/*$$$$$ Name.printUpperVar(), 1,
  		7, 24, 1, java.lang.String#toUpperCase()
*/
