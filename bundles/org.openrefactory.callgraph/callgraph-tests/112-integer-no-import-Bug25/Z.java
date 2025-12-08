// Issue 25
// No import needed for integers

public class Example9 {
    void demo() {
        Integer x = new Integer(10);   // old style (deprecated)
        Integer y = Integer.valueOf(10);  // preferred factory method
    }
}

// We do not need import for things such as integer.

/*$$$$$ Example9#demo(), 2,
  		6,21, 1, java.lang.Integer#Integer(int),
  		7,21, 1, java.lang.Integer.valueOf(int),
*/
