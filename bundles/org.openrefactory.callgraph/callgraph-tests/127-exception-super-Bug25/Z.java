// Issue 25
// Subclass of a Library Exception Class

public class MyException extends Exception {
    @Override
    public String getMessage() {
        return "Custom prefix: " + super.getMessage();  // calls Throwable.getMessage()
    }

    public MyException(String msg) {
        super(msg);
    }

    public static void main(String[] args) {
        try {
            throw new MyException("failure");
        } catch (MyException e) {
            System.out.println(e.getMessage());
        }
    }
}

/*$$$$$
  MyException#getMessage(), 1,
  java.lang.Exception#getMessage(),
*/


/*$$$$$
  MyException#MyException(String), 2,
  java.lang.Exception#Exception(String),
  MyException.<init>(),
*/

/*$$$$$
MyException.main(String[]), 2,
MyException#MyException(String),
MyException#getMessage(),
*/
