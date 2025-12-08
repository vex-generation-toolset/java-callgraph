// Issue 54
// Test for library instance method edges for String receivers

class Name {
    public static void printUpperProperty() {
        String upper = System.getProperty("user.home");
    }
}

public class Demo {
    public static void fooProperty() {
        Name.printUpperProperty();
    }
}

/*$$$$$ Demo.fooProperty(), 1,
  		12, 9, 1, Name.printUpperProperty()
*/

/*$$$$$ Name.printUpperProperty(), 1,
  		6, 24, 1, java.lang.System.getProperty(String)
*/
