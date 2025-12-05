// Issue 54
// Test for library instance method edges for String.substring on literal receiver

public class Name {
    public static void printSubstringLiteral() {
        String sub = "hello".substring(1);
    }
}

/*$$$$$ Name.printSubstringLiteral(),
  1,
  java.lang.String#substring(int)
*/
