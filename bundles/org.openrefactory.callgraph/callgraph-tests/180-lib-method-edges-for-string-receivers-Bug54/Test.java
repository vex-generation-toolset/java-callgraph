// Issue 54
// Test for library instance method edges for String.indexOf on variable receiver

public class Name {
    public static void printIndexOfVar() {
        String s = "hello";
        int pos = s.indexOf("e");
    }
}

/*$$$$$ Name.printIndexOfVar(), 1,
  		7, 19, 1, java.lang.String#indexOf(String)
*/
